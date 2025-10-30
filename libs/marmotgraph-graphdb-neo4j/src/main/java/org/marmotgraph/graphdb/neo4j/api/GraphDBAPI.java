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

package org.marmotgraph.graphdb.neo4j.api;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesMinimalData;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.model.query.QuerySpecification;
import org.marmotgraph.commons.model.relations.IncomingRelation;
import org.marmotgraph.graphdb.GraphDB;
import org.marmotgraph.graphdb.neo4j.Neo4J;
import org.marmotgraph.graphdb.neo4j.model.PreparedCypherQuery;
import org.marmotgraph.graphdb.neo4j.service.Neo4jService;
import org.marmotgraph.graphdb.neo4j.service.QueryToCypherService;
import org.springframework.stereotype.Service;

import java.util.*;

@Neo4J
@Service
@AllArgsConstructor
public class GraphDBAPI implements GraphDB {

    private final Neo4jService service;
    private final QueryToCypherService queryToCypherService;

    @Override
    public void delete(UUID instanceId, SpaceName spaceName, DataStage dataStage) {
        service.delete(instanceId, dataStage);
    }

    @Override
    public void upsert(UUID instanceId, SpaceName spaceName, NormalizedJsonLd payload, DataStage stage, Set<IncomingRelation> incomingRelations) {
        service.upsert(instanceId, stage, spaceName, payload, incomingRelations);
    }

    @Override
    public Tuple<Collection<NormalizedJsonLd>, Long> executeQuery(QuerySpecification query, DataStage stage, Map<String, String> params, PaginationParam paginationParam, Tuple<Set<SpaceName>, Set<UUID>> accessFilter){
        PreparedCypherQuery cypherQuery = queryToCypherService.createCypherQuery(stage, query, paginationParam, accessFilter);
        Map<String, String> evaluatedParameters = new HashMap<>();
        cypherQuery.filterMap().forEach((k, v) -> {
            String value = null;
            if(StringUtils.isNotBlank(v.getParameter())){
                String parametrized = params.get(v.getParameter().trim());
                if(StringUtils.isNotBlank(parametrized)){
                    value = parametrized;
                }
            }
            if(value == null && StringUtils.isNotBlank(v.getValue())){
                value = v.getValue().trim();
            }
            evaluatedParameters.put(k, value);
        });

        //TODO fetch total results
        return new Tuple<>(service.query(cypherQuery.query(), evaluatedParameters, cypherQuery.aliasMap(), paginationParam), null);
    }

    @Override
    @ExposesMinimalData
    public ScopeElement getScopeForInstance(String space, UUID id, DataStage stage, boolean applyRestrictions){
        throw new NotImplementedException();
    }
}
