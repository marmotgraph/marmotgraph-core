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

package org.marmotgraph.primaryStore.instances.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.FunctionalityInstance;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.primaryStore.instances.model.Space;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;

    private final EntityManager entityManager;

    private final Permissions permissions;

    private final AuthContext authContext;

    public Optional<Space> getSpace(SpaceName spaceName){
        return spaceRepository.findById(spaceName.getName());
    }

    public boolean isAutoRelease(SpaceName spaceName){
        Optional<Space> byId = spaceRepository.findById(spaceName.getName());
        return byId.map(Space::isAutoRelease).orElse(false);
    }

    private Set<SpaceName> whitelistedSpaceReads(UserWithRoles userWithRoles){
        if(!permissions.hasGlobalPermission(userWithRoles, Functionality.READ) && !permissions.hasGlobalPermission(userWithRoles, Functionality.READ_RELEASED) ){
            //We only need to filter if there is no "global" read available...
            return userWithRoles.getPermissions().stream().filter(p -> p.getId() == null && (p.getFunctionality() == Functionality.READ || p.getFunctionality() == Functionality.READ_RELEASED)).map(FunctionalityInstance::getSpace).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return null;
    }



    public Paginated<SpaceInformation> listSpaces(PaginationParam paginationParam) {
        Set<SpaceName> whitelistedSpaceReads = whitelistedSpaceReads(authContext.getUserWithRoles());
        TypedQuery<Space> query;
        if(whitelistedSpaceReads == null){
            query = entityManager.createQuery("select s from Space s order by s.name", Space.class);
        }
        else{
            query = entityManager.createQuery("select s from Space s where s.name in :whitelist order by s.name", Space.class).setParameter("whitelist", whitelistedSpaceReads);
        }
        if(paginationParam.getSize()!=null) {
            query = query.setFirstResult((int) paginationParam.getFrom()).setMaxResults(paginationParam.getSize().intValue());
        }
        List<SpaceInformation> result = query.getResultStream().map(Space::toSpaceInformation).toList();
        //FIXME we need to count the total results.
        return new Paginated<>(result, 0L, result.size(), paginationParam.getFrom());

    }
}
