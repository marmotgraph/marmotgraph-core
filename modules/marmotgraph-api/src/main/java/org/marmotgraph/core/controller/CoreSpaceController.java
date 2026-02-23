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

import lombok.AllArgsConstructor;
import org.marmotgraph.auth.service.AuthContext;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.primaryStore.api.SpacesAPI;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class CoreSpaceController {

    private final SpacesAPI spaces;
    private final AuthContext authContext;

    public Paginated<SpaceInformation> listSpaces(PaginationParam pagination, boolean permissions) {
        return spaces.listSpaces(pagination, permissions);
    }

    public SpaceInformation getSpace(SpaceName space, boolean permissions) {
        return spaces.getSpace(space, permissions);
    }

    public SpaceSpecification getSpaceSpecification(String space) {
        return spaces.getSpaceSpecification(new SpaceName(space));
    }

    public void createSpaceSpecification(SpaceSpecification spaceSpec) {
        spaces.specifySpace(spaceSpec);
    }

    public void removeSpaceSpecification(SpaceName space) {
        spaces.removeSpaceSpecification(space);
    }

    public boolean checkTypeInSpace(SpaceName space, String type) {
        return spaces.checkTypeInSpace(space, type);
    }

    public void addTypeToSpace(SpaceName space, String type) {
        spaces.addTypeToSpace(space, type);
    }

    public void removeTypeFromSpace(SpaceName space, String type) {
        spaces.removeTypeFromSpace(space, type);
    }

}
