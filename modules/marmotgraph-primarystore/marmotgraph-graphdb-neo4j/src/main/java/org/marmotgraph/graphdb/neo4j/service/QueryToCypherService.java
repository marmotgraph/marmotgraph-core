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
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.exceptions.InvalidRequestException;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.query.QuerySpecification;
import org.marmotgraph.commons.services.JsonAdapter;
import org.marmotgraph.graphdb.neo4j.Neo4J;
import org.marmotgraph.graphdb.neo4j.model.PreparedCypherQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;

@Neo4J
@Service
@AllArgsConstructor
public class QueryToCypherService {

    private final JsonAdapter jsonAdapter;


    public Tuple<PreparedCypherQuery, Optional<PreparedCypherQuery>> createCypherQuery(DataStage stage, QuerySpecification query, PaginationParam paginationParam, Tuple<Set<SpaceName>, Set<UUID>> accessFilter) {
        PreparedCypherQuery dataQuery = new QueryTranslator(stage, query, paginationParam, accessFilter, false).createQuery();
        if(paginationParam.isReturnTotalResults()){
            return new Tuple<>(dataQuery, Optional.of(new QueryTranslator(stage, query, paginationParam, accessFilter, true).createQuery()));
        }
        else{
            return new Tuple<>(dataQuery, Optional.empty());
        }
    }

    private static class QueryTranslator {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final StringBuilder cypher = new StringBuilder();
        private final Map<String, String> aliasMap = new HashMap<>();
        private final Map<String, String> substitutionMap;
        private final PaginationParam paginationParam;
        private final QuerySpecification query;
        private final String responseVocab;
        private final Tuple<Set<SpaceName>, Set<UUID>> accessFilter;
        private final Map<String, QuerySpecification.ValueFilter> filterParameters = new HashMap<>();
        private final boolean countQuery;
        private Boolean directAccessFilterForRoot = null;

        public QueryTranslator(DataStage stage, QuerySpecification query, PaginationParam paginationParam, Tuple<Set<SpaceName>, Set<UUID>> accessFilter, boolean countQuery) {
            this.substitutionMap = Map.of(
                    "stage", Neo4JCommons.getStageLabel(stage, ":"),
                    "rootType", ":" + Neo4JCommons.sanitizeLabel(query.getMeta().getType())
            );
            this.paginationParam = paginationParam;
            this.query = query;
            this.accessFilter = accessFilter;
            this.responseVocab = query.getMeta().getResponseVocab();
            this.countQuery = countQuery;
        }


        private String relationFilter(String alias, Optional<QuerySpecification.Path> path) {
            List<String> filters;
            if (path.isPresent()) {
                filters = Stream.of(accessFilter(alias), typeFilter(alias, path.get())).filter(Optional::isPresent).map(Optional::get).toList();
            } else {
                filters = accessFilter(alias).map(Collections::singletonList).orElse(Collections.emptyList()); //Without the path, we can only apply the accessFilter (e.g. for root)
            }
            return filters.isEmpty() ? "" : buildFilters(filters);
        }

        private static String buildFilters(List<String> filters) {
            StringBuilder filter = new StringBuilder();
            filter.append("WHERE ");
            Iterator<String> iterator = filters.iterator();
            while (iterator.hasNext()) {
                String partialFilter = iterator.next();
                filter.append('(');
                filter.append(partialFilter);
                filter.append(')');
                if (iterator.hasNext()) {
                    filter.append(" AND ");
                }
            }
            filter.append('\n');
            return filter.toString();
        }

        private Optional<String> typeFilter(String alias, QuerySpecification.Path path) {
            if (path.getTypeFilter() != null) {
                StringBuilder filter = new StringBuilder();
                Iterator<String> iterator = path.getTypeFilter().stream().map(f -> Neo4JCommons.sanitizeLabel(f.getId())).iterator();
                filter.append(String.format("any(label IN labels(%s) WHERE label IN [", alias));
                while (iterator.hasNext()) {
                    filter.append('\'').append(iterator.next()).append('\'');
                    if (iterator.hasNext()) {
                        filter.append(',');
                    }
                }
                filter.append("])");
                return Optional.of(filter.toString());
            }
            return Optional.empty();
        }

