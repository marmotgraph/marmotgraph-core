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

package org.marmotgraph.authorization.api;

import lombok.AllArgsConstructor;
import org.marmotgraph.authorization.models.Permission;
import org.marmotgraph.authorization.service.InvitationsService;
import org.marmotgraph.authorization.service.PermissionsService;
import org.marmotgraph.authorization.service.UserInfoService;
import org.marmotgraph.commons.AuthTokenContext;
import org.marmotgraph.commons.AuthTokens;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.api.authorization.Authorization;
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
public class AuthorizationAPI implements Authorization.Client {

    private final PermissionsService permissionsService;

    private final Permissions permissions;

    private final JsonAdapter jsonAdapter;

    private final UserInfoService userInfoService;

    private final AuthTokenContext authTokenContext;

    private final InvitationsService invitationsService;

    @Override
    public User getMyUserInfo() {
        AuthTokens authTokens = authTokenContext.getAuthTokens();
        if(authTokens != null && authTokenContext.getAuthTokens().getUserAuthToken() != null) {
            return userInfoService.getUserOrClientProfile(authTokenContext.getAuthTokens().getUserAuthToken().getBearerToken()).getA();
        }
        return null;
    }

    @Override
    public UserWithRoles getRoles() {
        AuthTokens authTokens = authTokenContext.getAuthTokens();
        if(authTokens != null && authTokens.getUserAuthToken() != null) {
            Tuple<User, List<String>> userProfile = userInfoService.getUserOrClientProfile(authTokens.getUserAuthToken().getBearerToken());
            if (userProfile != null) {
                Tuple<User, List<String>> clientProfile = null;
                if(authTokens.getClientAuthToken() != null) {
                    clientProfile = userInfoService.getUserOrClientProfile(authTokens.getClientAuthToken().getBearerToken());
                    if (clientProfile != null && !clientProfile.getA().isServiceAccount()) {
                        throw new UnauthorizedException("The client authorization credentials you've passed doesn't belong to a service account. This is not allowed!");
                    }
                }
                //TODO we could skip the invitation roles if the user already has global permissions starting from REVIEWER role (since the user is allowed to read everything)
                List<UUID> invitationRoles = invitationsService.getAllInvitationsForUserId(userProfile.getA().getNativeId());
                return new UserWithRoles(userProfile.getA(), userProfile.getB(), clientProfile != null ? clientProfile.getB() : null, invitationRoles, clientProfile != null ? clientProfile.getA().getSimpleServiceAccountName().orElse(null) : null);
            }
        }
        return null;
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
