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

package org.marmotgraph.ids.service;

import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.exception.AmbiguousException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.IdWithAlternatives;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.ids.model.PersistedId;
import org.marmotgraph.ids.model.RegisteredId;
import org.marmotgraph.ids.repository.IdRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class IdService {

    private final IdUtils idUtils;

    private final IdRepository idRepository;

    private final EntityManager entityManager;

    public PersistedId getId(UUID uuid, DataStage stage) {
        Optional<RegisteredId> byId = idRepository.findById(new RegisteredId.CompositeId(uuid, stage));
        return byId.map(RegisteredId::toPersistedId).orElse(null);
    }

    public IdService(IdRepository idRepository, IdUtils idUtils, EntityManager entityManager) {
        this.idRepository = idRepository;
        this.idUtils = idUtils;
        this.entityManager = entityManager;
    }

    public void remove(DataStage stage, PersistedId id) {
        this.idRepository.deleteById(new RegisteredId.CompositeId(id.getUUID(), stage));
    }

    public void upsert(DataStage stage, PersistedId id) {
        if (stage == DataStage.IN_PROGRESS) {
            Optional<RegisteredId> existingId = this.idRepository.findById(new RegisteredId.CompositeId(id.getUUID(), stage));
            if (existingId.isPresent()) {
                RegisteredId registeredId = existingId.get();
                //It could happen that identifiers disappear during updates. We need to make sure that the old identifiers are not lost though (getting rid of them is called "splitting" and is a separate process).
                if(!CollectionUtils.isEmpty(registeredId.getAlternativeIds())){
                    JsonLdId instanceId = idUtils.buildAbsoluteUrl(registeredId.getCompositeId().getUuid());
                    List<String> alternativeIds = new ArrayList<>(id.getAlternativeIds());
                    alternativeIds.addAll(registeredId.getAlternativeIds());
                    id.setAlternativeIds(alternativeIds.stream().filter(a -> !a.equals(instanceId.getId())).collect(Collectors.toSet()));
                }
            }
        }
        //Add the id in its fully qualified form as an alternative
        id.setAlternativeIds(new HashSet<>(id.getAlternativeIds() != null ? id.getAlternativeIds() : Collections.emptySet()));
        id.getAlternativeIds().add(idUtils.buildAbsoluteUrl(id.getUUID()).getId());
        this.idRepository.saveAndFlush(RegisteredId.fromPersistedId(id, stage));
    }

    /**
     * Finds instances either by UUID or by identifiers
     *
     * @param stage
     * @param uuid
     * @param identifiers
     * @return
     * @throws AmbiguousException
     */
    public InstanceId findInstanceByIdentifiers(DataStage stage, UUID uuid, Collection<String> identifiers) throws AmbiguousException{
        Set<String> alternativeIdentifiers = new HashSet<>(identifiers);
        if(uuid!=null){
            Optional<RegisteredId> byId = idRepository.findById(new RegisteredId.CompositeId(uuid, stage));
            if (byId.isPresent()) {
                return byId.get().toInstanceId();
            }
            alternativeIdentifiers.add(idUtils.buildAbsoluteUrl(uuid).getId());
        }
        List<RegisteredId> resultList = entityManager.createQuery("SELECT i FROM RegisteredId i JOIN i.alternativeIds alt WHERE alt IN :alternatives and i.compositeId.stage = :stage", RegisteredId.class).setParameter("stage", stage).setParameter("alternatives", alternativeIdentifiers).getResultList();
        return switch (resultList.size()) {
            case 0 -> null;
            case 1 -> {
                RegisteredId registeredId = resultList.getFirst();
                yield new InstanceId(registeredId.getCompositeId().getUuid(), SpaceName.fromString(registeredId.getSpace()));
            }
            default ->
                    throw new AmbiguousException(StringUtils.joinWith(", ", resultList.stream().map(p -> new InstanceId(p.getCompositeId().getUuid(), SpaceName.fromString(p.getSpace())).serialize()).collect(Collectors.toList())));
        };
    }


    public Map<UUID, InstanceId> resolveIds(DataStage stage, List<IdWithAlternatives> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        Map<UUID, InstanceId> result = ids.stream().map(id -> {
            try {
                return new Tuple<>(id.getId(), findInstanceByIdentifiers(stage, null, id.getAlternatives()));
            } catch (AmbiguousException e) {
                return new Tuple<UUID, InstanceId>(id.getId(), null);
            }
        }).filter(i -> i.getB() != null).collect(Collectors.toMap(Tuple::getA, Tuple::getB));
        ids.stream().filter(id -> !result.containsKey(id.getId())).forEach(id -> result.put(id.getId(), null));
        return result;
    }

}
