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

package org.marmotgraph.core.api.v3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.marmotgraph.auth.api.AuthContext;
import org.marmotgraph.commons.constants.EBRAINSVocabulary;
import org.marmotgraph.commons.exceptions.InstanceNotFoundException;
import org.marmotgraph.commons.exceptions.ResultBasedException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.query.MarmotGraphQuery;
import org.marmotgraph.commons.services.JsonLd;
import org.marmotgraph.core.api.Version;
import org.marmotgraph.core.api.config.openApi.Simple;
import org.marmotgraph.core.controller.CoreInstanceController;
import org.marmotgraph.core.controller.CoreQueryController;
import org.marmotgraph.core.model.ExposedStage;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The query API allows to execute and manage queries on top of the MarmotGraph. This is the main interface for reading clients.
 */
@AllArgsConstructor
@RestController
@RequestMapping(Version.V3 + "/queries")
public class QueriesV3 {

    private final CoreQueryController queryController;

    private final AuthContext authContext;

    private final JsonLd jsonLd;

    private final CoreInstanceController instances;


    @Operation(summary = "List the queries and filter them by root type and/or text in the label, name or description")
    @GetMapping
    @Simple
    public PaginatedResult<NormalizedJsonLd> listQueriesPerRootType(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "type", required = false) String rootType, @RequestParam(value = "search", required = false) String search) {
        Paginated<NormalizedJsonLd> data;
        if (rootType != null) {
            data = queryController.listQueriesPerRootType(search, new Type(rootType), paginationParam);
        } else {
            data = queryController.listQueries(search, paginationParam);
        }
        handleResponse(data);
        return PaginatedResult.ok(data);
    }

    private void handleResponse(Paginated<NormalizedJsonLd> data) {
        final SpaceName privateSpace = authContext.getUserWithRoles().getPrivateSpace();
        data.getData().forEach(d -> d.renameSpace(privateSpace, queryController.isInvited(d)));
    }


    @Operation(summary = "Execute the query in the payload (e.g. for execution before saving with the MarmotGraph QueryBuilder)")
    @PostMapping
    @Simple
    public PaginatedStreamResult<? extends JsonLdDoc> runDynamicQuery(@RequestBody JsonLdDoc query, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "instanceId", required = false) UUID instanceId, @RequestParam(value = "restrictToSpaces", required = false) List<String> restrictToSpaces, @RequestParam(defaultValue = "{}") Map<String, String> allRequestParams) {
        //Remove the non-dynamic parameters from the map
        allRequestParams.remove("stage");
        allRequestParams.remove("instanceId");
        allRequestParams.remove("from");
        allRequestParams.remove("size");
        NormalizedJsonLd normalizedJsonLd = jsonLd.normalize(query, true);
        MarmotGraphQuery q = new MarmotGraphQuery(normalizedJsonLd, stage.getStage());
        q.setIdRestriction(instances.resolveId(instanceId, stage.getStage()));
        if (restrictToSpaces != null) {
            q.setRestrictToSpaces(restrictToSpaces.stream().filter(Objects::nonNull).map(r -> SpaceName.getInternalSpaceName(r, authContext.getUserWithRoles().getPrivateSpace())).collect(Collectors.toList()));
        }
        final PaginatedStream<? extends JsonLdDoc> data = queryController.executeQuery(q, allRequestParams, paginationParam);
        return PaginatedStreamResult.ok(data);
    }


    @Operation(summary = "Get the query specification with the given query id in a specific space")
    @GetMapping("/{queryId}")
    @Simple
    public ResponseEntity<ResultWithExecutionDetails<NormalizedJsonLd>> getQuerySpecification(@PathVariable("queryId") UUID queryId) {
        InstanceId instanceId = instances.resolveId(queryId, DataStage.IN_PROGRESS);
        if (instanceId != null) {
            NormalizedJsonLd kgQuery = queryController.fetchQueryById(instanceId);
            if (kgQuery == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ResultWithExecutionDetails.ok(kgQuery.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), queryController.isInvited(kgQuery))));
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Remove a query specification")
    @DeleteMapping("/{queryId}")
    @Simple
    public void removeQuery(@PathVariable("queryId") UUID queryId) {
        queryController.deleteQuery(queryId);
    }

    @Operation(summary = "Create or save a query specification")
    @PutMapping("/{queryId}")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> saveQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam(value = "space", required = false) @Parameter(description = "Required only when the instance is created to specify where it should be stored (" + SpaceName.PRIVATE_SPACE + " for your private space) - but not if it's updated.") String space) {
        NormalizedJsonLd normalizedJsonLd = jsonLd.normalize(query, true);
        normalizedJsonLd.addTypes(EBRAINSVocabulary.META_QUERY_TYPE);
        InstanceId resolveId = instances.resolveId(queryId, DataStage.IN_PROGRESS);
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if (resolveId != null) {
            if (spaceName != null && !resolveId.getSpace().equals(spaceName)) {
                throw new ResultBasedException(ResultWithExecutionDetails.nok(HttpStatus.CONFLICT.value(), "The query with this UUID already exists in a different space", resolveId.getUuid()));
            }
            final ResultWithExecutionDetails<NormalizedJsonLd> result = queryController.updateQuery(normalizedJsonLd, resolveId);
            if (result != null) {
                final NormalizedJsonLd data = result.getData();
                if (data != null) {
                    data.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), queryController.isInvited(data));
                }
            }
            return result;
        }
        if (spaceName == null) {
            throw new ResultBasedException(ResultWithExecutionDetails.nok(HttpStatus.BAD_REQUEST.value(), "The query with this UUID doesn't exist yet. You therefore need to specify the space where it should be stored."));
        }
        final ResultWithExecutionDetails<NormalizedJsonLd> result = queryController.createNewQuery(normalizedJsonLd, queryId, spaceName);
        if (result != null) {
            final NormalizedJsonLd data = result.getData();
            if (data != null) {
                data.renameSpace(authContext.getUserWithRoles().getPrivateSpace(), queryController.isInvited(data));
            }
        }
        return result;
    }

    @Operation(summary = "Execute a stored query to receive the instances")
    @GetMapping("/{queryId}/instances")
    @Simple
    public PaginatedStreamResult<? extends JsonLdDoc> executeQueryById(@PathVariable("queryId") UUID queryId, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "instanceId", required = false) UUID instanceId, @RequestParam(value = "restrictToSpaces", required = false) List<String> restrictToSpaces, @RequestParam(defaultValue = "{}") Map<String, String> allRequestParams) {
        //Remove the non-dynamic parameters from the map
        allRequestParams.remove("stage");
        allRequestParams.remove("instanceId");
        allRequestParams.remove("from");
        allRequestParams.remove("size");
        InstanceId queryInstance = instances.resolveId(queryId, DataStage.IN_PROGRESS);
        final NormalizedJsonLd queryPayload = queryController.fetchQueryById(queryInstance);
        if (queryPayload == null) {
            throw new InstanceNotFoundException(String.format("Query with id %s not found", queryId));
        }
        MarmotGraphQuery query = new MarmotGraphQuery(queryPayload, stage.getStage());

        query.setIdRestriction(instances.resolveId(instanceId, stage.getStage()));
        if (restrictToSpaces != null) {
            query.setRestrictToSpaces(restrictToSpaces.stream().filter(Objects::nonNull).map(r -> SpaceName.getInternalSpaceName(r, authContext.getUserWithRoles().getPrivateSpace())).collect(Collectors.toList()));
        }
        final PaginatedStream<? extends JsonLdDoc> data = queryController.executeQuery(query, allRequestParams, paginationParam);
        return PaginatedStreamResult.ok(data);
    }

}
