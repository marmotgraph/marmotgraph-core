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

package org.marmotgraph.core.api.v3beta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.config.openApiGroups.Simple;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.core.api.v3.QueriesV3;
import org.marmotgraph.core.model.ExposedStage;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The query API allows to execute and manage queries on top of the MarmotGraph. This is the main interface for reading clients.
 */
@AllArgsConstructor
@RestController
@RequestMapping(Version.V3_BETA + "/queries")
@Simple
public class QueriesV3Beta {
    private QueriesV3 queriesV3;

    @Operation(summary = "List the queries and filter them by root type and/or text in the label, name or description")
    @GetMapping
    public PaginatedResult<NormalizedJsonLd> listQueriesPerRootType(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "type", required = false) String rootType, @RequestParam(value = "search", required = false) String search) {
        return queriesV3.listQueriesPerRootType(paginationParam, rootType, search);
    }

    @Operation(summary = "Execute the query in the payload (e.g. for execution before saving with the MarmotGraph QueryBuilder)")
    @PostMapping
    public PaginatedStreamResult<? extends JsonLdDoc> runDynamicQuery(@RequestBody JsonLdDoc query, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "instanceId", required = false) UUID instanceId, @RequestParam(value = "restrictToSpaces", required = false) List<String> restrictToSpaces, @RequestParam(defaultValue = "{}") Map<String, String> allRequestParams) {
        return queriesV3.runDynamicQuery(query, paginationParam, stage, instanceId, restrictToSpaces, allRequestParams);
    }


    @Operation(summary = "Get the query specification with the given query id in a specific space")
    @GetMapping("/{queryId}")
    public ResponseEntity<ResultWithExecutionDetails<NormalizedJsonLd>> getQuerySpecification(@PathVariable("queryId") UUID queryId) {
        return queriesV3.getQuerySpecification(queryId);
    }

    @Operation(summary = "Remove a query specification")
    @DeleteMapping("/{queryId}")
    public void removeQuery(@PathVariable("queryId") UUID queryId) {
        queriesV3.removeQuery(queryId);
    }

    @Operation(summary = "Create or save a query specification")
    @PutMapping("/{queryId}")
    public ResultWithExecutionDetails<NormalizedJsonLd> saveQuery(@RequestBody JsonLdDoc query, @PathVariable(value = "queryId") UUID queryId, @RequestParam(value = "space", required = false) @Parameter(description = "Required only when the instance is created to specify where it should be stored (" + SpaceName.PRIVATE_SPACE + " for your private space) - but not if it's updated.") String space) {
        return queriesV3.saveQuery(query, queryId, space);
    }

    @Operation(summary = "Execute a stored query to receive the instances")
    @GetMapping("/{queryId}/instances")
    public PaginatedStreamResult<? extends JsonLdDoc> executeQueryById(@PathVariable("queryId") UUID queryId, @ParameterObject PaginationParam paginationParam, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "instanceId", required = false) UUID instanceId, @RequestParam(value = "restrictToSpaces", required = false) List<String> restrictToSpaces, @RequestParam(defaultValue = "{}") Map<String, String> allRequestParams) {
       return queriesV3.executeQueryById(queryId, paginationParam, stage, instanceId, restrictToSpaces, allRequestParams);
    }

}
