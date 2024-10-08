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

package org.marmotgraph.authentication.keycloak;

import org.marmotgraph.authentication.controller.AuthenticationRepository;
import org.marmotgraph.commons.cache.CacheConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserInfoMapping {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final KeycloakClient keycloakClient;

    private final AuthenticationRepository authenticationRepository;

    public UserInfoMapping(KeycloakClient keycloakClient, AuthenticationRepository authenticationRepository) {
        this.keycloakClient = keycloakClient;
        this.authenticationRepository = authenticationRepository;
    }

    @Cacheable(CacheConstant.CACHE_KEYS_USER_ROLE_MAPPINGS)
    public List<String> getUserOrClientProfile(String token){
        Map<String, Object> userInfo = keycloakClient.getUserInfo(token);
        return authenticationRepository.getRolesFromUserInfo(userInfo);
    }

    @Scheduled(fixedRate = 1000*60*60) //TODO this is a quickfix to make sure the cache is cleared regularly. Please replace with a proper cache implementation supporting a TTL on a per-entry level
    @CacheEvict(value = CacheConstant.CACHE_KEYS_USER_ROLE_MAPPINGS, allEntries = true)
    public void evictUserOrClientProfiles() {
        logger.info("Wiping cached user role mappings");
    }

}
