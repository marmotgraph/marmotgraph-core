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


import org.marmotgraph.graphdb.arango.Arango;
import org.marmotgraph.graphdb.arango.model.ArangoDocumentReference;
import org.marmotgraph.graphdb.arango.model.InternalSpace;
import org.marmotgraph.commons.TypeUtils;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.graphdb.arango.commons.controller.EntryHookDocuments;
import org.marmotgraph.graphdb.arango.commons.model.ArangoEdge;
import org.marmotgraph.graphdb.arango.ingestion.model.UpsertOperation;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Arango
public class ReleasingController {

    private final EntryHookDocuments entryHookDocuments;
    private final TypeUtils typeUtils;

    public ReleasingController(EntryHookDocuments entryHookDocuments, TypeUtils typeUtils) {
        this.entryHookDocuments = entryHookDocuments;
        this.typeUtils = typeUtils;
    }

    UpsertOperation getReleaseStatusUpdateOperation(ArangoDocumentReference documentReference, boolean doRelease) {
        ArangoDocumentReference releaseDocument = entryHookDocuments.getOrCreateReleaseStatusHookDocument(doRelease);
        ArangoEdge arangoEdge = entryHookDocuments.createEdgeFromHookDocument(InternalSpace.RELEASE_STATUS_EDGE_COLLECTION, documentReference, releaseDocument, null);
        ArangoDocumentReference releaseInstanceId = getReleaseStatusEdgeId(documentReference);
        arangoEdge.redefineId(releaseInstanceId);
        return new UpsertOperation(documentReference, typeUtils.translate(arangoEdge.getPayload(), NormalizedJsonLd.class), releaseInstanceId, true, false);
    }

    public ArangoDocumentReference getReleaseStatusEdgeId(ArangoDocumentReference documentReference) {
        return InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.doc(UUID.nameUUIDFromBytes(("releaseStatusInstance"+documentReference.getDocumentId().toString()).getBytes(StandardCharsets.UTF_8)));
    }

}
