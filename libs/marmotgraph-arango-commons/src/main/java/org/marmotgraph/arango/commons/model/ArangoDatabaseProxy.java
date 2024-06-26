/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.arango.commons.model;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.HashIndexOptions;
import com.arangodb.model.SkiplistIndexOptions;
import org.marmotgraph.arango.commons.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.commons.jsonld.IndexedJsonLdDoc;
import org.marmotgraph.commons.jsonld.JsonLdConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;

public class ArangoDatabaseProxy {

    public static final String BROWSE_AND_SEARCH_INDEX = "browseAndSearch";
    private static final Logger logger = LoggerFactory.getLogger(ArangoDatabaseProxy.class);

    private static final int ARANGO_CONNECTION_RETRIES = 5;

    private final ArangoDB arangoDB;
    private final String databaseName;
    private boolean exists;

    public ArangoDatabaseProxy(ArangoDB arangoDB, String databaseName) {
        this.arangoDB = arangoDB;
        this.databaseName = databaseName;
    }

    public synchronized void removeDatabase() {
        if (arangoDB.db(databaseName).exists()) {
            logger.info("Removing database {}", databaseName);
            arangoDB.db(databaseName).drop();
        }
        exists = false;
    }

    private void doCreateIfItDoesntExist(ArangoDatabase db, int retry){
        try {
            if (!db.exists()) {
                db.create();
            }
        }
        catch(ArangoDBException exception){
            if(retry>=ARANGO_CONNECTION_RETRIES){
                throw exception;
            }
            else{
                try {
                    int waitingTime = (int)Math.pow(2.0, retry+1.0);
                    logger.info("Was not able to connect to arango database - retry in {} secs...", waitingTime);
                    Thread.sleep(waitingTime*1000L);
                    doCreateIfItDoesntExist(db, retry+1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public synchronized void createIfItDoesntExist() {
        ArangoDatabase db = arangoDB.db(databaseName);
        doCreateIfItDoesntExist(db, 0);
    }

    public ArangoDatabase get() {
        return arangoDB.db(databaseName);
    }

    /**
     * Use {@link #createIfItDoesntExist()} in post construct and {@link #get()} whenever possible to not suffer from the lookup overhead.
     *
     * @return
     */
    public ArangoDatabase getOrCreate() {
        ArangoDatabase db = arangoDB.db(databaseName);
        if (!exists) {
            //If the database is flagged to not exist yet, we're asking the database to be sure.
            exists = db.exists();
        }
        if (!exists) {
            //The database really doesn't exist -> let's create it.
            db.create();
            exists = true;
        }
        return db;
    }

    public synchronized void createCollectionIfItDoesntExist(ArangoCollectionReference collection) {
        final ArangoDatabase db = get();
        ArangoCollection c = db.collection(collection.getCollectionName());
        if (!c.exists()) {
            db.createCollection(collection.getCollectionName(), new CollectionCreateOptions().type(collection.isEdge() != null && collection.isEdge() ? CollectionType.EDGES : CollectionType.DOCUMENT));
        }
    }

    public synchronized void createCollectionIfItDoesntExist(String collection) {
        ArangoCollection c = get().collection(collection);
        if (!c.exists()) {
            c.create();
        }
    }

    public static ArangoCollection getOrCreateArangoCollection(ArangoDatabase db, ArangoCollectionReference c) {
        ArangoCollection collection = db.collection(c.getCollectionName());
        if (!collection.exists()) {
            return createArangoCollection(db, c);
        }
        return collection;
    }

    private static synchronized ArangoCollection createArangoCollection(ArangoDatabase db, ArangoCollectionReference c) {
        ArangoCollection collection = db.collection(c.getCollectionName());
        //We check again, if the collection has been created in the meantime
        if (!collection.exists()) {
            logger.debug("Creating collection {}", c.getCollectionName());
            db.createCollection(c.getCollectionName(), new CollectionCreateOptions().waitForSync(true).type(c.isEdge() != null && c.isEdge() ? CollectionType.EDGES : CollectionType.DOCUMENT));
            ensureIndicesOnCollection(collection);
        }
        return collection;
    }


    public static void ensureIndicesOnCollection(ArangoCollection collection) {
        if(logger.isDebugEnabled()) {
            logger.debug("Ensuring indices properly set for collection {}", collection.name());
        }
        collection.ensureHashIndex(Collections.singleton(ArangoVocabulary.COLLECTION), new HashIndexOptions());
        collection.ensureSkiplistIndex(Collections.singletonList(JsonLdConsts.ID), new SkiplistIndexOptions());
        if (collection.getInfo().getType() == CollectionType.EDGES) {
            collection.ensureSkiplistIndex(Collections.singletonList(IndexedJsonLdDoc.ORIGINAL_TO), new SkiplistIndexOptions());
        } else {
            collection.ensureSkiplistIndex(Arrays.asList(JsonLdConsts.TYPE + "[*]", IndexedJsonLdDoc.EMBEDDED, IndexedJsonLdDoc.LABEL, ArangoVocabulary.KEY), new SkiplistIndexOptions().name(BROWSE_AND_SEARCH_INDEX));
            collection.ensureSkiplistIndex(Collections.singletonList(IndexedJsonLdDoc.IDENTIFIERS + "[*]"), new SkiplistIndexOptions());
            collection.ensureSkiplistIndex(Collections.singletonList(IndexedJsonLdDoc.EMBEDDED), new SkiplistIndexOptions());
        }
    }

}