        private Optional<String> accessFilter(String alias) {
            if (accessFilter != null) {
                StringBuilder filter = new StringBuilder();
                if (!accessFilter.getA().isEmpty()) {
                    filter.append(String.format("any(label IN labels(%s) WHERE label IN allowedSpaces)", alias));
                }
                if (!accessFilter.getB().isEmpty()) {
                    if (!filter.isEmpty()) {
                        filter.append(" OR ");
                    }
                    filter.append(String.format("%s._id IN allowedInstances", alias));
                }
                return Optional.of(filter.toString());
            }
            return Optional.empty();
        }

        private PreparedCypherQuery createQuery() {
            ArrayList<String> withAliases = new ArrayList<>(List.of("root"));
            if (accessFilter != null) {
                if (!accessFilter.getA().isEmpty()) {
                    cypher.append("WITH [");
                    Iterator<String> iterator = accessFilter.getA().stream().map(s -> Neo4JCommons.getSpaceLabel(s, "", null)).filter(Objects::nonNull).iterator();
                    while (iterator.hasNext()) {
                        cypher.append('\'');
                        cypher.append(iterator.next());
                        cypher.append('\'');
                        if (iterator.hasNext()) {
                            cypher.append(',');
                        }
                    }
                    cypher.append("] AS allowedSpaces \n");
                    withAliases.add("allowedSpaces");
                }
                if (!accessFilter.getB().isEmpty()) {
                    cypher.append("WITH [");
                    Iterator<String> iterator = accessFilter.getB().stream().map(UUID::toString).iterator();
                    while (iterator.hasNext()) {
                        cypher.append('\'');
                        cypher.append(iterator.next());
                        cypher.append('\'');
                        if (iterator.hasNext()) {
                            cypher.append(',');
                        }
                    }
                    cypher.append("] AS allowedInstances \n");
                    withAliases.add("allowedInstances");
                }
            }
            cypher.append("MATCH (root${stage}${rootType})\n");
            String rootFilter = relationFilter("root", Optional.empty());
            if(StringUtils.isNotBlank(rootFilter)){
                directAccessFilterForRoot = true;
            }
            cypher.append(rootFilter);
            handleStructure(query.getStructure(), "root", withAliases);
            return new PreparedCypherQuery(new StringSubstitutor(this.substitutionMap).replace(cypher), aliasMap, filterParameters);
        }

        private String filterValue(QuerySpecification.ValueFilter valueFilter, String source) {
            if (Objects.requireNonNull(valueFilter.getOp()) == QuerySpecification.FilterOperation.IS_EMPTY) {
                return new StringSubstitutor(Map.of("source", source)).replace("${source} IS NULL OR ${source} = [] OR ${source} = {} or ${source} = \"\"");
            } else {
                Optional<String> cypherOp = translateStandardFilterOp(valueFilter.getOp());
                if (cypherOp.isPresent()) {
                    String filterParam = String.format("param%d", filterParameters.size());
                    filterParameters.put(filterParam, valueFilter);
                    return new StringSubstitutor(Map.of(
                            "source", source,
                            "operation", cypherOp.get(),
                            "filterParam", String.format("$%s", filterParam)
                    )).replace("(${source} ${operation} ${filterParam} OR any(v IN ${source} WHERE v ${operation} ${filterParam}))");
                }
            }
            throw new InvalidRequestException(String.format("Was not able to find corresponding operation for %s", valueFilter.getOp()));
        }

        private void returnStatement(Map<String, String> returnMap, Map<String, QuerySpecification.ValueFilter> filterMap, Set<String> requiredSet) {
            if (!filterMap.isEmpty() || !requiredSet.isEmpty()) {
                if(directAccessFilterForRoot != null && directAccessFilterForRoot) {
                    cypher.append("AND ");
                }
                else {
                    cypher.append("WHERE ");
                }
            }
            if (!filterMap.isEmpty()) {
                Iterator<String> iterator = filterMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String target = iterator.next();
                    String source = returnMap.get(target);
                    QuerySpecification.ValueFilter valueFilter = filterMap.get(target);
                    cypher.append(filterValue(valueFilter, source));
                    if (iterator.hasNext()) {
                        cypher.append(" AND ");
                    }
                }
            }
            if (!requiredSet.isEmpty()) {
                if (!filterMap.isEmpty()) {
                    cypher.append(" AND ");
                }
                Iterator<String> iterator = requiredSet.iterator();
                while (iterator.hasNext()) {
                    cypher.append(new StringSubstitutor(Map.of("alias", iterator.next())).replace("(${alias} IS NOT NULL AND NOT ${alias} = [] AND NOT ${alias} = {} AND NOT ${alias} = \"\")"));
                    if (iterator.hasNext()) {
                        cypher.append(" AND ");
                    }
                }
            }
            if (!filterMap.isEmpty() || !requiredSet.isEmpty()) {
                cypher.append("\n");
            }
            if(countQuery){
                cypher.append("RETURN count(root)");
            }
            else {
                if (paginationParam != null) {
                    cypher.append("ORDER BY root._id\n"); //If we paginate, we need to ensure the order to have a consistent list
                    cypher.append(String.format("SKIP %d\n", paginationParam.getFrom()));
                    if (paginationParam.getSize() != null) {
                        cypher.append(String.format("LIMIT %d\n", paginationParam.getSize()));
                    }
                }

                cypher.append("RETURN {");
                List<String> innerItems = new ArrayList<>();
                returnMap.forEach((k, v) -> {
                    innerItems.add(String.format(String.format("%s: %s", k, v)));
                });
                cypher.append(String.join(", ", innerItems));
                cypher.append('}');
            }
        }

