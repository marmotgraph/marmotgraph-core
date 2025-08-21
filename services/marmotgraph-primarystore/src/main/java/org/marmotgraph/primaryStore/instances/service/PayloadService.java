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

package org.marmotgraph.primaryStore.instances.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.exception.InstanceNotFoundException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.PersistedEvent;
import org.marmotgraph.commons.model.ReleaseStatus;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.primaryStore.ids.service.IdService;
import org.marmotgraph.primaryStore.instances.model.InferredPayload;
import org.marmotgraph.primaryStore.instances.model.NativePayload;
import org.marmotgraph.primaryStore.instances.model.ReleasedPayload;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class PayloadService {

    private final JsonAdapter jsonAdapter;
    private final NativePayloadRepository nativeRepository;
    private final InferredPayloadRepository inferredPayloadRepository;
    private final ReleasedPayloadRepository releasedPayloadRepository;
    private final IdService idService;
    private final EntityManager entityManager;


    public void upsertInferredPayload(UUID instanceId, SpaceName spaceName, NormalizedJsonLd payload, JsonLdDoc alternatives, boolean autoRelease, Long reportedTimeStampInMs) {
        ReleaseStatus releaseStatus = ReleaseStatus.UNRELEASED;
        String newPayload = jsonAdapter.toJson(payload);
        ReleasedPayload releasedPayload = null;
        if (autoRelease) {
            releasedPayload = releasePayload(instanceId, spaceName.getName(), jsonAdapter.toJson(payload), reportedTimeStampInMs, payload.types());
            releaseStatus = ReleaseStatus.RELEASED;
        } else {
            Optional<ReleasedPayload> releasedInstance = releasedPayloadRepository.findById(instanceId);
            if (releasedInstance.isPresent()) {
                ReleasedPayload existingReleasedInstance = releasedInstance.get();
                if (existingReleasedInstance.getJsonPayload().equals(newPayload)) {
                    releaseStatus = ReleaseStatus.RELEASED;
                } else {
                    releaseStatus = ReleaseStatus.HAS_CHANGED;
                }
            }
        }
        InferredPayload inferredPayload = new InferredPayload();
        inferredPayload.setUuid(instanceId);
        inferredPayload.setAlternative(jsonAdapter.toJson(alternatives));
        inferredPayload.setJsonPayload(newPayload);
        inferredPayload.setSpaceName(spaceName.getName());
        inferredPayload.setTypes(payload.types());
        inferredPayload.setReleaseStatus(releaseStatus);
        if (releasedPayload != null) {
            inferredPayload.setFirstRelease(releasedPayload.getFirstRelease());
            inferredPayload.setLastRelease(releasedPayload.getLastRelease());
        }
        inferredPayloadRepository.save(inferredPayload);
    }

    private ReleasedPayload releasePayload(UUID instanceId, String spaceName, String jsonPayload, Long reportedTimestamp, List<String> types) {
        Optional<ReleasedPayload> byId = releasedPayloadRepository.findById(instanceId);
        Long firstReleaseTimestamp = reportedTimestamp;
        if (byId.isPresent()) {
            firstReleaseTimestamp = byId.get().getFirstRelease();
        }
        ReleasedPayload releasedPayload = new ReleasedPayload();
        releasedPayload.setUuid(instanceId);
        releasedPayload.setJsonPayload(jsonPayload);
        releasedPayload.setFirstRelease(firstReleaseTimestamp);
        releasedPayload.setLastRelease(reportedTimestamp);
        releasedPayload.setSpaceName(spaceName);
        releasedPayload.setTypes(types);
        releasedPayloadRepository.save(releasedPayload);
        return releasedPayload;
    }

    public ReleaseStatus getReleaseStatus(UUID instanceId) {
        Optional<InferredPayload> byId = inferredPayloadRepository.findById(instanceId);
        if (byId.isPresent()) {
            return byId.get().getReleaseStatus();
        }
        throw new InstanceNotFoundException(String.format("Instance %s not found", instanceId));
    }

    public List<ReleaseStatus> getDistinctReleaseStatus(Set<UUID> instanceIds) {
        return entityManager.createQuery("select distinct r.releaseStatus from InferredPayload r where r.id in :instanceIds", ReleaseStatus.class).setParameter("instanceIds", instanceIds).getResultList();
    }

    public Map<InstanceId, ReleaseStatus> getReleaseStatus(List<UUID> instanceIds) {
        return entityManager.createQuery("select p.uuid, p.spaceName, p.releaseStatus from InferredPayload p where p.uuid in :instanceIds", ReleaseStatusById.class).setParameter("instanceIds", instanceIds).getResultStream().collect(Collectors.toMap(k -> new InstanceId(k.uuid, SpaceName.fromString(k.spaceName)), v -> v.releaseStatus));
    }

    record ReleaseStatusById(UUID uuid, String spaceName, ReleaseStatus releaseStatus) {
    }

    public void removeInferredPayload(UUID instanceId) {
        inferredPayloadRepository.deleteById(instanceId);
    }

    public void removeReleasedPayload(UUID instanceId) {
        releasedPayloadRepository.deleteById(instanceId);
    }

    public NormalizedJsonLd releaseExistingPayload(UUID instanceId, Long reportedTimestamp) {
        Optional<InferredPayload> inferredPayload = inferredPayloadRepository.findById(instanceId);
        if (inferredPayload.isPresent()) {
            InferredPayload existingPayload = inferredPayload.get();
            existingPayload.setReleaseStatus(ReleaseStatus.RELEASED);
            inferredPayloadRepository.save(existingPayload);
            ReleasedPayload releasedPayload = releasePayload(instanceId, existingPayload.getSpaceName(), existingPayload.getJsonPayload(), reportedTimestamp, existingPayload.getTypes());
            existingPayload.setFirstRelease(releasedPayload.getFirstRelease());
            existingPayload.setLastRelease(releasedPayload.getLastRelease());
            return jsonAdapter.fromJson(existingPayload.getJsonPayload(), NormalizedJsonLd.class);
        } else {
            throw new InstanceNotFoundException(String.format("Could not release instance %s because it was not found", instanceId));
        }
    }

    public void upsertNativePayloadFromEvent(PersistedEvent event) {
        if (event != null) {
            NativePayload payload = new NativePayload();
            payload.setJsonPayload(jsonAdapter.toJson(event.getData()));
            payload.setCompositeId(new NativePayload.CompositeId(event.getInstanceId(), event.getUserId()));
            nativeRepository.save(payload);
        }
    }

    public void removeNativePayloadsFromEvent(PersistedEvent event) {
        if (event != null) {
            entityManager.createQuery("delete from NativePayload p where p.compositeId.instanceId = :instanceId").setParameter("instanceId", event.getInstanceId()).executeUpdate();
        }
    }

    public Stream<NormalizedJsonLd> getSourceDocumentsForInstanceFromDB(UUID instanceId, String excludeUserId) {
        return entityManager.createQuery("select d.jsonPayload from NativePayload d where d.compositeId.instanceId = :instanceId and d.compositeId.userId != :userId", String.class)
                .setParameter("instanceId", instanceId)
                .setParameter("userId", excludeUserId)
                .getResultStream()
                .map(d -> jsonAdapter.fromJson(d, NormalizedJsonLd.class));
    }


    public Optional<NormalizedJsonLd> getInstanceById(UUID id, DataStage stage) {
        NormalizedJsonLd result = null;
        switch (stage) {
            case NATIVE -> throw new UnsupportedOperationException("You can not request an instance by id for the native stage");
            case IN_PROGRESS -> {
                Optional<InferredPayload> byId = inferredPayloadRepository.findById(id);
                result = byId.map(inferredPayload -> jsonAdapter.fromJson(inferredPayload.getJsonPayload(), NormalizedJsonLd.class)).orElse(null);
            }
            case RELEASED -> {
                Optional<ReleasedPayload> byId = releasedPayloadRepository.findById(id);
                result = byId.map(releasedPayload -> jsonAdapter.fromJson(releasedPayload.getJsonPayload(), NormalizedJsonLd.class)).orElse(null);
            }
        }
        return Optional.ofNullable(result);
    }

    public Optional<NormalizedJsonLd> getNativeInstanceById(UUID id, String userId){
        return nativeRepository.findById(new NativePayload.CompositeId(id, userId)).map(n -> jsonAdapter.fromJson(n.getJsonPayload(), NormalizedJsonLd.class));
    }

}
