/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.authentication.controller;

import org.marmotgraph.authentication.model.InstanceScope;
import org.marmotgraph.commons.api.GraphDBScopes;
import org.marmotgraph.commons.api.Ids;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.ScopeElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class InvitationController {
    private final AuthenticationRepository authenticationRepository;
    private final GraphDBScopes.Client graphDBScopes;
    private final Ids.Client ids;

    public InvitationController(AuthenticationRepository authenticationRepository, GraphDBScopes.Client graphDBScopes, Ids.Client ids) {
        this.authenticationRepository = authenticationRepository;
        this.graphDBScopes = graphDBScopes;
        this.ids = ids;
    }

    public void calculateInstanceScope(UUID id) {
        final InstanceId instance = this.ids.findInstanceByIdentifiers(id, new ArrayList<>(), DataStage.IN_PROGRESS);
        if(instance!=null) {
            final ScopeElement scopeForInstance = graphDBScopes.getScopeForInstance(instance.getSpace().getName(), id, DataStage.IN_PROGRESS, false);
            final Set<UUID> uuids = collectIds(scopeForInstance, new HashSet<>());
            authenticationRepository.createOrUpdateInstanceScope(new InstanceScope(id.toString(), new ArrayList<>(uuids)));
        }
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
