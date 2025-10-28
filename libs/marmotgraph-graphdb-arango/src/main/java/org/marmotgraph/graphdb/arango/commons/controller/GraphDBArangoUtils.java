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
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.AqlQueryOptions;
import jakarta.annotation.PostConstruct;
import org.marmotgraph.graphdb.arango.Arango;
import org.marmotgraph.graphdb.arango.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.graphdb.arango.model.ArangoCollectionReference;
import org.marmotgraph.graphdb.arango.model.ArangoDatabaseProxy;
import org.marmotgraph.graphdb.arango.model.InternalSpace;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.jsonld.IndexedJsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.graphdb.arango.instances.model.ArangoRelation;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Arango
public class GraphDBArangoUtils {

    private final ArangoDatabases arangoDatabases;

    private final IdUtils idUtils;


    @PostConstruct
    public void setup() {
        arangoDatabases.setup();
    }

    public GraphDBArangoUtils(ArangoDatabases arangoDatabases, IdUtils idUtils) {
        this.arangoDatabases = arangoDatabases;
        this.idUtils = idUtils;
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_ARANGO_COLLECTION, key = "{#db.name(), #c.collectionName}")
    public ArangoCollection getOrCreateArangoCollection(ArangoDatabase db, ArangoCollectionReference c) {
        return ArangoDatabaseProxy.getOrCreateArangoCollection(db, c);
    }


    public boolean isInternalCollection(ArangoCollectionReference collectionReference) {
        return collectionReference.getCollectionName().startsWith("internal");
    }

    public List<NormalizedJsonLd> getDocumentsByRelation(ArangoDatabase db, SpaceName space, UUID id, ArangoRelation relation, boolean incoming, boolean useOriginalTo) {
        ArangoCollectionReference relationColl;
        if (relation.isInternal()) {
            relationColl = ArangoCollectionReference.fromSpace(new InternalSpace(relation.getRelationField()), true);
        } else {
            relationColl = new ArangoCollectionReference(relation.getRelationField(), true);
        }
        ArangoCollectionReference documentSpace = ArangoCollectionReference.fromSpace(space);
        if (documentSpace != null && db.collection(relationColl.getCollectionName()).exists() && db.collection(documentSpace.getCollectionName()).exists()) {
            String aql = "LET docs = (FOR d IN @@relation\n" +
                    "    FILTER d." + (incoming ? useOriginalTo ? IndexedJsonLdDoc.ORIGINAL_TO : ArangoVocabulary.TO : ArangoVocabulary.FROM) + " == @id \n" +
                    "    LET doc = DOCUMENT(d." + (incoming ? ArangoVocabulary.FROM : ArangoVocabulary.TO) + ") \n" +
                    "    FILTER IS_SAME_COLLECTION(@@space, doc) \n" +
                    "    RETURN doc) \n" +
                    "    FOR doc IN docs" +
                    "       RETURN DISTINCT doc";
            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("@relation", relationColl.getCollectionName());
            bindVars.put("@space", documentSpace.getCollectionName());
            bindVars.put("id", useOriginalTo ? idUtils.buildAbsoluteUrl(id).getId() : space.getName() + "/" + id);
            try(ArangoCursor<NormalizedJsonLd> result = db.query(aql, NormalizedJsonLd.class, bindVars, new AqlQueryOptions())){
                return result.asListRemaining();
            }
            catch (IOException e){
                throw new ArangoDBException(e.getMessage());
            }
        }
        return Collections.emptyList();
    }
}
