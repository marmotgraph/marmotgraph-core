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

package org.marmotgraph.primaryStore.api.performance;

import org.junit.jupiter.api.Test;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.primaryStore.events.model.PrimaryStoreEvent;
import org.marmotgraph.primaryStore.events.service.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.UUID;

@DataJpaTest(showSql = false)
@Rollback(false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PerformanceForEvents {

    @Autowired
    PlatformTransactionManager txManager;

    @Autowired
    EventRepository eventRepo;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private PrimaryStoreEvent createEvent(){
        String payload = """
                 {"@type":["https://marmotgraph.org/types/MountainRange"],"@id":"https://kg.ebrains.eu/api/instances/38db785c-8759-4fd1-a559-6f0e83898979","https://marmotgraph.org/properties/name":"Alps","https://marmotgraph.org/properties/countries":[{"@id":"https://marmotgraph.org/instances/countries/Austria"},{"@id":"https://marmotgraph.org/instances/countries/France"},{"@id":"https://marmotgraph.org/instances/countries/Germany"},{"@id":"https://marmotgraph.org/instances/countries/Italy"},{"@id":"https://marmotgraph.org/instances/countries/Liechtenstein"},{"@id":"https://marmotgraph.org/instances/countries/Monaco"},{"@id":"https://marmotgraph.org/instances/countries/Slovenia"},{"@id":"https://marmotgraph.org/instances/countries/Switzerland"}],"http://schema.org/identifier":["https://marmotgraph.org/instances/mountainRanges/Alps"]}
                """;

        PrimaryStoreEvent event = new PrimaryStoreEvent();
        event.setUserId(UUID.randomUUID().toString());
        event.setSpace("demo");
        event.setType(Event.Type.INSERT);
        event.setIndexedTimestamp(new Date().getTime());
        event.setInstanceUUID(UUID.randomUUID());
        event.setJsonPayload(payload);
        event.setReportedTimestamp(new Date().getTime());
        return event;
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testPostEvent() throws IOException {

        final int executions = 20000000;
        int currentExecution = 0;
        Path file = Path.of("performanceTests/events.csv");
        Files.createDirectories(file.getParent());
        if(!Files.exists(file)) {
            Files.writeString(file, "");
        }
        long originalCount = eventRepo.count();
        while(currentExecution<executions){
            long eventCount = originalCount+currentExecution;
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.execute(status -> {
                long startTimestamp = System.nanoTime();
                eventRepo.saveAndFlush(createEvent());
                long endTimestamp = System.nanoTime();
                try {
                    Files.writeString(file, String.format("%d,%d\n", eventCount, endTimestamp-startTimestamp), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null; // transaction committed at end of block
            });
            currentExecution++;
        }
    }


}