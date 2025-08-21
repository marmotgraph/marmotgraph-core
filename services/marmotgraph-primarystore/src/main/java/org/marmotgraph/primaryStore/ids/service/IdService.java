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

package org.marmotgraph.primaryStore.ids.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.exception.AmbiguousException;
import org.marmotgraph.commons.exception.InstanceNotFoundException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.IdWithAlternatives;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.primaryStore.ids.model.RegisteredId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class IdService {

    private final IdUtils idUtils;

    private final IdRepository idRepository;

    private final EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @NotNull
    public SpaceName getSpace(UUID id){
        Optional<RegisteredId> byId = idRepository.findById(new RegisteredId.CompositeId(id, DataStage.IN_PROGRESS));
        if(byId.isPresent()){
            String space = byId.get().getSpace();
            if(space!=null){
                return SpaceName.fromString(space);
            }
            else{
                throw new IllegalStateException(String.format("Was not able to evaluate the space for %s", id));
            }
        }
        throw new InstanceNotFoundException(id);
    }

    public void upsertId(IdWithAlternatives idWithAlternatives, DataStage stage) {
        if (idWithAlternatives != null && idWithAlternatives.getId() != null) {
            logger.debug(String.format("Updating id %s%s", idWithAlternatives.getId(), idWithAlternatives.getAlternatives() != null ? "with alternatives " + String.join(", ", idWithAlternatives.getAlternatives()) : ""));
            RegisteredId registeredId = new RegisteredId();
            registeredId.setCompositeId(new RegisteredId.CompositeId(idWithAlternatives.getId(), stage));
            registeredId.setAlternativeIds(idWithAlternatives.getAlternatives());
            if (stage == DataStage.IN_PROGRESS) {
                registeredId.setSpace(idWithAlternatives.getSpace());
                Optional<RegisteredId> existingId = this.idRepository.findById(registeredId.getCompositeId());
                if (existingId.isPresent()) {
                    RegisteredId existingRegisteredId = existingId.get();
                    //It could happen that identifiers disappear during updates.
                    if (!CollectionUtils.isEmpty(existingRegisteredId.getAlternativeIds())) {
                        JsonLdId instanceId = idUtils.buildAbsoluteUrl(existingRegisteredId.getCompositeId().getUuid());
                        List<String> alternativeIds = new ArrayList<>(registeredId.getAlternativeIds());
                        alternativeIds.addAll(existingRegisteredId.getAlternativeIds());
                        registeredId.setAlternativeIds(alternativeIds.stream().filter(a -> !a.equals(instanceId.getId())).collect(Collectors.toSet()));
                    }
                }
            }
            //Add the id in its fully qualified form as an alternative
            registeredId.setAlternativeIds(new HashSet<>(registeredId.getAlternativeIds() != null ? registeredId.getAlternativeIds() : Collections.emptySet()));
            registeredId.getAlternativeIds().add(idUtils.buildAbsoluteUrl(registeredId.getCompositeId().getUuid()).getId());
            this.idRepository.save(registeredId);
        }
        else {
            throw new IllegalArgumentException("Invalid payload");
        }
    }

    public void removeId(DataStage stage, UUID uuid) {
        idRepository.findById(new RegisteredId.CompositeId(uuid, stage)).ifPresent(id -> idRepository.deleteById(id.getCompositeId()));
    }


    /**
     * Finds instances either by UUID or by identifiers
     */
    public InstanceId findInstanceByIdentifiers(DataStage stage, UUID uuid, Collection<String> identifiers) throws AmbiguousException {
        Set<String> alternativeIdentifiers = new HashSet<>(identifiers);
        if (uuid != null) {
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


    public List<RegisteredId> findIds(DataStage stage, List<UUID> ids){
        return entityManager.createQuery("select i from RegisteredId i where i.id.stage == :stage and i.id.uuid in :ids", RegisteredId.class).setParameter("stage", stage).setParameter("ids", ids).getResultList();
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
