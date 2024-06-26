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

package org.marmotgraph.core.controller;

import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class VirtualSpaceController {

    private final CoreInstanceController instanceController;
    private final AuthContext authContext;

    public VirtualSpaceController(CoreInstanceController instanceController, AuthContext authContext) {
        this.instanceController = instanceController;
        this.authContext = authContext;
    }

    public boolean isVirtualSpace(String spaceName){
        return SpaceName.REVIEW_SPACE.equals(spaceName);
    }


    public List<NormalizedJsonLd> getInstancesByInvitation(ResponseConfiguration responseConfiguration, DataStage stage, String type){
        final ExtendedResponseConfiguration r = new ExtendedResponseConfiguration();
        r.setReturnAlternatives(responseConfiguration.isReturnAlternatives());
        r.setReturnEmbedded(responseConfiguration.isReturnEmbedded());
        r.setReturnPayload(responseConfiguration.isReturnPayload());
        r.setReturnPermissions(responseConfiguration.isReturnPermissions());
        final List<String> invitationIds = authContext.getUserWithRoles().getInvitations().stream().map(UUID::toString).sorted().collect(Collectors.toList());
        final Map<String, Result<NormalizedJsonLd>> instancesByIds = instanceController.getInstancesByIds(invitationIds, stage, r, type);
        return instancesByIds.values().stream().map(Result::getData).filter(Objects::nonNull).collect(Collectors.toList());
    }


}
