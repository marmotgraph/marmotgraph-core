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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.config.openApiGroups.Advanced;
import org.marmotgraph.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import org.marmotgraph.commons.markers.ExposesSpace;
import org.marmotgraph.commons.model.PaginatedResult;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.ResultWithExecutionDetails;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.commons.model.external.types.TypeInSpace;
import org.marmotgraph.core.api.v3.SpacesV3;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

/**
 * The spaces API provides information about existing MarmotGraph spaces
 */
@RestController
@RequestMapping(Version.V3_BETA + "/spaces")
@AllArgsConstructor
public class SpacesV3Beta {

    private final SpacesV3 spacesV3;


    @GetMapping("{space}")
    @Advanced
    public ResultWithExecutionDetails<SpaceInformation> getSpace(@PathVariable("space") @Parameter(description = "The space to be read or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        return spacesV3.getSpace(space, permissions);
    }

    @GetMapping
    @Advanced
    public PaginatedResult<SpaceInformation> listSpaces(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        return spacesV3.listSpaces(paginationParam, permissions);
    }

    @Operation(summary = "Check type for a specific space")
    @GetMapping("{space}/types")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relation between space and type", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = TypeInSpace.class)) }),
            @ApiResponse(responseCode = "404", description = "Space not found", content = @Content),
            @ApiResponse(responseCode = "204", description = "No relation", content = @Content)})
    @Admin
    public TypeInSpace listSpaceType(
            @PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space,
            @RequestParam(value = "type") String type
    ) {
        return spacesV3.typeInSpace(space, type);
    }

    @Operation(summary = "Assign a type to a space")
    @PutMapping("{space}/types")
    @Admin
    public void assignTypeToSpace(@PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spacesV3.assignTypeToSpace(space, type);
    }

    @Operation(summary = "Remove a type in space definition")
    @DeleteMapping("{space}/types")
    @Admin
    public void removeTypeFromSpace(@PathVariable("space") @Parameter(description = "The space the type shall be removed from or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spacesV3.removeTypeFromSpace(space, type);
    }

    @Operation(summary = "Get space specification")
    @GetMapping("{space}/specification")
    @Admin
    public SpaceSpecification spaceSpecification(
            @PathVariable(value = "space")
            @Parameter(description = "The space the specification is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space
    ) {
        return spacesV3.spaceSpecification(space);
    }

    @Operation(summary = "Explicitly specify a space")
    @PutMapping("{space}/specification")
    @Admin
    public void createSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space, @RequestParam(value = "autorelease", required = false, defaultValue = "false") boolean autoRelease, @RequestParam(value = "clientSpace", required = false, defaultValue = "false") boolean clientSpace, @RequestParam(value = "deferCache", required = false, defaultValue = "false") boolean deferCache) {
        spacesV3.createSpaceSpecification(space, autoRelease, clientSpace, deferCache, false);
    }


    @Operation(summary = "Remove a space specification")
    @DeleteMapping("{space}/specification")
    @Admin
    public void removeSpaceDefinition(@PathVariable(value = "space") @Parameter(description = "The space the definition should be removed for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space) {
        spacesV3.removeSpaceSpecification(space);
    }


    @Operation(summary = "Triggers the inference of all documents of the given space")
    @Admin
    @PostMapping("/{space}/inference")
    public void triggerInference(@PathVariable(value = "space") String space, @RequestParam(value = "identifier", required = false) String identifier, @RequestParam(value = "async", required = false, defaultValue = "false") boolean async) {
        spacesV3.triggerInference(space, identifier, async);
    }

}
