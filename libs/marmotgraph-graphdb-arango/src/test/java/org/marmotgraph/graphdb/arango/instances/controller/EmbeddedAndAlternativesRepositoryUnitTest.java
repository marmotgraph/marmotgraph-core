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

import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.api.primaryStore.Users;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.graphdb.arango.commons.controller.ArangoDatabases;
import org.marmotgraph.graphdb.arango.commons.controller.GraphDBArangoUtils;
import org.marmotgraph.test.JsonAdapter4Test;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EmbeddedAndAlternativesRepositoryUnitTest {

    @Test
    public void mergeEmbeddedDocuments() {
        //Given
        EmbeddedAndAlternativesRepository repository = new EmbeddedAndAlternativesRepository(Mockito.mock(Users.Client.class), Mockito.mock(IdUtils.class), Mockito.mock(ArangoDatabases.class), Mockito.mock(GraphDBArangoUtils.class));
        JsonAdapter jsonAdapter = new JsonAdapter4Test();
        String originalDoc = """
           {
              "helloWorld": {
                 "@id": "http://foobar"
              },
              "@id": "http://foo"
           }
           """;
        NormalizedJsonLd original = jsonAdapter.fromJson(originalDoc, NormalizedJsonLd.class);
        String embeddedDoc = """
           { 
              "name": "foobar"
           }
           """;
        Map<String, NormalizedJsonLd> embedded = new HashMap<>();
        embedded.put("http://foobar", jsonAdapter.fromJson(embeddedDoc, NormalizedJsonLd.class));

        //When
        repository.mergeEmbeddedDocuments(original, embedded);

        //Then
        assertEquals("{\"helloWorld\":{\"name\":\"foobar\"},\"@id\":\"http://foo\"}", jsonAdapter.toJson(original));
    }

}