        private Optional<String> translateStandardFilterOp(QuerySpecification.FilterOperation operation) {
            switch (operation) {
                case REGEX -> {
                    return Optional.of("=~");
                }
                case CONTAINS -> {
                    return Optional.of("CONTAINS");
                }
                case EQUALS -> {
                    return Optional.of("=");
                }
                case STARTS_WITH -> {
                    return Optional.of("STARTS WITH");
                }
                case ENDS_WITH -> {
                    return Optional.of("ENDS WITH");
                }
            }
            return Optional.empty();
        }


        private String withStatements(Map<String, String> returnMap, Map<String, QuerySpecification.ValueFilter> filterMap, List<String> path, String currentAlias, Set<String> requiredSet, boolean flat) {
            directAccessFilterForRoot = false; //There
            List<String> relevantSubPath = path.subList(0, Math.min(path.size(), path.indexOf(currentAlias) + 1));
            cypher.append("WITH ");
            cypher.append(String.join(", ", relevantSubPath.subList(0, relevantSubPath.size() - 1)));
            cypher.append(", ");
            if (!flat) {
                cypher.append("[o IN ");
            }
            cypher.append("collect(");
            cypher.append(String.format("CASE WHEN %s IS NOT NULL ", relevantSubPath.getLast()));
            if (!CollectionUtils.isEmpty(requiredSet)) {
                cypher.append("AND ");
                Iterator<String> iterator = requiredSet.iterator();
                while (iterator.hasNext()) {
                    String required = iterator.next();
                    cypher.append(String.format("%s IS NOT NULL AND NOT %s = [] AND NOT %s = {} ", required, required, required));
                    if (iterator.hasNext()) {
                        cypher.append("AND ");
                    }
                }
            }
            cypher.append("THEN ");
            List<String> innerItems = new ArrayList<>();
            List<String> filterItems = new ArrayList<>();
            returnMap.forEach((k, v) -> {
                if (flat) {
                    innerItems.add(v);
                } else {
                    QuerySpecification.ValueFilter valueFilter = filterMap.get(k);
                    if (valueFilter != null) {
                        filterItems.add(k);
                        innerItems.add(String.format("%s: CASE WHEN %s THEN %s END", k, filterValue(valueFilter, v), v));
                    } else {
                        innerItems.add(String.format("%s: %s", k, v));
                    }
                }
            });
            if (!flat) {
                if (!filterItems.isEmpty()) {
                    cypher.append("apoc.map.clean(");
                }
                cypher.append('{');
            }
            cypher.append(String.join(", ", innerItems));
            String resultAlias = relevantSubPath.getLast() + "s";
            if (!flat) {
                cypher.append('}');
                if (!filterItems.isEmpty()) {
                    cypher.append(", [], [NULL])");
                }
            }
            cypher.append(" END ) ");
            if (!flat) {
                cypher.append("WHERE size(keys(o))>0 ] ");
            }
            cypher.append("AS ").append(resultAlias);
            cypher.append('\n');
            return resultAlias;
        }

        private boolean isRelevantForQuery(String currentAlias, QuerySpecification.StructureItem structureItem){
            return !countQuery || (currentAlias.equals("root") && structureItem.getFilter() != null) || structureItem.isRequired();
        }

