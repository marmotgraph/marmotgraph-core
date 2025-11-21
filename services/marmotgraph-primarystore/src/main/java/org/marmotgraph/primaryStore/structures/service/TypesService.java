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
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.external.types.*;
import org.marmotgraph.primaryStore.instances.model.TypeStructure;
import org.marmotgraph.primaryStore.instances.service.SpaceService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TypesService {

    private final EntityManager entityManager;
    private final SpaceService spaceService;
    private record TypePerSpaceInfo(Long instanceCount, String type, String space){}

    private record PropertyPerSpaceInfo(Long instanceCount, String type, String space, String propertyName){}

    private record IncomingLinksInfo(String sourceType, String spaceName, String propertyName,  String targetType){}

    @Transactional
    public Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties,
                                                boolean withIncomingLinks, PaginationParam paginationParam) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        Class<? extends TypeStructure> clazz = stage == DataStage.IN_PROGRESS ? TypeStructure.InferredTypeStructure.class : TypeStructure.ReleasedTypeStructure.class;
        Tuple<Map<String, List<TypePerSpaceInfo>>, Optional<Long>> byTypeWithTotal = fetchGenericTypeInformation(clazz, criteriaBuilder, paginationParam);
        Map<String, List<TypePerSpaceInfo>> byType = byTypeWithTotal.getA();
        Map<String, List<PropertyPerSpaceInfo>> propertiesByType = fetchPropertiesByType(withProperties, criteriaBuilder, clazz, byType);
        Map<String, List<IncomingLinksInfo>> incomingLinksByType = fetchIncomingLinks(withIncomingLinks, criteriaBuilder, clazz, byType);
        List<TypeInformation> result = new ArrayList<>();
        byType.forEach((k, v) -> {
            TypeInformation typeInformation = new TypeInformation();
            typeInformation.setIdentifier(k);
            Map<String, List<PropertyPerSpaceInfo>> propertyPerSpaceInfos = propertiesByType.getOrDefault(k, Collections.emptyList()).stream().collect(Collectors.groupingBy(p -> p.space));
            typeInformation.setSpaces(evaluateSpaceTypeInformation(withProperties, v, propertyPerSpaceInfos));
            if(withIncomingLinks) {
                evaluateIncomingLinks(k, incomingLinksByType, typeInformation);
            }
            consolidateProperties(withProperties, typeInformation);
            result.add(typeInformation);
        });
        result.sort(Comparator.comparing(TypeInformation::getIdentifier));
        Long total = null;
        if(paginationParam == null){
            total = (long)result.size();
        }
        else if(byTypeWithTotal.getB().isPresent()){
            total = byTypeWithTotal.getB().get();
        }

        return new Paginated<>(result, total, result.size(), paginationParam != null ? paginationParam.getFrom() : 0);
    }

    private Tuple<Map<String, List<TypePerSpaceInfo>>, Optional<Long>> fetchGenericTypeInformation(Class<? extends TypeStructure> clazz, CriteriaBuilder criteriaBuilder, PaginationParam paginationParam){
        CriteriaQuery<TypePerSpaceInfo> query = criteriaBuilder.createQuery(TypePerSpaceInfo.class);
        Root<? extends TypeStructure> root = query.from(clazz);
        Join<Object, Object> instanceInformation = root.join("instanceInformation", JoinType.LEFT);
        query.select(criteriaBuilder.construct(TypePerSpaceInfo.class, criteriaBuilder.count(root.get("compositeId").get("uuid")), root.get("compositeId").get("type"), instanceInformation.get("spaceName")));
        query.groupBy(root.get("compositeId").get("type"), instanceInformation.get("spaceName"));
        query.orderBy(criteriaBuilder.asc(root.get("compositeId").get("type")));
        TypedQuery<TypePerSpaceInfo> typedQuery = entityManager.createQuery(query);
        Long totalResults = null;
        if(paginationParam != null){
            if(paginationParam.isReturnTotalResults()) {
                CriteriaQuery<Long> totalCountQuery = criteriaBuilder.createQuery(Long.class);
                totalCountQuery.select(criteriaBuilder.countDistinct(totalCountQuery.from(clazz).get("compositeId").get("type")));
                totalResults = entityManager.createQuery(totalCountQuery).getSingleResult();
            }
            typedQuery.setFirstResult((int)paginationParam.getFrom());
            if(paginationParam.getSize()!=null){
                typedQuery.setMaxResults(paginationParam.getSize().intValue());
            }
        }
        return new Tuple<>(typedQuery.getResultStream().collect(Collectors.groupingBy(k -> k.type)), Optional.ofNullable(totalResults));
    }

    private static List<SpaceTypeInformation> evaluateSpaceTypeInformation(boolean withProperties, List<TypePerSpaceInfo> v, Map<String, List<PropertyPerSpaceInfo>> propertyPerSpaceInfos) {
        return v.stream().map(value -> {
            SpaceTypeInformation spaceTypeInformation = new SpaceTypeInformation();
            spaceTypeInformation.setOccurrences(value.instanceCount.intValue());
            spaceTypeInformation.setSpace(value.space);
            if (withProperties) {
                List<PropertyPerSpaceInfo> properties = propertyPerSpaceInfos.getOrDefault(value.space, Collections.emptyList());
                spaceTypeInformation.setProperties(properties.stream().map(p -> {
                    Property property = new Property();
                    property.setIdentifier(p.propertyName);
                    property.setOccurrences(p.instanceCount.intValue());
                    return property;
                }).sorted(Comparator.comparing(Property::getIdentifier)).toList());
            }
            return spaceTypeInformation;
        }).toList();
    }

    private static void consolidateProperties(boolean withProperties, TypeInformation typeInformation) {
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
                        properties.put(p.getIdentifier(), property);
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
        incomingLinksPerPropertyInfos.forEach((key, value ) -> {
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

    private Map<String, List<PropertyPerSpaceInfo>> fetchPropertiesByType(boolean withProperties, CriteriaBuilder criteriaBuilder, Class<? extends TypeStructure> clazz, Map<String, List<TypePerSpaceInfo>> byType) {
        Map<String, List<PropertyPerSpaceInfo>> propertiesByType;
        if(withProperties) {
            CriteriaQuery<PropertyPerSpaceInfo> propertyQuery = criteriaBuilder.createQuery(PropertyPerSpaceInfo.class);
            Root<? extends TypeStructure> propertyRoot = propertyQuery.from(clazz);
            Join<Object, Object> properties = propertyRoot.join("properties", JoinType.LEFT);
            Join<Object, Object> info = propertyRoot.join("instanceInformation", JoinType.LEFT);
            propertyQuery.select(criteriaBuilder.construct(PropertyPerSpaceInfo.class, criteriaBuilder.count(properties), propertyRoot.get("compositeId").get("type"), info.get("spaceName"), properties));
            propertyQuery.groupBy(properties, propertyRoot.get("compositeId").get("type"), info.get("spaceName"));
            propertyQuery.where(propertyRoot.get("compositeId").get("type").in(byType.keySet()));
            propertiesByType = entityManager.createQuery(propertyQuery).getResultStream().collect(Collectors.groupingBy(k -> k.type));
        }
        else{
            propertiesByType = Collections.emptyMap();
        }
        return propertiesByType;
    }

    private Map<String, List<IncomingLinksInfo>> fetchIncomingLinks(boolean withIncomingLinks, CriteriaBuilder criteriaBuilder, Class<? extends TypeStructure> clazz, Map<String, List<TypePerSpaceInfo>> byType) {
        Map<String, List<IncomingLinksInfo>> incomingLinksByType;
        if(withIncomingLinks) {
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
        }
        else{
            incomingLinksByType = Collections.emptyMap();
        }
        return incomingLinksByType;
    }


}
