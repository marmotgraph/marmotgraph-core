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

package org.marmotgraph.primaryStore.queries.service;

import lombok.AllArgsConstructor;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.primaryStore.instances.service.SpaceService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Service
public class QueryPermissions {

    private final Permissions permissions;
    private final SpaceService spaceService;


    /**
     * @param restrictedSpaces
     * @param userWithRoles
     * @param stage
     * @return a tuple of spaces and instances the user has read access for in the given stage or null if all instances can be read
     */
    public Tuple<Set<SpaceName>, Set<UUID>> queryFilter(Collection<SpaceName> restrictedSpaces, UserWithRoles userWithRoles, DataStage stage) {
        Functionality readFunctionality = getReadFunctionality(stage);
        if (!permissions.hasGlobalPermission(userWithRoles, readFunctionality)){
            Set<SpaceName> spacesWithReadPermission = permissions.getSpacesForPermission(restrictedSpaces != null ? restrictedSpaces : spaceService.allSpaces(), userWithRoles, readFunctionality);
            Set<UUID> instancesWithReadPermissions = getInstancesWithExplicitPermission(userWithRoles, stage);
            return new Tuple<>(spacesWithReadPermission, instancesWithReadPermissions);
        }
        return null;
    }

    private Set<UUID> getInstancesWithExplicitPermission(UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        return permissions.getInstancesWithExplicitPermission(userWithRoles.getPermissions(), readFunctionality);
    }


    private Functionality getReadFunctionality(DataStage stage) {
        switch (stage) {
            case IN_PROGRESS:
                return Functionality.READ;
            case RELEASED:
                return Functionality.READ_RELEASED;
            default:
                return null;
        }
    }


}
