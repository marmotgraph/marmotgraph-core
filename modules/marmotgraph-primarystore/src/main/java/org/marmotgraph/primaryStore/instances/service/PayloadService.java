/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2025 ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.primaryStore.instances.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.marmotgraph.auth.api.Permissions;
import org.marmotgraph.auth.models.UserWithRoles;
import org.marmotgraph.auth.api.AuthContext;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.constants.EBRAINSVocabulary;
import org.marmotgraph.commons.exceptions.ForbiddenException;
import org.marmotgraph.commons.exceptions.InstanceNotFoundException;
import org.marmotgraph.commons.jsonld.*;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.model.auth.Functionality;
import org.marmotgraph.commons.model.query.QuerySpecification;
import org.marmotgraph.commons.model.relations.IncomingRelation;
import org.marmotgraph.commons.model.relations.OutgoingRelation;
import org.marmotgraph.commons.services.JsonAdapter;
import org.marmotgraph.commons.utils.TypeUtils;
import org.marmotgraph.primaryStore.instances.model.*;
import org.marmotgraph.primaryStore.instances.repositories.*;
import org.marmotgraph.primaryStore.structures.service.TypesService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class PayloadService {

    private final AuthContext authContext;
    private final JsonAdapter jsonAdapter;
    private final NativePayloadRepository nativeRepository;
    private final InferredPayloadRepository inferredPayloadRepository;
    private final ReleasedPayloadRepository releasedPayloadRepository;
    private final EntityManager entityManager;
    private final InstanceInformationRepository instanceInformationRepository;
    private final InferredDocumentRelationRepository inferredDocumentRelationRepository;
    private final ReleasedDocumentRelationRepository releasedDocumentRelationRepository;
    private final CURIEPrefixRepository curiePrefixRepository;
    private final SpaceRepository spaceRepository;
    private final Permissions permissions;
    private final TypeSpecificationRepository typeSpecificationRepository;
    private final PropertySpecificationRepository propertySpecificationRepository;
    private final TypesService typesService;

    public Optional<SpaceName> getSpace(UUID instanceID) {
        return instanceInformationRepository.findById(instanceID).map(i -> SpaceName.fromString(i.getSpaceName()));
    }


    private record InstanceResolutionResult(UUID uuid, String spaceName, String alternative) {
    }

    private String resolveSpaceName(String spaceName, SpaceName privateSpace) {
        //TODO handle invitation
        if (spaceName.equals(privateSpace.getName())) {
            return SpaceName.PRIVATE_SPACE;
        }
        return spaceName;
    }

    private <T> TypedQuery<T> populateInstanceByTypeQuery(Class<T> targetClass, Class<? extends Payload<?>> clazz, String space, Set<String> typeNames, String search, Function<Tuple<CriteriaBuilder, Root<? extends Payload>>, Selection<? extends T>> selector, boolean fetchRelations) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(targetClass);
        Root<? extends Payload<?>> root = cq.from(clazz);
        Join<? extends Payload<?>, ?> typesJoin = root.join("types");
        Join<? extends Payload<?>, ?> infoJoin = root.join("instanceInformation");
        if (fetchRelations) {
            root.fetch("documentRelations", JoinType.LEFT);
            cq.orderBy(cb.asc(root.get("label")), cb.asc(root.get("uuid")));
        }
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(typesJoin.in(typeNames));
        if (targetClass != Long.class) {
            root.fetch("instanceInformation", JoinType.LEFT);
        }
        if (space != null) {
            predicates.add(cb.equal(infoJoin.get("spaceName"), space));
        }
        cq = cq.select(selector.apply(new Tuple<>(cb, root)));
        if (StringUtils.isNotBlank(search)) {
            String searchString = "%" + search.toLowerCase() + "%";
            predicates.add(cb.like(root.get("searchable"), searchString));
        }
        cq.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(cq);
    }


    public Paginated<NormalizedJsonLd> getInstancesByTypes(DataStage stage, Set<String> typeNames, String space, String search, String filterProperty, String filterValue, boolean returnPayload, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam) {
        //FIXME take into account all the other attributes
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        Class<? extends Payload<?>> clazz = stage == DataStage.IN_PROGRESS ? Payload.InferredPayload.class : Payload.ReleasedPayload.class;
        boolean countTotal = paginationParam != null && paginationParam.isReturnTotalResults();
        List<NormalizedJsonLd> results = null;
        Long totalCount = null;
        if (countTotal) {
            TypedQuery<Long> countQuery = populateInstanceByTypeQuery(Long.class, clazz, space, typeNames, search, s -> s.getA().countDistinct(s.getB()), false);
            totalCount = countQuery.getSingleResult();
            if (totalCount == 0) {
                results = Collections.emptyList();
            }
        }
        if (results == null) {
            TypedQuery<? extends Payload> payloadQuery = populateInstanceByTypeQuery(Payload.class, clazz, space, typeNames, search, Tuple::getB, true);
            // Pagination
            if (paginationParam != null && paginationParam.getSize() != null) {
                payloadQuery.setMaxResults(paginationParam.getSize().intValue());
                payloadQuery.setFirstResult((int) paginationParam.getFrom());
            }
            List<? extends Payload> resultList = payloadQuery.getResultList();
            results = resultList.stream().map(i -> {
                NormalizedJsonLd document;
                if (returnPayload) {
                    document = jsonAdapter.fromJson(i.getJsonPayload(), NormalizedJsonLd.class);
                    document.resolveOutgoingRelations(((Payload<?>) i).getDocumentRelations().stream().filter(f -> f.getResolvedTarget() != null).map(f -> new OutgoingRelation(f.getCompositeId().getTargetReference(), f.getResolvedTarget())).collect(Collectors.toSet()));
                    if (returnAlternatives && i instanceof Payload.InferredPayload) {
                        document.put(EBRAINSVocabulary.META_ALTERNATIVE, jsonAdapter.fromJson(((Payload.InferredPayload) i).getAlternative(), DynamicJson.class));
                    }
                    if (!returnEmbedded) {
                        document.removeEmbedded();
                    }
                    document.removeAllInternalProperties();
                } else {
                    document = new NormalizedJsonLd();
                    document.setId(i.getUuid().toString());
                }
                document.put(EBRAINSVocabulary.META_SPACE, resolveSpaceName(i.getInstanceInformation().getSpaceName(), userWithRoles.getPrivateSpace()));
                return document;
            }).toList();
        }
        return new Paginated<>(results, totalCount, results.size(), paginationParam != null ? (int) paginationParam.getFrom() : 0);
    }


    public Map<UUID, InstanceId> resolveIds(List<IdWithAlternatives> idWithAlternatives, DataStage stage) {
        Map<UUID, UUID> directUUIDMap = new HashMap<>();
        Set<UUID> flatUUIDList = new HashSet<>();
        idWithAlternatives.forEach(i -> {
            Optional<UUID> directUUID = i.getAlternatives().stream().map(a -> {
                try {
                    return UUID.fromString(a);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }).filter(Objects::nonNull).findFirst();
            if(directUUID.isPresent()) {
                directUUIDMap.put(directUUID.get(), i.getId());
                flatUUIDList.add(directUUID.get());
            }
            else{
                flatUUIDList.add(i.getId());
            }
        });
        List<InstanceInformation> allById = instanceInformationRepository.findAllById(flatUUIDList);
        Map<UUID, InstanceId> resultMap = new HashMap<>();
        Stream<InstanceInformation> allByIdStream = allById.stream();
        List<ReleaseStatus> acceptedStatusForReleasedStage = Arrays.asList(ReleaseStatus.RELEASED, ReleaseStatus.HAS_CHANGED);
        if (stage == DataStage.RELEASED) {
            allByIdStream = allByIdStream.filter(i -> acceptedStatusForReleasedStage.contains(i.getReleaseStatus()));
        }
        allByIdStream.forEach(i -> {
            if(directUUIDMap.containsKey(i.getUuid())){
                resultMap.put(directUUIDMap.get(i.getUuid()), new InstanceId(i.getUuid(), SpaceName.fromString(i.getSpaceName())));
            }
            else{
                resultMap.put(i.getUuid(), new InstanceId(i.getUuid(), SpaceName.fromString(i.getSpaceName())));
            }
        });
        Set<String> flatListOfAlternatives = idWithAlternatives.stream().filter(i -> !resultMap.containsKey(i.getId())).map(IdWithAlternatives::getAlternatives).flatMap(Collection::stream).collect(Collectors.toSet());
        if(!flatListOfAlternatives.isEmpty()) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<InstanceResolutionResult> criteriaQuery = criteriaBuilder.createQuery(InstanceResolutionResult.class);
            Root<InstanceInformation> root = criteriaQuery.from(InstanceInformation.class);
            Join<InstanceInformation, String> alternativeIds = root.join("alternativeIds");
            criteriaQuery.select(criteriaBuilder.construct(InstanceResolutionResult.class, root.get("uuid"), root.get("spaceName"), alternativeIds));
            List<Predicate> predicates = new ArrayList<>();
            if (stage == DataStage.RELEASED) {
                // If we are looking for released information only, we should exclude those resolved ids which are unreleased.
                predicates.add(root.get("releaseStatus").in(Arrays.asList(ReleaseStatus.RELEASED, ReleaseStatus.HAS_CHANGED)));
            }
            predicates.add(alternativeIds.in(flatListOfAlternatives));
            criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
            Stream<InstanceResolutionResult> result = entityManager.createQuery(criteriaQuery).getResultList().stream();
            if (result != null) {
                Map<String, InstanceId> instanceIdsByAlternative = result.collect(Collectors.toMap(InstanceResolutionResult::alternative, v -> new InstanceId(v.uuid(), SpaceName.fromString(v.spaceName()))));
                idWithAlternatives.forEach(id -> {
                    Optional<String> alternative = id.getAlternatives().stream().filter(instanceIdsByAlternative::containsKey).findFirst();
                    alternative.ifPresent(s -> resultMap.put(id.getId(), instanceIdsByAlternative.get(s)));
                });
            }
        }
        return resultMap;
    }


    public InstanceInformation getOrCreateGlobalInstanceInformation(UUID instanceId) {
        return instanceInformationRepository.findById(instanceId).orElseGet(() -> {
            InstanceInformation i = new InstanceInformation();
            i.setUuid(instanceId);
            return i;
        });
    }


    public InstanceInformation upsertInstanceInformation(UUID instanceId, SpaceName spaceName, Set<String> alternativeIds) {
        InstanceInformation instanceInformation = getOrCreateGlobalInstanceInformation(instanceId);
        instanceInformation.setSpaceName(spaceName.getName());
        instanceInformation.setAlternativeIds(alternativeIds);
        instanceInformationRepository.save(instanceInformation);
        return instanceInformation;
    }


    private CURIEPrefix generateNewCURIEPRefix(String n) {
        String[] split = n.replace("https://", "").replace("http://", "").split("/");
        String domain = split[0];
        String[] domainSplit = domain.split("\\.");
        String prefix = getPrefix(domainSplit, split);
        Long countByPrefix = curiePrefixRepository.countByPrefixPattern(prefix);
        CURIEPrefix newPrefix = new CURIEPrefix();
        newPrefix.setNamespace(n);
        if (countByPrefix == 0L) {
            newPrefix.setPrefix(prefix);
        } else {
            newPrefix.setPrefix(prefix + countByPrefix);
        }
        curiePrefixRepository.save(newPrefix);
        return newPrefix;
    }

    private static String getPrefix(String[] domainSplit, String[] split) {
        String firstLevelDomain = domainSplit[domainSplit.length - 1];
        String secondLevelDomain = domainSplit.length > 1 ? domainSplit[domainSplit.length - 2] : null;
        StringBuilder prefix = new StringBuilder(secondLevelDomain == null ? "" : secondLevelDomain.substring(0, Math.min(secondLevelDomain.length(), 2)) + "_" + firstLevelDomain.charAt(0)); //This is the minimal pattern
        if (domainSplit.length > 2) {
            prefix.append("_");
            //If there are further subdomains, we append them with one character each
            for (int i = domainSplit.length - 2; i > 0; i--) {
                prefix.append(domainSplit[i].charAt(0));
            }
        }
        if (split.length > 1) {
            prefix.append("_");
            //Same goes for subpaths
            for (int i = 1; i < split.length; i++) {
                prefix.append(split[i].charAt(0));
            }
        }
        prefix.append("_");
        return prefix.toString();
    }

    public Set<IncomingRelation> applyCURIEPrefixes(Set<IncomingRelation> incomingRelations) {
        Set<String> namespaces = incomingRelations.stream().map(r -> NormalizedJsonLd.getNamespace(r.property())).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, String> prefixMap = getPrefixMap(namespaces);
        return incomingRelations.stream().map(r -> {
            String oldNamespace = NormalizedJsonLd.getNamespace(r.property());
            if (oldNamespace != null && prefixMap.get(oldNamespace) != null) {
                String newProperty = r.property().replace(oldNamespace + "/", prefixMap.get(oldNamespace));
                return new IncomingRelation(r.lifecycleId(), newProperty, r.orderNumber(), r.from(), r.to());
            }
            return r;
        }).collect(Collectors.toSet());
    }

    private record NeighborInformation(UUID uuid, String spaceName, String type, UUID inboundId, UUID outboundId){}

    @Transactional
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage){
        Optional<? extends Payload<?>> payload = stage == DataStage.IN_PROGRESS ? inferredPayloadRepository.findById(id) : releasedPayloadRepository.findById(id);
        if(payload.isPresent()){
            GraphEntity entity = new GraphEntity();
            entity.setId(id.toString());
            Payload<?> p = payload.get();
            entity.setName(p.getLabel());
            entity.setTypes(new ArrayList<>(p.getTypes()));
            entity.setSpace(p.getInstanceInformation().getSpaceName());
            if(!CollectionUtils.isEmpty(p.getIncomingRelations())) {
                entity.setInbound(p.getIncomingRelations().stream().map(i -> {
                    Payload<?> incomingPayload = i.getPayload();
                    GraphEntity incoming = new GraphEntity();
                    incoming.setId(incomingPayload.getUuid().toString());
                    incoming.setName(incomingPayload.getLabel());
                    incoming.setTypes(new ArrayList<>(incomingPayload.getTypes()));
                    incoming.setSpace(incomingPayload.getInstanceInformation().getSpaceName());
                    return incoming;
                }).toList());
            }
            if(!CollectionUtils.isEmpty(p.getDocumentRelations())) {
                entity.setOutbound(p.getDocumentRelations().stream().map(o -> {
                    Payload<?> targetPayload = o.getTargetPayload();
                    if (targetPayload != null) {
                        GraphEntity outbound = new GraphEntity();
                        outbound.setId(targetPayload.getUuid().toString());
                        outbound.setName(targetPayload.getLabel());
                        outbound.setTypes(new ArrayList<>(targetPayload.getTypes()));
                        outbound.setSpace(targetPayload.getInstanceInformation().getSpaceName());
                        if (CollectionUtils.isEmpty(targetPayload.getDocumentRelations())) {
                            outbound.setOutbound(targetPayload.getDocumentRelations().stream().map(o2 -> {
                                Payload<?> targetPayload2 = o2.getTargetPayload();
                                GraphEntity outbound2 = new GraphEntity();
                                outbound2.setId(targetPayload2.getUuid().toString());
                                outbound2.setName(targetPayload2.getLabel());
                                outbound2.setTypes(new ArrayList<>(targetPayload2.getTypes()));
                                outbound2.setSpace(targetPayload2.getInstanceInformation().getSpaceName());
                                return outbound2;
                            }).toList());
                        }
                        return outbound;
                    }
                    return null;
                }).filter(Objects::nonNull).toList());
            }
            return entity;
        }
        return null;


//        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
//        Class<? extends Payload<?>> clazz = stage == DataStage.IN_PROGRESS ? Payload.InferredPayload.class : Payload.ReleasedPayload.class;
//        CriteriaQuery<NeighborInformation> query = criteriaBuilder.createQuery(NeighborInformation.class);
//        Root<? extends Payload<?>> root = query.from(clazz);
//        Join<Object, Object> instanceInformation = root.join("instanceInformation", JoinType.LEFT);
//        Join<Object, Object> types = root.join("types", JoinType.LEFT);
//        Join<Object, Object> documentRelations = root.join("documentRelations", JoinType.LEFT);
//        Join<Object, Object> incomingRelations = root.join("incomingRelations", JoinType.LEFT);
//        query.select(criteriaBuilder.construct(NeighborInformation.class, root.get("uuid"), instanceInformation.get("spaceName"), types, documentRelations.get("resolvedTarget"), incomingRelations.get("compositeId").get("uuid")));
//        query.where(criteriaBuilder.equal(root.get("uuid"), id));
//        List<NeighborInformation> resultList = entityManager.createQuery(query).getResultList();
//        GraphEntity result = new GraphEntity();
//        Map<String, List<NeighborInformation>> byType = resultList.stream().collect(Collectors.groupingBy(r -> r.type));
//        String type = byType.keySet().stream().findFirst().get();
//
//
//
//        return null;
    }

    public ScopeElement getScopeForInstance(String space, UUID id, DataStage stage, boolean applyRestrictions) {
        Optional<? extends Payload<?>> instance = stage == DataStage.IN_PROGRESS ? inferredPayloadRepository.findById(id) : releasedPayloadRepository.findById(id);
        if (instance.isPresent()) {
            Payload<?> payload = instance.get();
            NormalizedJsonLd normalizedJsonLd = jsonAdapter.fromJson(payload.getJsonPayload(), NormalizedJsonLd.class);
            //TODO populate scope
            return new ScopeElement(id, normalizedJsonLd.types(), Collections.emptyList(), id.toString(), payload.getInstanceInformation().getSpaceName(), payload.getLabel());
        }
        return null;
    }

    public void applyCURIEPrefixes(QuerySpecification querySpecification) {
        Set<String> namespaces = querySpecification.extractPropertyAndTypeNamespaces();
        querySpecification.applyPrefixMap(getPrefixMap(namespaces));
    }

    public void applyCURIEPrefixes(NormalizedJsonLd normalizedJsonLd) {
        Set<String> namespaces = normalizedJsonLd.extractPropertyAndTypeNamespaces();
        normalizedJsonLd.applyPrefixMap(getPrefixMap(namespaces));
    }

    private Map<String, String> getPrefixMap(Set<String> namespaces) {
        List<CURIEPrefix> all = curiePrefixRepository.findAll();
        List<CURIEPrefix> allById = all.stream().filter(p -> namespaces.contains(p.getNamespace())).toList();
        //List<CURIEPrefix> allById = curiePrefixRepository.findAllById(namespaces);
        Stream<CURIEPrefix> prefixStream;
        if (namespaces.size() != allById.size()) {
            Set<String> foundNamespaces = allById.stream().map(CURIEPrefix::getNamespace).collect(Collectors.toSet());
            Set<CURIEPrefix> newPrefixes = namespaces.stream().filter(n -> !foundNamespaces.contains(n)).map(this::generateNewCURIEPRefix).collect(Collectors.toSet());
            prefixStream = Stream.concat(allById.stream(), newPrefixes.stream());
        } else {
            prefixStream = allById.stream();
        }
        return prefixStream.collect(Collectors.toMap(CURIEPrefix::getNamespace, CURIEPrefix::getPrefix));
    }


    @Transactional
    public Tuple<Set<IncomingRelation>, Set<OutgoingRelation>> upsertInferredPayload(UUID instanceId, InstanceInformation instanceInformation, NormalizedJsonLd payload, Set<Tuple<String, String>> relations, JsonLdDoc alternatives, boolean autoRelease, Long reportedTimeStampInMs, boolean createSpace) {
        boolean query = payload.types().contains(EBRAINSVocabulary.META_QUERY_TYPE);
        String newPayload = jsonAdapter.toJson(payload);
        if (createSpace) {
            Space s = new Space();
            s.setName(instanceInformation.getSpaceName());
            spaceRepository.save(s);
        }
        if (!autoRelease) {
            Optional<Payload.ReleasedPayload> releasedInstance = releasedPayloadRepository.findById(instanceId);
            if (releasedInstance.isPresent()) {
                Payload.ReleasedPayload existingReleasedInstance = releasedInstance.get();
                if (existingReleasedInstance.getJsonPayload().equals(newPayload)) {
                    instanceInformation.setReleaseStatus(ReleaseStatus.RELEASED);
                } else {
                    instanceInformation.setReleaseStatus(ReleaseStatus.HAS_CHANGED);
                }
            }
        }
        String firstType = payload.types().getFirst();
        Optional<TypeSpecification> globalTypeSpec = typeSpecificationRepository.findById(firstType);
        String labelProperty = null;
        List<String> searchableProperties = Collections.emptyList();
        if (globalTypeSpec.isPresent()) {
            NormalizedJsonLd normalizedJsonLd = jsonAdapter.fromJson(globalTypeSpec.get().getPayload(), NormalizedJsonLd.class);
            labelProperty = normalizedJsonLd.getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class);
            searchableProperties = normalizedJsonLd.getAsListOf(EBRAINSVocabulary.META_PROPERTY_SEARCHABLE, String.class);
        }

        Payload.InferredPayload inferredPayload = new Payload.InferredPayload();
        inferredPayload.setUuid(instanceId);
        inferredPayload.setAlternative(jsonAdapter.toJson(alternatives));
        inferredPayload.setJsonPayload(newPayload);
        inferredPayload.setTypes(payload.types());
        List<EmbeddedTypeInformation.InferredEmbeddedTypeInformation> embeddedTypeInformation = new ArrayList<>();
        payload.walkMaps((key, map, parentMap) -> {
            if (map != payload && map.containsKey(JsonLdConsts.TYPE) && map.containsKey("_id")) {
                String parentType = TypeUtils.concatenate(parentMap.get(JsonLdConsts.TYPE));
                String embeddedType = TypeUtils.concatenate(map.get(JsonLdConsts.TYPE));
                EmbeddedTypeInformation.InferredEmbeddedTypeInformation embeddedInfo = new EmbeddedTypeInformation.InferredEmbeddedTypeInformation();
                embeddedInfo.setCompositeId(new EmbeddedTypeInformation.CompositeId(instanceId, (String) parentType, key, embeddedType));
                if(!embeddedTypeInformation.contains(embeddedInfo)) {
                    embeddedTypeInformation.add(embeddedInfo);
                }
            }
        });
        inferredPayload.setEmbeddedTypeInformation(embeddedTypeInformation);
        StringBuilder searchString = new StringBuilder();
        if (labelProperty != null) {
            inferredPayload.setLabel(payload.getAs(labelProperty, String.class));
            searchString.append(payload.getAs(labelProperty, String.class).toLowerCase());
        }
        if (!CollectionUtils.isEmpty(searchableProperties)) {
            String concatenatedSearchableEntries = searchableProperties.stream().map(s -> payload.getAs(s, String.class)).filter(Objects::nonNull).collect(Collectors.joining(" ")).toLowerCase();
            if (!searchString.isEmpty()) {
                searchString.append(" ");
            }
            searchString.append(concatenatedSearchableEntries);
        }
        if (!searchString.isEmpty()) {
            inferredPayload.setSearchable(searchString.toString());
        }
        if (query) {
            Query q = new Query();
            q.setUuid(instanceId);
            inferredPayload.setQuery(q);
        }
        payload.types().forEach(t -> {
            if (!typeSpecificationRepository.existsById(t)) {
                TypeSpecification typeSpecification = new TypeSpecification();
                typeSpecification.setType(t);
                typeSpecificationRepository.save(typeSpecification);
            }
        });
        handleStructure(instanceId, payload, inferredPayload, TypeStructure.InferredTypeStructure.class);
        inferredPayloadRepository.save(inferredPayload);
        Set<IncomingRelation> incomingRelations = query ? Collections.emptySet() : handleIncomingDocumentRelations(instanceId, instanceInformation.getAlternativeIds(), inferredDocumentRelationRepository, DataStage.IN_PROGRESS);
        Set<OutgoingRelation> outgoingRelations = query ? Collections.emptySet() : handleOutgoingDocumentRelations(instanceId, relations, DocumentRelation.InferredDocumentRelation.class, inferredDocumentRelationRepository, DataStage.IN_PROGRESS);
        return new Tuple<>(incomingRelations, outgoingRelations);
    }

    private <T extends TypeStructure> void handleStructure(UUID instanceId, NormalizedJsonLd jsonld, Payload<T> payload, Class<T> typeStructureClass) {
        Map<TypeStructure.CompositeId, T> typeStructures = new HashMap<>();
        jsonld.walk((key, value, parentMap) -> {
            if (!key.equals(JsonLdConsts.ID) && !key.equals(JsonLdConsts.TYPE) && !key.equals("_id")) {
                Object embeddedId = parentMap.get("_id");
                Object type = parentMap.get(JsonLdConsts.TYPE);
                String typeString = null;
                if (type instanceof Collection<?>) {
                    typeString = StringUtils.join((Collection<?>) type, ", ");
                } else if (type != null) {
                    typeString = type.toString();
                }

                String embeddedIdString = null;
                if (embeddedId instanceof String) {
                    embeddedIdString = embeddedId.toString();
                }
                try {
                    TypeStructure.CompositeId typeStructureId = new TypeStructure.CompositeId(instanceId, typeString, embeddedIdString);
                    T typeStructure = typeStructures.get(typeStructureId);
                    if (typeStructure == null) {
                        typeStructure = typeStructureClass.getConstructor().newInstance();
                        typeStructure.setCompositeId(typeStructureId);
                        typeStructure.setProperties(new ArrayList<>());
                        typeStructures.put(typeStructureId, typeStructure);
                    }
                    typeStructure.getProperties().add(key);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        payload.setTypeStructures(new ArrayList<>(typeStructures.values()));
    }


    private record IncomingRelationInformation(String targetReference, UUID origin, UUID resolvedTarget,
                                               String sourcePayload, String propertyName) {

        public Set<IncomingRelation> getRelationInformation(JsonAdapter jsonAdapter) {
            if (resolvedTarget != null) {
                NormalizedJsonLd normalizedJsonLd = jsonAdapter.fromJson(sourcePayload, NormalizedJsonLd.class);
                return normalizedJsonLd.fetchInformationForIncomingRelation(origin, resolvedTarget, targetReference());
            }
            return Collections.emptySet();
        }

    }

    private <T extends DocumentRelation> Set<IncomingRelation> handleIncomingDocumentRelations(UUID instanceId, Set<String> alternativeIds, JpaRepository<T, DocumentRelation.CompositeId> repository, DataStage stage) {
        //return Collections.emptySet();
        //TODO we could exclude those relations which are already resolved (to the correct UUID) in the DB query
        if (stage == DataStage.NATIVE) {
            throw new IllegalArgumentException("Can not handle document relations in native space");
        }
        Class<? extends DocumentRelation> documentRelation = stage == DataStage.RELEASED ? DocumentRelation.ReleasedDocumentRelation.class : DocumentRelation.InferredDocumentRelation.class;
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IncomingRelationInformation> criteriaQuery = criteriaBuilder.createQuery(IncomingRelationInformation.class);
        Root<? extends DocumentRelation> root = criteriaQuery.from(documentRelation);
        Join<Object, Object> payloadJoin = root.join("payload", JoinType.LEFT);
        criteriaQuery.select(criteriaBuilder.construct(IncomingRelationInformation.class, root.get("compositeId").get("targetReference"), root.get("compositeId").get("uuid"), root.get("resolvedTarget"), payloadJoin.get("jsonPayload"), root.get("compositeId").get("propertyName")));
        criteriaQuery.where(root.get("compositeId").get("targetReference").in(alternativeIds));
        List<IncomingRelationInformation> incomingRelations = entityManager.createQuery(criteriaQuery).getResultList();
        return incomingRelations.stream().map(i -> {
            if (i.resolvedTarget() == null || !i.resolvedTarget().equals(instanceId)) {
                try {
                    T upsertRelation = (T) documentRelation.getConstructor().newInstance();
                    upsertRelation.setCompositeId(new DocumentRelation.CompositeId(i.origin(), i.targetReference(), i.propertyName()));
                    upsertRelation.setResolvedTarget(instanceId);
                    repository.save(upsertRelation);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                return new IncomingRelationInformation(i.targetReference(), i.origin(), instanceId, i.sourcePayload(), i.propertyName());
            }
            return i;
        }).map(i -> i.getRelationInformation(jsonAdapter)).flatMap(Collection::stream).collect(Collectors.toSet());
    }


    private <T extends DocumentRelation> Set<OutgoingRelation> handleOutgoingDocumentRelations(UUID instanceId, Set<Tuple<String, String>> newRelations, Class<T> clazz, JpaRepository<T, DocumentRelation.CompositeId> repository, DataStage stage) {
        //Analyze
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
        Root<T> root = criteriaQuery.from(clazz);
        criteriaQuery.where(criteriaBuilder.equal(root.get("compositeId").get("uuid"), instanceId));
        List<T> existingDocumentRelations = entityManager.createQuery(criteriaQuery).getResultList();
        Map<Boolean, List<T>> existingRelationsByToKeepStatus = existingDocumentRelations.stream().collect(Collectors.partitioningBy(e -> newRelations.contains(e.getCompositeId().getTargetReference())));
        List<T> toBeRemoved = existingRelationsByToKeepStatus.get(false);
        List<T> toBeKept = existingRelationsByToKeepStatus.get(true);
        Set<String> toBeKeptTargetReference = toBeKept.stream().map(e -> e.getCompositeId().getTargetReference()).collect(Collectors.toSet());
        Map<Boolean, List<T>> toBeKeptByResolutionState = toBeKept.stream().collect(Collectors.partitioningBy(e -> e.getResolvedTarget() != null));
        List<T> toBeKeptUnresolved = toBeKeptByResolutionState.get(false);
        List<T> toBeKeptResolved = toBeKeptByResolutionState.get(true);
        Set<Tuple<String, String>> toBeAdded = newRelations.stream().filter(n -> !toBeKeptTargetReference.contains(n)).collect(Collectors.toSet());

        //Remove
        toBeRemoved.forEach(r -> repository.deleteById(r.getCompositeId()));

        //Update unresolved
        if (!toBeKeptUnresolved.isEmpty()) {
            Map<UUID, InstanceId> resolvedExisting = resolveIds(toBeKeptUnresolved.stream().map(i -> new IdWithAlternatives(UUID.nameUUIDFromBytes(i.getCompositeId().getTargetReference().getBytes(StandardCharsets.UTF_8)), null, Collections.singleton(i.getCompositeId().getTargetReference()))).toList(), stage);
            if (!resolvedExisting.isEmpty()) {
                toBeKeptUnresolved.forEach(c -> {
                    UUID resolutionKey = UUID.nameUUIDFromBytes(c.getCompositeId().getTargetReference().getBytes(StandardCharsets.UTF_8));
                    if (resolvedExisting.containsKey(resolutionKey)) {
                        c.setResolvedTarget(resolvedExisting.get(resolutionKey).getUuid());
                        repository.save(c);
                    }
                });
            }
        }

        //Add new
        Map<UUID, InstanceId> resolvedLinks = resolveIds(toBeAdded.stream().map(i -> new IdWithAlternatives(UUID.nameUUIDFromBytes(i.getB().getBytes(StandardCharsets.UTF_8)), null, Collections.singleton(i.getB()))).toList(), stage);
        Set<T> added = toBeAdded.stream().map(r -> {
            try {
                T documentRelation = clazz.getConstructor().newInstance();
                UUID resolutionKey = UUID.nameUUIDFromBytes(r.getB().getBytes(StandardCharsets.UTF_8));
                if (resolvedLinks.containsKey(resolutionKey)) {
                    documentRelation.setResolvedTarget(resolvedLinks.get(resolutionKey).getUuid());
                }
                documentRelation.setCompositeId(new DocumentRelation.CompositeId(instanceId, r.getB(), r.getA()));
                repository.save(documentRelation);
                return documentRelation;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
        return Stream.concat(Stream.concat(toBeKeptUnresolved.stream(), toBeKeptResolved.stream()), added.stream()).map(i -> new OutgoingRelation(i.getCompositeId().getTargetReference(), i.getResolvedTarget())).collect(Collectors.toSet());
    }


    @Transactional
    public Tuple<Set<IncomingRelation>, Set<OutgoingRelation>> release(UUID instanceId, NormalizedJsonLd jsonPayload, Set<Tuple<String, String>> outgoingRelations, Long reportedTimestamp, InstanceInformation instanceInformation, List<String> types) {
        if (instanceInformation.getFirstRelease() == null) {
            instanceInformation.setFirstRelease(reportedTimestamp);
        }
        instanceInformation.setLastRelease(reportedTimestamp);
        instanceInformation.setReleaseStatus(ReleaseStatus.RELEASED);
        Payload.ReleasedPayload releasedPayload = new Payload.ReleasedPayload();
        releasedPayload.setUuid(instanceId);
        releasedPayload.setJsonPayload(jsonAdapter.toJson(jsonPayload));
        releasedPayload.setTypes(types);
        instanceInformationRepository.save(instanceInformation);
        handleStructure(instanceId, jsonPayload, releasedPayload, TypeStructure.ReleasedTypeStructure.class);
        releasedPayloadRepository.save(releasedPayload);
        Set<IncomingRelation> incomingRelationsSet = handleIncomingDocumentRelations(instanceId, instanceInformation.getAlternativeIds(), releasedDocumentRelationRepository, DataStage.RELEASED);
        Set<OutgoingRelation> outgoingRelationsSet = handleOutgoingDocumentRelations(instanceId, outgoingRelations, DocumentRelation.ReleasedDocumentRelation.class, releasedDocumentRelationRepository, DataStage.RELEASED);
        return new Tuple<>(incomingRelationsSet, outgoingRelationsSet);
    }


    public ReleaseStatus getReleaseStatus(UUID instanceId) {
        Optional<InstanceInformation> byId = instanceInformationRepository.findById(instanceId);
        if (byId.isPresent()) {
            return byId.get().getReleaseStatus();
        }
        throw new InstanceNotFoundException(String.format("Instance %s not found", instanceId));
    }

    public List<ReleaseStatus> getDistinctReleaseStatus(Set<UUID> instanceIds) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReleaseStatus> criteriaQuery = criteriaBuilder.createQuery(ReleaseStatus.class);
        Root<InstanceInformation> root = criteriaQuery.from(InstanceInformation.class);
        criteriaQuery.select(root.get("releaseStatus"));
        criteriaQuery.where(root.get("uuid").in(instanceIds));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Transactional
    public Map<InstanceId, ReleaseStatus> getReleaseStatus(List<UUID> instanceIds) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReleaseStatusById> criteriaQuery = criteriaBuilder.createQuery(ReleaseStatusById.class);
        Root<InstanceInformation> root = criteriaQuery.from(InstanceInformation.class);
        criteriaQuery.select(criteriaBuilder.construct(ReleaseStatusById.class, root.get("uuid"), root.get("spaceName"), root.get("releaseStatus")));
        criteriaQuery.where(root.get("uuid").in(instanceIds));
        return entityManager.createQuery(criteriaQuery).getResultStream().collect(Collectors.toMap(k -> new InstanceId(k.uuid, SpaceName.fromString(k.spaceName)), v -> v.releaseStatus));
    }

    record ReleaseStatusById(UUID uuid, String spaceName, ReleaseStatus releaseStatus) {
    }


    private <R extends DocumentRelation> void removeRelations(UUID instanceId, Class<R> relationClass) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<R> criteriaDelete = cb.createCriteriaDelete(relationClass);
        Root<R> deleteRoot = criteriaDelete.from(relationClass);
        criteriaDelete.where(cb.equal(deleteRoot.get("compositeId").get("uuid"), instanceId));
        entityManager.createQuery(criteriaDelete).executeUpdate();

        CriteriaUpdate<R> criteriaUpdate = cb.createCriteriaUpdate(relationClass);
        Root<R> updateRoot = criteriaUpdate.from(relationClass);
        criteriaUpdate.set(updateRoot.get("resolvedTarget"), (Object) null).where(cb.equal(updateRoot.get("resolvedTarget"), instanceId));
        entityManager.createQuery(criteriaUpdate).executeUpdate();
    }

    @Transactional
    public void delete(UUID instanceId) {
        Optional<InstanceInformation> byId = instanceInformationRepository.findById(instanceId);
        if (byId.isPresent()) {
            if (byId.get().getReleaseStatus() != ReleaseStatus.UNRELEASED) {
                throw new IllegalStateException(String.format("It's not allowed to delete instance %s because it is still released", instanceId));
            }
        }
        removeRelations(instanceId, DocumentRelation.InferredDocumentRelation.class);
        inferredPayloadRepository.deleteById(instanceId);
        instanceInformationRepository.deleteById(instanceId);
    }


    @Transactional
    public void unrelease(UUID instanceId) {
        InstanceInformation instanceInformation = getOrCreateGlobalInstanceInformation(instanceId);
        instanceInformation.setReleaseStatus(ReleaseStatus.UNRELEASED);
        instanceInformation.setLastRelease(null);
        removeRelations(instanceId, DocumentRelation.ReleasedDocumentRelation.class);
        releasedPayloadRepository.deleteById(instanceId);
        instanceInformationRepository.save(instanceInformation);
    }


    public Tuple<NormalizedJsonLd, Set<IncomingRelation>> prepareToIndex(NormalizedJsonLd payload, Tuple<Set<IncomingRelation>, Set<OutgoingRelation>> incomingAndOutgoingRelations) {
        NormalizedJsonLd preprocessedForIndexing = new NormalizedJsonLd(payload);
        applyCURIEPrefixes(preprocessedForIndexing);
        preprocessedForIndexing.resolveOutgoingRelations(incomingAndOutgoingRelations.getB());
        Set<IncomingRelation> incomingRelations = applyCURIEPrefixes(incomingAndOutgoingRelations.getA());
        return new Tuple<>(preprocessedForIndexing, incomingRelations);
    }

    public NormalizedJsonLd getPayloadToRelease(UUID instanceId) {
        Optional<Payload.InferredPayload> inferredPayload = inferredPayloadRepository.findById(instanceId);
        if (inferredPayload.isPresent()) {
            Payload.InferredPayload existingPayload = inferredPayload.get();
            return jsonAdapter.fromJson(existingPayload.getJsonPayload(), NormalizedJsonLd.class);
        } else {
            throw new InstanceNotFoundException(String.format("Could not release instance %s because it was not found", instanceId));
        }
    }

    public void upsertNativePayloadFromEvent(PersistedEvent event) {
        if (event != null) {
            NativePayload payload = new NativePayload();
            payload.setJsonPayload(jsonAdapter.toJson(event.getData()));
            payload.setMd5hash(DigestUtils.md5DigestAsHex(payload.getJsonPayload().getBytes()));
            payload.setCompositeId(new NativePayload.CompositeId(event.getInstanceId(), event.getUserId()));
            payload.setPropertyUpdates(jsonAdapter.toJson(event.getData().getFieldUpdateTimes()));
            nativeRepository.save(payload);
        }
    }

    public void removeNativePayloadsFromEvent(PersistedEvent event) {
        if (event != null) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaDelete<NativePayload> criteriaDelete = criteriaBuilder.createCriteriaDelete(NativePayload.class);
            Root<NativePayload> deleteRoot = criteriaDelete.from(NativePayload.class);
            criteriaDelete.where(criteriaBuilder.equal(deleteRoot.get("compositeId").get("uuid"), event.getInstanceId()));
            entityManager.createQuery(criteriaDelete).executeUpdate();
        }
    }

    private record SourceDocument(String jsonPayload, String propertyUpdates) {

        private NormalizedJsonLd toNormalizedJsonLd(JsonAdapter jsonAdapter) {
            NormalizedJsonLd normalizedJsonLd = jsonAdapter.fromJson(this.jsonPayload, NormalizedJsonLd.class);
            normalizedJsonLd.setFieldUpdateTimes(jsonAdapter.fromJson(this.propertyUpdates, NormalizedJsonLd.FieldUpdateTimes.class));
            return normalizedJsonLd;
        }
    }

    public Stream<NormalizedJsonLd> getSourceDocumentsForInstanceFromDB(UUID instanceId, String excludeUserId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SourceDocument> query = criteriaBuilder.createQuery(SourceDocument.class);
        Root<NativePayload> root = query.from(NativePayload.class);
        query.select(criteriaBuilder.construct(SourceDocument.class, root.get("jsonPayload"), root.get("propertyUpdates")));
        query.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("compositeId").get("uuid"), instanceId),
                        criteriaBuilder.notEqual(root.get("compositeId").get("userId"), excludeUserId)
                ));
        return entityManager.createQuery(query).getResultList().stream().map(d -> d.toNormalizedJsonLd(jsonAdapter));
    }


    private List<? extends Payload<?>> fetchInstancesByIds(Class<? extends Payload<?>> payloadType, List<UUID> ids) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<? extends Payload<?>> criteriaQuery = criteriaBuilder.createQuery(payloadType);
        Root<? extends Payload<?>> root = criteriaQuery.from(payloadType);
        root.fetch("instanceInformation", JoinType.LEFT);
        root.fetch("documentRelations", JoinType.LEFT);
        criteriaQuery.where(root.get("uuid").in(ids));
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    @Transactional
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<UUID> ids, DataStage stage, String typeRestriction, boolean returnPayload, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        //TODO typeRestriction
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        if (!returnPayload) {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<InstanceInformation> query = criteriaBuilder.createQuery(InstanceInformation.class);
            Root<InstanceInformation> root = query.from(InstanceInformation.class);
            query.where(root.get("uuid").in(ids));
            return entityManager.createQuery(query).getResultStream().collect(Collectors.toMap(InstanceInformation::getUuid, v -> {
                NormalizedJsonLd normalizedJsonLd = new NormalizedJsonLd();
                normalizedJsonLd.setId(v.getUuid().toString());
                normalizedJsonLd.put(EBRAINSVocabulary.META_SPACE, resolveSpaceName(v.getSpaceName(), userWithRoles.getPrivateSpace()));
                return Result.ok(normalizedJsonLd);
            }));
        } else {
            if (stage == DataStage.NATIVE) {
                throw new UnsupportedOperationException("You can not request an instance by id for the native stage");
            } else {
                List<? extends Payload<?>> results = fetchInstancesByIds(stage == DataStage.IN_PROGRESS ? Payload.InferredPayload.class : Payload.ReleasedPayload.class, ids);
                Map<UUID, Map<String, Map<String, Map<?, ?>>>> incomingLinks = returnIncomingLinks ? getIncomingLinks(results.stream().map(Payload::getUuid).collect(Collectors.toSet()), stage) : null;
                return results.stream().collect(Collectors.toMap(Payload::getUuid, v -> {
                    NormalizedJsonLd result = jsonAdapter.fromJson(v.getJsonPayload(), NormalizedJsonLd.class);
                    result.resolveOutgoingRelations(v.getDocumentRelations().stream().filter(r -> r.getResolvedTarget() != null).map(r -> new OutgoingRelation(r.getCompositeId().getTargetReference(), r.getResolvedTarget())).collect(Collectors.toSet()));
                    if (!returnEmbedded) {
                        result.removeEmbedded();
                    }
                    if (returnAlternatives && v instanceof Payload.InferredPayload) {
                        result.put(EBRAINSVocabulary.META_ALTERNATIVE, jsonAdapter.fromJson(((Payload.InferredPayload) v).getAlternative(), DynamicJson.class));
                    }
                    result.removeAllInternalProperties();
                    result.put(EBRAINSVocabulary.META_SPACE, resolveSpaceName(v.getInstanceInformation().getSpaceName(), userWithRoles.getPrivateSpace()));
                    if (returnIncomingLinks) {
                        result.put(EBRAINSVocabulary.META_INCOMING_LINKS, incomingLinks.getOrDefault(v.getUuid(), null));
                    }
                    return Result.ok(result);
                }));
            }
        }
    }


    private record IncomingLinkInformation(String propertyName, String type, String label, String space, UUID uuid,
                                           UUID sourceUUID) {

    }


    private Map<UUID, List<IncomingLinkInformation>> fetchIncomingLinks(DataStage stage, Set<UUID> instances) {
        Class<? extends DocumentRelation> clazz = stage == DataStage.IN_PROGRESS ? DocumentRelation.InferredDocumentRelation.class : DocumentRelation.ReleasedDocumentRelation.class;
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<IncomingLinkInformation> query = criteriaBuilder.createQuery(IncomingLinkInformation.class);
        Root<? extends DocumentRelation> root = query.from(clazz);
        Join<Object, Object> sourcePayload = root.join("payload", JoinType.LEFT);
        Join<Object, Object> types = sourcePayload.join("types");
        Join<Object, Object> instanceInformation = sourcePayload.join("instanceInformation", JoinType.LEFT);
        query.select(criteriaBuilder.construct(IncomingLinkInformation.class, root.get("compositeId").get("propertyName"), types, sourcePayload.get("label"), instanceInformation.get("spaceName"), root.get("resolvedTarget"), root.get("compositeId").get("uuid")));
        query.where(root.get("resolvedTarget").in(instances));
        return entityManager.createQuery(query).getResultStream().collect(Collectors.groupingBy(i -> i.uuid));
    }


    @Transactional
    public Optional<NormalizedJsonLd> getInstanceById(UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, UserWithRoles userWithRoles) {
        NormalizedJsonLd result = null;
        switch (stage) {
            case NATIVE ->
                    throw new UnsupportedOperationException("You can not request an instance by id for the native stage");
            case IN_PROGRESS -> {
                Optional<Payload.InferredPayload> byId = inferredPayloadRepository.findById(id);
                if (byId.isPresent()) {
                    Payload.InferredPayload inferredPayload = byId.get();
                    if (!permissions.hasPermission(Functionality.READ, SpaceName.fromString(inferredPayload.getInstanceInformation().getSpaceName()), inferredPayload.getUuid())) {
                        throw new ForbiddenException();
                    }
                    result = jsonAdapter.fromJson(inferredPayload.getJsonPayload(), NormalizedJsonLd.class);
                    result.resolveOutgoingRelations(inferredPayload.getDocumentRelations().stream().filter(r -> r.getResolvedTarget() != null).map(r -> new OutgoingRelation(r.getCompositeId().getTargetReference(), r.getResolvedTarget())).collect(Collectors.toSet()));

                    if (!returnEmbedded) {
                        result.removeEmbedded();
                    }
                    if (returnAlternatives) {
                        result.put(EBRAINSVocabulary.META_ALTERNATIVE, jsonAdapter.fromJson(inferredPayload.getAlternative(), DynamicJson.class));
                    }
                    if (returnIncomingLinks) {
                        result.put(EBRAINSVocabulary.META_INCOMING_LINKS, getIncomingLinks(Collections.singleton(id), stage).getOrDefault(id, null));
                    }

                    result.removeAllInternalProperties();
                    result.put(EBRAINSVocabulary.META_SPACE, resolveSpaceName(inferredPayload.getInstanceInformation().getSpaceName(), userWithRoles.getPrivateSpace()));

                }
            }
            case RELEASED -> {
                Optional<Payload.ReleasedPayload> byId = releasedPayloadRepository.findById(id);
                if (byId.isPresent()) {
                    Payload.ReleasedPayload releasedPayload = byId.get();
                    if (!permissions.hasPermission(Functionality.READ_RELEASED, SpaceName.fromString(releasedPayload.getInstanceInformation().getSpaceName()), releasedPayload.getUuid())) {
                        throw new ForbiddenException();
                    }
                    result = jsonAdapter.fromJson(releasedPayload.getJsonPayload(), NormalizedJsonLd.class);
                    result.resolveOutgoingRelations(releasedPayload.getDocumentRelations().stream().filter(r -> r.getResolvedTarget() != null).map(r -> new OutgoingRelation(r.getCompositeId().getTargetReference(), r.getResolvedTarget())).collect(Collectors.toSet()));
                    if (!returnEmbedded) {
                        result.removeEmbedded();
                    }
                    result.removeAllInternalProperties();
                    result.put(EBRAINSVocabulary.META_SPACE, resolveSpaceName(releasedPayload.getInstanceInformation().getSpaceName(), userWithRoles.getPrivateSpace()));
                }
            }
        }
        return Optional.ofNullable(result);
    }

    private Map<UUID, Map<String, Map<String, Map<?, ?>>>> getIncomingLinks(Set<UUID> ids, DataStage stage) {
        Map<UUID, List<IncomingLinkInformation>> incomingLinkInformation = fetchIncomingLinks(stage, ids);
        Map<UUID, Map<String, Map<String, Map<?, ?>>>> result = new HashMap<>();
        incomingLinkInformation.forEach((uuid, incomingLinks) -> {
            List<String> typesOfIncomingLinks = incomingLinks.stream().map(i -> i.type).distinct().toList();
            Map<String, NormalizedJsonLd> genericTypeInformation = typesService.fetchTypeInformation(stage, null, null, typesOfIncomingLinks).typeSpecification();
            Map<String, List<IncomingLinkInformation>> incomingLinksByProperty = incomingLinks.stream().collect(Collectors.groupingBy(i -> i.propertyName));
            Map<String, Map<String, Map<?, ?>>> incomingLinksResult = new HashMap<>();
            incomingLinksByProperty.forEach((p, links) -> {
                Map<String, Map<?, ?>> propertyResults = new HashMap<>();
                Map<String, List<IncomingLinkInformation>> incomingLinksByType = links.stream().collect(Collectors.groupingBy(i -> i.type));
                incomingLinksByType.forEach((k, v) -> {
                    Paginated<NormalizedJsonLd> results = new Paginated<>(v.stream().map(i -> {
                        NormalizedJsonLd r = new NormalizedJsonLd();
                        r.setId(i.sourceUUID.toString());
                        r.put(EBRAINSVocabulary.META_SPACE, i.space);
                        r.put(EBRAINSVocabulary.LABEL, i.label);
                        return r;
                    }).toList(), (long) v.size(), v.size(), 0);
                    NormalizedJsonLd normalizedJsonLd = jsonAdapter.fromJson(jsonAdapter.toJson(results), NormalizedJsonLd.class);
                    NormalizedJsonLd typeInformation = genericTypeInformation.get(k);
                    if (typeInformation != null) {
                        normalizedJsonLd.putAll(typeInformation);
                    }
                    normalizedJsonLd.put(EBRAINSVocabulary.META_OCCURRENCES, 0); //TODO FIX
                    propertyResults.put(k, normalizedJsonLd);
                });
                incomingLinksResult.put(p, propertyResults);
            });
            result.put(uuid, incomingLinksResult);
        });
        return result;
    }

    public Optional<Tuple<NormalizedJsonLd, String>> getNativeInstanceById(UUID id, String userId) {
        return nativeRepository.findById(new NativePayload.CompositeId(id, userId)).map(n -> new Tuple<>(addFieldUpdateTimes(jsonAdapter.fromJson(n.getJsonPayload(), NormalizedJsonLd.class), jsonAdapter.fromJson(n.getPropertyUpdates(), NormalizedJsonLd.FieldUpdateTimes.class)), n.getMd5hash()));
    }

    private NormalizedJsonLd addFieldUpdateTimes(NormalizedJsonLd normalizedJsonLd, NormalizedJsonLd.FieldUpdateTimes fieldUpdateTimes) {
        normalizedJsonLd.setFieldUpdateTimes(fieldUpdateTimes);
        return normalizedJsonLd;
    }

}
