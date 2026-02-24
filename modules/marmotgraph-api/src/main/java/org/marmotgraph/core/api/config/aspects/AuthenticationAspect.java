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

package org.marmotgraph.core.api.config.aspects;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.marmotgraph.auth.models.UserAuthToken;
import org.marmotgraph.auth.service.AuthTokenContext;
import org.marmotgraph.auth.service.KeycloakClient;
import org.marmotgraph.commons.exceptions.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Aspect
@Component
public class AuthenticationAspect {

    public AuthenticationAspect(KeycloakClient keycloakClient, AuthTokenContext authTokenContext) {
        this.jwtVerifier = keycloakClient.getJWTVerifier();
        this.authTokenContext = authTokenContext;
    }

    private final JWTVerifier jwtVerifier;
    private final AuthTokenContext authTokenContext;

    @Before("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * org.marmotgraph..*.*(..)) && !@annotation(NoAuthentication)")
    public void authenticate(JoinPoint joinPoint) {
        // ✅ Your custom authentication logic
        Optional<UserAuthToken> authToken = authTokenContext.getAuthToken();
        if (authToken.isEmpty()) {
            throw new UnauthorizedException("You haven't provided the required credentials! Please define an Authorization header with your bearer token!");
        }
        try {
            jwtVerifier.verify(authToken.get().getRawToken());
        } catch (JWTVerificationException ex) {
            throw new UnauthorizedException(ex);
        }
    }
}
