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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.query.QuerySpecification;
import org.marmotgraph.graphdb.neo4j.Neo4J;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Neo4J
@Service
@AllArgsConstructor
public class QueryToCypherService {

    private final JsonAdapter jsonAdapter;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public Tuple<String, Map<String, String>> createCypherQuery(DataStage stage, QuerySpecification query, PaginationParam paginationParam) {
        Tuple<String, Map<String, String>> cypherQuery = new QueryTranslator(stage, query, paginationParam).createCypherQuery();
        return cypherQuery;
    }

    private static class QueryTranslator {

        private final StringBuilder cypher = new StringBuilder();
        private final Map<String, String> aliasMap = new HashMap<>();
        private final Map<String, String> substitutionMap;
        private final PaginationParam paginationParam;
        private final QuerySpecification query;
        private final String responseVocab;



        public QueryTranslator(DataStage stage, QuerySpecification query, PaginationParam paginationParam) {
             this.substitutionMap = Map.of(
                    "stage", Neo4JCommons.getStageLabel(stage),
                    "rootType", ":" + Neo4JCommons.sanitizeLabel(query.getMeta().getType())
            );
            this.paginationParam = paginationParam;
            this.query = query;
            this.responseVocab =  query.getMeta().getResponseVocab();
        }

        private Tuple<String, Map<String, String>> createCypherQuery() {
            cypher.append("MATCH (root${stage}${rootType})\n");
            handleStructure(query.getStructure(), new LinkedHashMap<>(), "root", new ArrayList<>(List.of("root")));
            return new Tuple<>(new StringSubstitutor(this.substitutionMap).replace(cypher), aliasMap);
        }

        private void returnStatement(Map<String, Object> returnMap) {
            cypher.append("RETURN {");
            List<String> innerItems = new ArrayList<>();
            returnMap.forEach((k, v) -> {
                if (v instanceof Map<?, ?>) {
                    innerItems.add(String.format("%s: [i in %ss]", k, k));
                } else if (v instanceof String) {
                    innerItems.add(String.format(String.format("%s: %s", k, v)));
                }
            });
            cypher.append(String.join(", ", innerItems));
            cypher.append('}');
        }


        private String withStatements(Map<String, Object> returnMap, List<String> path, boolean flat) {
            cypher.append("WITH ");
            cypher.append(String.join(", ", path.subList(0, path.size() - 1)));
            cypher.append(String.format(", collect(CASE WHEN %s IS NOT NULL THEN ", path.getLast()));
            if (!flat) {
                cypher.append('{');
            }
            List<String> innerItems = new ArrayList<>();
            returnMap.forEach((k, v) -> {
                if (v instanceof Map<?, ?>) {
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(k);
                    String alias = withStatements((Map<String, Object>) v, newPath, flat);
                    innerItems.add(String.format("%s: [i in %s]", k, alias));
                } else if (v instanceof String) {
                    if(flat){
                        innerItems.add((String)v);
                    }
                    else {
                        innerItems.add(String.format("%s: %s", k, v));
                    }
                }
            });
            cypher.append(String.join(", ", innerItems));
            String resultAlias = path.getLast()+"s";
            if(!flat){
                cypher.append('}');
            }
            cypher.append(" END ) AS ").append(resultAlias);
            cypher.append('\n');
            return resultAlias;
        }

