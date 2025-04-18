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

package org.marmotgraph.authentication.api;

import org.marmotgraph.authentication.config.AuthorizationConfiguration;
import org.marmotgraph.authentication.controller.AuthenticationRepository;
import org.marmotgraph.authentication.controller.TermsOfUseRepository;
import org.marmotgraph.authentication.keycloak.KeycloakClient;
import org.marmotgraph.authentication.keycloak.KeycloakConfig;
import org.marmotgraph.authentication.keycloak.KeycloakController;
import org.marmotgraph.authentication.model.UserOrClientProfile;
import org.marmotgraph.commons.api.Authentication;
import org.marmotgraph.commons.exception.NotAcceptedTermsOfUseException;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.TermsOfUse;
import org.marmotgraph.commons.model.TermsOfUseResult;
import org.marmotgraph.commons.model.User;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.roles.RoleMapping;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AuthenticationAPI implements Authentication.Client {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeycloakConfig keycloakConfig;

    private final KeycloakClient keycloakClient;

    private final KeycloakController keycloakController;

    private final AuthenticationRepository authenticationRepository;

    private final TermsOfUseRepository termsOfUseRepository;

    private final Permissions permissions;

    private final AuthorizationConfiguration authorizationConfiguration;

    public AuthenticationAPI(KeycloakConfig keycloakConfig, KeycloakClient keycloakClient, KeycloakController keycloakController, AuthenticationRepository authenticationRepository, TermsOfUseRepository termsOfUseRepository, Permissions permissions, AuthorizationConfiguration authorizationConfiguration) {
        this.keycloakController = keycloakController;
        this.keycloakClient = keycloakClient;
        this.keycloakConfig = keycloakConfig;
        this.authenticationRepository = authenticationRepository;
        this.termsOfUseRepository = termsOfUseRepository;
        this.permissions = permissions;
        this.authorizationConfiguration = authorizationConfiguration;
        if(authorizationConfiguration.isDisablePermissionAuthorization()){
            logger.warn("ATTENTION: You have disabled the authorization requirement for defining permissions! This is meant to be active only for the first execution! Please define a mapping for your administrator and set this property to false!");
        }
    }


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
        return userProfile != null ? keycloakController.buildUserInfoFromKeycloak(userProfile.getClaims()) : null;
    }


    @Override
    public UserWithRoles getRoles(boolean checkForTermsOfUse) {
        UserOrClientProfile userProfile = keycloakController.getUserProfile(true);
        if (userProfile != null) {
            User user = keycloakController.buildUserInfoFromKeycloak(userProfile.getClaims());
            UserOrClientProfile clientProfile = keycloakController.getClientProfile(true);
            if(clientProfile!=null && !keycloakController.isServiceAccount(clientProfile.getClaims())){
                throw new UnauthorizedException("The client authorization credentials you've passed doesn't belong to a service account. This is not allowed!");
            }
            List<UUID> invitationRoles = authenticationRepository.getInvitationRoles(user.getNativeId());
            UserWithRoles userWithRoles = new UserWithRoles(user, userProfile.getRoleNames(), clientProfile != null ? clientProfile.getRoleNames() : null, invitationRoles,
                    keycloakController.getClientInfoFromKeycloak(clientProfile != null ? clientProfile.getClaims() : null));
            // We only do the terms of use check for direct access calls (the clients are required to ensure that the user
            // agrees to the terms of use.)
            if(checkForTermsOfUse && clientProfile==null) {
                TermsOfUse termsOfUseToAccept = authenticationRepository.findTermsOfUseToAccept(user.getNativeId());
                if (termsOfUseToAccept != null) {
                    throw new NotAcceptedTermsOfUseException(termsOfUseToAccept);
                }
            }
            return userWithRoles;
        } else {
            return null;
        }
    }


    @Override
    public TermsOfUseResult getTermsOfUse() {
        TermsOfUse termsOfUse;
        UserOrClientProfile userProfile = keycloakController.getUserProfile(false);
        if (userProfile != null) {
            User user = keycloakController.buildUserInfoFromKeycloak(userProfile.getClaims());
            if (user != null) {
                termsOfUse = authenticationRepository.findTermsOfUseToAccept(user.getNativeId());
                if (termsOfUse != null) {
                    return new TermsOfUseResult(termsOfUse, false);
                }
            }
        }
        termsOfUse = termsOfUseRepository.getCurrentTermsOfUse();
        return termsOfUse != null ? new TermsOfUseResult(termsOfUse, true) : null;
    }

    @Override
    public void acceptTermsOfUse(String version) {
        User user = getMyUserInfo();
        if (user != null) {
            authenticationRepository.acceptTermsOfUse(version, user.getNativeId());
        } else {
            throw new IllegalArgumentException("Was not able to resolve the user information");
        }
    }

    @Override
    public void registerTermsOfUse(TermsOfUse termsOfUse) {
        //This is special -> the user doesn't need to accept the terms of use to register them (otherwise we would have a dead-lock)
        if (!permissions.hasGlobalPermission(this.getRoles(false), Functionality.DEFINE_TERMS_OF_USE)){
            throw new UnauthorizedException("You don't have the rights to define terms of use");
        }
        termsOfUseRepository.setCurrentTermsOfUse(termsOfUse);
    }

    /**
     * PERMISSIONS
     **/
    @Override
    public JsonLdDoc updateClaimForRole(RoleMapping role, String space, Map<String, Object> claimPattern, boolean removeClaim) {
        if(removeClaim){
            if(authorizationConfiguration.isDisablePermissionAuthorization() || permissions.hasGlobalPermission(this.getRoles(false), Functionality.DELETE_PERMISSION)) {
                return authenticationRepository.removeClaimFromRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to remove permissions");
            }
        }
        else{
            if(authorizationConfiguration.isDisablePermissionAuthorization() || permissions.hasGlobalPermission(this.getRoles(false), Functionality.CREATE_PERMISSION)) {
                return authenticationRepository.addClaimToRole(role.toRole(SpaceName.fromString(space)), claimPattern);
            }
            else{
                throw new UnauthorizedException("You don't have the rights to add permissions");
            }
        }
    }

    @Override
    public JsonLdDoc getClaimForRole(RoleMapping role, String space) {
        if(canShowPermissions()) {
            return authenticationRepository.getClaimForRole(role.toRole(SpaceName.fromString(space)));
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }

    private boolean canShowPermissions(){
        final UserWithRoles roles = this.getRoles(false);
        return authorizationConfiguration.isDisablePermissionAuthorization() || permissions.hasGlobalPermission(roles, Functionality.DELETE_PERMISSION) || permissions.hasGlobalPermission(roles, Functionality.CREATE_PERMISSION);
    }

    @Override
    public List<JsonLdDoc> getAllRoleDefinitions() {
        this.getRoles(false);
        if(canShowPermissions()) {
            List<JsonLdDoc> allRoleDefinitions = authenticationRepository.getAllRoleDefinitions();
            return allRoleDefinitions.stream().filter(rd -> !(rd.keySet().stream().allMatch(r -> r.startsWith("_")))).collect(Collectors.toList());
        }
        else{
            throw new UnauthorizedException("You don't have the rights to show permissions");
        }
    }
}
