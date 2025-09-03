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

package org.marmotgraph.graphdb.arango.commons.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.marmotgraph.graphdb.arango.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.graphdb.arango.model.ArangoDocumentReference;
import org.marmotgraph.commons.jsonld.IndexedJsonLdDoc;
import org.marmotgraph.commons.jsonld.JsonLdId;

import java.util.UUID;

public class ArangoEdge implements ArangoInstance {

    @JsonProperty("_orderNumber")
    private Integer orderNumber;

    @JsonProperty(IndexedJsonLdDoc.ORIGINAL_TO)
    private String originalTo;

    @JsonProperty(IndexedJsonLdDoc.ORIGINAL_DOCUMENT)
    private String originalDocument;

    @JsonProperty("_originalLabel")
    private String originalLabel;

    @JsonProperty(ArangoVocabulary.TO)
    private String to;

    @JsonProperty(ArangoVocabulary.FROM)
    private String from;

    @JsonProperty(ArangoVocabulary.KEY)
    private String key;

    @JsonProperty(ArangoVocabulary.ID)
    private String id;

    @JsonProperty(ArangoVocabulary.COLLECTION)
    private String collection;

    private transient JsonLdId resolvedTargetId;

    public JsonLdId getResolvedTargetId() {
        return resolvedTargetId;
    }

    public void setResolvedTargetId(JsonLdId resolvedTargetId) {
        this.resolvedTargetId = resolvedTargetId;
    }

    @JsonIgnore
    public ArangoDocumentReference getToReference() {
        return to != null ? ArangoDocumentReference.fromArangoId(to, false) : null;
    }

    @JsonIgnore
    public void setToReference(ArangoDocumentReference to) {
        this.to = to != null ? to.getId() : null;
    }

    @JsonIgnore
    public ArangoDocumentReference getFromReference() {
        return from != null ? ArangoDocumentReference.fromArangoId(from, false) : null;
    }

    @JsonIgnore
    public void setFromReference(ArangoDocumentReference from) {
        this.from = from != null ? from.getId() : null;
    }

//    @Override
//    public NormalizedJsonLd dumpPayload() {
//        return TypeUtils.translate(this, NormalizedJsonLd.class);
//    }

    public UUID getKey() {
        return key != null ? UUID.fromString(key) : null;
    }

    private void setKey(UUID key) {
        this.key = key != null ? key.toString() : null;
    }


    public JsonLdId getOriginalTo(){
        return this.originalTo != null ? new JsonLdId(this.originalTo) : null;
    }

    public void setOriginalTo(String originalTo){
        this.originalTo = originalTo;
    }

    @JsonIgnore
    public void redefineId(ArangoDocumentReference reference){
        this.id = reference.getId();
        setKey(reference.getDocumentId());
        defineCollectionById();
    }

    public String getOriginalLabel() {
        return originalLabel;
    }

    public void setOriginalLabel(String originalLabel) {
        this.originalLabel = originalLabel;
    }

    @JsonIgnore
    public void setOriginalDocumentReference(ArangoDocumentReference originalDocument) {
        this.originalDocument = originalDocument != null ? originalDocument.getId() : null;
    }

    @JsonIgnore
    public ArangoDocumentReference getOriginalDocumentReference(){
        return this.originalDocument == null ? null : ArangoDocumentReference.fromArangoId(this.originalDocument, true);
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    @JsonIgnore
    public ArangoDocumentReference getReference() {
        return ArangoDocumentReference.fromArangoId(id, true);
    }

    @JsonIgnore
    @Override
    public Object getPayload() {
        return this;
    }

    public String getCollection() {
        return collection;
    }

    @JsonIgnore
    public ArangoEdge defineCollectionById() {
        this.collection = getReference() != null ? getReference().getArangoCollectionReference().getCollectionName() : null;
        return this;
    }
}