        private String handleStructure(List<QuerySpecification.StructureItem> structure, Map<String, Object> returnMap, String
                                                    currentAlias, List<String> withAliases) {
            if (structure != null) {
                List<String> newWithAliases = new ArrayList<>(withAliases);
                Map<String, Object> newReturnMap = new HashMap<>();
                structure.forEach(structureItem -> {
                    //We only treat structure items containing a path
                    if (!structureItem.getPath().isEmpty()) {
                        String propertyName = structureItem.getPropertyName().getId();
                        if (StringUtils.isNotBlank(responseVocab) && propertyName.startsWith(responseVocab)) {
                            propertyName = propertyName.substring(responseVocab.length());
                        }
                        if (structureItem.getStructure() == null) {
                            if (structureItem.getPath().size() == 1) {
                                //It's a direct element and there is no further structure defined -> we expect this to be a property, not a relation
                                handleLeaf(newReturnMap, currentAlias, structureItem.getPath().getFirst(), propertyName, null);
                            } else {
                                // it's a leaf but we first need to walk the path...

//MATCH (root:_STG_PRGRS:ma_o_t_Country)
//OPTIONAL MATCH (root)-[r2 {property: 'ma_o_p_officialLanguages'}]->(i3)
//OPTIONAL MATCH (i3)<-[r5 {property: 'ma_o_p_dialectOf'}]-(i6)
//OPTIONAL MATCH (i6)<-[r6 {property: 'ma_o_p_language'}]-(i7)
//WITH root, i3, i6, collect(CASE WHEN i7 IS NOT NULL THEN {i7a: i7.ma_o_p_value} END) as i7s
//WITH root, i3, collect(CASE WHEN i6 IS NOT NULL THEN {i6: i6.ma_o_p_name, i7: i7s} END) as i6s
//WITH root, collect(CASE WHEN i3 IS NOT NULL THEN {i4: i3.ma_o_p_name, i6: i6s} END) AS i3s
//OPTIONAL MATCH (root)<-[r7 {property: 'ma_o_p_countries'}]-(i8)
//WITH root, i3s, collect(CASE WHEN i8 IS NOT NULL THEN {i9: i8.ma_o_p_name} END) AS i8s
//RETURN {i0: root._id, i1: root.ma_o_p_name, i3: [i in i3s], i8: i8s}
                                String parentAlias = currentAlias;
                                List<String> innerAliases = new ArrayList<>(newWithAliases);
                                List<QuerySpecification.Path> subpath = structureItem.getPath().subList(0, structureItem.getPath().size() - 1);
                                for (QuerySpecification.Path structureItemPath : subpath) {
                                    parentAlias = handleRelation(structureItemPath, parentAlias, structureItem.isRequired());
                                    innerAliases.add(parentAlias);
                                }
                                Map<String, Object> m = new HashMap<>();
                                handleLeaf(m, parentAlias, structureItem.getPath().getLast(), propertyName, parentAlias);
                                String resultAlias = withStatements(m, innerAliases, true);
                                newReturnMap.put(parentAlias, resultAlias);
                            }
                        } else {
                            //It's a substructure, so we need to traverse the graph
                            String parentAlias = currentAlias;
                            List<String> innerAliases = new ArrayList<>(newWithAliases);
                            for (QuerySpecification.Path structureItemPath : structureItem.getPath()) {
                                parentAlias = handleRelation(structureItemPath, parentAlias, structureItem.isRequired());
                                innerAliases.add(parentAlias);
                            }
                            aliasMap.put(parentAlias, propertyName);
                            String substructureAlias = handleStructure(structureItem.getStructure(), newReturnMap, parentAlias, innerAliases);
                            newReturnMap.put(parentAlias, substructureAlias);
                            newWithAliases.add(substructureAlias);
                        }
                    }
                });
                if(currentAlias.equals("root")){
                    returnStatement(newReturnMap);
                }
                else{
                    String resultAlias = withStatements(newReturnMap, newWithAliases, false);
                    newReturnMap.put(currentAlias, resultAlias);
                    return resultAlias;
                }
            }
            return null;
        }

        private void handleLeaf(Map<String, Object> returnMap, String currentAlias, QuerySpecification.Path path, String propertyName, String alias) {
            String a = alias == null ? String.format("i%d", aliasMap.size()) : alias;
            aliasMap.put(a, propertyName);
            returnMap.put(a, String.format("%s.%s", currentAlias, Neo4JCommons.sanitizeLabel(path.getId())));
        }

        private String handleRelation(QuerySpecification.Path structureItemPath, String parentAlias, boolean required) {
            String relationAlias = String.format("r%d", aliasMap.size());
            aliasMap.put(relationAlias, null);
            String targetAlias = String.format("i%d", aliasMap.size());
            aliasMap.put(targetAlias, null);
            StringBuilder snippet = new StringBuilder();
            if(!required){
                snippet.append("OPTIONAL ");
            }
            snippet.append("MATCH (${currentAlias})");
            snippet.append(structureItemPath.isReverse() ? "<-" : "-");
            snippet.append("[${relationAlias} {property: '${relationName}'}]");
            snippet.append(structureItemPath.isReverse() ? "-" : "->");
            snippet.append("(${targetAlias})\n");
            cypher.append(new StringSubstitutor(Map.of(
                    "currentAlias", parentAlias,
                    "relationAlias", relationAlias,
                    "relationName", Neo4JCommons.sanitizeLabel(structureItemPath.getId()),
                    "targetAlias", targetAlias
            )).replace(snippet));
            return targetAlias;
        }
    }



}
