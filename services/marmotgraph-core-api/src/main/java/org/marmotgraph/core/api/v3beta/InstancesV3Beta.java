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
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.config.openApiGroups.Advanced;
import org.marmotgraph.commons.config.openApiGroups.Extra;
import org.marmotgraph.commons.config.openApiGroups.Simple;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.params.ReleaseTreeScope;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.model.ExposedStage;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The instance API manages the CCRUD (Create, Contribute, Read, Update, Delete) operations for individual entity representations
 */
@RestController
@RequestMapping(Version.V3_BETA)
@AllArgsConstructor
public class InstancesV3Beta {

    private final InstancesV3 instancesV3;

    @Operation(
            summary = "Create new instance with a system generated id",
            description = """
                    The invocation of this endpoint causes the ingestion of the payload (if valid) in the MarmotGraph by assigning a new "@id" to it.
                    
                    Please note that any "@id" specified in the payload will be interpreted as an additional identifier and therefore added to the "http://schema.org/identifier" array.
                    """)
    @PostMapping("/instances")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> createNewInstance(@RequestBody JsonLdDoc jsonLdDoc, @RequestParam(value = "space") @Parameter(description = "The space name the instance shall be stored in or \"" + SpaceName.PRIVATE_SPACE + "\" if you want to store it to your private space") String space, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.createNewInstance(jsonLdDoc, space, false, false, responseConfiguration);
    }


    @Operation(
            summary = "Create new instance with a client defined id",
            description = """
                    The invocation of this endpoint causes the ingestion of the payload (if valid) in the MarmotGraph by using the specified UUID
                    
                    Please note that any "@id" specified in the payload will be interpreted as an additional identifier and therefore added to the "http://schema.org/identifier" array.
                    """)
    @PostMapping("/instances/{id}")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> createNewInstanceWithId(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @RequestParam(value = "space") @Parameter(description = "The space name the instance shall be stored in or \"" + SpaceName.PRIVATE_SPACE + "\" if you want to store it to your private space") String space, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.createNewInstanceWithId(jsonLdDoc, id, space, false, false, responseConfiguration);
    }


