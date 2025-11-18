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
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.jsonld.*;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.model.relations.IncomingRelation;
import org.marmotgraph.commons.model.relations.OutgoingRelation;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.primaryStore.events.exceptions.FailedEventException;
import org.marmotgraph.primaryStore.indexing.service.IndexingService;
import org.marmotgraph.primaryStore.instances.model.InstanceInformation;
import org.marmotgraph.primaryStore.instances.model.Space;
import org.marmotgraph.primaryStore.instances.service.PayloadService;
import org.marmotgraph.primaryStore.instances.service.Reconcile;
import org.marmotgraph.primaryStore.instances.service.SpaceService;
import org.marmotgraph.primaryStore.users.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
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
    private final JsonAdapter jsonAdapter;

    private final Logger logger = LoggerFactory.getLogger(getClass());

   public InstanceId postEvent(Event event) {
        SpaceName space = evaluateSpace(event).orElseThrow(() -> new IllegalStateException(String.format("Space information for instance %s is missing", event.getInstanceId())));
        //We only require the extended space information for INSERT and UPDATE
        Optional<Space> spaceInformation;
        switch (event.getType()){
            case INSERT:
            case UPDATE:
                spaceInformation = spaceService.getSpace(space);
                break;
            default:
                spaceInformation = Optional.empty();
        }
        PersistedEvent eventToPersist = checkPermission(event, space, spaceInformation);
        PersistedEvent persistedEvent = eventService.persistEvent(eventToPersist);
        processEvent(persistedEvent, spaceInformation);
        return new InstanceId(persistedEvent.getInstanceId(), persistedEvent.getSpaceName());
    }

    private Optional<SpaceName> evaluateSpace(Event event){
        Optional<SpaceName> space = Optional.ofNullable(event.getSpaceName());
        if(space.isEmpty()){
            //If the space information is not yet provided, we read it from the db (which requires
            space = payloadService.getSpace(event.getInstanceId());
        }
        return space;
    }

    private void processEvent(PersistedEvent persistedEvent, Optional<Space> spaceInformation) {
        switch(persistedEvent.getType()){
            case INSERT:
            case UPDATE:
                //We need to add the current payload explicitly because the transaction is not yet committed and therefore not returned by the db
                List<NormalizedJsonLd> sourceDocumentsForInstance = Stream.concat(payloadService.getSourceDocumentsForInstanceFromDB(persistedEvent.getInstanceId(), persistedEvent.getUserId()), Stream.of(persistedEvent.getData()).filter(Objects::nonNull)).toList();
                InferredJsonLdDoc inferredDocument = reconcile.reconcile(sourceDocumentsForInstance);
                NormalizedJsonLd inferredDocumentPayload = inferredDocument.asIndexed().getDoc();
                boolean autorelease = spaceInformation.isPresent() && spaceInformation.get().isAutoRelease();
                InstanceInformation instanceInformation = payloadService.upsertInstanceInformation(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), persistedEvent.getData().identifiers());
                Set<String> outgoingRelations = inferredDocumentPayload.findOutgoingRelations();
                inferredDocumentPayload.addIdsToEmbedded(persistedEvent.getInstanceId());
                Tuple<Set<IncomingRelation>, Set<OutgoingRelation>> incomingAndOutgoingRelations = payloadService.upsertInferredPayload(persistedEvent.getInstanceId(), instanceInformation, inferredDocumentPayload, outgoingRelations, inferredDocument.getAlternatives(), autorelease, persistedEvent.getReportedTimeStampInMs(), spaceInformation.isEmpty());
                Tuple<NormalizedJsonLd, Set<IncomingRelation>> preparedToIndex = payloadService.prepareToIndex(inferredDocumentPayload, incomingAndOutgoingRelations);
                indexing.upsert(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), preparedToIndex.getA(), DataStage.IN_PROGRESS, preparedToIndex.getB());
                if(autorelease){
                    Tuple<Set<IncomingRelation>, Set<OutgoingRelation>> releasedIncomingAndOutgoingRelations = payloadService.release(persistedEvent.getInstanceId(), inferredDocumentPayload, inferredDocumentPayload.findOutgoingRelations(), persistedEvent.getReportedTimeStampInMs(), instanceInformation, inferredDocumentPayload.types());
                    Tuple<NormalizedJsonLd, Set<IncomingRelation>> releasedPreparedToIndex = payloadService.prepareToIndex(inferredDocumentPayload, releasedIncomingAndOutgoingRelations);
                    indexing.upsert(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), releasedPreparedToIndex.getA(), DataStage.RELEASED, releasedPreparedToIndex.getB());
                }
                break;
            case DELETE:
                payloadService.delete(persistedEvent.getInstanceId());
                indexing.delete(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), DataStage.IN_PROGRESS);
                break;
            case RELEASE:
                NormalizedJsonLd payloadToRelease = payloadService.getPayloadToRelease(persistedEvent.getInstanceId());
                Tuple<Set<IncomingRelation>, Set<OutgoingRelation>> releasedIncomingAndOutgoingRelations = payloadService.release(persistedEvent.getInstanceId(), payloadToRelease, payloadToRelease.findOutgoingRelations(), persistedEvent.getReportedTimeStampInMs(), payloadService.getOrCreateGlobalInstanceInformation(persistedEvent.getInstanceId()), payloadToRelease.types());
                Tuple<NormalizedJsonLd, Set<IncomingRelation>> releasedPreparedToIndex = payloadService.prepareToIndex(payloadToRelease, releasedIncomingAndOutgoingRelations);
                indexing.upsert(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), releasedPreparedToIndex.getA(), DataStage.RELEASED, releasedPreparedToIndex.getB());
                break;
            case UNRELEASE:
                payloadService.unrelease(persistedEvent.getInstanceId());
                indexing.delete(persistedEvent.getInstanceId(), persistedEvent.getSpaceName(), DataStage.RELEASED);
                break;
        }
    }


    private PersistedEvent checkPermission(Event event, SpaceName space, Optional<Space> spaceInformation) {
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
                if(spaceInformation.isEmpty()){
                    //The space doesn't exist - this means the user has to have space creation rights to execute this insertion.
                    boolean spaceCreationPermission = permissions.hasPermission(userWithRoles, Functionality.MANAGE_SPACE, space);
                    if (!spaceCreationPermission) {
                        throw new ForbiddenException(String.format("The creation of this instance involves the creation of the non-existing space %s - you don't have the according rights to do so!", event.getSpaceName()));
                    }
                }
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
                if(permissions.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.RELEASE), space, event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                break;
            case UNRELEASE:
                if(permissions.hasPermission(userWithRoles, Functionality.withSemanticsForOperation(semantics, event.getType(), Functionality.UNRELEASE), space, event.getInstanceId())){
                    return new PersistedEvent(event, userWithRoles.getUser(), space);
                }
                break;
        }
        throw new ForbiddenException();
    }




}
