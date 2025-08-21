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

package org.marmotgraph.graphdb.arango;

import jakarta.validation.constraints.NotNull;
import org.marmotgraph.graphdb.arango.model.ArangoCollectionReference;
import org.marmotgraph.graphdb.arango.model.ArangoDocumentReference;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.api.authentication.Authentication;
import org.marmotgraph.commons.api.primaryStore.Ids;
import org.marmotgraph.commons.api.primaryStore.Users;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.graphdb.arango.commons.controller.ArangoDatabases;
import org.marmotgraph.graphdb.arango.ingestion.controller.TodoListProcessor;
import org.marmotgraph.test.factory.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

@SpringBootTest
@TestPropertySource(properties = {"KEYCLOAK_ISSUER_URI = http://invalid/", ""})
public class AbstractGraphTest {

    @MockitoBean
    protected Authentication.Client authClient;

    @Autowired
    protected TodoListProcessor todoListProcessor;

    @Autowired
    private ArangoDatabases arangoDatabases;

    @Autowired
    protected JsonAdapter jsonAdapter;

    @BeforeEach
    public void setup(){
        Mockito.doAnswer(a -> UserFactory.globalAdmin().getUserWithRoles()).when(authClient).getRoles();
        arangoDatabases.clearAll();
    }

    @MockitoBean
    protected Ids.Client ids;

    @MockitoBean
    protected Users.Client primaryStoreUsers;

    protected ArangoDocumentReference upsert(SpaceName spaceName, NormalizedJsonLd payload, DataStage stage){
        return upsert(spaceName, UUID.randomUUID(), payload, stage);
    }

    protected ArangoDocumentReference upsert(SpaceName spaceName, UUID uuid, @NotNull NormalizedJsonLd payload, DataStage stage){
        return todoListProcessor.upsertDocument(ArangoCollectionReference.fromSpace(spaceName).doc(uuid),payload, stage, spaceName);
    }
}
