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

package org.marmotgraph.graphdb.arango.commons.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import org.marmotgraph.graphdb.arango.model.ArangoCollectionReference;
import org.marmotgraph.graphdb.arango.model.ArangoDocumentReference;
import org.marmotgraph.graphdb.arango.model.InternalSpace;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.ReleaseStatus;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.marmotgraph.graphdb.arango.commons.model.ArangoDocument;
import org.marmotgraph.graphdb.arango.commons.model.ArangoEdge;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntryHookDocuments {

    private final ArangoDatabases databases;
    private final GraphDBArangoUtils utils;

    public EntryHookDocuments(ArangoDatabases databases, GraphDBArangoUtils utils) {
        this.databases = databases;
        this.utils = utils;
    }

    public ArangoDocumentReference getOrCreateDocumentIdHookDocument(ArangoDocumentReference documentRef, ArangoDatabase database) {
        return getOrCreateSingleDocumentWithNamePayload(ArangoCollectionReference.fromSpace(InternalSpace.DOCUMENT_ID_SPACE).doc(documentRef.getDocumentId()), documentRef.getId(), database);
    }

    public ArangoDocumentReference getOrCreateTypeHookDocument(DataStage stage, String type) {
        return getOrCreateSingleDocumentWithNamePayload(ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE).docWithStableId(type), type, databases.getByStage(stage));
    }

    public ArangoDocumentReference getOrCreateReleaseStatusHookDocument(boolean doRelease) {
        return getOrCreateSingleDocumentWithNamePayload(ArangoCollectionReference.fromSpace(InternalSpace.RELEASE_STATUS_SPACE).docWithStableId(doRelease ? ReleaseStatus.RELEASED.name() :  ReleaseStatus.HAS_CHANGED.name()), doRelease ? ReleaseStatus.RELEASED.name() : ReleaseStatus.HAS_CHANGED.name(), databases.getByStage(DataStage.IN_PROGRESS));
    }


    private ArangoDocumentReference getOrCreateSingleDocumentWithNamePayload(ArangoDocumentReference documentId, String name, ArangoDatabase db) {
        ArangoCollection collection = utils.getOrCreateArangoCollection(db, documentId.getArangoCollectionReference());
        if (!collection.documentExists(documentId.getDocumentId().toString())) {
            //This is an indirection, since we only want to have the call to be synchronized when there is actually the need of creating the document.
            createSingleDocumentWithNamePayload(documentId, name, db);
        }
        return documentId;
    }

    private synchronized ArangoDocumentReference createSingleDocumentWithNamePayload(ArangoDocumentReference documentId, String name, ArangoDatabase db) {
        ArangoCollection collection = utils.getOrCreateArangoCollection(db, documentId.getArangoCollectionReference());
        //We check again if the document has been created in the meantime
        if (!collection.documentExists(documentId.getDocumentId().toString())) {
            ArangoDocument newDoc = ArangoDocument.create();
            newDoc.setReference(documentId);
            newDoc.getDoc().addProperty(SchemaOrgVocabulary.NAME, name);
            collection.insertDocument(newDoc.getDoc());
        }
        return documentId;
    }

    public ArangoEdge createEdgeFromHookDocument(ArangoCollectionReference edgeCollection, ArangoDocumentReference targetDocument, ArangoDocumentReference hookDocument, JsonLdId originalTo) {
        ArangoEdge arangoEdge = new ArangoEdge();
        arangoEdge.setFromReference(hookDocument);
        arangoEdge.setToReference(targetDocument);
        arangoEdge.setOriginalTo(originalTo);
        arangoEdge.redefineId(new ArangoDocumentReference(edgeCollection,  UUID.randomUUID()));
        return arangoEdge;
    }
}
