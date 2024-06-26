/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class QuerySpecification {

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
        private String propertyName;
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
