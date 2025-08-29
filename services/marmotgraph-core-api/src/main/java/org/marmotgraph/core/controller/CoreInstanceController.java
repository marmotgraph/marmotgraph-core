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

package org.marmotgraph.core.controller;

import lombok.AllArgsConstructor;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.api.authentication.Invitation;
import org.marmotgraph.commons.api.graphDB.GraphDB;
import org.marmotgraph.commons.api.primaryStore.Events;
import org.marmotgraph.commons.api.primaryStore.Instances;
import org.marmotgraph.commons.api.primaryStore.Scopes;
import org.marmotgraph.commons.exception.*;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdConsts;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.params.ReleaseTreeScope;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.FunctionalityInstance;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The instance controller contains the orchestration logic for the instance operations
 */
@AllArgsConstructor
@Component
public class CoreInstanceController {

    private final GraphDB.Client graphDB;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final Events.Client events;
    private final Instances.Client instances;
    private final Scopes.Client scopes;
    private final Invitation.Client invitation;
    private final Permissions permissions;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Map<UUID, InstanceId> resolveIds(List<IdWithAlternatives> idWithAlternatives, DataStage stage) {
        return instances.resolveIds(idWithAlternatives, stage);
    }

    public InstanceId resolveId(UUID id, DataStage stage) {
        if(id!=null) {
            List<InstanceId> documentIds = resolveIdsByUUID(Collections.singletonList(id), false, stage);
            if (documentIds != null && documentIds.size() == 1) {
                return documentIds.getFirst();
            }
        }
        return null;
    }


    public List<InstanceId> resolveIdsByUUID(List<UUID> ids, boolean returnUnresolved, DataStage stage) {
        List<IdWithAlternatives> idWithAlternatives = ids.stream().map(id -> new IdWithAlternatives().setId(id).setAlternatives(Collections.singleton(idUtils.buildAbsoluteUrl(id).getId()))).collect(Collectors.toList());
        return resolveIds(idWithAlternatives, returnUnresolved, stage);
    }


