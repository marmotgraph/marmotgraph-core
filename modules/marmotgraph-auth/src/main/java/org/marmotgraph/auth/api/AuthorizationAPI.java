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

package org.marmotgraph.auth.api;

import lombok.AllArgsConstructor;
import org.marmotgraph.auth.models.Permission;
import org.marmotgraph.auth.models.roles.RoleMapping;
import org.marmotgraph.auth.service.PermissionsService;
import org.marmotgraph.commons.exceptions.UnauthorizedException;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.auth.Functionality;
import org.marmotgraph.commons.services.JsonAdapter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class AuthorizationAPI {

    private final PermissionsService permissionsService;

    private final Permissions permissions;

    private final JsonAdapter jsonAdapter;

    /**
     * PERMISSIONS
     **/
    public JsonLdDoc updateClaimForRole(RoleMapping role, String space, Map<String, Object> claimPattern, boolean removeClaim) {
        if(removeClaim){
            if(permissions.hasGlobalPermission(Functionality.DELETE_PERMISSION)) {
                return permissionsService.removeClaimFromRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to remove permissions");
            }
        }
        else{
            if(permissions.hasGlobalPermission(Functionality.CREATE_PERMISSION)) {
                return permissionsService.addClaimToRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to add permissions");
            }
        }
    }

    public JsonLdDoc getClaimForRole(RoleMapping role, String space) {
        if(canShowPermissions()) {
            return permissionsService.getClaimForRole(role.toRole(SpaceName.fromString(space)));
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }

    private boolean canShowPermissions(){
        return permissions.hasGlobalPermission(Functionality.DELETE_PERMISSION) || permissions.hasGlobalPermission(Functionality.CREATE_PERMISSION);
    }

    public List<JsonLdDoc> getAllRoleDefinitions() {
        if(canShowPermissions()) {
            List<Permission> allRoleDefinitions = permissionsService.getAllRoleDefinitions();
            return allRoleDefinitions.stream().map(rd -> {
                JsonLdDoc jsonLdDoc = jsonAdapter.fromJson(rd.getClaims(), JsonLdDoc.class);
                jsonLdDoc.addProperty("_key", rd.getId());
                return jsonLdDoc;
            }).collect(Collectors.toList());
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }
}
