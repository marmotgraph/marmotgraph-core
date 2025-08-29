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
import org.marmotgraph.authentication.service.KeycloakClient;
import org.marmotgraph.authentication.service.KeycloakConfig;
import org.marmotgraph.authentication.service.AuthenticationService;
import org.marmotgraph.commons.api.authentication.Authentication;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Map;

@AllArgsConstructor
@Component
public class AuthenticationAPI implements Authentication.Client {

    private final KeycloakConfig keycloakConfig;

    private final KeycloakClient keycloakClient;

    private final AuthenticationService keycloakController;
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

    @Override
    public String loginClientId(){
        return keycloakConfig.getLoginClientId();
    }

    @Override
    public String tokenEndpoint() {
        return keycloakClient.getTokenEndpoint();
    }


    @Override
    public Map<String, Object> getUserInfo(String token){
        return this.keycloakClient.getUserInfo(token);
    }

    @Override
    public void validateAuthentication() throws UnauthorizedException {
        keycloakController.validateAuthentication();
    }



}
