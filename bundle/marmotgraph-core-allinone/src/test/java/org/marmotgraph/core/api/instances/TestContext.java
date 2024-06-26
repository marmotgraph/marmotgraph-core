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

package org.marmotgraph.core.api.instances;

import org.marmotgraph.arango.commons.model.ArangoDatabaseProxy;
import org.marmotgraph.authentication.api.AuthenticationAPI;
import org.marmotgraph.authentication.controller.AuthenticationRepository;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.SetupLogic;
import org.marmotgraph.commons.permission.roles.Role;
import org.marmotgraph.commons.permission.roles.RoleMapping;
import org.springframework.cache.CacheManager;

import java.util.*;
import java.util.stream.Collectors;


public class TestContext {
    private final List<ArangoDatabaseProxy> databaseProxies;
    private final AuthenticationAPI authentication;
    private final Collection<List<Role>> roleCollections;
    private final IdUtils idUtils;
    private final CacheManager cacheManager;
    private final List<SetupLogic> setupLogics;
    private final AuthenticationRepository authenticationRepository;

    public TestContext(IdUtils idUtils, List<ArangoDatabaseProxy> databaseProxies, AuthenticationAPI authentication, RoleMapping[] roleMappings, List<SetupLogic> setupLogics, AuthenticationRepository authenticationRepository, CacheManager cacheManager) {
        this(idUtils, databaseProxies, authentication,  Arrays.stream(roleMappings).filter(Objects::nonNull).map(r -> Collections.singletonList(r.toRole(null))).collect(Collectors.toSet()), setupLogics, authenticationRepository, cacheManager);
    }

    public TestContext(IdUtils idUtils, List<ArangoDatabaseProxy> databaseProxies, AuthenticationAPI authentication, Collection<List<Role>> roleCollections, List<SetupLogic> setupLogics, AuthenticationRepository authenticationRepository, CacheManager cacheManager) {
        this.databaseProxies = databaseProxies;
        this.authentication = authentication;
        this.roleCollections = roleCollections;
        this.idUtils = idUtils;
        this.cacheManager = cacheManager;
        this.setupLogics = setupLogics;
        this.authenticationRepository = authenticationRepository;
    }

    public List<ArangoDatabaseProxy> getDatabaseProxies() {
        return databaseProxies;
    }

    public AuthenticationAPI getAuthentication() {
        return authentication;
    }

    public Collection<List<Role>> getRoleCollections() {
        return roleCollections;
    }

    public IdUtils getIdUtils() {
        return idUtils;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public List<SetupLogic> getSetupLogics() {
        return setupLogics;
    }

    public AuthenticationRepository getAuthenticationRepository() {
        return authenticationRepository;
    }
}
