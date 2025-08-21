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

package org.marmotgraph.primaryStore.events.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.jsonld.InferredJsonLdDoc;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.primaryStore.events.exceptions.FailedEventException;
import org.marmotgraph.primaryStore.ids.service.IdService;
import org.marmotgraph.primaryStore.indexing.service.IndexingService;
import org.marmotgraph.primaryStore.instances.service.PayloadService;
import org.marmotgraph.primaryStore.instances.service.Reconcile;
import org.marmotgraph.primaryStore.structures.service.SpaceService;
import org.marmotgraph.primaryStore.users.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@AllArgsConstructor
@Component
public class EventProcessor {
    private final IndexingService indexing;
    private final Permissions permissions;
    private final AuthContext authContext;
    private final Reconcile reconcile;
    private final UserService userService;
    private final IdUtils idUtils;
    private final EventService eventService;
    private final PayloadService payloadService;
    private final SpaceService spaceService;
    private final IdService idService;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Transactional
    // We spawn the transaction across the whole event persistence to ensure that we don't end up with partial data
    public InstanceId postEvent(Event event) {
        SpaceName space = evaluateSpace(event);
        PersistedEvent eventToPersist = checkPermission(event, space);
        PersistedEvent persistedEvent = persistEvent(eventToPersist);
        processEvent(persistedEvent);
        return new InstanceId(persistedEvent.getInstanceId(), persistedEvent.getSpaceName());
    }

    private SpaceName evaluateSpace(Event event){
        SpaceName space = event.getSpaceName();
        if(space == null){
            //If the space information is not yet provided, we read it from the db (which requires
            space = idService.getSpace(event.getInstanceId());
        }
        return space;
    }

