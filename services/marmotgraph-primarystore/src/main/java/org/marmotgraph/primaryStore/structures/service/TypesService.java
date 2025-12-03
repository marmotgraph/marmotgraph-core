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

package org.marmotgraph.primaryStore.structures.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.external.types.*;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.primaryStore.instances.model.*;
import org.marmotgraph.primaryStore.instances.service.PropertyInTypeSpecificationRepository;
import org.marmotgraph.primaryStore.instances.service.PropertySpecificationRepository;
import org.marmotgraph.primaryStore.instances.service.SpaceService;
import org.marmotgraph.primaryStore.instances.service.TypeSpecificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TypesService {

    private final JsonAdapter jsonAdapter;
    private final EntityManager entityManager;
    private final TypeSpecificationRepository typeSpecificationRepository;
    private final PropertySpecificationRepository propertySpecificationRepository;
    private final PropertyInTypeSpecificationRepository propertyInTypeSpecificationRepository;



    private final SpaceService spaceService;

    private record TypePerSpaceInfo(Long instanceCount, String type, String space) {
    }

    private record TargetTypeInformation(Long instanceCount, String sourceType, String propertyName, String targetType, String space) {
    }

    private record PropertyPerSpaceInfo(Long instanceCount, String type, String space, String propertyName) {
    }

    private record IncomingLinksInfo(String sourceType, String spaceName, String propertyName, String targetType) {
    }


    private record GenericTypeInformation(Map<String, NormalizedJsonLd> typeSpecification, Map<String, List<TypePerSpaceInfo>> typePerSpaceInformation, Optional<Long> totalCount){}

    @Transactional
    public Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties,
                                                boolean withIncomingLinks, PaginationParam paginationParam, String clientId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        Class<? extends TypeStructure> typeStructureClazz = stage == DataStage.IN_PROGRESS ? TypeStructure.InferredTypeStructure.class : TypeStructure.ReleasedTypeStructure.class;
        Class<? extends EmbeddedTypeInformation> embeddedTargetTypeClazz = stage == DataStage.IN_PROGRESS ? EmbeddedTypeInformation.InferredEmbeddedTypeInformation.class : EmbeddedTypeInformation.ReleasedEmbeddedTypeInformation.class;

        //TODO space filter
        GenericTypeInformation genericTypeInformation = fetchGenericTypeInformation(typeStructureClazz, criteriaBuilder, paginationParam, space, null);
        List<TypeInformation> result = enrichTypeInformation(withProperties, withIncomingLinks, genericTypeInformation, space, criteriaBuilder, typeStructureClazz, embeddedTargetTypeClazz, clientId);
        result.sort(Comparator.comparing(TypeInformation::getIdentifier));
        Long total = null;
        if (paginationParam == null) {
            total = (long) result.size();
        } else if (genericTypeInformation.totalCount.isPresent()) {
            total = genericTypeInformation.totalCount.get();
        }

        return new Paginated<>(result, total, result.size(), paginationParam != null ? paginationParam.getFrom() : 0);
    }

    @Transactional
    public Map<String, Result<TypeInformation>> getByName(List<String> types, DataStage stage, String space, boolean withProperties, boolean withIncomingLinks, String clientId) {
        //TODO space filter
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        Class<? extends TypeStructure> typeInformationClazz = stage == DataStage.IN_PROGRESS ? TypeStructure.InferredTypeStructure.class : TypeStructure.ReleasedTypeStructure.class;
        Class<? extends EmbeddedTypeInformation> embeddedTargetTypeClazz = stage == DataStage.IN_PROGRESS ? EmbeddedTypeInformation.InferredEmbeddedTypeInformation.class : EmbeddedTypeInformation.ReleasedEmbeddedTypeInformation.class;
        GenericTypeInformation genericTypeInformation = fetchGenericTypeInformation(typeInformationClazz, criteriaBuilder, null, space, types); //No pagination due to restriction by types filter
        List<TypeInformation> result = enrichTypeInformation(withProperties, withIncomingLinks, genericTypeInformation, space, criteriaBuilder, typeInformationClazz, embeddedTargetTypeClazz, clientId);
        Map<String, Result<TypeInformation>> resultMap = result.stream().collect(Collectors.toMap(TypeInformation::getIdentifier, Result::ok));
        types.stream().filter(t -> !resultMap.containsKey(t)).forEach(t ->
                resultMap.put(t, Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()))
        );
        return resultMap;
    }


    private List<TypeInformation> enrichTypeInformation(boolean withProperties, boolean withIncomingLinks, GenericTypeInformation genericTypeInformation, String spaceRestriction, CriteriaBuilder criteriaBuilder, Class<? extends TypeStructure> typeStructureClazz, Class<? extends EmbeddedTypeInformation> embeddedTypeInfoClazz, String clientId) {
        Map<String, List<PropertyPerSpaceInfo>> propertiesByType = fetchPropertiesByType(withProperties, spaceRestriction, criteriaBuilder, typeStructureClazz, genericTypeInformation.typePerSpaceInformation);
        Map<String, List<TargetTypeInformation>> targetTypeInformation = fetchTargetTypeInformation(withProperties, criteriaBuilder, embeddedTypeInfoClazz, genericTypeInformation.typePerSpaceInformation);

        // TODO do we also need to restrict the incoming links to only those relevant for the filtered space?
        Map<String, List<IncomingLinksInfo>> incomingLinksByType = fetchIncomingLinks(withIncomingLinks, criteriaBuilder, typeStructureClazz, genericTypeInformation.typePerSpaceInformation);
        List<TypeInformation> result = new ArrayList<>();
        genericTypeInformation.typePerSpaceInformation.forEach((k, v) -> {
            TypeInformation typeInformation = new TypeInformation();
            typeInformation.setIdentifier(k);
            NormalizedJsonLd typeSpecification = genericTypeInformation.typeSpecification.get(k);
            List<String> searchableProperties = Collections.emptyList();
            if(typeSpecification != null) {
                typeInformation.putAll(typeSpecification);
                searchableProperties = typeSpecification.getAsListOf(EBRAINSVocabulary.META_PROPERTY_SEARCHABLE, String.class);
            }
            Map<String, List<TargetTypeInformation>> targetTypeInformationBySpace = targetTypeInformation.getOrDefault(k, Collections.<TargetTypeInformation>emptyList()).stream().collect(Collectors.groupingBy(t -> t.space));
            if(typeInformation.getName() == null) {
                String[] nameSplit = k.split("/");
                typeInformation.setName(nameSplit[nameSplit.length - 1]);
            }
            Map<String, List<PropertyPerSpaceInfo>> propertyBySpaceInfos = propertiesByType.getOrDefault(k, Collections.emptyList()).stream().collect(Collectors.groupingBy(p -> p.space));
            typeInformation.setSpaces(evaluateSpaceTypeInformation(withProperties, v, propertyBySpaceInfos, targetTypeInformationBySpace));
            if (withIncomingLinks) {
                evaluateIncomingLinks(k, incomingLinksByType, typeInformation);
            }
            consolidateProperties(withProperties, typeInformation, searchableProperties);
            enrichProperties(typeInformation.getProperties(), typeInformation.getName(), clientId);
            if(spaceRestriction!=null){
                // The result is restricted to a single space - accordingly, the substructure of space information is redundant and removed
                typeInformation.setSpaces(null);
            }
            result.add(typeInformation);
        });
        return result;
    }

    private void enrichProperties(List<Property> properties, String typeName, String clientId){
        Set<String> propertyNames = properties.stream().map(Property::getIdentifier).collect(Collectors.toSet());
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PropertySpecification> propertyCriteriaQuery = criteriaBuilder.createQuery(PropertySpecification.class);
        Root<PropertySpecification> root = propertyCriteriaQuery.from(PropertySpecification.class);
        Predicate clientIdPredicate;
        Path<Object> clientIdPath = root.get("compositeId").get("clientId");
        if(clientId != null && !clientId.equals(TypeSpecification.GLOBAL_CLIENT_ID)){
            clientIdPredicate = criteriaBuilder.or(criteriaBuilder.equal(clientIdPath, TypeSpecification.GLOBAL_CLIENT_ID), criteriaBuilder.equal(clientIdPath, clientId));
        }
        else{
            clientIdPredicate = criteriaBuilder.equal(clientIdPath, TypeSpecification.GLOBAL_CLIENT_ID);
        }
        propertyCriteriaQuery.where(criteriaBuilder.and(root.get("compositeId").get("property").in(propertyNames), clientIdPredicate));
        List<PropertySpecification> resultList = entityManager.createQuery(propertyCriteriaQuery).getResultList();
        Map<String, List<PropertySpecification>> propertySpecs = resultList.stream().collect(Collectors.groupingBy(k -> k.getCompositeId().getProperty()));
        properties.forEach(p -> {
            List<PropertySpecification> propertySpecifications = propertySpecs.get(p.getIdentifier());
            if(propertySpecifications!=null){
                //First, we apply the global elements
                Map<Boolean, NormalizedJsonLd> propSpecs = propertySpecifications.stream().collect(Collectors.toMap(k -> k.getCompositeId().getClientId().equals(TypeSpecification.GLOBAL_CLIENT_ID), v -> jsonAdapter.fromJson(v.getPayload(), NormalizedJsonLd.class)));
                NormalizedJsonLd globalSpec = propSpecs.get(true);
                if(globalSpec!=null){
                    p.putAll(globalSpec);
                }
                NormalizedJsonLd clientSpec = propSpecs.get(false);
                if(clientSpec!=null){
                    p.putAll(clientSpec);
                }
            }
        });
    }


    private GenericTypeInformation fetchGenericTypeInformation(Class<? extends TypeStructure> clazz, CriteriaBuilder criteriaBuilder, PaginationParam paginationParam, String space, List<String> typeNameFilter) {
        //Read in the generic types
        Long totalResults = null;
        CriteriaQuery<TypeSpecification> simpleTypeCriteriaQuery = criteriaBuilder.createQuery(TypeSpecification.class);
        Root<TypeSpecification> simpleTypeRoot = simpleTypeCriteriaQuery.from(TypeSpecification.class);
        simpleTypeCriteriaQuery.select(simpleTypeRoot);
        Set<Predicate> simpleTypePredicates = new HashSet<>();
        simpleTypePredicates.add(criteriaBuilder.equal(simpleTypeRoot.get("compositeId").get("clientId"), TypeSpecification.GLOBAL_CLIENT_ID));
        if(typeNameFilter!=null){
            simpleTypePredicates.add(simpleTypeRoot.get("compositeId").get("type").in(typeNameFilter));
        }
        //TODO filter by space in the first query to make sure the pagination works as expected
        simpleTypeCriteriaQuery.where(criteriaBuilder.and(simpleTypePredicates.toArray(new Predicate[0])));
        simpleTypeCriteriaQuery.orderBy(criteriaBuilder.asc(simpleTypeRoot.get("compositeId").get("type")));
        TypedQuery<TypeSpecification> simpleTypeQuery = entityManager.createQuery(simpleTypeCriteriaQuery);
        if (paginationParam != null) {
            if (paginationParam.isReturnTotalResults()) {
                CriteriaQuery<Long> totalCountQuery = criteriaBuilder.createQuery(Long.class);
                Root<TypeSpecification> totalCountQueryRoot = totalCountQuery.from(TypeSpecification.class);
                totalCountQuery.select(criteriaBuilder.countDistinct(totalCountQueryRoot.get("compositeId").get("type")));
                totalCountQuery.where(criteriaBuilder.equal(totalCountQueryRoot.get("compositeId").get("clientId"), TypeSpecification.GLOBAL_CLIENT_ID));
                totalResults = entityManager.createQuery(totalCountQuery).getSingleResult();
            }
            simpleTypeQuery.setFirstResult((int) paginationParam.getFrom());
            if (paginationParam.getSize() != null) {
                simpleTypeQuery.setMaxResults(paginationParam.getSize().intValue());
            }
        }
        List<TypeSpecification> resultList = simpleTypeQuery.getResultList();
        Map<String, NormalizedJsonLd> typeSpecifications =resultList.isEmpty() ? Collections.emptyMap() : resultList.stream().filter(r -> r.getPayload() != null).collect(Collectors.toMap(k -> k.getCompositeId().getType(), v -> jsonAdapter.fromJson(v.getPayload(), NormalizedJsonLd.class)));
        List<String> filteredTypeNames = resultList.stream().map(r -> r.getCompositeId().getType()).toList();

        // The second query reads in the reflected information
        CriteriaQuery<TypePerSpaceInfo> query = criteriaBuilder.createQuery(TypePerSpaceInfo.class);
        Set<Predicate> predicates = new HashSet<>();
        Root<? extends TypeStructure> root = query.from(clazz);
        Join<Object, Object> instanceInformation = root.join("instanceInformation", JoinType.LEFT);
        query.select(criteriaBuilder.construct(TypePerSpaceInfo.class, criteriaBuilder.count(root.get("compositeId").get("uuid")), root.get("compositeId").get("type"), instanceInformation.get("spaceName")));
        predicates.add(root.get("compositeId").get("type").in(filteredTypeNames));
        if(space != null) {
            predicates.add(criteriaBuilder.equal(instanceInformation.get("spaceName"), space));
        }
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
        query.groupBy(root.get("compositeId").get("type"), instanceInformation.get("spaceName"));
        return new GenericTypeInformation(typeSpecifications, entityManager.createQuery(query).getResultStream().collect(Collectors.groupingBy(k -> k.type)), Optional.ofNullable(totalResults));
    }

    private static List<SpaceTypeInformation> evaluateSpaceTypeInformation(boolean withProperties, List<TypePerSpaceInfo> v, Map<String, List<PropertyPerSpaceInfo>> propertyBySpaceInfos, Map<String, List<TargetTypeInformation>> targetTypeBySpace) {
        return v.stream().map(value -> {
            SpaceTypeInformation spaceTypeInformation = new SpaceTypeInformation();
            spaceTypeInformation.setOccurrences(value.instanceCount.intValue());
            spaceTypeInformation.setSpace(value.space);
            if (withProperties) {
                List<PropertyPerSpaceInfo> properties = propertyBySpaceInfos.getOrDefault(value.space, Collections.emptyList());
                Map<String, List<TargetTypeInformation>> targetTypeByPropertyName = targetTypeBySpace.getOrDefault(value.space, Collections.emptyList()).stream().collect(Collectors.groupingBy(t -> t.propertyName));
                spaceTypeInformation.setProperties(properties.stream().map(p -> {
                    Property property = new Property();
                    property.setIdentifier(p.propertyName);
                    property.setOccurrences(p.instanceCount.intValue());
                    List<TargetTypeInformation> targetTypeInformation = targetTypeByPropertyName.get(p.propertyName);
                    if(!CollectionUtils.isEmpty(targetTypeInformation)){
                        property.setTargetTypes(targetTypeInformation.stream().map(t -> {
                            TargetType targetType = new TargetType();
                            targetType.setType(t.targetType);
                            targetType.setOccurrences(t.instanceCount.intValue());
                            SpaceReference reference = new SpaceReference();
                            reference.setSpace(t.space);
                            reference.setOccurrences(t.instanceCount.intValue());
                            targetType.setSpaces(Collections.singletonList(reference));
                            return targetType;
                        }).collect(Collectors.toList()));
                    }
                    return property;
                }).sorted(Comparator.comparing(Property::getIdentifier)).toList());
            }
            return spaceTypeInformation;
        }).toList();
    }

    private static void consolidateProperties(boolean withProperties, TypeInformation typeInformation, List<String> searchableProperties) {
        typeInformation.setOccurrences(0);
        Map<String, Property> properties = new HashMap<>();
        typeInformation.getSpaces().forEach(s -> {
            typeInformation.setOccurrences(typeInformation.getOccurrences() + s.getOccurrences());
            if (withProperties) {
                s.getProperties().forEach(p -> {
                    Property property = properties.get(p.getIdentifier());
                    if (property == null) {
                        property = new Property();
                        property.setIdentifier(p.getIdentifier());
                        property.setOccurrences(0);
                        if (searchableProperties.contains(p.getIdentifier())) {
                            property.put(EBRAINSVocabulary.META_PROPERTY_SEARCHABLE, true);
                        }
                        properties.put(p.getIdentifier(), property);
                    }
                    if(!CollectionUtils.isEmpty(p.getTargetTypes())){
                        List<TargetType> propertyTargetTypes = property.getTargetTypes();
                        Map<String, TargetType> fromSpaceByType = p.getTargetTypes().stream().collect(Collectors.toMap(TargetType::getType, v-> v));
                        List<TargetType> newTargetTypes = new ArrayList<>(property.getTargetTypes());
                        newTargetTypes.addAll(fromSpaceByType.keySet().stream().map(t -> {
                            Optional<TargetType> existingTargetType = propertyTargetTypes.stream().filter(existingType -> existingType.getType().equals(t)).findFirst();
                            TargetType newTargetType = fromSpaceByType.get(t);
                            TargetType targetType;
                            if (existingTargetType.isPresent()) {
                                targetType = existingTargetType.get();
                                targetType.setOccurrences(targetType.getOccurrences() + newTargetType.getOccurrences());
                                targetType.getSpaces().addAll(newTargetType.getSpaces());
                                return null;
                            } else {
                                targetType = new TargetType();
                                targetType.setType(t);
                                targetType.setOccurrences(newTargetType.getOccurrences());
                                targetType.setSpaces(new ArrayList<>(newTargetType.getSpaces()));
                                return targetType;
                            }
                        }).filter(Objects::nonNull).toList());
                        property.setTargetTypes(newTargetTypes);
                    }
                    property.setOccurrences(property.getOccurrences() + p.getOccurrences());
                });
                typeInformation.setProperties(properties.values().stream().sorted(Comparator.comparing(Property::getIdentifier)).toList());
            }
        });
    }

    private static void evaluateIncomingLinks(String k, Map<String, List<IncomingLinksInfo>> incomingLinksByType, TypeInformation typeInformation) {
        List<IncomingLink> incomingLinks = new ArrayList<>();
        Map<String, List<IncomingLinksInfo>> incomingLinksPerPropertyInfos = incomingLinksByType.getOrDefault(k, Collections.emptyList()).stream().collect(Collectors.groupingBy(p -> p.propertyName));
        incomingLinksPerPropertyInfos.forEach((key, value) -> {
            IncomingLink incomingLink = new IncomingLink();
            incomingLink.setIdentifier(key);
            Map<String, List<IncomingLinksInfo>> bySourceType = value.stream().collect(Collectors.groupingBy(p -> p.sourceType));
            List<SourceType> sourceTypes = new ArrayList<>();
            bySourceType.forEach((sourceTypeK, sourceTypeV) -> {
                SourceType sourceType = new SourceType();
                sourceType.setType(sourceTypeK);
                sourceType.setSpaces(sourceTypeV.stream().map(sourceTypeVal -> {
                    SpaceReference spaceReference = new SpaceReference();
                    spaceReference.setSpace(sourceTypeVal.spaceName);
                    return spaceReference;
                }).toList());
                sourceTypes.add(sourceType);
            });
            incomingLink.setSourceTypes(sourceTypes);
            incomingLinks.add(incomingLink);
        });
        typeInformation.setIncomingLinks(incomingLinks);
    }

    private Map<String, List<TargetTypeInformation>> fetchTargetTypeInformation(boolean withProperties, CriteriaBuilder criteriaBuilder, Class<? extends EmbeddedTypeInformation> clazz, Map<String, List<TypePerSpaceInfo>> byType){
        if(!withProperties){
            return Collections.emptyMap();
        }
        CriteriaQuery<TargetTypeInformation> embeddedTargetTypesCriteriaQuery = criteriaBuilder.createQuery(TargetTypeInformation.class);
        Root<? extends EmbeddedTypeInformation> embeddedTypesRoot = embeddedTargetTypesCriteriaQuery.from(clazz);
        Path<Object> compositeId = embeddedTypesRoot.get("compositeId");
        Join<Object, Object> instanceInformation = embeddedTypesRoot.join("payload", JoinType.LEFT).join("instanceInformation", JoinType.LEFT);
        embeddedTargetTypesCriteriaQuery.select(criteriaBuilder.construct(TargetTypeInformation.class, criteriaBuilder.count(compositeId.get("uuid")), compositeId.get("sourceType"), compositeId.get("propertyName"), compositeId.get("targetType"), instanceInformation.get("spaceName")));
        embeddedTargetTypesCriteriaQuery.groupBy(compositeId.get("sourceType"), compositeId.get("propertyName"), compositeId.get("targetType"), instanceInformation.get("spaceName"));
        embeddedTargetTypesCriteriaQuery.where(compositeId.get("sourceType").in(byType.keySet()));
        return  entityManager.createQuery(embeddedTargetTypesCriteriaQuery).getResultList().stream().collect(Collectors.groupingBy(k -> k.sourceType));
    }

    private Map<String, List<PropertyPerSpaceInfo>> fetchPropertiesByType(boolean withProperties, String spaceRestriction, CriteriaBuilder criteriaBuilder, Class<? extends TypeStructure> clazz, Map<String, List<TypePerSpaceInfo>> byType) {
        Map<String, List<PropertyPerSpaceInfo>> propertiesByType;
        if (withProperties) {
            CriteriaQuery<PropertyPerSpaceInfo> propertyQuery = criteriaBuilder.createQuery(PropertyPerSpaceInfo.class);
            Root<? extends TypeStructure> propertyRoot = propertyQuery.from(clazz);
            Join<Object, Object> properties = propertyRoot.join("properties", JoinType.LEFT);
            Join<Object, Object> info = propertyRoot.join("instanceInformation", JoinType.LEFT);
            propertyQuery.select(criteriaBuilder.construct(PropertyPerSpaceInfo.class, criteriaBuilder.count(properties), propertyRoot.get("compositeId").get("type"), info.get("spaceName"), properties));
            propertyQuery.groupBy(properties, propertyRoot.get("compositeId").get("type"), info.get("spaceName"));
            Predicate typeFilter = propertyRoot.get("compositeId").get("type").in(byType.keySet());
            if(spaceRestriction!=null){
                propertyQuery.where(criteriaBuilder.and(typeFilter, criteriaBuilder.equal(info.get("spaceName"), spaceRestriction)));
            }
            else {
                propertyQuery.where(typeFilter);
            }
            propertiesByType = entityManager.createQuery(propertyQuery).getResultStream().collect(Collectors.groupingBy(k -> k.type));
            Class<? extends DocumentRelation> documentRelationClazz = clazz == TypeStructure.InferredTypeStructure.class ? DocumentRelation.InferredDocumentRelation.class : DocumentRelation.ReleasedDocumentRelation.class;
        } else {
            propertiesByType = Collections.emptyMap();
        }
        return propertiesByType;
    }

    private Map<String, List<IncomingLinksInfo>> fetchIncomingLinks(boolean withIncomingLinks, CriteriaBuilder criteriaBuilder, Class<? extends TypeStructure> clazz, Map<String, List<TypePerSpaceInfo>> byType) {
        Map<String, List<IncomingLinksInfo>> incomingLinksByType;
        if (withIncomingLinks) {
            CriteriaQuery<IncomingLinksInfo> incomingLinks = criteriaBuilder.createQuery(IncomingLinksInfo.class);
            Root<? extends TypeStructure> incomingLinksRoot = incomingLinks.from(clazz);
            Join<Object, Object> payloadJoin = incomingLinksRoot.join("payload", JoinType.LEFT);
            Join<Object, Object> incomingRelationsJoin = payloadJoin.join("incomingRelations", JoinType.LEFT);
            Join<Object, Object> incomingRelationPayloadJoin = incomingRelationsJoin.join("payload", JoinType.LEFT);
            Join<Object, Object> typeOfIncomingLinksJoin = incomingRelationPayloadJoin.join("typeStructures", JoinType.LEFT);
            Join<Object, Object> instanceInfoOfIncomingLinksJoin = incomingRelationPayloadJoin.join("instanceInformation", JoinType.LEFT);
            incomingLinks.select(criteriaBuilder.construct(IncomingLinksInfo.class, typeOfIncomingLinksJoin.get("compositeId").get("type"), instanceInfoOfIncomingLinksJoin.get("spaceName"), incomingRelationsJoin.get("compositeId").get("propertyName"), incomingLinksRoot.get("compositeId").get("type")));
            incomingLinks.groupBy(typeOfIncomingLinksJoin.get("compositeId").get("type"), instanceInfoOfIncomingLinksJoin.get("spaceName"), incomingRelationsJoin.get("compositeId").get("propertyName"), incomingLinksRoot.get("compositeId").get("type"));
            incomingLinks.where(criteriaBuilder.and(
                    typeOfIncomingLinksJoin.get("compositeId").get("type").isNotNull(),
                    incomingLinksRoot.get("compositeId").get("type").in(byType.keySet())
            ));

            incomingLinksByType = entityManager.createQuery(incomingLinks).getResultStream().collect(Collectors.groupingBy(k -> k.targetType));
        } else {
            incomingLinksByType = Collections.emptyMap();
        }
        return incomingLinksByType;
    }

    public DynamicJson getTypeSpecification(String type, String clientId){
        Optional<TypeSpecification> byId = typeSpecificationRepository.findById(new TypeSpecification.CompositeId(type, clientId));
        return byId.map(typeSpecification -> jsonAdapter.fromJson(typeSpecification.getPayload(), DynamicJson.class)).orElse(null);
    }

    @Transactional
    public void specifyType(JsonLdId typeName, NormalizedJsonLd normalizedJsonLd, String clientId){
        TypeSpecification entity = new TypeSpecification();
        entity.setCompositeId(new TypeSpecification.CompositeId(typeName.getId(), clientId));
        entity.setPayload(jsonAdapter.toJson(normalizedJsonLd));
        typeSpecificationRepository.save(entity);
    }

    @Transactional
    public void removeType(JsonLdId typeName, String clientId){
        typeSpecificationRepository.deleteById(new TypeSpecification.CompositeId(typeName.getId(), clientId));
    }

    public DynamicJson getPropertySpecification(String propertyName, String clientId) {
        Optional<PropertySpecification> byId = propertySpecificationRepository.findById(new PropertySpecification.CompositeId(propertyName, clientId));
        return byId.map(specification -> jsonAdapter.fromJson(specification.getPayload(), DynamicJson.class)).orElse(null);
    }

    public void specifyProperty(JsonLdId propertyName, NormalizedJsonLd normalizedJsonLd, String clientId) {
        PropertySpecification entity = new PropertySpecification();
        entity.setCompositeId(new PropertySpecification.CompositeId(propertyName.getId(), clientId));
        entity.setPayload(jsonAdapter.toJson(normalizedJsonLd));
        propertySpecificationRepository.save(entity);
    }

    public void removePropertySpecification(JsonLdId propertyName, String clientId) {
        propertySpecificationRepository.deleteById(new PropertySpecification.CompositeId(propertyName.getId(), clientId));
    }


    public DynamicJson getPropertyInTypeSpecification(String type, String propertyName, String clientId) {
        Optional<PropertyInTypeSpecification> byId = propertyInTypeSpecificationRepository.findById(new PropertyInTypeSpecification.CompositeId(type, propertyName, clientId));
        return byId.map(specification -> jsonAdapter.fromJson(specification.getPayload(), DynamicJson.class)).orElse(null);
    }

    public void specifyPropertyInType(String typeName, String propertyName, NormalizedJsonLd normalizedJsonLd, String clientId) {
        PropertyInTypeSpecification entity = new PropertyInTypeSpecification();
        entity.setCompositeId(new PropertyInTypeSpecification.CompositeId(typeName, propertyName, clientId));
        entity.setPayload(jsonAdapter.toJson(normalizedJsonLd));
        propertyInTypeSpecificationRepository.save(entity);
    }

    public void removePropertyInTypeSpecification(String typeName, String propertyName, String clientId) {
        propertyInTypeSpecificationRepository.deleteById(new PropertyInTypeSpecification.CompositeId(typeName, propertyName, clientId));
    }

}
