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

package org.marmotgraph.graphdb.arango.instances.controller;

import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.api.primaryStore.Instances;
import org.marmotgraph.commons.jsonld.*;
import org.marmotgraph.commons.markers.ExposesData;
import org.marmotgraph.commons.markers.ExposesIds;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.graphdb.arango.ArangoQueries;
import org.marmotgraph.graphdb.arango.Arango;
import org.marmotgraph.graphdb.arango.aqlbuilder.AQL;
import org.marmotgraph.graphdb.arango.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.graphdb.arango.commons.controller.ArangoDatabases;
import org.marmotgraph.graphdb.arango.commons.controller.GraphDBArangoUtils;
import org.marmotgraph.graphdb.arango.commons.controller.PermissionsController;
import org.marmotgraph.graphdb.arango.commons.model.ArangoDocument;
import org.marmotgraph.graphdb.arango.instances.model.ArangoRelation;
import org.marmotgraph.graphdb.arango.model.*;
import org.marmotgraph.graphdb.arango.structure.controller.MetaDataController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Arango
public class DocumentsRepository extends  AbstractRepository{

    private final ArangoDatabases databases;
    private final AuthContext authContext;
    private final PermissionsController permissionsController;
    private final MetaDataController metaDataController;
    private final JsonAdapter jsonAdapter;
    private final GraphDBArangoUtils graphDBArangoUtils;
    private final EmbeddedAndAlternativesRepository embeddedAndAlternatives;
    private final IdUtils idUtils;
    private final Permissions permissions;
    private final IncomingLinksRepository incomingLinks;
    private final Instances.Client instances;


