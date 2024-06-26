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

package org.marmotgraph.commons;

import org.marmotgraph.commons.exception.NotAcceptedTermsOfUseException;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.models.UserWithRoles;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final Logger requestLogger = LoggerFactory.getLogger(getClass());
    private final AuthContext authContext;

    public RequestLoggingFilter(AuthContext authContext) {
        this.authContext = authContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean publicAPI = request.getRequestURI().startsWith(String.format("/%s", Version.V3_BETA)) || request.getRequestURI().startsWith(String.format("/%s", Version.V3));;
        UUID apiRequestId = UUID.randomUUID();
        if (publicAPI) {
            UserWithRoles userWithRoles;
            try {
                userWithRoles = authContext.getUserWithRoles();
            } catch (UnauthorizedException | NotAcceptedTermsOfUseException ex) {
                userWithRoles = null;
            }
            requestLogger.info("{}, {}, {}, {}, {}, {}, {}", StructuredArguments.keyValue("action", "API request"),
                    StructuredArguments.keyValue("id", apiRequestId),
                    StructuredArguments.keyValue("method", request.getMethod()),
                    StructuredArguments.keyValue("path", request.getRequestURI()),
                    StructuredArguments.keyValue("query", request.getQueryString()),
                    StructuredArguments.keyValue("authenticatedUser", userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getNativeId() : "anonymous"),
                    StructuredArguments.keyValue("authenticatedClient", userWithRoles != null && userWithRoles.getClientId()!=null ? userWithRoles.getClientId() : "direct access"));
            Date start = new Date();
            filterChain.doFilter(request, response);
            Date end = new Date();
            	requestLogger.info("{}, {}, {}, {}, {}, {}, {}, {}, {}", StructuredArguments.keyValue("action", "API response"),
                    StructuredArguments.keyValue("id", apiRequestId),
                    StructuredArguments.keyValue("method", request.getMethod()),
                    StructuredArguments.keyValue("path", request.getRequestURI()),
                    StructuredArguments.keyValue("query", request.getQueryString()),
                    StructuredArguments.keyValue("statusCode", response.getStatus()),
                    StructuredArguments.keyValue("executionDuration", String.format("%d ms", end.getTime()-start.getTime())),
                    StructuredArguments.keyValue("authenticatedUser", userWithRoles != null && userWithRoles.getUser() != null ? userWithRoles.getUser().getNativeId() : "anonymous"),
                    StructuredArguments.keyValue("authenticatedClient", userWithRoles != null && userWithRoles.getClientId()!=null ? userWithRoles.getClientId() : "direct access"));
        } else {
            filterChain.doFilter(request, response);
        }

    }

}
