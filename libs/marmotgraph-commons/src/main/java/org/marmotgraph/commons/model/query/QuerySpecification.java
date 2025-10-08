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

package org.marmotgraph.commons.model.query;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
public class QuerySpecification {

    public Set<String> extractPropertyAndTypeNamespaces() {
        Set<String> collector = new HashSet<>();
        String typeNamespace = NormalizedJsonLd.getNamespace(meta.type);
        collector.add(typeNamespace);
        recursiveVisit(structure, s-> {
            if(s.path!=null){
                return (s.path.stream().map(p -> {
                    Set<String> ids = new HashSet<>();
                    ids.add(NormalizedJsonLd.getNamespace(p.getId()));
                    if(p.getTypeFilter()!=null){
                        ids.addAll(p.getTypeFilter().stream().map(t -> NormalizedJsonLd.getNamespace(t.getId())).collect(Collectors.toSet()));
                    }
                    return ids;
                }).flatMap(Collection::stream).filter(Objects::nonNull).collect(Collectors.toSet()));
            }
            return null;
        }, collector);
        return collector;
    }


    public void applyPrefixMap(Map<String, String> prefixMap) {
        String typeNamespace = NormalizedJsonLd.getNamespace(meta.type);
        if(typeNamespace!=null && prefixMap.containsKey(typeNamespace)){
            meta.type = meta.type.replace(typeNamespace+"/", prefixMap.get(typeNamespace));
        }
        recursiveVisit(structure, s-> {
            if(s.path!=null){
                s.path.forEach(p -> {
                    String idNamespace = NormalizedJsonLd.getNamespace(p.getId());
                    if(idNamespace!=null && prefixMap.containsKey(idNamespace)){
                        p.setId(p.getId().replace(idNamespace+"/", prefixMap.get(idNamespace)));
                    }
                    if(p.getTypeFilter()!=null){
                        p.getTypeFilter().forEach(
                                t -> {
                                    String typeFilterNamespace = NormalizedJsonLd.getNamespace(t.getId());
                                    if(typeFilterNamespace!=null && prefixMap.containsKey(typeFilterNamespace)){
                                        t.setId(t.getId().replace(typeFilterNamespace+"/", prefixMap.get(typeFilterNamespace)));
                                    }
                                }
                        );
                    }
                });
            }
            return null;
        }, null);

    }

    public <T, R extends Collection<T>> R recursiveVisit(List<StructureItem> structureItem, Function<StructureItem, Collection<T>> visitor, R collector) {
        if(structureItem!=null){
            structureItem.forEach(s -> {
                if(s.structure!=null){
                    recursiveVisit(s.structure, visitor, collector);
                }
                Collection<T> result = visitor.apply(s);
                if(result!=null && collector!=null){
                    collector.addAll(result);
                }
            });
        }
        return collector;
    }




    @Getter
    @Setter
    public static class ValueFilter {
        private FilterOperation op;
        private String parameter;
        private String value;

    }

    public enum FilterOperation {
        IS_EMPTY, STARTS_WITH, ENDS_WITH, CONTAINS, EQUALS, REGEX
    }


    @Getter
    @Setter
    public static class Path {
        @JsonProperty("@id")
        private String id;
        private boolean reverse;
        private List<TypeFilter> typeFilter;
        private transient String queryAlias;
    }

    @Getter
    @Setter
    public static class TypeFilter {
        @JsonProperty("@id")
        private String id;
    }





    public static class PathDeserializer extends JsonDeserializer<List<Path>>{

        @Override
        public List<Path> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            if(treeNode.isArray()){
                List<Path> result = new ArrayList<>();
                for(JsonNode n : ((JsonNode)treeNode)){
                    final Path e = deserializePathItem(jsonParser, n);
                    if(e!=null){
                        result.add(e);
                    }
                }
                return result;
            }
            else {
                final Path element = deserializePathItem(jsonParser, treeNode);
                return element != null ? Collections.singletonList(element) : null;
            }
        }

        private Path deserializePathItem(JsonParser jsonParser, TreeNode treeNode) throws JsonProcessingException {
            if(treeNode.isValueNode()){
                Path path = new Path();
                path.setId(((JsonNode)treeNode).asText());
                return path;
            }
            else if(treeNode.isObject()){
                return jsonParser.getCodec().treeToValue(treeNode, Path.class);
            }
            return null;
        }

    }

    @Getter
    @Setter
    public static class Meta {
        private String type;
        private String responseVocab;
    }

    @Getter
    @Setter
    public static class StructureItem {
        private JsonLdId propertyName;
        @JsonDeserialize(using = PathDeserializer.class)
        private List<Path> path;
        private List<StructureItem> structure;
        private boolean required;
        private boolean sort;
        private ValueFilter filter;
        private SingleItemStrategy singleValue;

    }

    public enum SingleItemStrategy {
        FIRST, CONCAT //Does only make sense on leafs -> do we even need it?
    }


    private Meta meta;

    private List<StructureItem> structure;



}
