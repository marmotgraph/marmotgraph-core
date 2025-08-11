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

package org.marmotgraph.authentication.api;

import lombok.AllArgsConstructor;
import org.marmotgraph.authentication.models.Invitation;
import org.marmotgraph.authentication.service.InstanceScopeService;
import org.marmotgraph.authentication.service.InvitationsService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Component
public class InvitationAPI implements org.marmotgraph.commons.api.Invitation.Client {

    private final InvitationsService invitationsService;
    private final InstanceScopeService instanceScopeService;

    @Override
    public void inviteUserForInstance(UUID id, UUID userId) {
        invitationsService.createInvitation(new Invitation(new Invitation.CompositeId(id.toString(), userId)));
        this.instanceScopeService.calculateInstanceScope(id);
    }

    @Override
    public void revokeUserInvitation(UUID id, UUID userId) {
        invitationsService.deleteInvitation(new Invitation.CompositeId(id.toString(), userId));
    }

    @Override
    public List<String> listInvitedUserIds(UUID id) {
        if (id != null) {
            return invitationsService.getAllInvitedUsersByInstanceId(id);
        }
        return Collections.emptyList();
    }

    @Override
    public List<UUID> listInstances() {
        return invitationsService.getAllInstancesWithInvitation();
    }

    @Override
    public void calculateInstanceScope(UUID id) {
        this.instanceScopeService.calculateInstanceScope(id);
    }
}
