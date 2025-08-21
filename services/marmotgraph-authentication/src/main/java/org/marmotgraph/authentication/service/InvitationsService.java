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

package org.marmotgraph.authentication.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.marmotgraph.authentication.models.Invitation;
import org.marmotgraph.commons.api.primaryStore.Scopes;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@AllArgsConstructor
@Component
public class InvitationsService {

    private final EntityManager em;
    private final InvitationsRepository repository;
    private final Scopes.Client scopes;

    public List<String> getAllInvitedUsersByInstanceId(UUID instanceId){
        return em.createQuery("select i.compositeId.userId from Invitation i where i.compositeId.instanceId = :instanceId", String.class).setParameter("instanceId", instanceId).getResultList();
    }

    public List<UUID> getAllInstancesWithInvitation(){
        return em.createQuery("select distinct i.compositeId.instanceId from Invitation i", UUID.class).getResultList();
    }

    public List<UUID> getAllInvitationsForUserId(String userId) {
        List<UUID> instanceIds = em.createQuery("select i.compositeId.instanceId from Invitation i where i.compositeId.userId = :userId", UUID.class).setParameter("userId", userId).getResultList();
        List<UUID> relatedInstances = scopes.relatedInstancesByScope(instanceIds);
        return Stream.concat(instanceIds.stream(), relatedInstances.stream()).distinct().toList();
    }

    public void createInvitation(Invitation invitation) {
        repository.saveAndFlush(invitation);
    }

    public void deleteInvitation(Invitation.CompositeId invitation) {
        repository.deleteById(invitation);
    }


}
