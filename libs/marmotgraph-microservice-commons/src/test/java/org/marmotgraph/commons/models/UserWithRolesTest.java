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

package org.marmotgraph.commons.models;

import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.User;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permission.FunctionalityInstance;
import org.marmotgraph.commons.permission.roles.RoleMapping;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserWithRolesTest {

    List<String> adminClientRoles = Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName());

    List<String> adminRole = Collections.singletonList(RoleMapping.ADMIN.toRole(null).getName());
    SpaceName space = new SpaceName("test");
    User user = new User("testUser", "Test", "test@test.xy", "Test", "User", null);

    private List<String> getUserRoles(RoleMapping role, SpaceName space){
        return Collections.singletonList(role.toRole(space).getName());
    }

    @Test
    void testEvaluatePermissionsFullUserAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(RoleMapping.ADMIN, null), adminClientRoles,  "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
    }

    @Test
    void testEvaluatePermissionsFullServiceAccountAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, adminRole, adminClientRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
    }

    @Test
    void testEvaluatePermissionsSpaceUserAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(RoleMapping.ADMIN, space), adminClientRoles,  "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, space, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.WRITE, space, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, space, null)));
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.CREATE, space, null)));

        //Ensure there is no global permissions...
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
    }

    @Test
    void testEvaluatePermissionsSpaceReviewUserAccess(){
        //Given
        UserWithRoles userWithRoles = new UserWithRoles(user, getUserRoles(RoleMapping.REVIEWER, space), adminClientRoles, "testClient");

        //when
        List<FunctionalityInstance> permissions = userWithRoles.getPermissions();

        //Then
        assertTrue(permissions.contains(new FunctionalityInstance(Functionality.READ, space, null)));

        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.WRITE, space, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, space, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.CREATE, space, null)));

        //Ensure there is no global permissions either...
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.READ, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.WRITE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.RELEASE, null, null)));
        assertFalse(permissions.contains(new FunctionalityInstance(Functionality.CREATE, null, null)));
    }

}