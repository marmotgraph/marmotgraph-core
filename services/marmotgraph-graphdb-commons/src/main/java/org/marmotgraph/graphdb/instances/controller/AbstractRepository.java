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

package org.marmotgraph.graphdb.instances.controller;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionsReadOptions;
import org.marmotgraph.arango.commons.aqlbuilder.AQL;
import org.marmotgraph.arango.commons.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.arango.commons.model.ArangoCollectionReference;
import org.marmotgraph.arango.commons.model.ArangoDocumentReference;
import org.marmotgraph.arango.commons.model.InternalSpace;
import org.marmotgraph.commons.jsonld.IndexedJsonLdDoc;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.Type;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.graphdb.commons.controller.ArangoDatabases;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractRepository {


    protected void iterateThroughTypeList(List<Type> types, List<String> searchableProperties, Map<String, Object> bindVars, AQL aql) {
        if (types.size() == 0) {
            aql.addLine(AQL.trust("FOR typeDefinition IN []"));
        } else if (types.size() == 1) {
            aql.addLine(AQL.trust("LET typeDefinition = "));
        } else {
            aql.addLine(AQL.trust("FOR typeDefinition IN ["));
        }
        if (types.size() > 0) {
            ArangoCollectionReference typeCollection = ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE);
            bindVars.put("@typeCollection", typeCollection.getCollectionName());
            for (int i = 0; i < types.size(); i++) {
                aql.addLine(AQL.trust(" {typeName: @typeName" + i + ", type: DOCUMENT(@@typeCollection, @documentId" + i + "), searchableProperties : @searchableProperties" + i + "}"));
                bindVars.put("documentId" + i, typeCollection.docWithStableId(types.get(i).getName()).getDocumentId().toString());
                bindVars.put("typeName" + i, types.get(i).getName());
                bindVars.put("searchableProperties" + i, null);
                if (searchableProperties != null && !searchableProperties.isEmpty()) {
                    bindVars.put("searchableProperties" + i, searchableProperties);
                }
                if (i < types.size() - 1) {
                    aql.add(AQL.trust(","));
                }
            }
        }
        if (types.size() > 1) {
            aql.addLine(AQL.trust("]"));
        }
    }


    protected void addSearchFilter(Map<String, Object> bindVars, AQL aql, String search, boolean withSearchableProperties) {
        if (search != null && !search.isBlank()) {
            List<String> searchTerms = Arrays.stream(search.trim().split(" ")).filter(s -> !s.isBlank()).map(s -> "%" + s.replaceAll("%", "") + "%").toList();
            if (!searchTerms.isEmpty()) {
                if (withSearchableProperties) {
                    aql.addLine(AQL.trust("LET found = (FOR name IN typeDefinition.searchableProperties FILTER "));
                    for (int i = 0; i < searchTerms.size(); i++) {
                        aql.addLine(AQL.trust("LIKE(v[name], @search" + i + ", true) "));
                        if (i < searchTerms.size() - 1) {
                            aql.add(AQL.trust("AND "));
                        }
                        bindVars.put("search" + i, searchTerms.get(i));
                    }
                    aql.addLine(AQL.trust("RETURN name) "));
                }
                aql.addLine(AQL.trust("FILTER "));
                for (int i = 0; i < searchTerms.size(); i++) {
                    aql.addLine(AQL.trust(String.format("LIKE(v.%s, @search%d, true)%s", IndexedJsonLdDoc.LABEL, i, withSearchableProperties ? " OR" : "")));
                    if (i < searchTerms.size() - 1) {
                        aql.add(AQL.trust("AND "));
                    }
                    bindVars.put("search" + i, searchTerms.get(i));
                }
                if (withSearchableProperties) {
                    aql.addLine(AQL.trust("LENGTH(found)>=1"));
                }
            }
        }
    }


    protected void exposeRevision(List<NormalizedJsonLd> documents) {
        documents.forEach(doc -> doc.put(EBRAINSVocabulary.META_REVISION, doc.get(ArangoVocabulary.REV)));
    }


    protected Map<UUID, String> getLabelsForInstances(DataStage stage, Set<InstanceId> ids, ArangoDatabases databases) {
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust(String.format("RETURN MERGE(FOR id IN ATTRIBUTES(@ids) RETURN { [ id ] : DOCUMENT(@ids[id]).%s })", IndexedJsonLdDoc.LABEL)));
        bindVars.put("ids", ids != null ? ids.stream().filter(Objects::nonNull).collect(Collectors.toMap(InstanceId::serialize, v -> ArangoDocumentReference.fromInstanceId(v).getId())) : null);
        List<JsonLdDoc> results = databases.getByStage(stage).query(aql.build().getValue(), JsonLdDoc.class, bindVars, new AqlQueryOptions()).asListRemaining();
        if (results.size() == 1) {
            JsonLdDoc map = results.get(0);
            Map<UUID, String> result = new HashMap<>();
            map.keySet().stream().filter(Objects::nonNull).forEach(k -> {
                UUID uuid = InstanceId.deserialize(k).getUuid();
                result.put(uuid, map.getAs(k, String.class));
            });
            return result;
        }
        return Collections.emptyMap();
    }

    protected Set<String> getAllEdgeCollections(ArangoDatabase db) {
        return db.getCollections(new CollectionsReadOptions().excludeSystem(true)).stream().filter(c ->
                //We're only interested in edges
                c.getType() == CollectionType.EDGES &&
                        //We want to exclude meta properties
                        !c.getName().startsWith(ArangoCollectionReference.fromSpace(new SpaceName(EBRAINSVocabulary.META), true).getCollectionName()) &&
                        //And we want to exclude the internal ones...
                        !InternalSpace.INTERNAL_NON_META_EDGES.contains(new ArangoCollectionReference(c.getName(), true))
        ).map(c -> AQL.preventAqlInjection(c.getName()).getValue()).collect(Collectors.toSet());
    }
}
