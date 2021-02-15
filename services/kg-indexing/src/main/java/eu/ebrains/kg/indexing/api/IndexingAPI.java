/*
 * Copyright 2021 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.indexing.api;

import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.api.GraphDBTodoLists;
import eu.ebrains.kg.commons.api.Indexing;
import eu.ebrains.kg.commons.model.PersistedEvent;
import eu.ebrains.kg.commons.model.TodoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class IndexingAPI implements Indexing.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final GraphDBTodoLists.Client graphDBTodoLists;
    private final IdUtils idUtils;
    private int counter;

    public IndexingAPI(GraphDBTodoLists.Client graphDBTodoLists, IdUtils idUtils) {
        this.graphDBTodoLists = graphDBTodoLists;
        this.idUtils = idUtils;
    }

    @Override
    public void indexEvent(PersistedEvent event) {
        int eventNr = ++counter;
        logger.info(String.format("Received event for indexing: %s (%d)", event.getEventId(), eventNr));
        if (isValidEvent(event)) {
            logger.debug(String.format("Received event %s is valid", event.getEventId()));
            graphDBTodoLists.processTodoList(Collections.singletonList(TodoItem.fromEvent(event)), event.getDataStage());
            //TODO spatial search
            logger.info(String.format("Done indexing event %d", eventNr));
        } else {
            throw new IllegalArgumentException("Received an invalid event - was not able to process the payload");
        }
    }

    private boolean isValidEvent(PersistedEvent event) {
        return event.getData() != null && idUtils.getUUID(event.getData().id()) != null;
    }

}