    private void processEvent(PersistedEvent persistedEvent) {
        switch(persistedEvent.getType()){
            case INSERT:
            case UPDATE:
                //We need to add the current payload explicitly because the transaction is not yet committed and therefore not returned by the db
                List<NormalizedJsonLd> sourceDocumentsForInstance = Stream.concat(payloadService.getSourceDocumentsForInstanceFromDB(persistedEvent.getInstanceId(), persistedEvent.getUserId()), Stream.of(persistedEvent.getData()).filter(Objects::nonNull)).toList();
                InferredJsonLdDoc inferredDocument = reconcile.reconcile(persistedEvent.getInstanceId(), sourceDocumentsForInstance);
                NormalizedJsonLd inferredDocumentPayload = inferredDocument.asIndexed().getDoc();
                boolean autorelease = spaceService.isAutoRelease(persistedEvent.getSpaceName());
                idService.upsertId(new IdWithAlternatives(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), persistedEvent.getData().identifiers()), DataStage.IN_PROGRESS);
                payloadService.upsertInferredPayload(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), inferredDocumentPayload, inferredDocument.getAlternatives(), autorelease, persistedEvent.getReportedTimeStampInMs());
                indexing.upsert(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), inferredDocumentPayload, DataStage.IN_PROGRESS);
                if(autorelease){
                    indexing.upsert(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), persistedEvent.getData(), DataStage.RELEASED);
                }
                break;
            case DELETE:
                idService.removeId(DataStage.IN_PROGRESS, persistedEvent.getInstanceId());
                payloadService.removeInferredPayload(persistedEvent.getInstanceId());
                indexing.delete(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), DataStage.IN_PROGRESS);
                break;
            case RELEASE:
                idService.upsertId(new IdWithAlternatives(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), persistedEvent.getData().identifiers()), DataStage.RELEASED);
                NormalizedJsonLd inferredPayload = payloadService.releaseExistingPayload(persistedEvent.getInstanceId(), persistedEvent.getReportedTimeStampInMs());
                indexing.upsert(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), inferredPayload, DataStage.RELEASED);
                break;
            case UNRELEASE:
                idService.removeId(DataStage.RELEASED, persistedEvent.getInstanceId());
                payloadService.removeReleasedPayload(persistedEvent.getInstanceId());
                indexing.delete(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), DataStage.RELEASED);
                break;
        }
    }


    private PersistedEvent checkPermission(Event event, SpaceName space) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        if (userWithRoles == null) {
            throw new UnauthorizedException("Can not persist an event without user information");
        }
        userService.save(userWithRoles.getUser());
        List<String> semantics = event.getData() != null && event.getData().types() != null ? event.getData().types() : Collections.emptyList();
        logger.info("Received event of type {} for instance {} in space {} by user {} via client {}",
                event.getType().name(),
                event.getInstanceId(),
                space != null ? space.getName() : null,
                userWithRoles.getUser().getUserName(),
                userWithRoles.getClientId() != null ? userWithRoles.getClientId() : "direct access"
        );
        Functionality functionality;
        switch (event.getType()) {
            case DELETE:
                functionality = Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.DELETE);
                if(permissions.hasPermission(userWithRoles, functionality, space, event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                break;
            case INSERT:
                //FIXME implement space creation if space doesn't exist yet.
//                if (event.getSpace() == null) {
//                    //The space doesn't exist - this means the user has to have space creation rights to execute this insertion.
//                    boolean spaceCreationPermission = permissions.hasPermission(userWithRoles, Functionality.MANAGE_SPACE, event.getSpaceName());
//                    if (!spaceCreationPermission) {
//                        throw new ForbiddenException(String.format("The creation of this instance involves the creation of the non-existing space %s - you don't have the according rights to do so!", event.getSpaceName()));
//                    }
//                }
                functionality = Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.CREATE);
                if(permissions.hasPermission(userWithRoles, functionality, space, event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                break;
            case UPDATE:
                functionality = Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.WRITE);
                if(permissions.hasPermission(userWithRoles, functionality, space, event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                else if(functionality == Functionality.WRITE){
                    if(permissions.hasPermission(userWithRoles, Functionality.SUGGEST, space, event.getInstanceId())){
                        PersistedEvent persistedEvent = new PersistedEvent(event, userWithRoles.getUser(), space);
                        persistedEvent.setSuggestion(true);
                        return persistedEvent;
                    }
                }
                break;
            case RELEASE:
                if(permissions.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.RELEASE), event.getSpaceName(), event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                break;
            case UNRELEASE:
                if(permissions.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.UNRELEASE), event.getSpaceName(), event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                break;
        }
        throw new ForbiddenException();
    }

    public PersistedEvent persistEvent(PersistedEvent eventToPersist) {
        try {
            ensureInternalIdInPayload(eventToPersist);
            if (Objects.requireNonNull(eventToPersist.getType()) == Event.Type.DELETE) {
                ReleaseStatus releaseStatus = payloadService.getReleaseStatus(eventToPersist.getInstanceId());
                if (releaseStatus != ReleaseStatus.UNRELEASED) {
                    throw new IllegalStateException(String.format("Was not able to remove instance %s because it is released still", eventToPersist.getInstanceId()));
                }
            }
            eventService.saveEvent(eventToPersist);
            switch (eventToPersist.getType()){
                case INSERT:
                case UPDATE:
                    payloadService.upsertNativePayloadFromEvent(eventToPersist);
                    break;
                case DELETE:
                    // The deletion causes all contributions to be removed as well.
                    payloadService.removeNativePayloadsFromEvent(eventToPersist);
                    break;
            }
        }
        catch (Exception e) {
            throw new FailedEventException(eventToPersist, e);
        }
        return eventToPersist;
    }

    private void ensureInternalIdInPayload(@NonNull PersistedEvent persistedEvent) {
        if (persistedEvent.getData() != null) {
            JsonLdId idFromPayload = persistedEvent.getData().id();
            if (idFromPayload != null) {
                //Save the original id as an "identifier"
                persistedEvent.getData().addIdentifiers(idFromPayload.getId());
            }
            //TODO don't prefix with absolute url
            persistedEvent.getData().setId(idUtils.buildAbsoluteUrl(persistedEvent.getInstanceId()));
        }
    }

}
