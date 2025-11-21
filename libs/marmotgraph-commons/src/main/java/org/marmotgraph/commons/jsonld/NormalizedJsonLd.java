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

package org.marmotgraph.commons.jsonld;

import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.relations.IncomingRelation;
import org.marmotgraph.commons.model.relations.OutgoingRelation;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A  JSON-LD in compacted format with applied context. It is the JSON-LD which comes the closest to basic JSON.
 */

public class NormalizedJsonLd extends JsonLdDoc {


    public final static String ID_ALIAS = "_id";
    public final static String LIFECYCLE_ALIAS = "_lifecycleId";
    public final static String SPACE_ALIAS = "_space";
    public final static String TYPE_ALIAS = "_types";

    public NormalizedJsonLd() {
    }

    public NormalizedJsonLd(Map<String, ?> m) {
        super(m);
    }

    public static class FieldUpdateTimes extends HashMap<String, ZonedDateTime> {
        public FieldUpdateTimes(Map<String, ZonedDateTime> map) {
            super(map);
        }

        public FieldUpdateTimes() {
            super();
        }
    }

    @Getter
    @Setter
    private FieldUpdateTimes fieldUpdateTimes;

    public Set<String> allIdentifiersIncludingId() {
        Set<String> identifiers = new HashSet<>(identifiers());
        if (id() != null) {
            identifiers.add(id().toString());
        }
        return identifiers;
    }

    public void applyVocab(String vocab) {
        JsonLdDoc context = new JsonLdDoc();
        context.addProperty(JsonLdConsts.VOCAB, vocab);
        addProperty(JsonLdConsts.CONTEXT, context);
        visitKeys((map, key) -> {
            if (key.startsWith(vocab)) {
                map.put(key.substring(vocab.length()), map.get(key));
                map.remove(key);
            }
        });
    }

    public NormalizedJsonLd renameSpace(SpaceName privateSpace, boolean invitation) {
        if (privateSpace != null || invitation) {
            visitKeys((map, key) -> {
                if (key.equals(EBRAINSVocabulary.META_SPACE)) {
                    if (privateSpace != null && privateSpace.getName().equals(map.get(key))) {
                        map.put(key, SpaceName.PRIVATE_SPACE);
                    } else if (invitation) {
                        map.put(key, SpaceName.REVIEW_SPACE);
                    }
                }
            });
        }
        return this;
    }


    public Set<Tuple<String, String>> findOutgoingRelations() {
        Set<Tuple<String, String>> result = new HashSet<>();
        recursiveOutgoingRelationDetection(this, null, true, result);
        return result;
    }

