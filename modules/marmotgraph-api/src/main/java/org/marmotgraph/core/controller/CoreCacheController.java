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

package org.marmotgraph.core.controller;

import org.marmotgraph.auth.api.Permissions;
import org.marmotgraph.commons.model.auth.Functionality;
import org.marmotgraph.auth.api.AuthContext;
import org.marmotgraph.commons.exceptions.UnauthorizedException;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class CoreCacheController {

    private static final String NO_RIGHTS_TO_FLUSH_CACHE = "You are not allowed to flush cache";

    private final Permissions permissions;
    private final CacheManager cacheManager;

    public CoreCacheController(Permissions permissions, CacheManager cacheManager) {
        this.permissions = permissions;
        this.cacheManager = cacheManager;
    }

    public List<String> clearKeys(List<String> keys) {

        if (!permissions.hasGlobalPermission(Functionality.CACHE_FLUSH)) {
            throw new UnauthorizedException(CoreCacheController.NO_RIGHTS_TO_FLUSH_CACHE);
        }

        List<String> keysFlushed = new ArrayList<>();

        cacheManager.getCacheNames().forEach(cacheName -> {
            if( keys.contains(cacheName) ) {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
                keysFlushed.add(cacheName);
            }
        });

        return keysFlushed;
    }

    public List<String> clearAllKeys() {

        if (!permissions.hasGlobalPermission(Functionality.CACHE_FLUSH)) {
            throw new UnauthorizedException(CoreCacheController.NO_RIGHTS_TO_FLUSH_CACHE);
        }

        return this.clearKeys(cacheManager.getCacheNames().stream().toList());
    }

    public List<String> getKeys() {
        if (!permissions.hasGlobalPermission(Functionality.CACHE_FLUSH)) {
            throw new UnauthorizedException(CoreCacheController.NO_RIGHTS_TO_FLUSH_CACHE);
        }
        return cacheManager.getCacheNames().stream().toList();
    }

}
