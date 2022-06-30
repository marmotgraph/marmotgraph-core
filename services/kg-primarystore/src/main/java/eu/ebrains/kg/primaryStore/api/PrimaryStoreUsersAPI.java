/*
 * Copyright 2022 EPFL/Human Brain Project PCO
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
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.primaryStore.api;

import eu.ebrains.kg.commons.api.PrimaryStoreUsers;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesUserInfo;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.commons.model.ReducedUserInformation;
import eu.ebrains.kg.primaryStore.controller.UsersRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class PrimaryStoreUsersAPI implements PrimaryStoreUsers.Client {

    private UsersRepository usersRepository;

    public PrimaryStoreUsersAPI(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    @ExposesUserInfo
    public Paginated<NormalizedJsonLd> getUsers(PaginationParam paginationParam){
        return usersRepository.getUsers(paginationParam);
    }

    @Override
    @ExposesUserInfo
    public Paginated<NormalizedJsonLd> getUsersWithLimitedInfo(PaginationParam paginationParam, String id){
        return usersRepository.getUsersWithLimitedInfo(paginationParam, id);
    }

    @Override
    @ExposesUserInfo
    public Map<String, ReducedUserInformation> getUsers(Set<UUID> uuids) {
        return usersRepository.getUsers(uuids);
    }
}