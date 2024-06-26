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

import org.marmotgraph.commons.api.PrimaryStoreEvents;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.primaryStore.controller.EventProcessor;
import org.marmotgraph.primaryStore.controller.InferenceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class PrimaryStoreEventsAPI implements PrimaryStoreEvents.Client {

    private final EventProcessor eventProcessor;

    private final InferenceProcessor inferenceProcessor;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PrimaryStoreEventsAPI(EventProcessor eventProcessor, InferenceProcessor inferenceProcessor) {
        this.eventProcessor = eventProcessor;
        this.inferenceProcessor = inferenceProcessor;
    }

    @Override
    public void rerunEvents(String space) {
       logger.info(String.format("Received request for rerunning the events of space %s", space));
       eventProcessor.rerunEvents(SpaceName.fromString(space));
    }

    @Override
    public Set<InstanceId> postEvent(Event event) {
        return eventProcessor.postEvent(event);
    }


    @Override
    public void infer(String space, UUID id) {
        logger.info(String.format("Received request to re-infer the space %s", space));
        eventProcessor.autoRelease(inferenceProcessor.triggerInference(new SpaceName(space), id));
    }

}
