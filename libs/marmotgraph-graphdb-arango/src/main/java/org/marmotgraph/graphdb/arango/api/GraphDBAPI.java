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

package org.marmotgraph.graphdb.arango.api;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesMinimalData;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.model.query.QuerySpecification;
import org.marmotgraph.commons.model.relations.IncomingRelation;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.graphdb.arango.Arango;
import org.marmotgraph.graphdb.arango.ingestion.controller.TodoListProcessor;
import org.marmotgraph.graphdb.arango.instances.controller.DocumentsRepository;
import org.marmotgraph.graphdb.arango.instances.controller.IncomingLinksRepository;
import org.marmotgraph.graphdb.arango.instances.controller.NeighborsRepository;
import org.marmotgraph.graphdb.arango.instances.controller.ScopeRepository;
import org.marmotgraph.graphdb.arango.queries.controller.QueryController;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Arango
@Service
@AllArgsConstructor
public class GraphDBAPI implements org.marmotgraph.graphdb.GraphDB {

    private final ScopeRepository scope;
    private final IncomingLinksRepository incomingLinks;
    private final NeighborsRepository neighbors;
    private final TodoListProcessor todoListProcessor;
    private final AuthContext authContext;
    private final QueryController queryController;
    private final DocumentsRepository documents;

    @Override
    public void delete(UUID instanceId, SpaceName spaceName, DataStage stage) {
        todoListProcessor.delete(instanceId, spaceName, stage);
    }

    @Override
    public void upsert(UUID instanceId, SpaceName spaceName, NormalizedJsonLd payload, DataStage stage, Set<IncomingRelation> incomingRelations) {
        todoListProcessor.upsert(instanceId, spaceName, payload, stage);
    }

    @Override
    public StreamedQueryResult executeQuery(QuerySpecification query, DataStage stage, Map<String, String> params, PaginationParam paginationParam){
//        UserWithRoles userWithRoles = authContext.getUserWithRoles();
//        checkPermissionForQueryExecution(userWithRoles);
//        return queryController.queryToStream(userWithRoles, query, paginationParam, params, false);
        throw new NotImplementedException();
    }

    private void checkPermissionForQueryExecution(UserWithRoles userWithRoles){
        //TODO this is a client permission, not a user permission... let's see how we can handle this.
        //Functionality executeQuery = graphDBMode.isSync() ? Functionality.EXECUTE_SYNC_QUERY : Functionality.EXECUTE_QUERY;
//        if (!permissions.hasPermission(Functionality.EXECUTE_QUERY)) {
//            throw new ForbiddenException();
//        }
    }

    @Override
    @ExposesMinimalData
    public ScopeElement getScopeForInstance(String space, UUID id, DataStage stage, boolean applyRestrictions){
        return this.scope.getScopeForInstance(new SpaceName(space), id, stage, applyRestrictions);
    }
}
