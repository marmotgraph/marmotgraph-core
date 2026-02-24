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

package org.marmotgraph.primaryStore.api;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.auth.api.Permissions;
import org.marmotgraph.auth.models.FunctionalityInstance;
import org.marmotgraph.auth.models.UserWithRoles;
import org.marmotgraph.auth.api.AuthContext;
import org.marmotgraph.commons.exceptions.ForbiddenException;
import org.marmotgraph.commons.exceptions.InvalidRequestException;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.auth.Functionality;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.primaryStore.instances.model.Space;
import org.marmotgraph.primaryStore.instances.repositories.SpaceRepository;
import org.marmotgraph.primaryStore.instances.service.SpaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class SpacesAPI {

    private static final String NO_RIGHTS_TO_MANAGE_SPACES = "You don't have the required rights to manage spaces";

    private final Permissions permissions;
    private final AuthContext authContext;
    private final SpaceRepository spaceDefinitionRepository;
    private final SpaceService spaceService;

    public SpaceInformation getSpace(SpaceName space, boolean permissions) {
        Optional<Space> s = spaceService.getSpace(space);
        if(s.isPresent()){
            Space foundSpace = s.get();
            UserWithRoles userWithRoles = authContext.getUserWithRoles();
            final SpaceName privateSpace = userWithRoles.getPrivateSpace();
            if(foundSpace.getName().equals(privateSpace.getName())){
                foundSpace.setName(SpaceName.PRIVATE_SPACE);
            }
            SpaceInformation spaceInformation = foundSpace.toSpaceInformation();
            if(permissions){
                applyPermissions(spaceInformation, userWithRoles);
            }
            return spaceInformation;
        }
        return null;
    }

    public Paginated<SpaceInformation> listSpaces(PaginationParam paginationParam, boolean returnPermissions) {
        UserWithRoles userWithRoles = authContext.getUserWithRoles();
        final SpaceName privateSpace = userWithRoles.getPrivateSpace();
        Paginated<SpaceInformation> spaceInformationPaginated = spaceService.listSpaces(paginationParam);
        spaceInformationPaginated.getData().stream().filter(s -> s.getName().equals(privateSpace.getName())).findFirst().ifPresent(s -> s.setName(SpaceName.PRIVATE_SPACE));
        if (userWithRoles.hasInvitations()) {
            if(spaceInformationPaginated.getTotalResults()-spaceInformationPaginated.getSize()<=spaceInformationPaginated.getFrom()){
                //We're on the last page -> let's add the virtual space
                SpaceInformation spaceInformation = new SpaceInformation();
                spaceInformation.setIdentifier(SpaceName.REVIEW_SPACE);
                spaceInformation.setName(SpaceName.REVIEW_SPACE);
                spaceInformationPaginated.addItemToData(spaceInformation);
            }
            spaceInformationPaginated.increaseTotalResult();
        }
        if (returnPermissions) {
            for (SpaceInformation spaceInformation : spaceInformationPaginated.getData()) {
                applyPermissions(spaceInformation, userWithRoles);
            }
        }
        return spaceInformationPaginated;
    }

    private static void applyPermissions(SpaceInformation spaceInformation, UserWithRoles userWithRoles) {
        String spaceIdentifier = spaceInformation.getIdentifier();
        if (spaceIdentifier != null) {
            final SpaceName internalSpaceName = SpaceName.getInternalSpaceName(spaceIdentifier, userWithRoles.getPrivateSpace());
            List<Functionality> applyingFunctionalities = userWithRoles.getPermissions().stream().
                    filter(f -> (f.functionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.INSTANCE
                                 || f.functionality().getFunctionalityGroup() == Functionality.FunctionalityGroup.TYPES) && f.appliesTo(internalSpaceName, null)
                    ).map(FunctionalityInstance::functionality).distinct().collect(Collectors.toList());
            spaceInformation.setPermissions(applyingFunctionalities);
        }
    }

    public SpaceSpecification getSpaceSpecification(SpaceName spaceName) {
        throw new NotImplementedException();
//        try {
//            this.checkOnSpaceSpecificationAdminOperations(spaceName);
//            return this.metaDataController.getSpaceSpecification(spaceName);
//        } catch (InvalidRequestException e) {
//            throw new InvalidRequestException("You can't provide a specification for your private space");
//        } catch (Exception e) {
//            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
//        }
    }

    private boolean canDefineScopeSpace() {
        return permissions.hasGlobalPermission(Functionality.DEFINE_SCOPE_RELEVANT_SPACE);
    }

    private boolean canManageSpaces(SpaceName spaceName){
        return permissions.hasPermission(Functionality.MANAGE_SPACE, spaceName);
    }

    public void specifySpace(SpaceSpecification spaceSpecification) {
        if (spaceSpecification.getScopeRelevant() != null && spaceSpecification.getScopeRelevant() && !canDefineScopeSpace()) {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
        if (canManageSpaces(SpaceName.fromString(spaceSpecification.getName()))) {
            switch (spaceSpecification.getName()) {
                case SpaceName.PRIVATE_SPACE:
                    throw new InvalidRequestException("You can't provide a definition for your private space");
                case SpaceName.REVIEW_SPACE:
                    throw new InvalidRequestException("You can't provide a definition for the invitation space");
                default:
                    break;
            }
            spaceDefinitionRepository.save(Space.fromSpaceSpecification(spaceSpecification));
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    public void removeSpaceSpecification(SpaceName spaceName) {
        if (canManageSpaces(spaceName)) {
            switch (spaceName.getName()) {
                case SpaceName.PRIVATE_SPACE:
                    throw new InvalidRequestException("You can't remove your private space");
                case SpaceName.REVIEW_SPACE:
                    throw new InvalidRequestException("You can't remove the invitation space");
                default:
                    break;
            }
            spaceDefinitionRepository.deleteById(spaceName.getName());
        } else {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
    }

    public boolean checkTypeInSpace(SpaceName spaceName, String typeName) {
        throw new NotImplementedException();
//        if (canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
//            return this.metaDataController.checkTypeToSpace(spaceName, typeName);
//        } else {
//            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
//        }
    }

    public void addTypeToSpace(SpaceName spaceName, String typeName) {
        throw new NotImplementedException();
//        if (canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
//            structureRepository.addLinkBetweenSpaceAndType(spaceName, typeName);
//            structureRepository.evictTypesInSpaceBySpecification(spaceName);
//        } else {
//            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
//        }
    }

    public void removeTypeFromSpace(SpaceName spaceName, String typeName) {
        throw new NotImplementedException();
//        if (canManageSpaces(authContext.getUserWithRoles(), spaceName)) {
//            structureRepository.removeLinkBetweenSpaceAndType(spaceName, typeName);
//            structureRepository.evictTypesInSpaceBySpecification(spaceName);
//        } else {
//            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
//        }
    }

    private void checkOnSpaceSpecificationAdminOperations(SpaceName spaceName) {
        if (!canManageSpaces(spaceName)) {
            throw new ForbiddenException(NO_RIGHTS_TO_MANAGE_SPACES);
        }
        switch (spaceName.getName()) {
            case SpaceName.PRIVATE_SPACE:
            case SpaceName.REVIEW_SPACE:
                throw new InvalidRequestException();
            default:
                break;
        }
    }
}
