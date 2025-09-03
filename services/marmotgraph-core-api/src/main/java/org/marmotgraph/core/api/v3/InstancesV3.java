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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.api.jsonld.JsonLd;
import org.marmotgraph.commons.api.primaryStore.Instances;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.config.openApiGroups.Advanced;
import org.marmotgraph.commons.config.openApiGroups.Extra;
import org.marmotgraph.commons.config.openApiGroups.Simple;
import org.marmotgraph.commons.exception.InvalidRequestException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.*;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.params.ReleaseTreeScope;
import org.marmotgraph.core.controller.CoreInstanceController;
import org.marmotgraph.core.controller.VirtualSpaceController;
import org.marmotgraph.core.model.ExposedStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The instance API manages the CCRUD (Create, Contribute, Read, Update, Delete) operations for individual entity representations
 */
@RestController
@RequestMapping(Version.V3)
@AllArgsConstructor
public class InstancesV3 {

    private final CoreInstanceController instanceController;
    private final AuthContext authContext;
    private final VirtualSpaceController virtualSpaceController;
    private final Instances.Client instances;
    private final JsonLd.Client jsonLd;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Operation(
            summary = "Create new instance with a system generated id",
            description = """
                    The invocation of this endpoint causes the ingestion of the payload (if valid) in the MarmotGraph by assigning a new "@id" to it.
                    
                    Please note that any "@id" specified in the payload will be interpreted as an additional identifier and therefore added to the "http://schema.org/identifier" array.
                    """)
//    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
//            @ExampleObject(name = InstancesExamples.PAYLOAD_MINIMAL_NAME, description = InstancesExamples.PAYLOAD_MINIMAL_DESC, value = InstancesExamples.PAYLOAD_MINIMAL),
//            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_PROPERTY_NAME, description = InstancesExamples.PAYLOAD_WITH_PROPERTY_DESC, value = InstancesExamples.PAYLOAD_WITH_PROPERTY),
//            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_LINK_NAME, description = InstancesExamples.PAYLOAD_WITH_LINK_DESC, value = InstancesExamples.PAYLOAD_WITH_LINK)
//    }))
    @PostMapping("/instances")
    @WritesData
    @ExposesData
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> createNewInstance(
            @RequestBody JsonLdDoc jsonLdDoc,

            @RequestParam(value = "space")
            @Parameter(description = "The space name the instance shall be stored in or \"" + SpaceName.PRIVATE_SPACE + "\" if you want to store it to your private space")
            String space,

            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        return createNewInstanceWithId(jsonLdDoc, UUID.randomUUID(), space, responseConfiguration);
    }


    @Operation(
            summary = "Create new instance with a client defined id",
            description = """
                    The invocation of this endpoint causes the ingestion of the payload (if valid) in the MarmotGraph by using the specified UUID
                    
                    Please note that any "@id" specified in the payload will be interpreted as an additional identifier and therefore added to the "http://schema.org/identifier" array.
                    """)
    @PostMapping("/instances/{id}")
    @ExposesData
    @WritesData
//    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
//            @ExampleObject(name = InstancesExamples.PAYLOAD_MINIMAL_NAME, description = InstancesExamples.PAYLOAD_MINIMAL_DESC, value = InstancesExamples.PAYLOAD_MINIMAL),
//            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_PROPERTY_NAME, description = InstancesExamples.PAYLOAD_WITH_PROPERTY_DESC, value = InstancesExamples.PAYLOAD_WITH_PROPERTY),
//            @ExampleObject(name = InstancesExamples.PAYLOAD_WITH_LINK_NAME, description = InstancesExamples.PAYLOAD_WITH_LINK_DESC, value = InstancesExamples.PAYLOAD_WITH_LINK)
//    }))
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> createNewInstanceWithId(
            @RequestBody JsonLdDoc jsonLdDoc,
            @PathVariable("id") UUID id,

            @RequestParam(value = "space")
            @Parameter(description = "The space name the instance shall be stored in or \"" + SpaceName.PRIVATE_SPACE + "\" if you want to store it to your private space")
            String space,

            @ParameterObject ExtendedResponseConfiguration responseConfiguration) {
        SpaceName spaceName = authContext.resolveSpaceName(space);
        logger.debug(String.format("Creating new instance with id %s", id));
        ResultWithExecutionDetails<NormalizedJsonLd> newInstance = instanceController.createNewInstance(normalizePayload(jsonLdDoc, true), id, spaceName, responseConfiguration);
        logger.debug(String.format("Done creating new instance with id %s", id));
        return newInstance;
    }