    private List<InstanceId> resolveIds(List<IdWithAlternatives> idWithAlternatives, boolean returnUnresolved, DataStage stage) {
        List<InstanceId> resultList;
        Map<UUID, InstanceId> result = resolveIds(idWithAlternatives, stage);
        if (result != null) {
            resultList = idWithAlternatives.stream().map(idWithAlternative -> {
                idWithAlternative.setFound(result.containsKey(idWithAlternative.getId()));
                return result.get(idWithAlternative.getId());
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        else{
            resultList = new ArrayList<>();
        }
        if (returnUnresolved) {
            List<InstanceId> unresolvedIds = idWithAlternatives.stream().filter(idWithAlternative -> !idWithAlternative.isFound()).map(idWithAlternative ->
                    {
                        InstanceId instanceId = new InstanceId(idWithAlternative.getId(), null);
                        instanceId.setUnresolved(true);
                        return instanceId;
                    }
            ).toList();
            resultList.addAll(unresolvedIds);
        }
        return resultList;
    }


    public InstanceId findIdForContribution(UUID uuid, Set<String> identifiers) {
        try {
            return this.instances.findInstanceByIdentifiers(uuid, new ArrayList<>(identifiers), DataStage.IN_PROGRESS);
        } catch (AmbiguousException e) {
            final ResultWithExecutionDetails<?> nok = ResultWithExecutionDetails.nok(HttpStatus.CONFLICT.value(), String.format("The payload you're providing contains a shared identifier of the instances %s. Please merge those instances if they are reflecting the same entity.", e.getMessage()));
            throw new CancelProcessException(nok, HttpStatus.CONFLICT.value());
        }
    }


    public void createInvitation(UUID instanceId, UUID userId) {
        //TODO move permission check to authentication module
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId, DataStage.IN_PROGRESS);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.INVITE_FOR_REVIEW, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to invite somebody to this instance.");
        }
        this.invitation.inviteUserForInstance(instanceId, userId);
    }

    private InstanceId resolveIdOrThrowException(UUID instanceId, DataStage stage) {
        final InstanceId resolvedInstanceId = resolveId(instanceId, stage);
        if (resolvedInstanceId == null) {
            throw new InstanceNotFoundException(String.format("Instance %s not found", instanceId));
        }
        return resolvedInstanceId;
    }

    public void revokeInvitation(UUID instanceId, UUID userId) {
        //TODO move permission check to authentication module
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId, DataStage.IN_PROGRESS);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.INVITE_FOR_REVIEW, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to invite somebody to this instance.");
        }
        this.invitation.revokeUserInvitation(instanceId, userId);
    }

    public List<String> listInvitedUserIds(UUID instanceId) {
        //TODO move permission check to authentication module
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId, DataStage.IN_PROGRESS);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.INVITE_FOR_REVIEW, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to list the invitations for this instance");
        }
        return this.invitation.listInvitedUserIds(instanceId);
    }

    public List<UUID> listInstancesWithInvitations() {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.LIST_INVITATIONS)) {
            throw new UnauthorizedException("You don't have the right to list instances with invitations");
        }
        return this.invitation.listInstances();
    }

    public void calculateInstanceInvitationScope(UUID instanceId) {
        final InstanceId resolvedInstanceId = resolveIdOrThrowException(instanceId, DataStage.IN_PROGRESS);
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.UPDATE_INVITATIONS, resolvedInstanceId.getSpace(), instanceId)) {
            throw new UnauthorizedException("You don't have the right to recalculate the invitation scope for this instance");
        }
        this.scopes.calculateInstanceScope(instanceId);
    }

    private void checkIdForCreation(UUID uuid, Set<String> identifiers){
        final InstanceId id = findIdForContribution(uuid, identifiers);
        if(id!=null){
            final ResultWithExecutionDetails<?> nok = ResultWithExecutionDetails.nok(HttpStatus.CONFLICT.value(), String.format("The payload you're providing is pointing to the instance %s (either by the %s or the %s field it contains). Please do a PUT or a PATCH to the mentioned id instead.", id.serialize(), JsonLdConsts.ID, SchemaOrgVocabulary.IDENTIFIER), id.getUuid());
            throw new CancelProcessException(nok, HttpStatus.CONFLICT.value());
        }
    }

    /**
     * Creates a new instance in the "in progress" section.
     * @param normalizedJsonLd
     * @param id
     * @param spaceName
     * @param responseConfiguration
     * @return
     *
     * @throws CancelProcessException in case an instance with the same id (or one of the given identifiers in http://schema.org/identifier) already exists in the DB
     */
    public ResultWithExecutionDetails<NormalizedJsonLd> createNewInstance(NormalizedJsonLd normalizedJsonLd, UUID id, SpaceName spaceName, ExtendedResponseConfiguration responseConfiguration) {
        checkIdForCreation(id, normalizedJsonLd.allIdentifiersIncludingId());
        normalizedJsonLd.setFieldUpdateTimes(new NormalizedJsonLd.FieldUpdateTimes(normalizedJsonLd.keySet().stream().collect(Collectors.toMap(k -> k, k -> ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)))));
        Event upsertEvent = createUpsertEvent(id, normalizedJsonLd, spaceName);
        InstanceId instanceId = events.postEvent(upsertEvent);
        return handleIngestionResponse(responseConfiguration, Collections.singleton(instanceId));
    }


    public ResultWithExecutionDetails<NormalizedJsonLd> contributeToInstance(NormalizedJsonLd normalizedJsonLd, InstanceId instanceId, boolean removeNonDeclaredProperties, ResponseConfiguration responseConfiguration) {
        normalizedJsonLd = patchInstance(instanceId, normalizedJsonLd, removeNonDeclaredProperties);
        Event upsertEvent = createUpsertEvent(instanceId.getUuid(), normalizedJsonLd, instanceId.getSpace());
        InstanceId updatedInstanceId = events.postEvent(upsertEvent);
        return handleIngestionResponse(responseConfiguration, Collections.singleton(updatedInstanceId));
    }


    public ResultWithExecutionDetails<Void> deleteInstance(UUID uuid) {
        try {
            ReleaseStatus releaseStatus = instances.getReleaseStatus(uuid, ReleaseTreeScope.TOP_INSTANCE_ONLY);
            if(releaseStatus != ReleaseStatus.UNRELEASED){
                throw new ResultBasedException(ResultWithExecutionDetails.nok(HttpStatus.CONFLICT.value(), "Was not able to remove instance because it is released still", uuid));
            }
            events.postEvent(Event.createDeleteEvent(uuid));
        }
        catch (InstanceNotFoundException e) {
            // Since it's a delete, we are ok with the instance not being available any longer.
        }
        return ResultWithExecutionDetails.ok();
    }

    public ResultWithExecutionDetails<NormalizedJsonLd> moveInstance(UUID id, SpaceName targetSpace, ExtendedResponseConfiguration responseConfiguration) {
        ReleaseStatus releaseStatus = getReleaseStatus(id, ReleaseTreeScope.TOP_INSTANCE_ONLY);
        if (releaseStatus != null && releaseStatus != ReleaseStatus.UNRELEASED) {
            throw new ResultBasedException(ResultWithExecutionDetails.nok(HttpStatus.CONFLICT.value(), "Was not able to move an instance because it is released still", id));
        }
        NormalizedJsonLd instance = instances.getInstanceById(id, DataStage.IN_PROGRESS, true);
        if (instance == null) {
            throw new InstanceNotFoundException(id);
        } else {
            if (permissions.hasPermission(authContext.getUserWithRoles(), Functionality.CREATE, targetSpace)) {
                //FIXME make this transactional.
                deleteInstance(id);
                return createNewInstance(instance, id, targetSpace, responseConfiguration);
            } else {
                throw new ForbiddenException(String.format("You are not allowed to move an instance to the space %s", targetSpace));
            }
        }
    }


    private Event createUpsertEvent(UUID id, NormalizedJsonLd normalizedJsonLd, SpaceName s) {
        return Event.createUpsertEvent(s, id, Event.Type.INSERT, normalizedJsonLd);
    }


    private NormalizedJsonLd patchInstance(InstanceId instanceId, NormalizedJsonLd normalizedJsonLd, boolean removeNonDefinedKeys) {
        NormalizedJsonLd instance = instances.getNativeInstanceById(instanceId.getUuid(), authContext.getUserId());
        if (instance == null) {
            NormalizedJsonLd.FieldUpdateTimes updateTimes = new NormalizedJsonLd.FieldUpdateTimes();
            normalizedJsonLd.keySet().forEach(k -> updateTimes.put(k, ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)));
            normalizedJsonLd.setFieldUpdateTimes(updateTimes);
            return normalizedJsonLd;
        } else {
            NormalizedJsonLd.FieldUpdateTimes updateTimesFromInstance = instance.getFieldUpdateTimes();
            NormalizedJsonLd.FieldUpdateTimes updateTimes = updateTimesFromInstance != null ? updateTimesFromInstance : new NormalizedJsonLd.FieldUpdateTimes();
            Set<String> oldKeys = new HashSet<>(instance.keySet());
            normalizedJsonLd.keySet().forEach(k -> {
                Object value = normalizedJsonLd.get(k);
                if (value != null && value.equals(EBRAINSVocabulary.RESET_VALUE)) {
                    updateTimes.remove(k);
                    instance.remove(k);
                } else {
                    updateTimes.put(k, ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC));
                    instance.put(k, value);
                }
            });
            if (removeNonDefinedKeys) {
                oldKeys.removeAll(normalizedJsonLd.keySet());
                oldKeys.forEach(k -> {
                    instance.remove(k);
                    updateTimes.remove(k);
                });
            }
            instance.setFieldUpdateTimes(updateTimes);
            return instance;
        }
    }

    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<UUID> ids, DataStage stage, ExtendedResponseConfiguration responseConfiguration, String typeRestriction) {
        List<InstanceId> instanceIds = resolveIdsByUUID(ids, true, stage);
        return getInstancesByInstanceIds(instanceIds, stage, responseConfiguration, typeRestriction);
    }

    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByInstanceIds(Collection<InstanceId> instanceIds, DataStage stage, ExtendedResponseConfiguration responseConfiguration, String typeRestriction) {
        Map<UUID, Result<NormalizedJsonLd>> result = new HashMap<>();
        instanceIds.stream().filter(InstanceId::isUnresolved).forEach(id -> result.put(id.getUuid(), ResultWithExecutionDetails.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase())));
        result.putAll(instances.getInstancesByIds(instanceIds.stream().map(InstanceId::getUuid).toList(), stage, typeRestriction, responseConfiguration.isReturnPayload(), responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize()));
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, result.values());
        }
        return result;
    }

    public Paginated<NormalizedJsonLd> getInstances(DataStage stage, Type type, SpaceName space, String searchByLabel, String filterProperty, String filterValue, ResponseConfiguration responseConfiguration, PaginationParam paginationParam) {
        Paginated<NormalizedJsonLd> instancesByType = instances.getInstancesByType(stage, type != null ? type.getName() : null, space != null ? space.getName() : null, searchByLabel, filterProperty, filterValue, responseConfiguration.isReturnPayload(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnEmbedded(), paginationParam);
        if (responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, instancesByType.getData().stream().map(ResultWithExecutionDetails::ok).collect(Collectors.toList()));
        }
        return instancesByType;
    }

    public ResultWithExecutionDetails<NormalizedJsonLd> handleIngestionResponse(ResponseConfiguration responseConfiguration, Set<InstanceId> instanceIds) {
        ResultWithExecutionDetails<NormalizedJsonLd> result;
        final SpaceName privateSpaceName = authContext.getUserWithRoles().getPrivateSpace();
        if (responseConfiguration.isReturnPayload()) {
            Map<UUID, Result<NormalizedJsonLd>> instancesByIds = instances.getInstancesByIds(instanceIds.stream().map(InstanceId::getUuid).collect(Collectors.toList()),
                    DataStage.IN_PROGRESS, null,
                    responseConfiguration.isReturnPayload(),
                    responseConfiguration.isReturnEmbedded(),
                    responseConfiguration.isReturnAlternatives(),
                    responseConfiguration instanceof ExtendedResponseConfiguration && ((ExtendedResponseConfiguration) responseConfiguration).isReturnIncomingLinks(), responseConfiguration instanceof ExtendedResponseConfiguration ? ((ExtendedResponseConfiguration) responseConfiguration).getIncomingLinksPageSize() : null);
            if (responseConfiguration.isReturnPermissions()) {
                enrichWithPermissionInformation(DataStage.IN_PROGRESS, instancesByIds.values());
            }
            result = AmbiguousResult.ok(instancesByIds.values().stream().map(Result::getData).map(r -> r.renameSpace(privateSpaceName, isInvited(r))).collect(Collectors.toList()));
        } else {
            result = AmbiguousResult.ok(instanceIds.stream().map(id -> {
                NormalizedJsonLd jsonLd = new NormalizedJsonLd();
                jsonLd.setId(idUtils.buildAbsoluteUrl(id.getUuid()));
                jsonLd.addProperty(EBRAINSVocabulary.META_SPACE, id.getSpace().getName());
                jsonLd.renameSpace(privateSpaceName, isInvited(jsonLd));
                return jsonLd;
            }).collect(Collectors.toList()));
        }
        return result;
    }

    public Paginated<NormalizedJsonLd> getIncomingLinks(UUID id, DataStage stage, String property, Type type, PaginationParam pagination) {
        InstanceId instanceId = resolveId(id, stage);
        if (instanceId == null) {
            return null;
        }
        final SpaceName privateSpaceName = authContext.getUserWithRoles().getPrivateSpace();
        final Paginated<NormalizedJsonLd> incomingLinks = instances.getIncomingLinks(instanceId.getSpace().getName(), instanceId.getUuid(), stage, property, type.getName(), pagination);
        incomingLinks.getData().forEach(d -> d.renameSpace(privateSpaceName, isInvited(d)));
        return incomingLinks;
    }

    public NormalizedJsonLd getInstanceById(UUID id, DataStage stage, ExtendedResponseConfiguration responseConfiguration) {
        NormalizedJsonLd instanceById = instances.getInstanceById(id, stage, responseConfiguration.isReturnPayload(), responseConfiguration.isReturnEmbedded(), responseConfiguration.isReturnAlternatives(), responseConfiguration.isReturnIncomingLinks(), responseConfiguration.getIncomingLinksPageSize(), true);
        if (instanceById != null && responseConfiguration.isReturnPermissions()) {
            enrichWithPermissionInformation(stage, Collections.singletonList(ResultWithExecutionDetails.ok(instanceById)));
        }
        return instanceById;
    }

    public boolean isInvited(NormalizedJsonLd normalizedJsonLd) {
        final UUID uuid = idUtils.getUUID(normalizedJsonLd.id());
        if (authContext.getUserWithRoles().getInvitations().contains(uuid)) {
            //The user is invited for this instance
            final String space = normalizedJsonLd.getAs(EBRAINSVocabulary.META_SPACE, String.class, null);
            if (space != null) {
                return !permissions.hasPermission(authContext.getUserWithRoles(), Functionality.READ, SpaceName.fromString(space));
            }
        }
        return false;
    }


    private void enrichWithPermissionInformation(DataStage stage, Collection<Result<NormalizedJsonLd>> documents) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();
        documents.forEach(result -> {
                    NormalizedJsonLd doc = result.getData();
                    if (doc != null) {
                        String space = doc.getAs(EBRAINSVocabulary.META_SPACE, String.class);

                        // standard SpaceName or resolves the real one if it's a private space
                        SpaceName sp = space != null ? authContext.resolveSpaceName(space) : null;

                        Set<Functionality> functionalities = permissions.stream().filter(p -> Functionality.FunctionalityGroup.INSTANCE == p.getFunctionality().getFunctionalityGroup() && stage != null && stage == p.getFunctionality().getStage()).filter(p -> p.appliesTo(sp, idUtils.getUUID(doc.id()))).map(FunctionalityInstance::getFunctionality).collect(Collectors.toSet());
                        doc.put(EBRAINSVocabulary.META_PERMISSIONS, functionalities);
                    }
                }
        );
    }

    private void enrichWithPermissionInformation(DataStage stage, ScopeElement scopeElement, List<FunctionalityInstance> permissions) {
        SpaceName sp = scopeElement.getSpace() != null ? new SpaceName(scopeElement.getSpace()) : null;
        scopeElement.setPermissions(permissions.stream().filter(p -> Functionality.FunctionalityGroup.INSTANCE == p.getFunctionality().getFunctionalityGroup() && stage != null && stage == p.getFunctionality().getStage()).filter(p -> p.appliesTo(sp, scopeElement.getId())).map(FunctionalityInstance::getFunctionality).collect(Collectors.toSet()));
        if (scopeElement.getChildren() != null) {
            scopeElement.getChildren().forEach(c -> enrichWithPermissionInformation(stage, c, permissions));
        }
    }

    public ScopeElement getScopeForInstance(UUID id, DataStage stage, boolean returnPermissions, boolean applyRestrictions) {
        InstanceId instanceId = resolveId(id, stage);
        if (instanceId != null) {
            ScopeElement scope = graphDB.getScopeForInstance(instanceId.getSpace().getName(), instanceId.getUuid(), stage, applyRestrictions);
            if (returnPermissions) {
                enrichWithPermissionInformation(stage, scope, authContext.getUserWithRoles().getPermissions());
            }
            return scope;
        }
        return null;
    }

    public GraphEntity getNeighbors(UUID id, DataStage stage) {
        InstanceId instanceId = resolveId(id, stage);
        if (instanceId != null) {
            return instances.getNeighbors(instanceId.getSpace().getName(), instanceId.getUuid(), stage);
        }
        return null;
    }

    public void release(UUID uuid){
        events.postEvent(Event.createReleaseEvent(uuid));
    }


    public void unrelease(UUID uuid){
        events.postEvent(Event.createUnreleaseEvent(uuid));
    }

    public ReleaseStatus getReleaseStatus(UUID uuid, ReleaseTreeScope releaseTreeScope){
        return instances.getReleaseStatus(uuid,releaseTreeScope);
    }

    public Map<UUID, ReleaseStatus> getReleaseStatus(List<UUID> ids, ReleaseTreeScope releaseTreeScope){
        return instances.getReleaseStatus(ids, releaseTreeScope);
    }

}
