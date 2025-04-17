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

package org.marmotgraph.authentication.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.OverwriteMode;
import jakarta.annotation.PostConstruct;
import org.marmotgraph.arango.commons.model.ArangoDatabaseProxy;
import org.marmotgraph.authentication.model.ArangoTermsOfUse;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.SetupLogic;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.model.TermsOfUse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class TermsOfUseRepository implements SetupLogic {
    private final ArangoDatabaseProxy arangoDatabase;
    private final JsonAdapter jsonAdapter;


    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist("termsOfUse");
    }

    public TermsOfUseRepository(@Qualifier("termsOfUseDB") ArangoDatabaseProxy arangoDatabase, JsonAdapter jsonAdapter) {
        this.arangoDatabase = arangoDatabase;
        this.jsonAdapter = jsonAdapter;
    }


    private ArangoCollection getTermsOfUseCollection() {
        ArangoDatabase database = arangoDatabase.getOrCreate();
        return database.collection("termsOfUse");
    }

    @Cacheable(CacheConstant.CACHE_KEYS_TERMS_OF_USE)
    public TermsOfUse getCurrentTermsOfUse() {
        return getTermsOfUseCollection().getDocument("current", TermsOfUse.class);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_TERMS_OF_USE, allEntries = true)
    public void setCurrentTermsOfUse(TermsOfUse termsOfUse) {
        if(termsOfUse==null || termsOfUse.getData() == null || termsOfUse.getVersion() == null){
            throw new IllegalArgumentException("Was receiving an invalid terms of use specification");
        }
        ArangoTermsOfUse versioned = new ArangoTermsOfUse(termsOfUse.getVersion(), termsOfUse.getData(), termsOfUse.getVersion());
        ArangoTermsOfUse current = new ArangoTermsOfUse(termsOfUse.getVersion(), termsOfUse.getData(), "current");
        getTermsOfUseCollection().insertDocuments(Arrays.asList(jsonAdapter.toJson(versioned), jsonAdapter.toJson(current)), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace).silent(true));
    }


}
