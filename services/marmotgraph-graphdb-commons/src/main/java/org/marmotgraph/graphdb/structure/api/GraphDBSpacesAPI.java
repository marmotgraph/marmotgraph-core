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

package org.marmotgraph.graphdb.structure.api;

import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.api.GraphDBSpaces;
import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.exception.InvalidRequestException;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.marmotgraph.graphdb.commons.controller.PermissionsController;
import org.marmotgraph.graphdb.structure.controller.MetaDataController;
import org.marmotgraph.graphdb.structure.controller.StructureRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class GraphDBSpacesAPI implements GraphDBSpaces.Client {


    private static final String NO_RIGHTS_TO_MANAGE_SPACES = "You don't have the required rights to manage spaces";
    private final StructureRepository structureRepository;
    private final MetaDataController metaDataController;
    private final PermissionsController permissionsController;
    private final AuthContext authContext;

    public GraphDBSpacesAPI(StructureRepository structureRepository, MetaDataController metaDataController, PermissionsController permissionsController, AuthContext authContext) {
        this.structureRepository = structureRepository;
        this.metaDataController = metaDataController;
        this.permissionsController = permissionsController;
        this.authContext = authContext;
    }

    private static Space createSpaceRepresentation(String name) {
        return new Space(new SpaceName(name), false, false, false);
    }

    private List<Space> getSpaces() {
        List<Space> spaces = this.metaDataController.getSpaces(DataStage.IN_PROGRESS, authContext.getUserWithRoles());
        final SpaceName privateSpace = authContext.getUserWithRoles().getPrivateSpace();
        final Optional<Space> existingPrivateSpace = spaces.stream().filter(s -> s.getName().equals(privateSpace)).findAny();
        if (existingPrivateSpace.isPresent()) {
            //Rename the existing private space
            existingPrivateSpace.get().setName(SpaceName.fromString(SpaceName.PRIVATE_SPACE));
            existingPrivateSpace.get().setIdentifier(SpaceName.PRIVATE_SPACE);
        } else {
            //The private space doesn't exist yet -> we create it virtually.
            spaces.add(createSpaceRepresentation(SpaceName.PRIVATE_SPACE));
        }
        if (authContext.getUserWithRoles().hasInvitations()) {
            spaces.add(createSpaceRepresentation(SpaceName.REVIEW_SPACE));
        }
        spaces.sort(Comparator.comparing(s -> s.getName().getName()));
        return spaces;
    }

    @Override
    public Space getSpace(SpaceName space) {
        return getSpaces().stream().filter(s -> s.getName().equals(space)).findFirst().orElse(null);
    }

    @Override
    public Paginated<Space> listSpaces(PaginationParam paginationParam) {
        return PaginationParam.paginate(getSpaces(), paginationParam);
    }

    @Override
    public SpaceSpecification getSpaceSpecification(SpaceName spaceName) {
        SpaceSpecification spaceSpecification = this.metaDataController.getSpaceSpecification(spaceName);
        try {
            this.checkOnSpaceSpecificationAdminOperations(spaceSpecification);
        } catch (InvalidRequestException e) {
            throw new InvalidRequestException("You can't provide a specification for your private space");
        } catch (Exception e) {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
        return spaceSpecification;
    }

    @Override
    public void specifySpace(SpaceSpecification spaceSpecification) {
        if (spaceSpecification.getScopeRelevant() != null && spaceSpecification.getScopeRelevant() && !permissionsController.canDefineScopeSpace(authContext.getUserWithRoles())) {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
        if(permissionsController.canManageSpaces(authContext.getUserWithRoles(), SpaceName.fromString(spaceSpecification.getName()))) {
            switch(spaceSpecification.getName()){
                case SpaceName.PRIVATE_SPACE:
                    throw new InvalidRequestException("You can't provide a definition for your private space");
                case SpaceName.REVIEW_SPACE:
                    throw new InvalidRequestException("You can't provide a definition for the invitation space");
                default:
                    break;
            }
            structureRepository.createOrUpdateSpaceDocument(spaceSpecification);
            structureRepository.evictSpacesCache();
            structureRepository.evictSpaceSpecificationsCache();
        }
        else{
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    @Override
    public void removeSpaceSpecification(SpaceName spaceName) {
        if(permissionsController.canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
            switch(spaceName.getName()){
                case SpaceName.PRIVATE_SPACE:
                    throw new InvalidRequestException("You can't remove your private space");
                case SpaceName.REVIEW_SPACE:
                    throw new InvalidRequestException("You can't remove the invitation space");
                default:
                    break;
            }
            structureRepository.removeSpaceDocument(spaceName);
            structureRepository.evictSpacesCache();
            structureRepository.evictSpaceSpecificationsCache();
        }
        else{
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    @Override
    public boolean checkTypeInSpace(SpaceName spaceName, String typeName) {
        if (permissionsController.canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
            return this.metaDataController.checkTypeToSpace(spaceName, typeName);
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    @Override
    public void addTypeToSpace(SpaceName spaceName, String typeName) {
        if (permissionsController.canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
            structureRepository.addLinkBetweenSpaceAndType(spaceName, typeName);
            structureRepository.evictTypesInSpaceBySpecification(spaceName);
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    @Override
    public void removeTypeFromSpace(SpaceName spaceName, String typeName) {
        if (permissionsController.canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
            structureRepository.removeLinkBetweenSpaceAndType(spaceName, typeName);
            structureRepository.evictTypesInSpaceBySpecification(spaceName);
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    private void checkOnSpaceSpecificationAdminOperations(SpaceSpecification spaceSpecification) {

        if (!permissionsController.canManageSpaces(authContext.getUserWithRoles(), SpaceName.fromString(spaceSpecification.getName()))) {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
        switch (spaceSpecification.getName()) {
            case SpaceName.PRIVATE_SPACE:
            case SpaceName.REVIEW_SPACE:
                throw new InvalidRequestException();
            default:
                break;
        }
    }
}
