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

package org.marmotgraph.core.api.config.openApi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenAPIv3 {
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public GroupedOpenApi v3Beta() {
        return GroupedOpenApi.builder()
                .group("v3-beta")
                .packagesToScan("org.marmotgraph.core.api.v3beta")
                .build();
    }

    @Bean
    public GroupedOpenApi v3() {
        return GroupedOpenApi.builder()
                .group("v3")
                .packagesToScan("org.marmotgraph.core.api.v3")
                .build();
    }

    @Bean
    public OpenAPI genericOpenAPI(@Value("${org.marmotgraph.login.endpoint}") String loginEndpoint, @Value("${org.marmotgraph.login.tokenEndpoint}") String tokenEndpoint, @Value("${org.marmotgraph.commit}") String commit, @Value("${org.marmotgraph.login.client}") String client) {
        OAuthFlow oAuthFlow = new OAuthFlow();
        oAuthFlow.refreshUrl(tokenEndpoint);
        oAuthFlow.tokenUrl(tokenEndpoint);
        oAuthFlow.authorizationUrl(loginEndpoint);
        oAuthFlow.addExtension("client_id", client);
        SecurityScheme authToken = new SecurityScheme().name("Authorization").type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().authorizationCode(oAuthFlow)).description("The user authentication");
        OpenAPI openapi = new OpenAPI().openapi("3.0.3");
        String description = String.format("This is the API of the MarmotGraph (commit %s).", commit);
        return openapi.tags(APINaming.orderedTags()).info(new Info().title("This is the MarmotGraph API").description(description)
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                        .termsOfService("https://kg.ebrains.eu/search-terms-of-use.html")).components(new Components())
                .schemaRequirement(AUTHORIZATION_HEADER, authToken)
                .security(Collections.singletonList(new SecurityRequirement().addList(AUTHORIZATION_HEADER)));
    }
}
