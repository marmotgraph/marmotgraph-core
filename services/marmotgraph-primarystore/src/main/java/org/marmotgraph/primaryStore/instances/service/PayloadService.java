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
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.primaryStore.instances.model.InstanceInformation;
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
    private final EntityManager entityManager;
    private final InstanceInformationRepository globalInstanceInformationRepository;


    public Optional<SpaceName> getSpace(UUID instanceID){
        return globalInstanceInformationRepository.findById(instanceID).map(i -> SpaceName.fromString(i.getSpaceName()));
    }

    private record InstanceResolutionResult(UUID uuid, String spaceName, String alternative) {}


    public Map<UUID, InstanceId> resolveIds(List<IdWithAlternatives> idWithAlternatives)  {
        Set<String> flatListOfAlternatives = idWithAlternatives.stream().map(IdWithAlternatives::getAlternatives).flatMap(Collection::stream).collect(Collectors.toSet());
        Stream<InstanceResolutionResult> result = entityManager.createQuery("select i.uuid, i.spaceName, alternative from InstanceInformation i JOIN i.alternativeIds alternative WHERE alternative IN :alternatives", InstanceResolutionResult.class).setParameter("alternatives", flatListOfAlternatives).getResultStream();
        if(result!=null) {
            Map<String, InstanceId> instanceIdsByAlternative = result.collect(Collectors.toMap(InstanceResolutionResult::alternative, v -> new InstanceId(v.uuid(), SpaceName.fromString(v.spaceName()))));
            Map<UUID, InstanceId> resultMap = new HashMap<>();
            idWithAlternatives.forEach(id -> {
                Optional<String> alternative = id.getAlternatives().stream().filter(instanceIdsByAlternative::containsKey).findFirst();
                alternative.ifPresent(s -> resultMap.put(id.getId(), instanceIdsByAlternative.get(s)));
            });
            return resultMap;
        }
        return new HashMap<>();
    }


    private InstanceInformation getOrCreateGlobalInstanceInformation(UUID instanceId) {
        return globalInstanceInformationRepository.findById(instanceId).orElseGet(() -> {
                InstanceInformation i = new InstanceInformation();
                i.setUuid(instanceId);
                return i;
        });
    }

    public InstanceInformation upsertGlobalInformation(UUID instanceId, SpaceName spaceName, Set<String> alternativeIds){
        InstanceInformation instanceInformation = getOrCreateGlobalInstanceInformation(instanceId);
        instanceInformation.setSpaceName(spaceName.getName());
        instanceInformation.setAlternativeIds(alternativeIds);
        globalInstanceInformationRepository.save(instanceInformation);
        return instanceInformation;
    }

    public void upsertInferredPayload(UUID instanceId, InstanceInformation instanceInformation, NormalizedJsonLd payload, JsonLdDoc alternatives, boolean autoRelease, Long reportedTimeStampInMs) {
        String newPayload = jsonAdapter.toJson(payload);
        if (autoRelease) {
            releasePayload(instanceId, jsonAdapter.toJson(payload), reportedTimeStampInMs, instanceInformation, payload.types());
        } else {
            Optional<ReleasedPayload> releasedInstance = releasedPayloadRepository.findById(instanceId);
            if (releasedInstance.isPresent()) {
                ReleasedPayload existingReleasedInstance = releasedInstance.get();
                if (existingReleasedInstance.getJsonPayload().equals(newPayload)) {
                    instanceInformation.setReleaseStatus(ReleaseStatus.RELEASED);
                } else {
                    instanceInformation.setReleaseStatus(ReleaseStatus.HAS_CHANGED);
                }
            }
        }
        InferredPayload inferredPayload = new InferredPayload();
        inferredPayload.setUuid(instanceId);
        inferredPayload.setAlternative(jsonAdapter.toJson(alternatives));
        inferredPayload.setJsonPayload(newPayload);
        inferredPayload.setTypes(payload.types());
        inferredPayloadRepository.save(inferredPayload);
    }

    private void releasePayload(UUID instanceId, String jsonPayload, Long reportedTimestamp, InstanceInformation instanceInformation, List<String> types) {
        if(instanceInformation.getFirstRelease() == null){
            instanceInformation.setFirstRelease(reportedTimestamp);
        }
        instanceInformation.setLastRelease(reportedTimestamp);
        instanceInformation.setReleaseStatus(ReleaseStatus.RELEASED);
        ReleasedPayload releasedPayload = new ReleasedPayload();
        releasedPayload.setUuid(instanceId);
        releasedPayload.setJsonPayload(jsonPayload);
        releasedPayload.setTypes(types);
        globalInstanceInformationRepository.save(instanceInformation);
        releasedPayloadRepository.save(releasedPayload);
    }

    public ReleaseStatus getReleaseStatus(UUID instanceId) {
        Optional<InstanceInformation> byId = globalInstanceInformationRepository.findById(instanceId);
        if (byId.isPresent()) {
            return byId.get().getReleaseStatus();
        }
        throw new InstanceNotFoundException(String.format("Instance %s not found", instanceId));
    }

    public List<ReleaseStatus> getDistinctReleaseStatus(Set<UUID> instanceIds) {
        return entityManager.createQuery("select distinct r.releaseStatus from InstanceInformation r where r.uuid in :instanceIds", ReleaseStatus.class).setParameter("instanceIds", instanceIds).getResultList();
    }

    public Map<InstanceId, ReleaseStatus> getReleaseStatus(List<UUID> instanceIds) {
        return entityManager.createQuery("select p.uuid, p.spaceName, p.releaseStatus from InstanceInformation p where p.uuid in :instanceIds", ReleaseStatusById.class).setParameter("instanceIds", instanceIds).getResultStream().collect(Collectors.toMap(k -> new InstanceId(k.uuid, SpaceName.fromString(k.spaceName)), v -> v.releaseStatus));
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
            releasePayload(instanceId, existingPayload.getJsonPayload(), reportedTimestamp, existingPayload.getInstanceInformation(), existingPayload.getTypes());
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


    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<UUID> ids, DataStage stage, String typeRestriction, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize){
        //TODO typeRestriction
        switch (stage) {
            case NATIVE -> throw new UnsupportedOperationException("You can not request an instance by id for the native stage");
            case IN_PROGRESS -> {
                Stream<InferredPayload> resultStream = entityManager.createQuery("select i from InferredPayload i where i.uuid in :uuids", InferredPayload.class).setParameter("uuids", ids).getResultStream();
                return resultStream.collect(Collectors.toMap(InferredPayload::getUuid, v->{
                    NormalizedJsonLd result = jsonAdapter.fromJson(v.getJsonPayload(), NormalizedJsonLd.class);
                    if(!returnEmbedded){
                        //TODO remove embedded entries
                    }
                    if(returnAlternatives){
                        NormalizedJsonLd alternative = jsonAdapter.fromJson(v.getAlternative(), NormalizedJsonLd.class);
                        result.put(EBRAINSVocabulary.META_ALTERNATIVE, alternative);
                    }

                    //TODO incoming links
                    return Result.ok(result);
                }));
            }
            case RELEASED -> {
                Stream<InferredPayload> resultStream = entityManager.createQuery("select i from ReleasedPayload i where i.uuid in :uuids", InferredPayload.class).setParameter("uuids", ids).getResultStream();
                return resultStream.collect(Collectors.toMap(InferredPayload::getUuid, v->{
                    NormalizedJsonLd result = jsonAdapter.fromJson(v.getJsonPayload(), NormalizedJsonLd.class);
                    if(!returnEmbedded){
                        //TODO remove embedded entries
                    }
                    if(returnAlternatives){
                        NormalizedJsonLd alternative = jsonAdapter.fromJson(v.getAlternative(), NormalizedJsonLd.class);
                        result.put(EBRAINSVocabulary.META_ALTERNATIVE, alternative);
                    }

                    //TODO incoming links
                    return Result.ok(result);
                }));
            }
        }
        return new HashMap<>();
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