    private NormalizedJsonLd normalizePayload(JsonLdDoc jsonLdDoc, boolean requiresTypeAtRootLevel) {
        try {
            jsonLdDoc.normalizeTypes();
            jsonLdDoc.validate(requiresTypeAtRootLevel);
        } catch (InvalidRequestException e) {
            //There have been validation errors -> we're going to normalize and validate again...
            final NormalizedJsonLd normalized = jsonLd.normalize(jsonLdDoc, true);
            normalized.validate(requiresTypeAtRootLevel);
            return normalized;
        }
        return new NormalizedJsonLd(jsonLdDoc);
    }

    private ResultWithExecutionDetails<NormalizedJsonLd> contributeToInstance(NormalizedJsonLd normalizedJsonLd, UUID id, ExtendedResponseConfiguration responseConfiguration, boolean removeNonDeclaredFields) {
        logger.debug(String.format("Contributing to instance with id %s", id));
        final InstanceId instanceId = instanceController.findIdForContribution(id, normalizedJsonLd.identifiers());
        if (instanceId == null) {
            return null;
        }
        ResultWithExecutionDetails<NormalizedJsonLd> result = instanceController.contributeToInstance(normalizedJsonLd, instanceId, removeNonDeclaredFields, responseConfiguration);
        logger.debug(String.format("Done contributing to instance with id %s", id));
        return result;
    }

    @Operation(summary = "Replace contribution to an existing instance")
    @PutMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> contributeToInstanceFullReplacement(
            @RequestBody JsonLdDoc jsonLdDoc,
            @PathVariable("id") UUID id,
            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        return contributeToInstance(normalizePayload(jsonLdDoc, true), id, responseConfiguration, true);
    }

    @Operation(summary = "Partially update contribution to an existing instance")
    @PatchMapping("/instances/{id}")
    @ExposesData
    @WritesData
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> contributeToInstancePartialReplacement(
            @RequestBody JsonLdDoc jsonLdDoc,
            @PathVariable("id") UUID id,
            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        return contributeToInstance(normalizePayload(jsonLdDoc, false), id, responseConfiguration, false);
    }

    @Operation(summary = "Get the instance")
    @GetMapping("/instances/{id}")
    @ExposesData
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> getInstanceById(
            @PathVariable("id") UUID id,
            @RequestParam("stage") ExposedStage stage,
            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        NormalizedJsonLd instanceById = instanceController.getInstanceById(id, stage.getStage(), responseConfiguration);
        return instanceById != null ? ResultWithExecutionDetails.ok(instanceById) : null;
    }

    @Operation(summary = "Get incoming links for a specific instance (paginated)")
    @GetMapping("/instances/{id}/incomingLinks")
    @ExposesData
    @Advanced
    public PaginatedResult<NormalizedJsonLd> getIncomingLinks(
            @PathVariable("id") UUID id,
            @RequestParam("stage") ExposedStage stage,
            @RequestParam("property") String property,
            @RequestParam("type") String type,
            @ParameterObject PaginationParam paginationParam
    ) {
        return PaginatedResult.ok(instanceController.getIncomingLinks(id, stage.getStage(), URLDecoder.decode(property, StandardCharsets.UTF_8), type != null ? new Type(type) : null, paginationParam));
    }


