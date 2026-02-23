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
import org.marmotgraph.commons.model.auth.Functionality;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A composite containing user information together with the assigned roles in the context of a client (originating from the authentication system)
 */
public class UserWithRoles {
    @Getter
    private User user;
    private List<String> clientRoles;
    private List<String> userRoles;
    @Getter
    private List<UUID> invitations;
    @Getter
    private String clientId;
    @Getter
    private List<FunctionalityInstance> permissions;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    // For serialization
    @SuppressWarnings("unused")
    private UserWithRoles() {
    }

    public UserWithRoles(User user, List<String> userRoles, List<String> clientRoles, String clientId){
        this(user, userRoles, clientRoles, Collections.emptyList(), clientId);
    }

    public UserWithRoles(User user, List<String> userRoles, List<String> clientRoles, List<UUID> invitations, String clientId) {
        this.user = user;
        this.userRoles = userRoles == null ? null : Collections.unmodifiableList(userRoles);
        this.clientRoles = clientRoles == null ? null : Collections.unmodifiableList(clientRoles);
        this.clientId = clientId;
        this.invitations = invitations == null ? null : Collections.unmodifiableList(invitations);
        this.permissions = calculatePermissions();
    }

    private List<FunctionalityInstance> calculatePermissions(){
        //Invitation permissions are added after permission evaluation (of global and space)
        final List<FunctionalityInstance> functionalityInstances = evaluatePermissions(userRoles, clientRoles);
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
     * Evaluates the roles of a user in the context of a client by combining the roles of both
     *
     * @return the list of permissions applicable for the user using this client.
     */
    List<FunctionalityInstance> evaluatePermissions(List<String> userRoleNames, List<String> clientRoleNames) {
        if (userRoleNames == null) {
            //We're lacking of authentication information -> we default to "no permissions"
            return Collections.emptyList();
        }
        List<FunctionalityInstance> userFunctionalities = reduceFunctionalities(userRoleNames.stream().map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::functionality)));
        Set<FunctionalityInstance> result = new HashSet<>();
        if(clientRoleNames != null) {
            Map<Functionality, List<FunctionalityInstance>> clientFunctionalities = clientRoleNames.stream().map(RoleMapping::fromRole).flatMap(Collection::stream).distinct().collect(Collectors.groupingBy(FunctionalityInstance::functionality));
            //Filter the user roles by the client permissions (only those user permissions are guaranteed which are also allowed by the client)
            for (FunctionalityInstance userRole : userFunctionalities) {
                Functionality functionality = userRole.functionality();
                if (clientFunctionalities.containsKey(functionality)) {
                    for (FunctionalityInstance clientFunctionality : clientFunctionalities.get(functionality)) {
                        FunctionalityInstance global = null;
                        FunctionalityInstance space = null;
                        FunctionalityInstance instance = null;
                        if (clientFunctionality.space() == null && clientFunctionality.id() == null) {
                            //The client provides this functionality on a global permission layer, so the user role can be accepted
                            global = userRole;
                        }
                        if (clientFunctionality.space() != null && clientFunctionality.id() == null) {
                            //This is a space-limited functionality for this client...
                            if (userRole.space() == null && userRole.getInstanceId() == null) {
                                // ... the user has a global permission, so we restrict it to the client space
                                space = clientFunctionality;
                            }
                            if (userRole.space() != null && userRole.space().equals(clientFunctionality.space())) {
                                //... the client has permission for the space so we can provide access to the user role
                                // (regardless if it is a space or instance permission since both are valid)
                                space = userRole;
                            }
                        }
                        if (clientFunctionality.space() != null && clientFunctionality.getInstanceId() != null) {
                            //This is an instance-limited functionality for this client...
                            if (userRole.space() == null && userRole.getInstanceId() == null) {
                                // ... the user has a global permission, so we restrict it to the instance of the client
                                instance = clientFunctionality;
                            }
                            if (userRole.space() != null && userRole.getInstanceId() == null && userRole.space().equals(clientFunctionality.space())) {
                                //... the user has a permission for the space so we restrict it to the instance of the client
                                instance = clientFunctionality;
                            }
                            if (userRole.space() != null && userRole.getInstanceId() != null && userRole.space().equals(clientFunctionality.space()) && userRole.getInstanceId().equals(clientFunctionality.getInstanceId())) {
                                //... the user has a permission for the same instance
                                instance = userRole;
                            }
                        }
                        if (global != null) {
                            result.add(global);
                        } else if (space != null) {
                            result.add(space);
                        } else if (instance != null) {
                            result.add(instance);
                        }
                    }
                }
            }
        }
        else{
           return userFunctionalities;
        }
        if(logger.isTraceEnabled()) {
            logger.trace(String.format("Available roles for user %s in client %s: %s", user != null ? user.getUserName() : "anonymous", clientId, String.join(", ", result.stream().map(Object::toString).collect(Collectors.toSet()))));
        }
        return new ArrayList<>(result);
    }

    public boolean hasInvitations(){
        return this.invitations!=null && !this.invitations.isEmpty();
    }

    public final static UserWithRoles INTERNAL_ADMIN = createInternalAdminUser();

    private static UserWithRoles createInternalAdminUser(){
        User user = new User("marmotGraphInternalAdmin", "MarmotGraph Internal admin user", "support@marmotgraph.org", "MarmotGraph Internal", "Admin", "marmotGraphInternalAdmin");
        return new UserWithRoles(user, Collections.singletonList(RoleMapping.ADMIN.toRole(null).name()), null, null);
    }


}
