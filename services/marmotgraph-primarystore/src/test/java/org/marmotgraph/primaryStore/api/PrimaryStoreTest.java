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

package org.marmotgraph.primaryStore.api;

import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.test.Simpsons;
import org.marmotgraph.test.TestCategories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Tag(TestCategories.API)
@Disabled //TODO fix test
public class PrimaryStoreTest {

    @Autowired
    PrimaryStoreEventsAPI primaryStore;

    @Test
    @Disabled("This is an integration test - make sure you wrap it with the docker-compose context it needs.")
    public void testRandomEvent(){
        NormalizedJsonLd data = new NormalizedJsonLd();
        data.addProperty("name", "test");
        data.setId(new JsonLdId("https://kg.ebrains.eu/api/instances/foo/bar"));
        Event e = new Event(Simpsons.SPACE_NAME, UUID.randomUUID(), data, Event.Type.INSERT, new Date());
        primaryStore.postEvent(e);
    }


}