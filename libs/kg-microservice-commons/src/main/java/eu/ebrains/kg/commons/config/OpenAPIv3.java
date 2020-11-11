/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.commons.config;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.config.openApiGroups.Advanced;
import eu.ebrains.kg.commons.config.openApiGroups.Simple;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class OpenAPIv3 {


    private static String[] getPathsByAnnotation(RequestMappingHandlerMapping requestHandlerMapping, String restrictToPackage, Class<? extends Annotation>... groupAnnotations){
        List<String> paths = new ArrayList<>();
        String[] pathsArr = {};
        requestHandlerMapping.getHandlerMethods()
                .forEach((key, value) -> {
                    if(restrictToPackage == null || value.getMethod().getDeclaringClass().getPackageName().startsWith(restrictToPackage)) {
                        boolean containsAnnotation = Arrays.stream(groupAnnotations).anyMatch(g -> AnnotationUtils.findAnnotation(value.getMethod(), g) != null || AnnotationUtils.findAnnotation(value.getMethod().getDeclaringClass(), g) != null);
                        if (containsAnnotation) {
                            paths.add(key.getPatternsCondition().getPatterns().iterator().next());
                        }
                    }
                });
       return paths.toArray(pathsArr);
    }

    @Bean
    public GroupedOpenApi simpleApi(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .group("0 simple")
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, "eu.ebrains.kg.core", Simple.class))
                .build();
    }

    @Bean
    public GroupedOpenApi advancedApi(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .group("1 advanced")
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, "eu.ebrains.kg.core", Simple.class, Advanced.class))
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(RequestMappingHandlerMapping requestHandlerMapping) {
        return GroupedOpenApi.builder()
                .group("2 admin")
                .pathsToMatch(getPathsByAnnotation(requestHandlerMapping, "eu.ebrains.kg.core", Admin.class))
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("3 all")
                .packagesToScan("eu.ebrains.kg.core")
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "eu.ebrains.kg.api.doc.hideInternal", havingValue = "false", matchIfMissing = true)
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
                .group("4 internal")
                .packagesToScan("eu.ebrains.kg")
                .packagesToExclude("eu.ebrains.kg.core")
                .build();
    }


    @Bean
    public OpenAPI customOpenAPI(@Value("${spring.application.name}") String applicationName, @Value("${eu.ebrains.kg.login.endpoint}") String loginEndpoint, @Value("${eu.ebrains.kg.api.basePath}") String basePath, @Value("${eu.ebrains.kg.api.versioned}") boolean versioned, @Value("${eu.ebrains.kg.server}") String server) {
        SecurityScheme clientId = new SecurityScheme().name("Client ID").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Id").description("The client-id for the proxied client-authentication. To be provided with \"client-secret\" and either \"client-serviceAccount-secret\" or the \"user-token\"");
        SecurityScheme clientSecret = new SecurityScheme().name("clientSecret").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Secret").description("The client-secret for the proxied client-authentication. To be provided with \"client-id\" and either \"client-serviceAccount-secret\" or the \"user-token\"");
        SecurityScheme clientServiceAccountSecret = new SecurityScheme().name("clientServiceAccountSecret").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-ServiceAccount-Secret").description("Provide the client-secret a second time to authenticate as the service account with the full authentication mechanisms. To be provided with \"client-id\" and \"client-secret\"");
        SecurityRequirement clientSecretSaReq = new SecurityRequirement().addList("clientId").addList("clientSecret").addList("clientServiceAccountSecret");
        SecurityRequirement clientSecretUserReq = new SecurityRequirement().addList("clientId").addList("clientSecret").addList("userToken");
        SecurityScheme clientToken = new SecurityScheme().name("clientToken").type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("Client-Authorization").description("The already resolved token for the client account. This is the recommended way of authenticating clients since you don't expose your static credentials to the KG core but handle it on the client side.");
        SecurityRequirement clientTokenReq = new SecurityRequirement().addList("clientToken").addList("userToken");
        OAuthFlow oAuthFlow = new OAuthFlow();
        oAuthFlow.authorizationUrl(loginEndpoint);
        SecurityScheme userToken = new SecurityScheme().name("userToken").type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().implicit(oAuthFlow)).description("The browser-based user authentication.");
        OpenAPI openapi = new OpenAPI().openapi("3.0.3");
        return openapi.info(new Info().version(Version.API).title(String.format("This is the %s API", applicationName)).license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")).termsOfService("https://kg.ebrains.eu/search-terms-of-use.html")).components(new Components()).schemaRequirement("clientId", clientId).schemaRequirement("clientSecret", clientSecret).schemaRequirement("clientServiceAccountSecret", clientServiceAccountSecret).schemaRequirement("clientToken", clientToken).schemaRequirement("userToken", userToken)
                .security(Arrays.asList(clientTokenReq, clientSecretUserReq, clientSecretSaReq));
    }
}