        private String handleStructure(List<QuerySpecification.StructureItem> structure, String
                currentAlias, List<String> withAliases) {
            if (structure != null) {
                List<String> newWithAliases = new ArrayList<>(withAliases);
                Map<String, String> returnMap = new HashMap<>();
                Map<String, QuerySpecification.ValueFilter> filterMap = new HashMap<>();
                Set<String> requiredSet = new HashSet<>();
                structure.forEach(structureItem -> {
                        //We only treat structure items containing a path
                        if (!structureItem.getPath().isEmpty() && isRelevantForQuery(currentAlias, structureItem)) {
                            String propertyName = structureItem.getPropertyName().getId();
                            if (StringUtils.isNotBlank(responseVocab) && propertyName.startsWith(responseVocab)) {
                                propertyName = propertyName.substring(responseVocab.length());
                            }
                            if (structureItem.getStructure() == null) {
                                if (structureItem.getPath().size() == 1) {
                                    if(!countQuery || (currentAlias.equals("root") && structureItem.getFilter() != null) || structureItem.isRequired()) {
                                        //It's a direct element and there is no further structure defined -> we expect this to be a property, not a relation
                                        String alias = handleLeaf(returnMap, currentAlias, structureItem.getPath().getFirst(), propertyName, null);
                                        if (structureItem.getFilter() != null) {
                                            filterMap.put(alias, structureItem.getFilter());
                                        }
                                        if (structureItem.isRequired()) {
                                            requiredSet.add(alias);
                                        }
                                    }
                                } else {
                                    // it's a leaf but we first need to walk the path...
                                    String parentAlias = currentAlias;
                                    List<String> innerAliases = new ArrayList<>(newWithAliases);
                                    List<QuerySpecification.Path> subpath = structureItem.getPath().subList(0, structureItem.getPath().size() - 1);
                                    for (QuerySpecification.Path structureItemPath : subpath) {
                                        parentAlias = handleRelation(structureItemPath, parentAlias, structureItem.isRequired());
                                        innerAliases.add(parentAlias);
                                    }
                                    Map<String, String> m = new HashMap<>();
                                    Map<String, QuerySpecification.ValueFilter> f = new HashMap<>();
                                    String alias = handleLeaf(m, parentAlias, structureItem.getPath().getLast(), propertyName, parentAlias);
                                    if (structureItem.getFilter() != null) {
                                        f.put(alias, structureItem.getFilter());
                                    }
                                    String resultAlias = withStatements(m, f, innerAliases, alias, Collections.emptySet(), true);
                                    if (structureItem.isRequired()) {
                                        requiredSet.add(resultAlias);
                                    }
                                    returnMap.put(parentAlias, resultAlias);
                                    newWithAliases.add(resultAlias);
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
                                String substructureAlias = handleStructure(structureItem.getStructure(), parentAlias, innerAliases);
                                returnMap.put(parentAlias, substructureAlias);
                                newWithAliases.add(substructureAlias);
                                if (structureItem.isRequired()) {
                                    requiredSet.add(substructureAlias);
                                }
                            }

                    }
                });
                if (currentAlias.equals("root")) {
                    returnStatement(returnMap, filterMap, requiredSet);
                } else {
                    String resultAlias = withStatements(returnMap, filterMap, newWithAliases, currentAlias, requiredSet, false);
                    returnMap.put(currentAlias, resultAlias);
                    return resultAlias;
                }
            }
            return null;
        }

        private String handleLeaf(Map<String, String> returnMap, String currentAlias, QuerySpecification.Path path, String propertyName, String alias) {
            String a = alias == null ? String.format("i%d", aliasMap.size()) : alias;
            aliasMap.put(a, propertyName);
            returnMap.put(a, String.format("%s.%s", currentAlias, Neo4JCommons.sanitizeLabel(path.getId())));
            return a;
        }

        private String handleRelation(QuerySpecification.Path structureItemPath, String parentAlias, boolean required) {
            String relationAlias = String.format("r%d", aliasMap.size());
            aliasMap.put(relationAlias, null);
            String targetAlias = String.format("i%d", aliasMap.size());
            aliasMap.put(targetAlias, null);
            StringBuilder snippet = new StringBuilder();
            snippet.append("OPTIONAL MATCH (${currentAlias})"); //We go with optional matches since our filter mechanisms are more detailed
            snippet.append(structureItemPath.isReverse() ? "<-" : "-");
            snippet.append("[${relationAlias} {property: '${relationName}'}]");
            snippet.append(structureItemPath.isReverse() ? "-" : "->");
            snippet.append("(${targetAlias})\n");
            snippet.append(relationFilter("${targetAlias}", Optional.of(structureItemPath)));
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
