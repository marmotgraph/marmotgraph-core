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

package org.marmotgraph.auth.service;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.marmotgraph.auth.models.tokens.AuthTokens;
import org.marmotgraph.commons.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthTokenContext authTokenContext;
    private final JWTVerifier jwtVerifier;

    public AuthenticationService(KeycloakClient keycloakClient, AuthTokenContext authTokenContext) {
        this.authTokenContext = authTokenContext;
        this.jwtVerifier = keycloakClient.getJWTVerifier();
    }

    public void validateAuthentication() throws UnauthorizedException {
        AuthTokens authTokens = authTokenContext.getAuthTokens();
        if (authTokens == null || authTokens.getUserAuthToken() == null) {
            throw new UnauthorizedException("You haven't provided the required credentials! Please define an Authorization header with your bearer token!");
        }
        verifyToken(authTokens.getUserAuthToken().getRawToken());
        if(authTokens.getClientAuthToken()!=null && authTokens.getClientAuthToken().getRawToken() != null){
            verifyToken(authTokens.getClientAuthToken().getRawToken());
        }
    }

    private void verifyToken(String token) throws UnauthorizedException{
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException ex) {
            throw new UnauthorizedException(ex);
        }
    }

}
