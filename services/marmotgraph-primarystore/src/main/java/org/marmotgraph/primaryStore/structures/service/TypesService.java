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
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.external.types.Property;
import org.marmotgraph.commons.model.external.types.SpaceTypeInformation;
import org.marmotgraph.commons.model.external.types.TypeInformation;
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

    @Transactional
    public Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties,
                                                boolean withIncomingLinks, PaginationParam paginationParam) {

        Class<? extends TypeStructure> clazz = stage == DataStage.IN_PROGRESS ? TypeStructure.InferredTypeStructure.class : TypeStructure.ReleasedTypeStructure.class;
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TypePerSpaceInfo> query = criteriaBuilder.createQuery(TypePerSpaceInfo.class);
        Root<? extends TypeStructure> root = query.from(clazz);
        Join<Object, Object> instanceInformation = root.join("instanceInformation", JoinType.LEFT);
        query.select(criteriaBuilder.construct(TypePerSpaceInfo.class, criteriaBuilder.count(root.get("compositeId").get("uuid")), root.get("compositeId").get("type"), instanceInformation.get("spaceName")));
        query.groupBy(root.get("compositeId").get("type"), instanceInformation.get("spaceName"));
        query.orderBy(criteriaBuilder.asc(root.get("compositeId").get("type")));
        Map<String, List<TypePerSpaceInfo>> byType = entityManager.createQuery(query).getResultStream().collect(Collectors.groupingBy(k -> k.type));
        List<TypeInformation> result = new ArrayList<>();
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
        byType.forEach((k, v) -> {
            TypeInformation typeInformation = new TypeInformation();
            typeInformation.setIdentifier(k);
            Map<String, List<PropertyPerSpaceInfo>> propertyPerSpaceInfos = propertiesByType.getOrDefault(k, Collections.emptyList()).stream().collect(Collectors.groupingBy(p -> p.space));
            typeInformation.setSpaces(v.stream().map(value -> {
                SpaceTypeInformation spaceTypeInformation = new SpaceTypeInformation();
                spaceTypeInformation.setOccurrences(value.instanceCount.intValue());
                spaceTypeInformation.setSpace(value.space);
                if(withProperties) {
                    List<PropertyPerSpaceInfo> properties = propertyPerSpaceInfos.getOrDefault(value.space, Collections.emptyList());
                    spaceTypeInformation.setProperties(properties.stream().map(p -> {
                        Property property = new Property();
                        property.setIdentifier(p.propertyName);
                        property.setOccurrences(p.instanceCount.intValue());
                        return property;
                    }).sorted(Comparator.comparing(Property::getIdentifier)).toList());
                }
                return spaceTypeInformation;
            }).toList());
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
            result.add(typeInformation);
        });
        return new Paginated<>(result, (long) result.size(), result.size(), 0);
    }



}
