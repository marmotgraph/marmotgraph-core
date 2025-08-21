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
import org.marmotgraph.commons.api.graphDB.GraphDB;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesMinimalData;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.query.KgQuery;
import org.marmotgraph.graphdb.neo4j.service.Neo4jService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Profile("neo4j")
@Component
@AllArgsConstructor
public class GraphDBAPI implements GraphDB.Client {

    private final Neo4jService service;

    @Override
    public void delete(UUID instanceId, SpaceName spaceName, DataStage dataStage) {
        service.delete(instanceId, dataStage);
    }

    @Override
    public void upsert(UUID instanceId, SpaceName spaceName, NormalizedJsonLd payload, DataStage stage) {
        service.upsert(instanceId, stage, payload);
    }

    @Override
    public StreamedQueryResult executeQuery(KgQuery query, Map<String, String> params, PaginationParam paginationParam){
        throw new NotImplementedException();
    }

    @Override
    @ExposesMinimalData
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage) {
        throw new NotImplementedException();
    }

    @Override
    public Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesMinimalData
    public ScopeElement getScopeForInstance(String space, UUID id, DataStage stage, boolean applyRestrictions){
        throw new NotImplementedException();
    }
}
