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

package org.marmotgraph.auth.models;

import lombok.Getter;
import org.marmotgraph.auth.models.roles.RoleMapping;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.User;
import org.marmotgraph.commons.model.auth.Functionality;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A composite containing user information together with the assigned roles in the context of a client (originating from the authentication system)
 */
public class UserWithRoles {
    @Getter
    private final User user;
    private final List<String> userRoles;
    @Getter
    private final List<UUID> invitations;
    @Getter
    private final List<FunctionalityInstance> permissions;

    public UserWithRoles(User user, List<String> userRoles){
        this(user, userRoles, Collections.emptyList());
    }

    public UserWithRoles(User user, List<String> userRoles, List<UUID> invitations) {
        this.user = user;
        this.userRoles = userRoles == null ? null : Collections.unmodifiableList(userRoles);
        this.invitations = invitations == null ? null : Collections.unmodifiableList(invitations);
        this.permissions = calculatePermissions();
    }

    private List<FunctionalityInstance> calculatePermissions(){
        //Invitation permissions are added after permission evaluation (of global and space)
        final List<FunctionalityInstance> functionalityInstances = evaluatePermissions(userRoles);
        if(!CollectionUtils.isEmpty(invitations)){
            return Stream.concat(functionalityInstances.stream(),
                    invitations.stream().map(i -> new FunctionalityInstance(Functionality.READ, null, i)))
                    .distinct().toList();
        }
        else {
            return functionalityInstances;
        }
    }

    public SpaceName getPrivateSpace(){
        return new SpaceName("private-"+user.getNativeId());
    }

    private List<FunctionalityInstance> reduceFunctionalities(Map<Functionality, List<FunctionalityInstance>> functionalityMap){
        List<FunctionalityInstance> reducedList = new ArrayList<>();
        for (Map.Entry<Functionality, List<FunctionalityInstance>> entry : functionalityMap.entrySet()) {
            Optional<FunctionalityInstance> globalFunctionality = entry.getValue().stream().filter(i -> i.id() == null && i.space() == null).findAny();
            if(globalFunctionality.isPresent()){
                //We have a global permission -> we don't need any other
                reducedList.add(globalFunctionality.get());
            }
            else{
                Map<SpaceName, List<FunctionalityInstance>> functionalityBySpace = entry.getValue().stream().collect(Collectors.groupingBy(FunctionalityInstance::space));
                functionalityBySpace.forEach((s, spaceInstances) -> {
                    Optional<FunctionalityInstance> spacePermission = spaceInstances.stream().filter(i -> i.id() == null).findAny();
                    if(spacePermission.isPresent()){
                        //We have a space permission for this functionality -> instance level permissions are not needed
                        reducedList.add(spacePermission.get());
                    }
                    else{
                        //We neither have a global nor a space permission -> we need all instance permissions in the list
                        reducedList.addAll(spaceInstances);
                    }
                });
            }
        }
        return reducedList;
    }

    /**
     * Evaluates the roles of a user
     * @return the list of permissions applicable for the user
     */
    List<FunctionalityInstance> evaluatePermissions(List<String> userRoleNames) {
        if (userRoleNames == null) {
            //We're lacking of authentication information -> we default to "no permissions"
            return Collections.emptyList();
        }
        return reduceFunctionalities(userRoleNames.stream().map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::functionality)));
    }

    public boolean hasInvitations(){
        return this.invitations!=null && !this.invitations.isEmpty();
    }

}
