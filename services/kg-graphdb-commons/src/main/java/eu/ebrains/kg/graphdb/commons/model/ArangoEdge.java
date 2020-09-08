/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.graphdb.commons.model;

import com.google.gson.annotations.SerializedName;
import eu.ebrains.kg.arango.commons.aqlBuilder.ArangoVocabulary;
import eu.ebrains.kg.arango.commons.model.ArangoDocumentReference;
import eu.ebrains.kg.commons.TypeUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;

import java.util.UUID;

public class ArangoEdge implements ArangoInstance {

    @SerializedName("_orderNumber")
    private Integer orderNumber;

    @SerializedName(IndexedJsonLdDoc.ORIGINAL_TO)
    private String originalTo;

    @SerializedName(IndexedJsonLdDoc.ORIGINAL_DOCUMENT)
    private String originalDocument;

    @SerializedName("_originalLabel")
    private String originalLabel;

    @SerializedName(ArangoVocabulary.TO)
    private String to;

    @SerializedName(ArangoVocabulary.FROM)
    private String from;

    @SerializedName(ArangoVocabulary.KEY)
    private String key;

    @SerializedName("_id")
    private String id;

    private transient JsonLdId resolvedTargetId;

    public JsonLdId getResolvedTargetId() {
        return resolvedTargetId;
    }

    public void setResolvedTargetId(JsonLdId resolvedTargetId) {
        this.resolvedTargetId = resolvedTargetId;
    }

    public ArangoDocumentReference getTo() {
        return to != null ? ArangoDocumentReference.fromArangoId(to, false) : null;
    }

    public void setTo(ArangoDocumentReference to) {
        this.to = to != null ? to.getId() : null;
    }

    public ArangoDocumentReference getFrom() {
        return from != null ? ArangoDocumentReference.fromArangoId(from, false) : null;
    }

    public void setFrom(ArangoDocumentReference from) {
        this.from = from != null ? from.getId() : null;
    }

    @Override
    public NormalizedJsonLd dumpPayload() {
        return TypeUtils.translate(this, NormalizedJsonLd.class);
    }

    public UUID getKey() {
        return key != null ? UUID.fromString(key) : null;
    }

    private void setKey(UUID key) {
        this.key = key != null ? key.toString() : null;
    }

    public JsonLdId getOriginalTo(){
        return this.originalTo != null ? new JsonLdId(this.originalTo) : null;
    }

    public void setOriginalTo(JsonLdId originalTo){
        this.originalTo = originalTo!=null ? originalTo.getId() : null;
    }

    public void redefineId(ArangoDocumentReference reference){
        this.id = reference.getId();
        setKey(reference.getDocumentId());
    }

    public String getOriginalLabel() {
        return originalLabel;
    }

    public void setOriginalLabel(String originalLabel) {
        this.originalLabel = originalLabel;
    }

    public void setOriginalDocument(ArangoDocumentReference originalDocument) {
        this.originalDocument = originalDocument != null ? originalDocument.getId() : null;
    }

    public ArangoDocumentReference getOriginalDocument(){
        return this.originalDocument == null ? null : ArangoDocumentReference.fromArangoId(this.originalDocument, true);
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    @Override
    public ArangoDocumentReference getId() {
        return ArangoDocumentReference.fromArangoId(id, true);
    }
}