    public static boolean isEmbeddedId(String id) {
        String[] split = id.split("_emb_");
        if (split.length == 2) {
            try {
                UUID.fromString(split[0]);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }

    public NormalizedJsonLd resolveOutgoingRelations(Set<OutgoingRelation> outgoingRelations) {
        Map<String, Optional<UUID>> relationsMap = outgoingRelations.stream().collect(Collectors.toMap(OutgoingRelation::targetReference, v -> Optional.ofNullable(v.resolvedTarget())));
        List<String> collector = new ArrayList<>();
        recursiveVisitOfProperties(this, Collections.emptyList(), this, (name, value, path, parentMap, orderNumber) -> {
            if (value instanceof Map map) {
                boolean rootLevel = path.isEmpty();
                if (!rootLevel && map.containsKey(JsonLdConsts.ID)) {
                    //A json ld id which is not on the root level means an outgoing relation
                    Object referenceValue = map.get(JsonLdConsts.ID);
                    if (referenceValue instanceof String && !isEmbeddedId((String) referenceValue)) {
                        Optional<UUID> uuid = relationsMap.get((String) referenceValue);
                        if (uuid.isPresent()) {
                            map.put(JsonLdConsts.ID, uuid.get().toString());
                        } else {
                            map.remove(JsonLdConsts.ID);
                        }
                    }
                }
            }
            return null;
        }, null, collector);
        return this;
    }

    public interface PropertyVisitor<T> {
        T handleProperty(String name, Object value, List<String> path, Map<String, Object> parentMap, Integer orderNumber);
    }

    //TODO move to a better place
    public static String getNamespace(String propertyName) {
        try {
            URI uri = URI.create(propertyName);
            int i = uri.toString().lastIndexOf("/");
            if (i != -1) {
                return propertyName.substring(0, i);
            }
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        return null;
    }

    public Set<String> extractPropertyAndTypeNamespaces() {
        List<List<String>> propertyNamespaces = recursiveVisitOfProperties(this, Collections.emptyList(), this, (name, value, path, parentMap, orderNumber) -> {
            if (JsonLdConsts.TYPE.equals(name)) {
                if (value instanceof String) {
                    String namespace = getNamespace((String) value);
                    return namespace != null ? Collections.singletonList(namespace) : null;
                } else if (value instanceof Collection<?>) {
                    return ((Collection<?>) value).stream().filter(v -> v instanceof String).map(v -> getNamespace((String) v)).filter(Objects::nonNull).toList();
                }
                return null;
            } else {
                String namespace = getNamespace(name);
                return namespace != null ? Collections.singletonList(namespace) : null;
            }
        }, null, new ArrayList<>());
        return Stream.concat(types().stream().map(NormalizedJsonLd::getNamespace).filter(Objects::nonNull), propertyNamespaces.stream().flatMap(Collection::stream)).collect(Collectors.toSet());
    }

    public void applyPrefixMap(Map<String, String> prefixMap) {
        recursiveVisitOfProperties(this, Collections.emptyList(), this, (name, value, path, parentMap, orderNumber) -> {
            if (JsonLdConsts.TYPE.equals(name)) {
                Stream<String> typesStream = null;
                if (value instanceof String) {
                    typesStream = Stream.of((String) value);
                } else if (value instanceof Collection<?>) {
                    typesStream = ((Collection<?>) value).stream().filter(v -> v instanceof String).map(v -> ((String) v));
                }
                if (typesStream != null) {
                    parentMap.put(TYPE_ALIAS, typesStream.map(t -> {
                        String namespace = getNamespace(t);
                        String prefix = prefixMap.get(namespace);
                        if (namespace != null && prefix != null) {
                            return t.replace(namespace + "/", prefix);
                        }
                        return null;
                    }).filter(Objects::nonNull).toList());
                }
            } else {
                String namespace = getNamespace(name);
                if (namespace != null) {
                    String prefix = prefixMap.get(namespace);
                    if (prefix != null) {
                        String prefixedProperty = name.replace(namespace + "/", prefix);
                        parentMap.put(prefixedProperty, value);
                        parentMap.remove(name);
                    }
                }
            }
            return null;
        }, null, new ArrayList<>());
    }


    public Set<IncomingRelation> fetchInformationForIncomingRelation(UUID instanceId, UUID targetId, String targetReference) {
        return new HashSet<>(recursiveVisitOfProperties(this, Collections.emptyList(), this, (name, value, path, parentMap, orderNumber) -> {
            if (!path.isEmpty() && value instanceof Map<?, ?> map && map.containsKey(JsonLdConsts.ID) && map.get(JsonLdConsts.ID).equals(targetReference) && parentMap.containsKey(NormalizedJsonLd.ID_ALIAS)) {
                return new IncomingRelation(instanceId, name, orderNumber != null ? orderNumber : 0, (String) parentMap.get(NormalizedJsonLd.ID_ALIAS), targetId);
            }
            return null;
        }, null, new ArrayList<>()));
    }


    public static <T> List<T> recursiveVisitOfProperties(Object object, List<String> path, Map<String, Object> parentMap, PropertyVisitor<T> visitor, Integer orderNumber, List<T> collector) {
          if (object instanceof Map<?, ?> map) {
            for (Object o : new HashSet<>(map.keySet())) {
                String key = (String) o;
                List<String> p = new ArrayList<>(path);
                p.add(key);
                recursiveVisitOfProperties(map.get(key), p, (Map<String, Object>) map, visitor, null, collector);
            }
        } else if (object instanceof Collection<?> collection) {
            if (collection instanceof List<?> collectionAsList) {
                for (int i = 0; i < collectionAsList.size(); i++) {
                    recursiveVisitOfProperties(collectionAsList.get(i), path, parentMap, visitor, i, collector);
                }
            } else {
                collection.forEach(o -> recursiveVisitOfProperties(o, path, parentMap, visitor, null, collector));
            }
        }
        if (!path.isEmpty()) {
            T result = visitor.handleProperty(path.getLast(), object, path, parentMap, orderNumber);
            if (result != null && collector != null) {
                collector.add(result);
            }
        }
        return collector;
    }

    public void addIdsToEmbedded(UUID instanceId) {
        ArrayList<Object> collector = new ArrayList<>();
        put(NormalizedJsonLd.ID_ALIAS, instanceId.toString());
        recursiveVisitOfProperties(this, Collections.emptyList(), this, (name, value, path, parentMap, orderNumber) -> {
            if (value instanceof Map map && !map.containsKey(JsonLdConsts.ID)) {
                //It's an embedded instance
                String generatedEmbeddedInstanceId = String.format("%s_%s", instanceId, collector.size());
                map.put(NormalizedJsonLd.ID_ALIAS, generatedEmbeddedInstanceId);
                return generatedEmbeddedInstanceId;
            }
            return null;
        }, null, collector);
    }

    private void recursiveOutgoingRelationDetection(Object object, String propertyName, boolean rootLevel, Set<Tuple<String, String>> collector) {
        if (object instanceof Map<?, ?> map) {
            if (!rootLevel && map.containsKey(JsonLdConsts.ID)) {
                //A json ld id which is not on the root level means an outgoing relation
                Object referenceValue = map.get(JsonLdConsts.ID);
                if (referenceValue instanceof String) {
                    collector.add(new Tuple<>(propertyName, (String) referenceValue));
                }
            } else {
                map.keySet().stream().filter(k -> k instanceof String).forEach(k -> recursiveOutgoingRelationDetection(map.get(k), (String)k, false, collector));
            }
        } else if (object instanceof List) {
            ((List<?>) object).forEach(o -> recursiveOutgoingRelationDetection(o, propertyName, false, collector));
        }
    }

    private boolean isEmbeddedItem(Object object) {
        if (object instanceof Map<?, ?>) {
            if (!((Map<?, ?>) object).containsKey(JsonLdConsts.ID)) {
                //It's a map but not a reference -> it's an embedded element
                return true;
            }
        }
        return false;
    }

    public void removeEmbedded() {
        visitKeys((map, key) -> {
            Object property = map.get(key);
            if (isEmbeddedItem(property)) {
                map.remove(key);
            } else if (property instanceof List<?>) {
                List<?> remainingItems = ((List<?>) property).stream().filter(o -> !isEmbeddedItem(o)).toList();
                if (remainingItems.isEmpty()) {
                    map.remove(key);
                } else {
                    map.put(key, remainingItems);
                }
            }
        });

    }


    public interface Visitor {
        void visit(String key, Object value, Map<?, ?> parentMap);
    }

    public void walk(Visitor visitor){
        walk(this, visitor, this);
    }

    private static void walk(Object obj, Visitor visitor, Map<?, ?> parentMap) {
        if (obj instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                visitor.visit(entry.getKey().toString(), entry.getValue(), map);
                walk(entry.getValue(), visitor, map);
            }
        } else if (obj instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                walk(element, visitor, parentMap);
            }
        }
    }


}
