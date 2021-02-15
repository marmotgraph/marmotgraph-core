/*
 * Copyright 2021 EPFL/Human Brain Project PCO
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

package eu.ebrains.kg.graphdb.users.api;

import eu.ebrains.kg.commons.api.GraphDBUsers;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.markers.ExposesUserInfo;
import eu.ebrains.kg.commons.model.Paginated;
import eu.ebrains.kg.commons.model.PaginationParam;
import eu.ebrains.kg.graphdb.users.controller.UsersRepository;
import org.springframework.stereotype.Component;

@Component
public class GraphDBUsersAPI implements GraphDBUsers.Client {

    private UsersRepository usersRepository;

    public GraphDBUsersAPI(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    @ExposesUserInfo
    public Paginated<NormalizedJsonLd> getUsers(PaginationParam paginationParam){
        return usersRepository.getUsers(paginationParam);
    }

}
