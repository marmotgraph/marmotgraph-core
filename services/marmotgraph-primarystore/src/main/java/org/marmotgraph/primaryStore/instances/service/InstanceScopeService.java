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
import org.marmotgraph.commons.api.graphDB.GraphDB;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.ScopeElement;
import org.marmotgraph.primaryStore.instances.model.InstanceInformation;
import org.marmotgraph.primaryStore.instances.model.InstanceScope;
import org.springframework.stereotype.Component;

import java.util.*;

@AllArgsConstructor
@Component
public class InstanceScopeService {

    private final InstanceScopeRepository repository;
    private final GraphDB.Client graphDB;
    private final EntityManager em;
    private final InstanceInformationRepository instanceInformationRepository;

    public void calculateInstanceScope(UUID id) {
        Optional<InstanceInformation> instanceInformationOptional = this.instanceInformationRepository.findById(id);
        if(instanceInformationOptional.isPresent()){
            InstanceInformation instanceInformation = instanceInformationOptional.get();
            final ScopeElement scopeForInstance = graphDB.getScopeForInstance(instanceInformation.getSpaceName(), instanceInformation.getUuid(), DataStage.IN_PROGRESS, false);
            final Set<UUID> uuids = collectIds(scopeForInstance, new HashSet<>());
            repository.saveAndFlush(new InstanceScope(id, new HashSet<>(uuids)));
        }
    }

    public Set<UUID> getRelatedIds(UUID id){
        return repository.findById(id).map(InstanceScope::getRelatedIds).orElse(Collections.emptySet());
    }

    public List<UUID> getRelatedIds(List<UUID> ids){
       return em.createQuery("select distinct r from InstanceScope s JOIN s.relatedIds r where s.instanceId IN :instanceIds", UUID.class).setParameter("instanceIds", ids).getResultList();
    }

    private Set<UUID> collectIds(ScopeElement s, Set<UUID> collector) {
        collector.add(s.getId());
        if (s.getChildren() != null) {
            for (ScopeElement c : s.getChildren()) {
                collectIds(c, collector);
            }
        }
        return collector;
    }

}
