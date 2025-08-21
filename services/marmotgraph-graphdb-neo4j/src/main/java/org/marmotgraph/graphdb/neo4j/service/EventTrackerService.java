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

package org.marmotgraph.graphdb.neo4j.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.marmotgraph.graphdb.neo4j.model.EventTracker;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EventTrackerService {

    private final EventTrackerRepository eventTrackerRepository;
    static final String CONSTANT_ID = "lastEvent";



    @PostConstruct
    @Transactional
    public void init() {
        eventTrackerRepository.findById(CONSTANT_ID).orElseGet(() -> {
            EventTracker e = new EventTracker();
            e.setId(CONSTANT_ID);
            e.setNumberOfEvents(0L);
            return eventTrackerRepository.save(e);
        });
    }

}