    @Operation(summary = "Replace contribution to an existing instance")
    @PutMapping("/instances/{id}")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> contributeToInstanceFullReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.contributeToInstanceFullReplacement(jsonLdDoc, id, false, responseConfiguration);
    }

    @Operation(summary = "Partially update contribution to an existing instance")
    @PatchMapping("/instances/{id}")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> contributeToInstancePartialReplacement(@RequestBody JsonLdDoc jsonLdDoc, @PathVariable("id") UUID id, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.contributeToInstancePartialReplacement(jsonLdDoc, id, responseConfiguration);
    }

    @Operation(summary = "Get the instance")
    @GetMapping("/instances/{id}")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> getInstanceById(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.getInstanceById(id, stage, responseConfiguration);
    }

    @Operation(summary = "Get incoming links for a specific instance (paginated)")
    @GetMapping("/instances/{id}/incomingLinks")
    @Advanced
    public PaginatedResult<NormalizedJsonLd> getIncomingLinks(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam("property") String property, @RequestParam("type") String type, @ParameterObject PaginationParam paginationParam) {
        return instancesV3.getIncomingLinks(id, stage, property, type, paginationParam);
    }


    @Operation(summary = "Get the scope for the instance by its MarmotGraph-internal ID")
    @GetMapping("/instances/{id}/scope")
    @Advanced
    public ResultWithExecutionDetails<ScopeElement> getInstanceScope(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions, @RequestParam(value = "applyRestrictions", required = false, defaultValue = "false") boolean applyRestrictions) {
        return instancesV3.getInstanceScope(id, stage, returnPermissions, applyRestrictions);
    }

    @Operation(summary = "Get the neighborhood for the instance by its MarmotGraph-internal ID")
    @GetMapping("/instances/{id}/neighbors")
    @Extra
    public ResultWithExecutionDetails<GraphEntity> getNeighbors(@PathVariable("id") UUID id, @RequestParam("stage") ExposedStage stage) {
        return instancesV3.getNeighbors(id, stage);
    }


    @Operation(summary = "Returns a list of instances according to their types")
    @GetMapping("/instances")
    @Simple
    public PaginatedResult<NormalizedJsonLd> listInstances(@RequestParam("stage") ExposedStage stage, @RequestParam("type") @Parameter(examples = {@ExampleObject(name = "person", value = "https://openminds.ebrains.eu/core/Person", description = "An openminds person"), @ExampleObject(name = "datasetVersion", value = "https://openminds.ebrains.eu/core/DatasetVersion", description = "An openminds dataset version")}) String type, @RequestParam(value = "space", required = false) @Parameter(description = "The space of the instances to be listed or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space", examples = {@ExampleObject(name = "myspace", value = "myspace"), @ExampleObject(name = "dataset", value = "dataset")}) String space, @RequestParam(value = "searchByLabel", required = false) String searchByLabel, @RequestParam(value = "filterProperty", required = false) String filterProperty, @RequestParam(value = "filterValue", required = false) String filterValue, @ParameterObject ResponseConfiguration responseConfiguration, @ParameterObject PaginationParam paginationParam) {
        return instancesV3.listInstances(stage, type, space, searchByLabel, filterProperty, filterValue, responseConfiguration, paginationParam);
    }

    @Operation(summary = "Bulk operation of /instances/{id} to read instances by their UUIDs")
    @PostMapping("/instancesByIds")
    @Advanced
    public ResultWithExecutionDetails<Map<UUID, Result<NormalizedJsonLd>>> getInstancesByIds(@RequestBody List<UUID> ids, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.getInstancesByIds(ids, stage, responseConfiguration);
    }


    @Operation(summary = "Read instances by the given list of (external) identifiers")
    @PostMapping("/instancesByIdentifiers")
    @Advanced
    public ResultWithExecutionDetails<Map<String, Result<NormalizedJsonLd>>> getInstancesByIdentifiers(@RequestBody List<String> identifiers, @RequestParam("stage") ExposedStage stage, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.getInstancesByIdentifiers(identifiers, stage, responseConfiguration);
    }

    @Operation(summary = "Move an instance to another space")
    @PutMapping("/instances/{id}/spaces/{space}")
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> moveInstance(@PathVariable("id") UUID id, @PathVariable("space") String targetSpace, @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        return instancesV3.moveInstance(id, targetSpace, responseConfiguration);
    }


    @Operation(summary = "Delete an instance")
    @DeleteMapping("/instances/{id}")
    @Simple
    public ResultWithExecutionDetails<Void> deleteInstance(@PathVariable("id") UUID id) {
        return instancesV3.deleteInstance(id);
    }


    //RELEASE instances
    @Operation(summary = "Release or re-release an instance")
    @PutMapping("/instances/{id}/release")
    @Simple
    public ResultWithExecutionDetails<Void> releaseInstance(@PathVariable("id") UUID id, @RequestParam(value = "revision", required = false) String revision) {
        return instancesV3.releaseInstance(id, revision);
    }

    @Operation(summary = "Unrelease an instance")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "The instance that has been unreleased"), @ApiResponse(responseCode = "404", description = "Instance not found")})
    @DeleteMapping("/instances/{id}/release")
    @Simple
    public ResultWithExecutionDetails<Void> unreleaseInstance(@PathVariable("id") UUID id) {
        return instancesV3.unreleaseInstance(id);
    }

    @Operation(summary = "Get the release status for an instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The release status of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found")})
    @GetMapping(value = "/instances/{id}/release/status")
    @Simple
    public ResultWithExecutionDetails<ReleaseStatus> getReleaseStatus(@PathVariable("id") UUID id, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        return instancesV3.getReleaseStatus(id, releaseTreeScope);
    }

    @Operation(summary = "Get the release status for multiple instances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The release status of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found")})
    @PostMapping(value = "/instancesByIds/release/status")
    @Advanced
    public ResultWithExecutionDetails<Map<UUID, Result<ReleaseStatus>>> getReleaseStatusByIds(@RequestBody List<UUID> listOfIds, @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope) {
        return instancesV3.getReleaseStatusByIds(listOfIds, releaseTreeScope);
    }


    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the MarmotGraph Editor). Please note: This service will return released values for \"additionalValue\" in case a user only has minimal read rights")
    @GetMapping("/instances/{id}/suggestedLinksForProperty")
    @Extra
    public ResultWithExecutionDetails<SuggestionResult> getSuggestedLinksForProperty(@RequestParam("stage") ExposedStage stage, @PathVariable("id") UUID id, @RequestParam(value = "property") String propertyName, @RequestParam(value = "sourceType", required = false) @Parameter(description = "The source type for which the given property shall be evaluated. If not provided, the API tries to figure out the type by analyzing the type of the root object of the persisted instance. Please note, that this parameter is mandatory for embedded structures.") String sourceType, @RequestParam(value = "targetType", required = false) @Parameter(description = "The target type of the suggestions. If not provided, suggestions of all possible target types will be returned.") String targetType, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        return instancesV3.getSuggestedLinksForProperty(stage, id, propertyName, sourceType, targetType, search, paginationParam);
    }

    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the MarmotGraph Editor) - and takes into account the passed payload (already chosen values, reflection on dependencies between properties - e.g. providing only parcellations for an already chosen brain atlas). Please note: This service will return released values for \"additionalValue\" in case a user only has minimal read rights")
    @PostMapping("/instances/{id}/suggestedLinksForProperty")
    @Extra
    public ResultWithExecutionDetails<SuggestionResult> getSuggestedLinksForProperty(@RequestBody NormalizedJsonLd payload, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "property") String propertyName, @PathVariable("id") UUID id, @Parameter(description = "The source type for which the given property shall be evaluated. If not provided, the API tries to figure out the type by analyzing the type of the root object originating from the payload. Please note, that this parameter is mandatory for embedded structures.") @RequestParam(value = "sourceType", required = false) String sourceType, @Parameter(description = "The target type of the suggestions. If not provided, suggestions of all possible target types will be returned.") @RequestParam(value = "targetType", required = false) String targetType, @RequestParam(value = "search", required = false) String search, @ParameterObject PaginationParam paginationParam) {
        return instancesV3.getSuggestedLinksForProperty(payload, stage, propertyName, id, sourceType, targetType, search, paginationParam);
    }

    @Operation(summary = "Create or update an invitation for the given user to review the given instance")
    @PutMapping("/instances/{id}/invitedUsers/{userId}")
    @Advanced
    public void inviteUserForInstance(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
        instancesV3.inviteUserForInstance(id, userId);
    }

    @Operation(summary = "Revoke an invitation for the given user to review the given instance")
    @DeleteMapping("/instances/{id}/invitedUsers/{userId}")
    @Advanced
    public void revokeUserInvitation(@PathVariable("id") UUID id, @PathVariable("userId") UUID userId) {
       instancesV3.revokeUserInvitation(id, userId);
    }

    @Operation(summary = "List invitations for review for the given instance")
    @GetMapping("/instances/{id}/invitedUsers")
    @Advanced
    public ResultWithExecutionDetails<List<String>> listInvitations(@PathVariable("id") UUID id) {
        return instancesV3.listInvitations(id);
    }

    @Operation(summary = "Update invitation scope for this instance")
    @PutMapping("/instances/{id}/invitationScope")
    @Admin
    public void calculateInstanceInvitationScope(@PathVariable("id") UUID id) {
        instancesV3.calculateInstanceInvitationScope(id);
    }

    @Operation(summary = "List instances with invitations")
    @GetMapping("/instancesWithInvitations")
    @Advanced
    public ResultWithExecutionDetails<List<UUID>> listInstancesWithInvitations() {
        return instancesV3.listInstancesWithInvitations();
    }


}
