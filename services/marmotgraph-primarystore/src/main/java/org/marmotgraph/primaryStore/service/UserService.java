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

package org.marmotgraph.primaryStore.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.validation.constraints.NotNull;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.User;
import org.marmotgraph.primaryStore.model.PrimaryStoreUser;
import org.marmotgraph.primaryStore.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final EntityManager entityManager;

    public UserService(UserRepository userRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public void save(User user) {
        userRepository.saveAndFlush(PrimaryStoreUser.fromUser(user, getUserUUID(user)));
    }

    public List<PrimaryStoreUser> getUsersByIds(Set<UUID> uuids){
        return userRepository.findByUuidIn(uuids);
    }

    public Paginated<PrimaryStoreUser> getUsers(PaginationParam paginationParam, Optional<String> nativeId){
        if(nativeId.isPresent()){
            PrimaryStoreUser byNativeId = userRepository.findByNativeId(nativeId.get());
            return byNativeId != null ? new Paginated<>(Collections.singletonList(byNativeId), 1L, 1L, 0L) : new Paginated<>(Collections.emptyList(), 0L, 0L, 0L);
        }
        else {
            Long totalCount = null;
            long from = 0L;
            TypedQuery<PrimaryStoreUser> query = entityManager.createQuery("SELECT p from PrimaryStoreUser p", PrimaryStoreUser.class);
            if (paginationParam != null) {
                from = paginationParam.getFrom();
                query.setFirstResult((int) from);
                if (paginationParam.getSize() != null) {
                    query.setMaxResults(paginationParam.getSize().intValue());
                }
                totalCount = userRepository.count();
            }
            List<PrimaryStoreUser> results = query.getResultList();
            return new Paginated<>(results, totalCount == null ? results.size() : totalCount, results.size(), from);
        }
    }

    @NotNull
    public static UUID getUserUUID(User user) {
        try {
            return UUID.fromString(user.getNativeId());
        } catch (IllegalArgumentException e) {
            //If the native id is not a UUID on its own, we build one based on the string.
            return UUID.nameUUIDFromBytes(user.getNativeId().getBytes(StandardCharsets.UTF_8));
        }
    }


}
