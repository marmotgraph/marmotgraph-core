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

package org.marmotgraph.graphdb.arango.commons.controller;

import org.marmotgraph.graphdb.arango.Arango;
import org.marmotgraph.graphdb.arango.aqlbuilder.AQL;
import org.marmotgraph.graphdb.arango.model.ArangoCollectionReference;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.FunctionalityInstance;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Arango
public class PermissionsController {

    private final Permissions permissions;

    public PermissionsController(Permissions permissions) {
        this.permissions = permissions;
    }

    public Set<SpaceName> whitelistedSpaceReads(UserWithRoles userWithRoles){
        if(!permissions.hasGlobalPermission(userWithRoles, Functionality.READ) && !permissions.hasGlobalPermission(userWithRoles, Functionality.READ_RELEASED) ){
            //We only need to filter if there is no "global" read available...
            return userWithRoles.getPermissions().stream().filter(p -> p.getId() == null && (p.getFunctionality() == Functionality.READ || p.getFunctionality() == Functionality.READ_RELEASED)).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return null;
    }

    public boolean canManageTypesAndProperties(UserWithRoles userWithRoles){
        return permissions.hasGlobalPermission(userWithRoles, Functionality.DEFINE_TYPES_AND_PROPERTIES);
    }

    public boolean canManageSpaces(UserWithRoles userWithRoles, SpaceName spaceName){
        return permissions.hasPermission(userWithRoles, Functionality.MANAGE_SPACE, spaceName);
    }

    public boolean hasGlobalReadPermissions(UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        return permissions.hasGlobalPermission(userWithRoles, readFunctionality);
    }

    public boolean canDefineScopeSpace(UserWithRoles userWithRoles) {
        return permissions.hasGlobalPermission(userWithRoles, Functionality.DEFINE_SCOPE_RELEVANT_SPACE);
    }

    public Set<SpaceName> removeSpacesWithoutReadAccess(Set<SpaceName> spaces, UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        Set<SpaceName> spacesWithReadPermission = permissions.getSpacesForPermission(spaces, userWithRoles, readFunctionality);
        spaces.retainAll(spacesWithReadPermission);
        return spaces;
    }

    public Set<UUID> getInstancesWithExplicitPermission(UserWithRoles userWithRoles, DataStage stage){
        Functionality readFunctionality = getReadFunctionality(stage);
        return permissions.getInstancesWithExplicitPermission(userWithRoles.getPermissions(), readFunctionality);
    }


    public Map<String, Object> whitelistFilterForReadInstances(Set<SpaceName> possibleSpaces, UserWithRoles userWithRoles, DataStage stage) {
        Functionality readFunctionality = getReadFunctionality(stage);
        if (!permissions.hasGlobalPermission(userWithRoles, readFunctionality)){
            //We only need to filter if there is no "global" read available...
            Map<String, Object> bindVars = new HashMap<>();
            Set<SpaceName> spacesWithReadPermission = permissions.getSpacesForPermission(possibleSpaces, userWithRoles, readFunctionality);
            Set<UUID> instancesWithReadPermissions = getInstancesWithExplicitPermission(userWithRoles, stage);
            bindVars.put(AQL.READ_ACCESS_BY_SPACE, spacesWithReadPermission.stream().map(s -> ArangoCollectionReference.fromSpace(s).getCollectionName()).collect(Collectors.toList()));
            bindVars.put(AQL.READ_ACCESS_BY_INVITATION, instancesWithReadPermissions != null ? instancesWithReadPermissions.stream().collect(Collectors.toMap(k -> k, v -> Collections.emptyMap())): Collections.emptyList());
            return bindVars;
        }
        return null;
    }


    public Functionality getMinimalReadFunctionality(DataStage stage) {
        return stage == DataStage.IN_PROGRESS ? Functionality.MINIMAL_READ : null;

    }

    public Functionality getReadFunctionality(DataStage stage) {
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