    public ArangoDocument getDocument(DataStage stage, ArangoDocumentReference reference) {
        return ArangoDocument.from(databases.getByStage(stage).collection(reference.getArangoCollectionReference().getCollectionName()).getDocument(reference.getDocumentId().toString(), NormalizedJsonLd.class));
    }
    @ExposesIds
    public List<String> getDocumentIdsBySpace(SpaceName space) {
        //FIXME: Shouldn't those be restricted to the ones we have at least minimal read access rights?
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("FOR doc IN @@space"));
        bindVars.put("@space", ArangoCollectionReference.fromSpace(space).getCollectionName());
        aql.addLine(AQL.trust("FILTER doc." + IndexedJsonLdDoc.EMBEDDED + " == NULL"));
        aql.addLine(AQL.trust("RETURN doc.`" + IndexedJsonLdDoc.DOCUMENT_ID + "`"));
        return databases.getByStage(DataStage.NATIVE).query(aql.build().getValue(), String.class, bindVars, aql.getQueryOptions()).asListRemaining();
    }

    @ExposesData
    public Paginated<NormalizedJsonLd> getDocumentsByTypes(DataStage stage, Type typeWithLabelInfo, SpaceName space, String filterProperty, String filterValue, PaginationParam paginationParam, String search, boolean embedded, boolean alternatives, List<String> searchableProperties) {
        if (typeWithLabelInfo != null) {
            final UserWithRoles userWithRoles = authContext.getUserWithRoles();
            //TODO find label field for type (and client) and filter by search if set.
            ArangoDatabase database = databases.getByStage(stage);
            Map<String, Object> bindVars = new HashMap<>();
            AQL aql = new AQL();
            Tuple<DocumentsByTypeMode, Set<SpaceName>> restrictToSpaces = restrictToSpaces(typeWithLabelInfo, stage, space);
            DocumentsByTypeMode mode = restrictToSpaces.getA();
            if (mode != DocumentsByTypeMode.EMPTY) {
                if (search != null && InstanceId.deserialize(search) != null) {
                    mode = DocumentsByTypeMode.BY_ID;
                }
                Map<String, Object> whitelistFilter = null;
                switch (mode) {
                    case BY_ID, DYNAMIC -> {
                        whitelistFilter = permissionsController.whitelistFilterForReadInstances(metaDataController.getSpaceNames(stage, userWithRoles), userWithRoles, stage);
                        if (whitelistFilter != null) {
                            aql.specifyWhitelist();
                            bindVars.putAll(whitelistFilter);
                        }
                    }
                }
                switch (mode) {
                    case SIMPLE, DYNAMIC ->
                            iterateThroughTypeList(Collections.singletonList(typeWithLabelInfo), searchableProperties, bindVars, aql);
                }
                switch (mode) {
                    case BY_ID -> {
                        aql.indent().addLine(AQL.trust("LET v = DOCUMENT(@documentById)"));
                        aql.addLine(AQL.trust(String.format("FILTER @typeFilter IN v.`%s` AND v.`%s` == null", JsonLdConsts.TYPE, IndexedJsonLdDoc.EMBEDDED)));
                        bindVars.put("typeFilter", typeWithLabelInfo.getName());
                        bindVars.put("documentById", search);
                    }
                    case SIMPLE -> {
                        aql.indent().addLine(AQL.trust(String.format("FOR v IN @@singleSpace OPTIONS {indexHint: \"%s\"}", ArangoDatabaseProxy.BROWSE_AND_SEARCH_INDEX)));
                        aql.addLine(AQL.trust(String.format("FILTER @typeFilter IN v.`%s` AND v.`%s` == null", JsonLdConsts.TYPE, IndexedJsonLdDoc.EMBEDDED)));
                        bindVars.put("typeFilter", typeWithLabelInfo.getName());
                        bindVars.put("@singleSpace", ArangoCollectionReference.fromSpace(restrictToSpaces.getB().iterator().next()).getCollectionName());
                        if (filterProperty != null && filterValue != null) {
                            aql.addLine(AQL.trust("AND v.@property == @value"));
                            bindVars.put("property", filterProperty);
                            bindVars.put("value", getParsedFilterValue(filterValue));
                        }
                    }
                    case DYNAMIC -> {
                        graphDBArangoUtils.getOrCreateArangoCollection(database, InternalSpace.TYPE_EDGE_COLLECTION);
                        aql.indent().addLine(AQL.trust("FOR v IN 1..1 OUTBOUND typeDefinition.type @@typeRelationCollection"));
                        bindVars.put("@typeRelationCollection", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
                    }
                }
                switch (mode) {
                    case BY_ID, DYNAMIC -> {
                        if (whitelistFilter != null) {
                            aql.addDocumentFilterWithWhitelistFilter(AQL.trust("v"));
                        }
                        if (space != null) {
                            aql.addLine(AQL.trust("FILTER v." + ArangoVocabulary.COLLECTION + " == @spaceFilter"));
                            bindVars.put("spaceFilter", ArangoCollectionReference.fromSpace(space).getCollectionName());
                        }
                        if (filterProperty != null && filterValue != null) {
                            aql.addLine(AQL.trust("FILTER v.@property == @value"));
                            bindVars.put("property", filterProperty);
                            bindVars.put("value", getParsedFilterValue(filterValue));
                        }
                    }
                }
                switch (mode) {
                    case SIMPLE, DYNAMIC -> {
                        addSearchFilter(bindVars, aql, search, searchableProperties != null && !searchableProperties.isEmpty());
                        if (paginationParam.getSize() != null) {
                            //We only sort if there is pagination involved.
                            aql.addLine(AQL.trust(String.format("SORT v.%s, v.%s ASC", IndexedJsonLdDoc.LABEL, ArangoVocabulary.KEY)));
                        }
                        aql.addPagination(paginationParam);
                    }
                }
                aql.addLine(AQL.trust("RETURN v"));
                Paginated<NormalizedJsonLd> normalizedJsonLdPaginated = ArangoQueries.queryDocuments(database, new AQLQuery(aql, bindVars), null);
                embeddedAndAlternatives.handleAlternativesAndEmbedded(normalizedJsonLdPaginated.getData(), stage, alternatives, embedded);
                exposeRevision(normalizedJsonLdPaginated.getData());
                final SpaceName privateSpace = authContext.getUserWithRoles().getPrivateSpace();
                normalizedJsonLdPaginated.getData().forEach(r -> {
                    r.removeAllInternalProperties();
                    final String s = r.getAs(EBRAINSVocabulary.META_SPACE, String.class);
                    if (privateSpace.getName().equals(s)) {
                        r.put(EBRAINSVocabulary.META_SPACE, SpaceName.PRIVATE_SPACE);
                    }
                });
                return normalizedJsonLdPaginated;
            }
        }
        return new Paginated<>(Collections.emptyList(), 0L, 0, 0);
    }

    @ExposesData
    public List<NormalizedJsonLd> getDocumentsByIncomingRelation(DataStage stage, SpaceName space, UUID id, ArangoRelation relation, boolean useOriginalTo, boolean embedded, boolean alternatives) {
        return getDocumentsByRelation(stage, space, id, relation, true, useOriginalTo, embedded, alternatives);
    }

    private List<NormalizedJsonLd> getDocumentsByRelation(DataStage stage, SpaceName space, UUID id, ArangoRelation relation, boolean incoming, boolean useOriginalTo, boolean embedded, boolean alternatives) {
        List<NormalizedJsonLd> result = graphDBArangoUtils.getDocumentsByRelation(databases.getByStage(stage), space, id, relation, incoming, useOriginalTo);
        embeddedAndAlternatives.handleAlternativesAndEmbedded(result, stage, alternatives, embedded);
        exposeRevision(result);
        return result;
    }

    @ExposesData
    public List<NormalizedJsonLd> getNativeDocumentsByInstanceId(SpaceName space, UUID id) {
        ArangoDatabase db = databases.getByStage(DataStage.NATIVE);
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(space);
        if (db.collection(collectionReference.getCollectionName()).exists()) {
            //Because Arango doesn't support indexed filtering of array in array search, we expand the (very limited) list of identifiers of the root document and state them explicitly as individual filter elements. This way, the index applies and we profit from more speed.
            NormalizedJsonLd rootDocument = db.collection(collectionReference.getCollectionName()).getDocument(id.toString(), NormalizedJsonLd.class);
            if (rootDocument != null) {
                List<NormalizedJsonLd> result = doGetDocumentsByIdentifiers(rootDocument.allIdentifiersIncludingId(), DataStage.NATIVE, space);
                if (result != null) {
                    embeddedAndAlternatives.handleAlternativesAndEmbedded(result, DataStage.NATIVE, false, true);
                    return result;
                }
            }
        }
        return Collections.emptyList();
    }

    @ExposesData
    public List<NormalizedJsonLd> getDocumentsByIdentifiers(Set<String> allIdentifiersIncludingId, DataStage stage, SpaceName space, boolean embedded, boolean alternatives) {
        List<NormalizedJsonLd> normalizedJsonLds = doGetDocumentsByIdentifiers(allIdentifiersIncludingId, stage, space);
        embeddedAndAlternatives.handleAlternativesAndEmbedded(normalizedJsonLds, stage, alternatives, embedded);
        return normalizedJsonLds;
    }

    private List<NormalizedJsonLd> doGetDocumentsByIdentifiers(Set<String> allIdentifiersIncludingId, DataStage stage, SpaceName space) {
        ArangoDatabase db = databases.getByStage(stage);
        ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(space);
        if (!db.collection(collectionReference.getCollectionName()).exists()) {
            return Collections.emptyList();
        }
        if (allIdentifiersIncludingId != null && !allIdentifiersIncludingId.isEmpty()) {
            Iterator<String> identifiers = allIdentifiersIncludingId.iterator();
            AQL aql = new AQL();
            HashMap<String, Object> bindVars = new HashMap<>();
            aql.addLine(AQL.trust("FOR doc IN @@space"));
            bindVars.put("@space", collectionReference.getCollectionName());
            aql.addLine(AQL.trust("FILTER @firstIdentifier IN doc." + IndexedJsonLdDoc.IDENTIFIERS));
            bindVars.put("firstIdentifier", identifiers.next());
            int identifierCnt = 0;
            while (identifiers.hasNext()) {
                aql.addLine(AQL.trust("OR @id" + identifierCnt + " IN doc." + IndexedJsonLdDoc.IDENTIFIERS));
                bindVars.put("id" + identifierCnt, identifiers.next());
                identifierCnt++;
            }
            aql.addLine(AQL.trust("RETURN doc"));
            List<NormalizedJsonLd> result = db.query(aql.build().getValue(), NormalizedJsonLd.class, bindVars, new AqlQueryOptions()).asListRemaining();
            exposeRevision(result);
            return result;
        }
        return null;
    }






    public Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> getDocumentsByReferenceList(DataStage stage, List<ArangoDocumentReference> documentReferences, String typeRestriction) {
        ArangoDatabase db = databases.getByStage(stage);
        AQL aql = new AQL();

        int counter = 0;
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("RETURN {"));
        if (typeRestriction != null && !CollectionUtils.isEmpty(documentReferences)) {
            bindVars.put("typeRestriction", typeRestriction);
        }
        for (ArangoDocumentReference reference : documentReferences) {
            bindVars.put("doc" + counter, reference.getId());
            if (typeRestriction != null) {
                aql.addLine(AQL.trust("\"" + reference.getDocumentId() + "\": @typeRestriction IN DOCUMENT(@doc" + counter + ").`@type` ? DOCUMENT(@doc" + counter + ") : null"));
            } else {
                aql.addLine(AQL.trust("\"" + reference.getDocumentId() + "\": DOCUMENT(@doc" + counter + ")"));
            }
            counter++;
            if (counter < documentReferences.size()) {
                aql.add(AQL.trust(", "));
            }
        }
        aql.addLine(AQL.trust("}"));
        Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> result = new HashMap<>();
        List<NormalizedJsonLd> results = db.query(aql.build().getValue(), NormalizedJsonLd.class, bindVars, new AqlQueryOptions()).asListRemaining().stream().filter(Objects::nonNull).toList();
        if (!results.isEmpty()) {
            // The response object is always just a single dictionary
            NormalizedJsonLd singleResult = results.get(0);
            for (String uuid : singleResult.keySet()) {
                NormalizedJsonLd doc = singleResult.getAs(uuid, NormalizedJsonLd.class);
                if (doc != null) {
                    result.put(UUID.fromString(uuid), ResultWithExecutionDetails.ok(doc));
                } else {
                    result.put(UUID.fromString(uuid), ResultWithExecutionDetails.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()));
                }
            }
        }
        return result;
    }
    public Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> getDocumentsByIdList(DataStage stage, List<InstanceId> instanceIds, String typeRestriction, boolean embedded, boolean alternatives, boolean incomingLinks, Long incomingLinksPageSize) {
        return getDocumentsByIdList(stage, instanceIds, typeRestriction, embedded, alternatives, incomingLinks, incomingLinksPageSize, getInvitationDocuments());
    }

    private Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> getDocumentsByIdList(DataStage stage, List<InstanceId> instanceIds, String typeRestriction, boolean embedded, boolean alternatives, boolean incomingLinks, Long incomingLinksPageSize, List<NormalizedJsonLd> invitationDocuments) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();

        Set<InstanceId> hasReadPermissions = instanceIds.stream().filter(i -> permissions.hasPermission(userWithRoles, permissionsController.getReadFunctionality(stage), i.getSpace(), i.getUuid())).collect(Collectors.toSet());
        Set<InstanceId> hasOnlyMinimalReadPermissions = instanceIds.stream().filter(i -> !hasReadPermissions.contains(i) && permissions.hasPermission(userWithRoles, permissionsController.getMinimalReadFunctionality(stage), i.getSpace(), i.getUuid())).collect(Collectors.toSet());
        Set<InstanceId> hasNoPermissions = instanceIds.stream().filter(i -> !hasReadPermissions.contains(i) && !hasOnlyMinimalReadPermissions.contains(i)).collect(Collectors.toSet());

        Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> documentsByReferenceList = getDocumentsByReferenceListWithPostProcessing(stage, hasReadPermissions.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), typeRestriction, embedded, alternatives, incomingLinks, incomingLinksPageSize, invitationDocuments);
        Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> documentsByReferenceListWithMinimalReadAccess = getDocumentsByReferenceList(stage, hasOnlyMinimalReadPermissions.stream().map(ArangoDocumentReference::fromInstanceId).collect(Collectors.toList()), typeRestriction);

        //Reduce the payload to the minimal fields
        documentsByReferenceListWithMinimalReadAccess.values().stream().filter(Objects::nonNull).map(ResultWithExecutionDetails::getData).filter(Objects::nonNull).forEach(d -> d.keepPropertiesOnly(getMinimalFields(stage, d.types(), invitationDocuments)));
        documentsByReferenceList.putAll(documentsByReferenceListWithMinimalReadAccess);

        //Define responses for no-permission instances
        hasNoPermissions.forEach(i -> documentsByReferenceList.put(i.getUuid(), ResultWithExecutionDetails.nok(HttpStatus.FORBIDDEN.value(), String.format("You don't have rights to read id %s", i.getUuid()), i.getUuid())));
        return documentsByReferenceList;
    }



    public Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> getDocumentsByReferenceListWithPostProcessing(DataStage stage, List<ArangoDocumentReference> documentReferences, String typeRestriction, boolean embedded, boolean alternatives, boolean showIncomingLinks, Long incomingLinksPageSize, List<NormalizedJsonLd> invitationDocuments) {
        final Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> result = getDocumentsByReferenceList(stage, documentReferences, typeRestriction);
        final List<NormalizedJsonLd> normalizedJsonLds = result.values().stream().map(ResultWithExecutionDetails::getData).filter(Objects::nonNull).collect(Collectors.toList());
        if (!normalizedJsonLds.isEmpty()) {
            embeddedAndAlternatives.handleAlternativesAndEmbedded(normalizedJsonLds, stage, alternatives, embedded);
            exposeRevision(normalizedJsonLds);
            if (showIncomingLinks) {
                List<String> involvedTypes = normalizedJsonLds.stream().map(JsonLdDoc::types).flatMap(Collection::stream).distinct().collect(Collectors.toList());
                final Set<String> excludedTypes = metaDataController.getTypesByName(involvedTypes, stage, null, false, false, authContext.getUserWithRoles(), authContext.getClientSpace() != null ? authContext.getClientSpace().getName() : null, invitationDocuments).values().stream()
                        .map(t -> Type.fromPayload(t.getData())).filter(t -> t.getIgnoreIncomingLinks() != null && t.getIgnoreIncomingLinks()).map(Type::getName).collect(Collectors.toSet());
                List<ArangoDocumentReference> toInspectForIncomingLinks = normalizedJsonLds.stream().filter(n -> n.types().stream().noneMatch(excludedTypes::contains)).map(n -> ArangoDocument.from(n).getReference()).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(toInspectForIncomingLinks)) {
                    NormalizedJsonLd instanceIncomingLinks = incomingLinks.fetchIncomingLinks(toInspectForIncomingLinks, stage, 0L, incomingLinksPageSize, null, null);
                    if (!CollectionUtils.isEmpty(instanceIncomingLinks)) {
                        incomingLinks.resolveIncomingLinks(stage, instanceIncomingLinks, invitationDocuments);
                        normalizedJsonLds.forEach(d -> {
                            d.put(EBRAINSVocabulary.META_INCOMING_LINKS, instanceIncomingLinks.get(d.id().toString()));
                        });
                    }
                }
            }
            normalizedJsonLds.forEach(NormalizedJsonLd::removeAllInternalProperties);
        }
        return result;
    }

    public List<NormalizedJsonLd> getInvitationDocuments(){
        final List<UUID> invitations = authContext.getUserWithRoles().getInvitations();
        final List<InstanceId> values = instances.resolveIds(invitations.stream().distinct().map(id -> new IdWithAlternatives().setId(id).setAlternatives(Collections.singleton(id.toString()))).filter(Objects::nonNull).toList(), DataStage.IN_PROGRESS).values().stream().toList();
        final Map<UUID, ResultWithExecutionDetails<NormalizedJsonLd>> documentsByIdList = getDocumentsByIdList(DataStage.IN_PROGRESS, values, null, false, false, false, null, null);
        return documentsByIdList.values().stream().map(ResultWithExecutionDetails::getData).collect(Collectors.toList());
    }

    Set<String> getMinimalFields(DataStage stage, List<String> types, List<NormalizedJsonLd> invitationDocuments) {
        Set<String> keepProperties = new HashSet<>(metaDataController.getTypesByName(types, stage, null, false, false, authContext.getUserWithRoles(), authContext.getClientSpace()!=null ? authContext.getClientSpace().getName() : null, invitationDocuments).values().stream().filter(r -> r.getData() != null).map(r -> r.getData().getAs(EBRAINSVocabulary.META_TYPE_LABEL_PROPERTY, String.class)).filter(Objects::nonNull).collect(Collectors.toSet()));
        keepProperties.add(JsonLdConsts.ID);
        keepProperties.add(IndexedJsonLdDoc.LABEL);
        keepProperties.add(JsonLdConsts.TYPE);
        keepProperties.add(EBRAINSVocabulary.META_SPACE);
        return keepProperties;
    }

    private enum DocumentsByTypeMode {
        EMPTY, BY_ID, SIMPLE, DYNAMIC
    }


    private Object getParsedFilterValue(String filterValue) {

        try {
            return jsonAdapter.fromJson(filterValue, Map.class);
        } catch (RuntimeException runtimeException1) {
            try {
                return jsonAdapter.fromJson(filterValue, List.class);
            } catch (RuntimeException runtimeException2) {
                return filterValue;
            }
        }
    }

    private Tuple<DocumentsByTypeMode, Set<SpaceName>> restrictToSpaces(Type typeWithLabelInfo, DataStage stage, SpaceName spaceFilter) {
        Set<SpaceName> spaces = typeWithLabelInfo.getSpacesForInternalUse(authContext.getUserWithRoles().getPrivateSpace());
        if (spaceFilter != null) {
            spaces = spaces.stream().filter(s -> spaceFilter.getName().equals(s.getName())).collect(Collectors.toSet());
        }
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        if (!permissionsController.hasGlobalReadPermissions(userWithRoles, stage)) {
            //We filter out those spaces to which the user doesn't have read access to.
            spaces = permissionsController.removeSpacesWithoutReadAccess(spaces, userWithRoles, stage);
        }
        spaces = spaces.stream().filter(s -> databases.getByStage(stage).collection(ArangoCollectionReference.fromSpace(s).getCollectionName()).exists()).collect(Collectors.toSet());
        if (permissionsController.getInstancesWithExplicitPermission(userWithRoles, stage).isEmpty()) {
            //We can only make use of a simple mode if the user doesn't have explicit instance permissions
            //If so, we fall back to the slightly slower dynamic resolution since this is rather an edge case.
            if (spaces.isEmpty()) {
                return new Tuple<>(DocumentsByTypeMode.EMPTY, spaces);
            } else if (spaces.size() == 1) {
                return new Tuple<>(DocumentsByTypeMode.SIMPLE, spaces);
            }
        }
        return new Tuple<>(DocumentsByTypeMode.DYNAMIC, spaces);
    }
}
