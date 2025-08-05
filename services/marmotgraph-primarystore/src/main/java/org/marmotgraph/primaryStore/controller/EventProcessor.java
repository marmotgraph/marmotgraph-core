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

package org.marmotgraph.primaryStore.controller;

import org.marmotgraph.commons.api.Indexing;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.commons.model.PersistedEvent;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.primaryStore.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EventProcessor {
    private final Indexing.Client indexing;

    private final EventService eventService;

    private final EventController eventController;

    private final InferenceProcessor inferenceProcessor;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EventProcessor(Indexing.Client indexing, EventService eventService, EventController eventController, InferenceProcessor inferenceProcessor) {
        this.indexing = indexing;
        this.eventService = eventService;
        this.eventController = eventController;
        this.inferenceProcessor = inferenceProcessor;
    }

    public Set<InstanceId> postEvent(Event event) {
        PersistedEvent persistedEvent = eventController.persistEvent(event, event.getType().getStage());
        List<PersistedEvent> inferredEvents = processEvent(persistedEvent);
        return inferredEvents.stream().map(e -> new InstanceId(e.getDocumentId(), e.getSpaceName())).collect(Collectors.toSet());
    }

    public List<PersistedEvent> processEvent(PersistedEvent persistedEvent) {
        try {
            indexing.indexEvent(persistedEvent);
        } catch (Exception e) {
            eventService.saveFailedEvent(persistedEvent, e);
            throw e;
        }
        if (persistedEvent.getDataStage() == DataStage.NATIVE) {
            return autoRelease(inferenceProcessor.triggerInference(persistedEvent.getSpaceName(), persistedEvent.getDocumentId()));
        }
        return Collections.emptyList();
    }

    public List<PersistedEvent> autoRelease(List<PersistedEvent> events) {
        events.forEach(e -> {
            if (e.getSpace() != null && e.getSpace().isAutoRelease()) {
                NormalizedJsonLd normalizedJsonLd = e.getData();
                normalizedJsonLd.removeAllInternalProperties();
                normalizedJsonLd.removeAllFieldsFromNamespace(EBRAINSVocabulary.META);
                postEvent(new Event(e.getSpaceName(), e.getDocumentId(), normalizedJsonLd, Event.Type.RELEASE, new Date()));
            }
        });
        return events;
    }

}
