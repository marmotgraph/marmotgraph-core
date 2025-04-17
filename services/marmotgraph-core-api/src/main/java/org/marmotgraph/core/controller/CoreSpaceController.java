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

import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.api.GraphDBSpaces;
import org.marmotgraph.commons.api.PrimaryStoreEvents;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.FunctionalityInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CoreSpaceController {

    private final GraphDBSpaces.Client graphDBSpaces;
    private final PrimaryStoreEvents.Client primaryStoreEvents;
    private final AuthContext authContext;

    public CoreSpaceController(GraphDBSpaces.Client graphDBSpaces, PrimaryStoreEvents.Client primaryStoreEvents, AuthContext authContext) {
        this.graphDBSpaces = graphDBSpaces;
        this.primaryStoreEvents = primaryStoreEvents;
        this.authContext = authContext;
    }

    SpaceInformation translateSpaceToSpaceInformation(Space space, boolean permissions) {
        final SpaceInformation spaceInformation = space.toSpaceInformation();
        if (permissions) {
            UserWithRoles userWithRoles = authContext.getUserWithRoles();
            String spaceIdentifier = spaceInformation.getIdentifier();
            if (spaceIdentifier != null) {
                final SpaceName internalSpaceName = SpaceName.getInternalSpaceName(spaceIdentifier, userWithRoles.getPrivateSpace());
                List<Functionality> applyingFunctionalities = userWithRoles.getPermissions().stream().
                        filter(f -> (f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.INSTANCE
                                || f.getFunctionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.TYPES) && f.appliesTo(internalSpaceName, null)
                        ).map(FunctionalityInstance::getFunctionality).distinct().collect(Collectors.toList());
                spaceInformation.setPermissions(applyingFunctionalities);
            }
        }
        return spaceInformation;
    }


    public Paginated<SpaceInformation> listSpaces(PaginationParam pagination, boolean permissions) {
        Paginated<Space> sp = graphDBSpaces.listSpaces(pagination);
        final List<SpaceInformation> spaceInformations = sp.getData().stream().map(s -> translateSpaceToSpaceInformation(s, permissions)).collect(Collectors.toList());
        return new Paginated<>(spaceInformations, sp.getTotalResults(), sp.getSize(), sp.getFrom());
    }


    public SpaceInformation getSpace(SpaceName space, boolean permissions) {
        Space sp = graphDBSpaces.getSpace(space);
        return sp != null ? translateSpaceToSpaceInformation(sp, permissions) : null;
    }

    public SpaceSpecification getSpaceSpecification(String space) {
        return graphDBSpaces.getSpaceSpecification(new SpaceName(space));
    }


    public void createSpaceSpecification(SpaceSpecification spaceSpec) {
        graphDBSpaces.specifySpace(spaceSpec);
    }

    public void removeSpaceSpecification(SpaceName space) {
        graphDBSpaces.removeSpaceSpecification(space);
    }

    public boolean checkTypeInSpace(SpaceName space, String type) {
        return graphDBSpaces.checkTypeInSpace(space, type);
    }

    public void addTypeToSpace(SpaceName space, String type) {
        graphDBSpaces.addTypeToSpace(space, type);
    }

    public void removeTypeFromSpace(SpaceName space, String type) {
        graphDBSpaces.removeTypeFromSpace(space, type);
    }

    public void rerunEvents(SpaceName space) {
        primaryStoreEvents.rerunEvents(space.getName());
    }

}