    @Operation(summary = "Get the scope for the instance by its MarmotGraph-internal ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The scope of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found"),
            @ApiResponse(responseCode = "509", description = "Bandwidth Limit Exceeded")})
    @GetMapping("/instances/{id}/scope")
    @ExposesMinimalData
    @Advanced
    public ResultWithExecutionDetails<ScopeElement> getInstanceScope(
            @PathVariable("id") UUID id,
            @RequestParam("stage") ExposedStage stage,
            @RequestParam(value = "returnPermissions", required = false, defaultValue = "false") boolean returnPermissions,
            @RequestParam(value = "applyRestrictions", required = false, defaultValue = "false") boolean applyRestrictions
    ) {
        ScopeElement scope = instanceController.getScopeForInstance(id, stage.getStage(), returnPermissions, applyRestrictions);
        return scope != null ? ResultWithExecutionDetails.ok(scope) : null;
    }

    @Operation(summary = "Get the neighborhood for the instance by its MarmotGraph-internal ID")
    @GetMapping("/instances/{id}/neighbors")
    @ExposesMinimalData
    @Extra
    public ResultWithExecutionDetails<GraphEntity> getNeighbors(
            @PathVariable("id") UUID id,
            @RequestParam("stage") ExposedStage stage
    ) {
        GraphEntity scope = instanceController.getNeighbors(id, stage.getStage());
        return scope != null ? ResultWithExecutionDetails.ok(scope) : null;
    }


    @Operation(summary = "Returns a list of instances according to their types")
    @GetMapping("/instances")
    @ExposesData
    @Simple
    public PaginatedResult<NormalizedJsonLd> listInstances(
            @RequestParam("stage")
            ExposedStage stage,

            @RequestParam("type")
//            @Parameter(
//                    examples = {@ExampleObject(name = "person", value = "https://openminds.ebrains.eu/core/Person", description = "An openminds person"), @ExampleObject(name = "datasetVersion", value = "https://openminds.ebrains.eu/core/DatasetVersion", description = "An openminds dataset version")}
//            )
            String type,

            @RequestParam(value = "space", required = false)
            @Parameter(
                    //examples = {@ExampleObject(name = "myspace", value = "myspace"), @ExampleObject(name = "dataset", value = "dataset")},
                    description = "The space of the instances to be listed or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space"
            )
            String space,

            @RequestParam(value = "searchByLabel", required = false)
            String searchByLabel,

            @RequestParam(value = "filterProperty", required = false)
            String filterProperty,

            @RequestParam(value = "filterValue", required = false)
            String filterValue,

            @ParameterObject
            ResponseConfiguration responseConfiguration,

            @ParameterObject
            PaginationParam paginationParam
    ) {
        PaginatedResult<NormalizedJsonLd> result;
        if (virtualSpaceController.isVirtualSpace(space)) {
            List<NormalizedJsonLd> instancesByInvitation = virtualSpaceController.getInstancesByInvitation(responseConfiguration, stage.getStage(), type);
            int total = instancesByInvitation.size();
            if (paginationParam.getFrom() != 0 || paginationParam.getSize() != null) {
                int lastIndex = paginationParam.getSize() == null ? instancesByInvitation.size() : Math.min(instancesByInvitation.size(), (int) (paginationParam.getFrom() + paginationParam.getSize()));
                instancesByInvitation = instancesByInvitation.subList((int) paginationParam.getFrom(), lastIndex);
            }
            return PaginatedResult.ok(new Paginated<>(instancesByInvitation, (long) instancesByInvitation.size(), total, paginationParam.getFrom()));
        } else {
            searchByLabel = enrichSearchTermIfItIsAUUID(searchByLabel, stage.getStage());
            result = PaginatedResult.ok(instanceController.getInstances(stage.getStage(), new Type(type), SpaceName.fromString(space), searchByLabel, filterProperty, filterValue, responseConfiguration, paginationParam));
        }
        return result;
    }

    @Operation(summary = "Bulk operation of /instances/{id} to read instances by their UUIDs")
    @PostMapping("/instancesByIds")
    @ExposesData
    @Advanced
    public ResultWithExecutionDetails<Map<UUID, Result<NormalizedJsonLd>>> getInstancesByIds(
            @RequestBody List<UUID> ids,
            @RequestParam("stage") ExposedStage stage,
            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        return ResultWithExecutionDetails.ok(instanceController.getInstancesByIds(ids, stage.getStage(), responseConfiguration, null));
    }


    @Operation(summary = "Read instances by the given list of (external) identifiers")
    @PostMapping("/instancesByIdentifiers")
    @ExposesData
    @Advanced
    public ResultWithExecutionDetails<Map<String, Result<NormalizedJsonLd>>> getInstancesByIdentifiers(
            @RequestBody List<String> identifiers,
            @RequestParam("stage") ExposedStage stage,
            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        List<IdWithAlternatives> idWithAlternatives = identifiers.stream().filter(Objects::nonNull).distinct().map(identifier -> new IdWithAlternatives(UUID.nameUUIDFromBytes(identifier.getBytes(StandardCharsets.UTF_8)), null, Collections.singleton(identifier))).toList();
        Map<UUID, InstanceId> uuidInstanceIdMap = instanceController.resolveIds(idWithAlternatives, stage.getStage());
        Map<UUID, Result<NormalizedJsonLd>> instancesByIds = instanceController.getInstancesByInstanceIds(uuidInstanceIdMap.values(), stage.getStage(), responseConfiguration, null);
        return ResultWithExecutionDetails.ok(identifiers.stream().collect(Collectors.toMap(k -> k, v -> {
            UUID mapKey = UUID.nameUUIDFromBytes(v.getBytes(StandardCharsets.UTF_8));
            InstanceId instanceId = uuidInstanceIdMap.get(mapKey);
            if (instanceId != null) {
                Result<NormalizedJsonLd> payload = instancesByIds.get(instanceId.getUuid());
                if (payload != null) {
                    return payload;
                }
            }
            return ResultWithExecutionDetails.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
        })));
    }

    @Operation(summary = "Move an instance to another space")
    @PutMapping("/instances/{id}/spaces/{space}")
    @WritesData
    @ExposesIds
    @Simple
    public ResultWithExecutionDetails<NormalizedJsonLd> moveInstance(
            @PathVariable("id") UUID id,
            @PathVariable("space") String targetSpace,
            @ParameterObject ExtendedResponseConfiguration responseConfiguration
    ) {
        return instanceController.moveInstance(id, authContext.resolveSpaceName(targetSpace), responseConfiguration);
    }


    @Operation(summary = "Delete an instance")
    @DeleteMapping("/instances/{id}")
    @WritesData
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @Simple
    public ResultWithExecutionDetails<Void> deleteInstance(
            @PathVariable("id") UUID id
    ) {
        return instanceController.deleteInstance(id);
    }


    //RELEASE instances
    @Operation(summary = "Release or re-release an instance")
    @PutMapping("/instances/{id}/release")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @WritesData
    @Simple
    public ResultWithExecutionDetails<Void> releaseInstance(
            @PathVariable("id") UUID id,
            @RequestParam(value = "revision", required = false) String revision
    ) {
        instanceController.release(id);
        return ResultWithExecutionDetails.ok();
    }

    @Operation(summary = "Unrelease an instance")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "The instance that has been unreleased"), @ApiResponse(responseCode = "404", description = "Instance not found")})
    @DeleteMapping("/instances/{id}/release")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @WritesData
    @Simple
    public ResultWithExecutionDetails<Void> unreleaseInstance(
            @PathVariable("id") UUID id
    ) {
        instanceController.unrelease(id);
        return ResultWithExecutionDetails.ok();
    }

    @Operation(summary = "Get the release status for an instance")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The release status of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found")})
    @GetMapping(value = "/instances/{id}/release/status")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @ExposesReleaseStatus
    @Simple
    public ResultWithExecutionDetails<ReleaseStatus> getReleaseStatus(
            @PathVariable("id") UUID id,
            @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope
    ) {
        return ResultWithExecutionDetails.ok(instanceController.getReleaseStatus(id, releaseTreeScope));
    }

    @Operation(summary = "Get the release status for multiple instances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The release status of the instance"),
            @ApiResponse(responseCode = "404", description = "Instance not found")})
    @PostMapping(value = "/instancesByIds/release/status")
    //It only indirectly exposes the ids due to its status codes (you can tell if an id exists based on the return code this method provides)
    @ExposesIds
    @ExposesReleaseStatus
    @Advanced
    public ResultWithExecutionDetails<Map<UUID, Result<ReleaseStatus>>> getReleaseStatusByIds(
            @RequestBody List<UUID> listOfIds,
            @RequestParam("releaseTreeScope") ReleaseTreeScope releaseTreeScope
    ) {
        final Map<UUID, ReleaseStatus> result = instanceController.getReleaseStatus(listOfIds, releaseTreeScope);
        return ResultWithExecutionDetails.ok(result.keySet().stream().collect(Collectors.toMap(k -> k, v -> Result.ok(result.get(v)))));
    }


    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the MarmotGraph Editor). Please note: This service will return released values for \"additionalValue\" in case a user only has minimal read rights")
    @GetMapping("/instances/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    @Extra
    public ResultWithExecutionDetails<SuggestionResult> getSuggestedLinksForProperty(
            @RequestParam("stage")
            ExposedStage stage,

            @PathVariable("id")
            UUID id,

            @RequestParam(value = "property")
            String propertyName,

            @RequestParam(value = "sourceType", required = false)
            @Parameter(description = "The source type for which the given property shall be evaluated. If not provided, the API tries to figure out the type by analyzing the type of the root object of the persisted instance. Please note, that this parameter is mandatory for embedded structures.")
            String sourceType,

            @RequestParam(value = "targetType", required = false)
            @Parameter(description = "The target type of the suggestions. If not provided, suggestions of all possible target types will be returned.")
            String targetType,

            @RequestParam(value = "search", required = false)
            String search,

            @ParameterObject PaginationParam paginationParam
    ) {
        return getSuggestedLinksForProperty(null, stage, propertyName, id, sourceType, targetType, search, paginationParam);
    }

    @Operation(summary = "Returns suggestions for an instance to be linked by the given property (e.g. for the MarmotGraph Editor) - and takes into account the passed payload (already chosen values, reflection on dependencies between properties - e.g. providing only parcellations for an already chosen brain atlas). Please note: This service will return released values for \"additionalValue\" in case a user only has minimal read rights")
    @PostMapping("/instances/{id}/suggestedLinksForProperty")
    @ExposesMinimalData
    @Extra
    public ResultWithExecutionDetails<SuggestionResult> getSuggestedLinksForProperty(
            @RequestBody
            NormalizedJsonLd payload,

            @RequestParam("stage")
            ExposedStage stage,

            @RequestParam("property")
            String propertyName,

            @PathVariable("id")
            UUID id,

            @Parameter(description = "The source type for which the given property shall be evaluated. If not provided, the API tries to figure out the type by analyzing the type of the root object originating from the payload. Please note, that this parameter is mandatory for embedded structures.")
            @RequestParam(value = "sourceType", required = false)
            String sourceType,

            @Parameter(description = "The target type of the suggestions. If not provided, suggestions of all possible target types will be returned.")
            @RequestParam(value = "targetType", required = false)
            String targetType,

            @RequestParam(value = "search", required = false)
            String search,

            @ParameterObject
            PaginationParam paginationParam
    ) {
        InstanceId instanceId = instanceController.resolveId(id, stage.getStage());
        search = enrichSearchTermIfItIsAUUID(search, stage.getStage());
        return ResultWithExecutionDetails.ok(instances.getSuggestedLinksForProperty(payload, stage.getStage(), instanceId != null && instanceId.getSpace() != null ? instanceId.getSpace().getName() : null, id, propertyName, sourceType != null && !sourceType.isBlank() ? new Type(sourceType).getName() : null, targetType != null && !targetType.isBlank() ? new Type(targetType).getName() : null, search, paginationParam));
    }

    private String enrichSearchTermIfItIsAUUID(String search, DataStage stage) {
        if (search != null) {
            try {
                //The search string is a UUID -> let's try to resolve it - if we're successful, we can shortcut the lookup process.
                UUID uuid = UUID.fromString(search);
                InstanceId resolvedSearchId = instanceController.resolveId(uuid, stage);
                if (resolvedSearchId != null) {
                    return resolvedSearchId.serialize();
                }
            } catch (IllegalArgumentException e) {
                //The search string is not an id -> we therefore don't treat it.
            }
        }
        return search;
    }

    @Operation(summary = "Create or update an invitation for the given user to review the given instance")
    @PutMapping("/instances/{id}/invitedUsers/{userId}")
    @Advanced
    public void inviteUserForInstance(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId
    ) {
        instanceController.createInvitation(id, userId);
    }

    @Operation(summary = "Revoke an invitation for the given user to review the given instance")
    @DeleteMapping("/instances/{id}/invitedUsers/{userId}")
    @Advanced
    public void revokeUserInvitation(
            @PathVariable("id") UUID id,
            @PathVariable("userId") UUID userId
    ) {
        instanceController.revokeInvitation(id, userId);
    }

    @Operation(summary = "List invitations for review for the given instance")
    @GetMapping("/instances/{id}/invitedUsers")
    @Advanced
    public ResultWithExecutionDetails<List<String>> listInvitations(
            @PathVariable("id") UUID id
    ) {
        return ResultWithExecutionDetails.ok(instanceController.listInvitedUserIds(id));
    }

    @Operation(summary = "Update invitation scope for this instance")
    @PutMapping("/instances/{id}/invitationScope")
    @Admin
    public void calculateInstanceInvitationScope(
            @PathVariable("id") UUID id
    ) {
        instanceController.calculateInstanceInvitationScope(id);
    }

    @Operation(summary = "List instances with invitations")
    @GetMapping("/instancesWithInvitations")
    @Advanced
    public ResultWithExecutionDetails<List<UUID>> listInstancesWithInvitations() {
        return ResultWithExecutionDetails.ok(instanceController.listInstancesWithInvitations());
    }


}
