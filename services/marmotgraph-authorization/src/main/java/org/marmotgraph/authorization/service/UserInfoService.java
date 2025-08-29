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

package org.marmotgraph.authorization.service;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.marmotgraph.authorization.models.Permission;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.api.authentication.Authentication;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class UserInfoService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Authentication.Client authentication;

    private final PermissionsService permissionsService;

    private final JsonAdapter jsonAdapter;

    @Cacheable(CacheConstant.CACHE_KEYS_USER_ROLE_MAPPINGS)
    public Tuple<User, List<String>> getUserOrClientProfile(String token) {
        Map<String, Object> userInfo = authentication.getUserInfo(token);
        return new Tuple<>(buildUserFromClaims(userInfo), getRolesFromUserInfo(userInfo));
    }

    public User buildUserFromClaims(Map<String, Object> claims) {
        Object userName = claims.get("preferred_username");
        Object nativeId = claims.get("sub");
        Object name = claims.get("name");
        Object givenName = claims.get("given_name");
        Object familyName = claims.get("family_name");
        Object email = claims.get("email");
        return new User(userName instanceof String ? (String) userName : null, name instanceof String ? (String) name : null, email instanceof String ? (String) email : null, givenName instanceof String ? (String) givenName : null, familyName instanceof String ? (String) familyName : null, nativeId instanceof String ? (String) nativeId : null);
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)
    //TODO this is a quickfix to make sure the cache is cleared regularly. Please replace with a proper cache implementation supporting a TTL on a per-entry level
    @CacheEvict(value = CacheConstant.CACHE_KEYS_USER_ROLE_MAPPINGS, allEntries = true)
    public void evictUserOrClientProfiles() {
        logger.info("Wiping cached user role mappings");
    }

    public List<String> getRolesFromUserInfo(Map<String, Object> userInfo) {
        if (userInfo == null || userInfo.isEmpty()) {
            return new ArrayList<>();
        }
        Object user = userInfo.get("sub");
        final List<Permission> allRoleDefinitions = permissionsService.getAllRoleDefinitions();
        return allRoleDefinitions.stream().map(roleDefinition -> {
            String role = roleDefinition.getId();
            DynamicJson claims = jsonAdapter.fromJson(roleDefinition.getClaims(), DynamicJson.class);
            if (claims.containsKey("authenticated") && (boolean) claims.get("authenticated") && user != null) {
                //If the role is specified to be applied whenever somebody is authenticated, we can just return it.
                //Please note, that it is not possible to apply regex pattern for these kind of assignments.
                return Collections.singletonList(role);
            }
            return claims.keySet().stream().map(k -> translateUserInfoToRole(role, k, claims, userInfo))
                    .flatMap(Collection::stream).collect(Collectors.toSet());
        }).flatMap(Collection::stream).distinct().collect(Collectors.toList());
    }


    @NotNull
    private Set<String> translateUserInfoToRole(String roleLabel, String key, Map<?, ?> role, Map<?, ?> userInfo) {
        if (!userInfo.containsKey(key)) {
            return Collections.emptySet();
        }
        Object r = role.get(key);
        Object u = userInfo.get(key);
        if (r instanceof Map && u instanceof Map) {
            return ((Map<?, ?>) r).keySet().stream()
                    .map(k -> translateUserInfoToRole(roleLabel, (String) k, (Map) r, (Map) u)).
                    flatMap(Collection::stream).collect(Collectors.toSet());
        }
        if (r != null && u != null) {
            return ensureCollection(u).stream().filter(userClaim -> userClaim instanceof String).map(userClaim ->
                    ensureCollection(r).stream()
                            .filter(roleClaim -> ((String) userClaim).matches((String) roleClaim))
                            .map(roleClaim -> ((String) userClaim).replaceAll((String) roleClaim, roleLabel))
                            .collect(Collectors.toSet())).flatMap(Collection::stream).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }


    private Collection<?> ensureCollection(Object o) {
        if (o == null) {
            return Collections.emptySet();
        } else if (o instanceof Collection) {
            return (Collection<?>) o;
        } else {
            return Collections.singleton(o);
        }
    }
}
