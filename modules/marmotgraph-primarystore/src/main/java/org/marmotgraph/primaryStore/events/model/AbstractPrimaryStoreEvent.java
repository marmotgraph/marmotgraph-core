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

package org.marmotgraph.primaryStore.events.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.services.JsonAdapter;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.commons.model.PersistedEvent;

import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
public abstract class AbstractPrimaryStoreEvent {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;
    private UUID instanceUUID;

    @Enumerated(EnumType.STRING)
    private Event.Type type;
    @Column(columnDefinition = "TEXT")
    private String jsonPayload;
    private String userId;
    private String space;
    private Long reportedTimestamp;
    private Long indexedTimestamp;
    private boolean suggestion;

    protected static <T extends AbstractPrimaryStoreEvent> T populateFromPersistedEvent(T e, PersistedEvent event, JsonAdapter jsonAdapter){
        e.setInstanceUUID(event.getInstanceId());
        e.setSuggestion(event.isSuggestion());
        e.setUserId(event.getUserId());
        e.setSpace(event.getSpaceName() != null ? event.getSpaceName().getName() : null);
        e.setType(event.getType());
        e.setReportedTimestamp(event.getReportedTimeStampInMs());
        e.setIndexedTimestamp(event.getIndexedTimestamp());
        e.setJsonPayload(jsonAdapter.toJson(event.getData()));
        return e;
    }
}
