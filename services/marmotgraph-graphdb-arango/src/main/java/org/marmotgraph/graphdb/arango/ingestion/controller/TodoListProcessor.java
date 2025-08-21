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

package org.marmotgraph.graphdb.arango.ingestion.controller;

import jakarta.validation.constraints.NotNull;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.graphdb.arango.commons.controller.ArangoRepositoryCommons;
import org.marmotgraph.graphdb.arango.commons.model.ArangoInstance;
import org.marmotgraph.graphdb.arango.ingestion.model.DBOperation;
import org.marmotgraph.graphdb.arango.ingestion.model.EdgeResolutionOperation;
import org.marmotgraph.graphdb.arango.ingestion.model.RemoveReleaseStateOperation;
import org.marmotgraph.graphdb.arango.model.ArangoCollectionReference;
import org.marmotgraph.graphdb.arango.model.ArangoDocumentReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Profile("arango")
@Component
public class TodoListProcessor {

    private final ArangoRepositoryCommons repository;

    private final StructureSplitter splitter;

    private final DataController dataController;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReleasingController releasingController;


    public TodoListProcessor(ArangoRepositoryCommons repository, StructureSplitter splitter, DataController dataController, ReleasingController releasingController) {
        this.repository = repository;
        this.splitter = splitter;
        this.dataController = dataController;
        this.releasingController = releasingController;
    }


    public void delete(UUID instanceId, SpaceName spaceName, DataStage stage) {
        deleteDocument(stage, new ArangoDocumentReference(ArangoCollectionReference.fromSpace(spaceName), instanceId));
    }

    public void upsert(UUID instanceId, SpaceName spaceName, NormalizedJsonLd payload, DataStage stage){
        upsertDocument(new ArangoDocumentReference(ArangoCollectionReference.fromSpace(spaceName), instanceId), payload, stage, spaceName);
    }

    public void unrelease(UUID instanceId, SpaceName spaceName){
        unreleaseDocument(new ArangoDocumentReference(ArangoCollectionReference.fromSpace(spaceName), instanceId));
    }

    public void release(UUID instanceId, SpaceName spaceName){
        NormalizedJsonLd payload = null;
        //FIXME - we need to

        releaseDocument(new ArangoDocumentReference(ArangoCollectionReference.fromSpace(spaceName), instanceId), payload, spaceName);
    }

    private void unreleaseDocument(ArangoDocumentReference rootDocumentReference) {
        deleteDocument(DataStage.RELEASED, rootDocumentReference);
        repository.executeTransactional(DataStage.IN_PROGRESS, Collections.singletonList(new RemoveReleaseStateOperation(releasingController.getReleaseStatusEdgeId(rootDocumentReference))));
    }

    private void releaseDocument(ArangoDocumentReference rootDocumentReference, @NotNull NormalizedJsonLd payload, SpaceName spaceName) {
        // Releasing a specific revision
        upsertDocument(rootDocumentReference, payload, DataStage.RELEASED, spaceName);
        repository.executeTransactional(DataStage.IN_PROGRESS, Collections.singletonList(releasingController.getReleaseStatusUpdateOperation(rootDocumentReference, true)));
    }

    private boolean hasChangedReleaseStatus(DataStage stage, ArangoDocumentReference documentReference) {
        //TODO analyze payload for change by comparison with current instance - ignore alternatives
        return stage == DataStage.IN_PROGRESS;
    }

    public ArangoDocumentReference upsertDocument(ArangoDocumentReference rootDocumentRef, @NotNull NormalizedJsonLd payload, DataStage stage, SpaceName spaceName) {
        if(spaceName!=null){
            payload.put(EBRAINSVocabulary.META_SPACE, spaceName);
        }
        List<ArangoInstance> arangoInstances = splitter.extractRelations(rootDocumentRef, payload);
        List<DBOperation> upsertOperationsForDocument = dataController.createUpsertOperations(rootDocumentRef, stage, arangoInstances, hasChangedReleaseStatus(stage, rootDocumentRef));
        repository.executeTransactional(stage, upsertOperationsForDocument);
        List<EdgeResolutionOperation> lazyIdResolutionOperations;
        if (stage != DataStage.NATIVE) {
            //We don't need to resolve links in NATIVE and neither do META structures... it is sufficient if we do this in IN_PROGRESS and RELEASED
            lazyIdResolutionOperations = dataController.createResolutionsForPreviouslyUnresolved(stage, rootDocumentRef, payload.allIdentifiersIncludingId());
            repository.executeTransactional(stage, lazyIdResolutionOperations);
        }
        return rootDocumentRef;
    }


    public void deleteDocument(DataStage stage, ArangoDocumentReference documentReference) {
        if (repository.doesDocumentExist(stage, documentReference)) {
            final List<DBOperation> deleteOperations = dataController.createDeleteOperations(Collections.singletonList(documentReference));
            repository.executeTransactional(stage, deleteOperations);
        } else {
            logger.warn(String.format("Tried to remove non-existent document with id %s in stage %s", documentReference.getId(), stage.name()));
        }
    }
}
