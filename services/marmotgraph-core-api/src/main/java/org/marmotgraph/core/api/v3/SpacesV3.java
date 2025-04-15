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

package org.marmotgraph.core.api.v3;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.config.openApiGroups.Advanced;
import org.marmotgraph.commons.exception.InstanceNotFoundException;
import org.marmotgraph.commons.exception.InvalidRequestException;
import org.marmotgraph.commons.exception.NoContentException;
import org.marmotgraph.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import org.marmotgraph.commons.markers.ExposesSpace;
import org.marmotgraph.commons.markers.WritesData;
import org.marmotgraph.commons.model.PaginatedResult;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.types.TypeInSpace;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.core.controller.CoreInferenceController;
import org.marmotgraph.core.controller.CoreSpaceController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

/**
 * The spaces API provides information about existing MarmotGraph spaces
 */
@RestController
@RequestMapping(Version.V3 + "/spaces")
public class SpacesV3 {
    private final CoreInferenceController inferenceController;
    private final AuthContext authContext;
    private final CoreSpaceController spaceController;

    public SpacesV3(CoreInferenceController inferenceController, AuthContext authContext, CoreSpaceController spaceController) {
        this.inferenceController = inferenceController;
        this.authContext = authContext;
        this.spaceController = spaceController;
    }

    @GetMapping("{space}")
    @ExposesSpace
    @Advanced
    public Result<SpaceInformation> getSpace(@PathVariable("space") @Parameter(description = "The space to be read or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        SpaceInformation s = spaceController.getSpace(SpaceName.fromString(space), permissions);
        if (s != null) {
            return Result.ok(s);
        }
        throw new InstanceNotFoundException(String.format("Space %s was not found", space));
    }

    @GetMapping
    @ExposesSpace
    @Advanced
    public PaginatedResult<SpaceInformation> listSpaces(@ParameterObject PaginationParam paginationParam, @RequestParam(value = "permissions", defaultValue = "false") boolean permissions) {
        return PaginatedResult.ok(spaceController.listSpaces(paginationParam, permissions));
    }

    @Operation(summary = "Check type for a specific space")
    @GetMapping("{space}/types")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relation between space and type", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = TypeInSpace.class)) }),
            @ApiResponse(responseCode = "404", description = "Space not found", content = @Content),
            @ApiResponse(responseCode = "204", description = "No relation", content = @Content)})
    @ExposesSpace
    @Admin
    public TypeInSpace typeInSpace(
            @PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space,
            @RequestParam(value = "type") String type
    ) {

        // check space exists
        SpaceSpecification spaceSpecifications = this.spaceController.getSpaceSpecification(space);
        if (spaceSpecifications == null) {
            throw new InstanceNotFoundException(String.format("Space %s was not found", space));
        }

        // retrieve relation from space and type
        if (spaceController.checkTypeInSpace(SpaceName.fromString(space), type)) {
            return new TypeInSpace(space, type);
        } else {
            throw new NoContentException("No Content");
        }
    }

    @Operation(summary = "Assign a type to a space")
    @PutMapping("{space}/types")
    @WritesData
    @Admin
    public void assignTypeToSpace(@PathVariable("space") @Parameter(description = "The space be linked to or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spaceController.addTypeToSpace(SpaceName.fromString(space), type);
    }

    @Operation(summary = "Remove a type in space definition")
    @DeleteMapping("{space}/types")
    @WritesData
    @Admin
    public void removeTypeFromSpace(@PathVariable("space") @Parameter(description = "The space the type shall be removed from or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space") String space, @RequestParam("type") String type) {
        spaceController.removeTypeFromSpace(SpaceName.fromString(space), type);
    }

    @Operation(summary = "Get space specification")
    @GetMapping("{space}/specification")
    @Admin
    @ExposesSpace
    public SpaceSpecification spaceSpecification(
            @PathVariable(value = "space")
            @Parameter(description = "The space the specification is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space
    ) {
        if (space == null) {
            throw new InvalidRequestException("You need to provide a space name to execute this functionality");
        }

        SpaceSpecification spaceSpecifications = this.spaceController.getSpaceSpecification(space);

        if (spaceSpecifications != null) {
            return spaceSpecifications;
        }
        throw new InstanceNotFoundException(String.format("Space %s was not found", space));
    }

    @Operation(summary = "Explicitly specify a space")
    @PutMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    public void createSpaceSpecification(@PathVariable(value = "space") @Parameter(description = "The space the definition is valid for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space, @RequestParam(value = "autorelease", required = false, defaultValue = "false") boolean autoRelease, @RequestParam(value = "clientSpace", required = false, defaultValue = "false") boolean clientSpace, @RequestParam(value = "deferCache", required = false, defaultValue = "false") boolean deferCache, @RequestParam(value = "scopeRelevant", required = false, defaultValue = "false") boolean scopeRelevant) {
        if (space == null) {
            throw new InvalidRequestException("You need to provide a space name to execute this functionality");
        }
        SpaceSpecification spaceSpecification = new SpaceSpecification();
        spaceSpecification.setName(space);
        spaceSpecification.setIdentifier(space);
        spaceSpecification.setAutoRelease(autoRelease);
        spaceSpecification.setDeferCache(deferCache);
        spaceSpecification.setClientSpace(clientSpace);
        spaceSpecification.setScopeRelevant(scopeRelevant);
        spaceController.createSpaceSpecification(spaceSpecification);
    }


    @Operation(summary = "Remove a space specification")
    @DeleteMapping("{space}/specification")
    @Admin
    @ExposesInputWithoutEnrichedSensitiveData
    public void removeSpaceSpecification(@PathVariable(value = "space") @Parameter(description = "The space the definition should be removed for. Please note that you can't do so for your private space (\"" + SpaceName.PRIVATE_SPACE + "\")") String space) {
        spaceController.removeSpaceSpecification(SpaceName.fromString(space));
    }

    @Operation(summary = "Trigger a rerun of the events of this space")
    @PutMapping("{space}/eventHistory")
    @Admin
    public void rerunEvents(@PathVariable(value = "space") @Parameter(description = "The space the event rerun shall be executed for.") String space) {
        spaceController.rerunEvents(SpaceName.fromString(space));
    }

    @Operation(summary = "Triggers the inference of all documents of the given space")
    @Admin
    @PostMapping("/{space}/inference")
    public void triggerInference(@PathVariable(value = "space") String space, @RequestParam(value = "identifier", required = false) String identifier, @RequestParam(value = "async", required = false, defaultValue = "false") boolean async) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        if (async) {
            inferenceController.asyncTriggerInference(spaceName, identifier);
        } else {
            inferenceController.triggerInference(spaceName, identifier);
        }
    }

}
