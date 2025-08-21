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

package org.marmotgraph.primaryStore.users.service;

import lombok.AllArgsConstructor;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.ReducedUserInformation;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.primaryStore.users.model.PrimaryStoreUser;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class UserController {

    private final Permissions permissions;
    private final AuthContext authContext;
    private final UserService userService;

    public Map<String, ReducedUserInformation> getUsers(Set<UUID> uuids) {
        if (!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.LIST_USERS_LIMITED, null)) {
            throw new ForbiddenException("No right to list users");
        }
        return userService.getUsersByIds(uuids).stream().collect(Collectors.toMap(k -> k.getUuid().toString(), PrimaryStoreUser::toUserWithLimitedInformation));
    }

    public Paginated<NormalizedJsonLd> getUsers(PaginationParam paginationParam){
        return doGetUsers(paginationParam, Optional.empty(), Functionality.LIST_USERS, PrimaryStoreUser::toUser);
    }

    public Paginated<NormalizedJsonLd> getUsersWithLimitedInfo(PaginationParam paginationParam, Optional<String> nativeId){
        return doGetUsers(paginationParam, nativeId, Functionality.LIST_USERS_LIMITED, PrimaryStoreUser::toUserWithLimitedInformation);
    }

    private Paginated<NormalizedJsonLd> doGetUsers(PaginationParam paginationParam, Optional<String> nativeId, Functionality functionality, Function<? super PrimaryStoreUser, ? extends NormalizedJsonLd> function){
        if (!permissions.hasPermission(authContext.getUserWithRoles(), functionality, null)) {
            throw new ForbiddenException("No right to list users");
        }
        Paginated<PrimaryStoreUser> users = this.userService.getUsers(paginationParam, Optional.empty());
        if(users != null){
            return new Paginated<>(users.getData().stream().filter(Objects::nonNull).map(function).map(u -> (NormalizedJsonLd) u).toList(), users.getTotalResults(), users.getSize(), users.getFrom());
        }
        return null;
    }

}
