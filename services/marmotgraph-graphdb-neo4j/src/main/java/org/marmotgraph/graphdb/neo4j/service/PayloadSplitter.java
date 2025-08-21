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

package org.marmotgraph.graphdb.neo4j.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PayloadSplitter {

    @Getter
    @AllArgsConstructor
    static class Instance {
        private final Map<String, Object> payload;
        private final boolean embedded;
        private final boolean unresolved;
        private final String alias;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    static class Relation {
        private final String from;
        private final String to;
        private final String relationName;
        private final int orderNumber;
        private UUID lifecycleId;
    }

    @AllArgsConstructor
    @Getter
    static class PayloadSplit {
        private List<Instance> instances;
        private List<Relation> relations;
    }

    @Getter
    private static class Collector{
        private final List<Instance> instances = new ArrayList<>();
        private final List<Relation> relations = new ArrayList<>();
        private int externalLinkCounter = 0;
        private final UUID lifecycleId;

        private Collector(UUID lifecycleId) {
            this.lifecycleId = lifecycleId;
        }

        private void addInstance(Instance instance) {
            instance.payload.put(Neo4jService.LIFECYCLE_ALIAS, lifecycleId.toString());
            this.instances.add(instance);
        }

        private void addRelation(Relation relation) {
            relation.setLifecycleId(lifecycleId);
            this.relations.add(relation);
        }

        private void incrementExternalLinkCounter() {
            externalLinkCounter++;
        }

        private PayloadSplit toSplit(){
            return new PayloadSplit(instances, relations);
        }

    }


    PayloadSplit createEntities(NormalizedJsonLd normalizedJsonLd, UUID lifecycleUUID) {
        Collector collector = new Collector(lifecycleUUID);
        String alias = "a";
        Map<String, Object> rootInstance = new LinkedHashMap<>();
        for (String k : normalizedJsonLd.keySet()) {
            Object r = createEntitiesRec(alias, 0, k, normalizedJsonLd.get(k), collector);
            if (r != null) {
                rootInstance.put(k, r);
            }
        }
        rootInstance.put(Neo4jService.UUID_ALIAS, lifecycleUUID.toString());
        collector.addInstance(new Instance(rootInstance, false, false, alias));
        return collector.toSplit();
    }

    private Object createEntitiesRec(String parentAlias, int orderNumber, String k, Object obj, Collector collector) {
        if (obj instanceof Map<?, ?> objMap) {
            if (objMap.containsKey("@id") && objMap.get("@id") instanceof String) {
                System.out.printf("Found relation in property %s pointing to %s%n", k, objMap.get("@id"));
                collector.incrementExternalLinkCounter();
                String targetAlias = String.format("b%s", collector.getExternalLinkCounter());
                collector.addRelation(new Relation(parentAlias, targetAlias, k, orderNumber, null));
                collector.addInstance(new Instance(new HashMap<>(Map.of("@id", objMap.get("@id"))), false, true, targetAlias));
                return null;
            } else {
                System.out.printf("Found embedded instance in property %s", k);
                String alias = String.format("a%s", collector.getInstances().size() + 1);
                Map<String, Object> resultMap = new LinkedHashMap<>();
                collector.addInstance(new Instance(resultMap, true, false, alias));
                for (Object key : objMap.keySet()) {
                    Object result = createEntitiesRec(alias, 0, (String) key, objMap.get(key), collector);
                    if (result != null) {
                        resultMap.put((String) key, result);
                    }
                }
                collector.addRelation(new Relation(parentAlias, alias, k, orderNumber, null));

                return null;
            }
        } else if (obj instanceof List<?> objList) {
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < objList.size(); i++) {
                Object r = createEntitiesRec(parentAlias, i, k, objList.get(i), collector);
                if (r != null) {
                    result.add(r);
                }
            }
            // The original list was not empty but the new one is means that we have resolved some relations.
            // Since the list is empty now, we can assume it only consisted of relations which allows us to ignore the property.
            return !objList.isEmpty() && result.isEmpty() ? null : result;
        } else {
            return obj;
        }
    }


}
