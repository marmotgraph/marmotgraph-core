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

package org.marmotgraph.primaryStore.events.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.services.JsonAdapter;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.commons.model.PersistedEvent;
import org.marmotgraph.commons.model.ReleaseStatus;
import org.marmotgraph.primaryStore.events.exceptions.FailedEventException;
import org.marmotgraph.primaryStore.events.model.PrimaryStoreEvent;
import org.marmotgraph.primaryStore.events.model.PrimaryStoreFailedEvent;
import org.marmotgraph.primaryStore.events.repositories.EventRepository;
import org.marmotgraph.primaryStore.instances.service.PayloadService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final JsonAdapter jsonAdapter;
    private final EntityManager entityManager;
    private final PayloadService payloadService;


    public void saveEvent(PersistedEvent event){
        eventRepository.save(PrimaryStoreEvent.fromPersistedEvent(event, jsonAdapter));
    }

    public void saveFailedEvent(PersistedEvent event, Exception e){
        eventRepository.save(PrimaryStoreFailedEvent.failedEvent(event, e, jsonAdapter));
    }

    @Cacheable("firstReleases")
    public String getFirstRelease(UUID instanceId){
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<PrimaryStoreEvent> root = criteriaQuery.from(PrimaryStoreEvent.class);
        criteriaQuery.select(root.get("indexedTimestamp"));
        criteriaQuery.where(criteriaBuilder.and(
                criteriaBuilder.equal(root.get("uuid"), instanceId),
                criteriaBuilder.equal(root.get("stage"), DataStage.RELEASED)
        ));
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("indexedTimestamp")));
        Long firstRelease = entityManager.createQuery(criteriaQuery).setMaxResults(1).getSingleResult();
        if(firstRelease != null){
            return Instant.ofEpochMilli(firstRelease).toString();
        }
        return null;

    }


    @Transactional
    public PersistedEvent persistEvent(PersistedEvent eventToPersist) {
        try {
            ensureInternalIdInPayload(eventToPersist);
            if (Objects.requireNonNull(eventToPersist.getType()) == Event.Type.DELETE) {
                ReleaseStatus releaseStatus = payloadService.getReleaseStatus(eventToPersist.getInstanceId());
                if (releaseStatus != ReleaseStatus.UNRELEASED) {
                    throw new IllegalStateException(String.format("Was not able to remove instance %s because it is released still", eventToPersist.getInstanceId()));
                }
            }
            saveEvent(eventToPersist);
            switch (eventToPersist.getType()){
                case INSERT:
                case UPDATE:
                    payloadService.upsertNativePayloadFromEvent(eventToPersist);
                    break;
                case DELETE:
                    // The deletion causes all contributions to be removed as well.
                    payloadService.removeNativePayloadsFromEvent(eventToPersist);
                    break;
            }
        }
        catch (Exception e) {
            throw new FailedEventException(eventToPersist, e);
        }
        return eventToPersist;
    }


    private void ensureInternalIdInPayload(@NonNull PersistedEvent persistedEvent) {
        if (persistedEvent.getData() != null) {
            String idFromPayload = persistedEvent.getData().id();
            if (idFromPayload != null) {
                //Save the original id as an "identifier"
                persistedEvent.getData().addIdentifiers(idFromPayload);
            }
            persistedEvent.getData().setId(persistedEvent.getInstanceId().toString());
        }
    }
}
