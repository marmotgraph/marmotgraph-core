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

package org.marmotgraph.tenants.config;

import com.arangodb.ArangoDB;
import com.arangodb.mapping.ArangoJack;
import org.marmotgraph.arango.commons.model.ArangoDatabaseProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantArangoConfiguration {

    @Bean
    @Qualifier("arangoBuilderForTenants")
    public ArangoDB.Builder produceTenantsArangoDB(
            @Value("${org.marmotgraph.arango.host}") String host,
            @Value("${org.marmotgraph.arango.port}") Integer port,
            @Value("${org.marmotgraph.arango.user}") String user,
            @Value("${org.marmotgraph.arango.pwd}") String pwd, @Value("${arangodb.timeout:}")
                    Integer timeout, @Value("${arangodb.connections.max:}")
                    Integer maxConnections) {
        ArangoDB.Builder builder = new ArangoDB.Builder().host(host, port).user(user).password(pwd);
        if (timeout != null) {
            builder.timeout(timeout);
        }
        if (maxConnections != null) {
            builder.maxConnections(maxConnections);
        }
        builder.serializer(new ArangoJack());
        return builder;
    }

    @Bean
    @Qualifier("tenantsDB")
    public ArangoDatabaseProxy produceTenantsDB(@Qualifier("arangoBuilderForTenants") ArangoDB.Builder arangoDB) {
        return new ArangoDatabaseProxy(arangoDB.build(), "kg1-tenants");
    }

}
