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

package org.marmotgraph.graphdb.ingestion.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.models.EventTracker;
import org.marmotgraph.graphdb.commons.controller.ArangoDatabases;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component(value = "main-event-tracker")
public class MainEventTracker implements EventTracker {

    private String lastSeenEventId;
    private Long numberOfEventsProcessed;

    @Autowired
    ArangoDatabases arangoDatabases;


    public void updateLastSeenEventId(DataStage stage, String eventId) {
        if (eventId != null && !eventId.equals(getLastSeenEventId(stage))) {
            this.lastSeenEventId = eventId;
            this.numberOfEventsProcessed = this.numberOfEventsProcessed == null ? 1 : this.numberOfEventsProcessed + 1;
            saveLastSeenEventIdToDB(stage, eventId);
        }
    }

    public String getLastSeenEventId(DataStage stage) {
        if (this.lastSeenEventId == null) {
            loadLastSeenEventIdFromDB(stage);
        }
        return this.lastSeenEventId;
    }

    private ArangoCollection getEventTracking(DataStage stage) {
        ArangoDatabase nativeDB = arangoDatabases.getByStage(stage);
        ArangoCollection eventTracking = nativeDB.collection("eventTracking");
        if (!eventTracking.exists()) {
            eventTracking.create();
        }
        return eventTracking;
    }

    private void saveLastSeenEventIdToDB(DataStage stage, String lastSeenEventId) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("_key", "primaryStore");
        doc.put("lastEventId", lastSeenEventId);
        doc.put("numberOfEvents", this.numberOfEventsProcessed);
    }

    private String loadLastSeenEventIdFromDB(DataStage stage) {
        Map primaryStore = getEventTracking(stage).getDocument("primaryStore", Map.class);
        if (primaryStore != null) {
            this.lastSeenEventId = (String) primaryStore.get("lastEventId");
            this.numberOfEventsProcessed = (Long) primaryStore.get("numberOfEvents");
        }
        return this.lastSeenEventId;
    }

}
