/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

package eu.ebrains.kg.graphdb.types.controller;

import eu.ebrains.kg.arango.commons.model.ArangoCollectionReference;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.SpaceName;
import eu.ebrains.kg.graphdb.ingestion.controller.TodoListProcessor;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class ArangoRepositoryTypesTest {


    @Autowired
    ArangoRepositoryTypes arangoRepositoryTypes;

    @Autowired
    TodoListProcessor todoListProcessor;


    private static final DataStage STAGE = DataStage.IN_PROGRESS;
    private static final String CLIENT = "kgeditor";


    private final ArangoCollectionReference simpsons = ArangoCollectionReference.fromSpace(TestObjectFactory.SIMPSONS);


    @Test
    public void getAllTypes() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/homer.json"), STAGE, null);

        //When
        Paginated<NormalizedJsonLd> allTypes = arangoRepositoryTypes.getAllTypes(CLIENT, STAGE, true, true, true, null);

        //Then
        assertEquals(5, allTypes.getSize());
    }


    @Test
    public void getTypesForSpace() {
        //Given
        todoListProcessor.upsertDocument(simpsons.doc(UUID.randomUUID()), TestObjectFactory.createJsonLd( "simpsons/homer.json"), STAGE, null);

        //When
        Paginated<NormalizedJsonLd> allTypesForSimpsonsSpace = arangoRepositoryTypes.getTypesForSpace(CLIENT, STAGE, TestObjectFactory.SIMPSONS, true, true, true, null);
        Paginated<NormalizedJsonLd> allTypesForFooBarSpace = arangoRepositoryTypes.getTypesForSpace(CLIENT, STAGE, new SpaceName("foobar"), true, true, true, null);

        //Then
        assertEquals(5, allTypesForSimpsonsSpace.getSize());
        assertEquals(0, allTypesForFooBarSpace.getSize());
    }
}