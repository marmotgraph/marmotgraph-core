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
import org.marmotgraph.authentication.models.Permission;
import org.marmotgraph.authentication.models.UserOrClientProfile;
import org.marmotgraph.authentication.service.InvitationsService;
import org.marmotgraph.authentication.service.PermissionsService;
import org.marmotgraph.authentication.service.keycloak.KeycloakClient;
import org.marmotgraph.authentication.service.keycloak.KeycloakConfig;
import org.marmotgraph.authentication.service.keycloak.KeycloakController;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.api.authentication.Authentication;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.User;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.roles.RoleMapping;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class AuthenticationAPI implements Authentication.Client {

    private final KeycloakConfig keycloakConfig;

    private final KeycloakClient keycloakClient;

    private final KeycloakController keycloakController;

    private final InvitationsService invitationsService;

    private final PermissionsService permissionsService;

    private final Permissions permissions;

    private final JsonAdapter jsonAdapter;

    /**
     * USERS
     **/
    @Override
    public String authEndpoint() {
        return keycloakClient.getServerUrl();
    }

    @Override
    public String openIdConfigUrl() {
        return keycloakConfig.getConfigUrl();
    }

    public String loginClientId(){
        return keycloakConfig.getLoginClientId();
    }

    @Override
    public String tokenEndpoint() {
        return keycloakClient.getTokenEndpoint();
    }

    @Override
    public User getMyUserInfo() {
        UserOrClientProfile userProfile = keycloakController.getUserProfile(false);
        return userProfile != null ? keycloakController.buildUserInfoFromKeycloak(userProfile.claims()) : null;
    }

    @Override
    public UserWithRoles getRoles() {
        UserOrClientProfile userProfile = keycloakController.getUserProfile(true);
        if (userProfile != null) {
            User user = keycloakController.buildUserInfoFromKeycloak(userProfile.claims());
            UserOrClientProfile clientProfile = keycloakController.getClientProfile(true);
            if(clientProfile!=null && !keycloakController.isServiceAccount(clientProfile.claims())){
                throw new UnauthorizedException("The client authorization credentials you've passed doesn't belong to a service account. This is not allowed!");
            }
            List<UUID> invitationRoles = invitationsService.getAllInvitationsForUserId(user.getNativeId());
            return new UserWithRoles(user, userProfile.roleNames(), clientProfile != null ? clientProfile.roleNames() : null, invitationRoles,
                    keycloakController.getClientInfoFromKeycloak(clientProfile != null ? clientProfile.claims() : null));
        } else {
            return null;
        }
    }

    /**
     * PERMISSIONS
     **/
    @Override
    public JsonLdDoc updateClaimForRole(RoleMapping role, String space, Map<String, Object> claimPattern, boolean removeClaim) {
        if(removeClaim){
            if(permissions.hasGlobalPermission(this.getRoles(), Functionality.DELETE_PERMISSION)) {
                return permissionsService.removeClaimFromRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to remove permissions");
            }
        }
        else{
            if(permissions.hasGlobalPermission(this.getRoles(), Functionality.CREATE_PERMISSION)) {
                return permissionsService.addClaimToRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to add permissions");
            }
        }
    }

    @Override
    public JsonLdDoc getClaimForRole(RoleMapping role, String space) {
        if(canShowPermissions()) {
            return permissionsService.getClaimForRole(role.toRole(SpaceName.fromString(space)));
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }

    private boolean canShowPermissions(){
        final UserWithRoles roles = this.getRoles();
        return permissions.hasGlobalPermission(roles, Functionality.DELETE_PERMISSION) || permissions.hasGlobalPermission(roles, Functionality.CREATE_PERMISSION);
    }

    @Override
    public List<JsonLdDoc> getAllRoleDefinitions() {
        this.getRoles();
        if(canShowPermissions()) {
            List<Permission> allRoleDefinitions = permissionsService.getAllRoleDefinitions();
            return allRoleDefinitions.stream().map(rd -> jsonAdapter.fromJson(rd.getClaims(), JsonLdDoc.class)).collect(Collectors.toList());
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }
}
