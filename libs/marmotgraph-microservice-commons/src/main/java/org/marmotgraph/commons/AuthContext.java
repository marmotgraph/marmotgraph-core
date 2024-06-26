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

package org.marmotgraph.commons;

import org.marmotgraph.commons.api.Authentication;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.marmotgraph.commons.models.UserWithRoles;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {


    private final AuthTokenContext authTokenContext;
    private final Authentication.Client authentication;


    public AuthContext(AuthTokenContext authTokenContext, Authentication.Client authentication) {
        this.authTokenContext = authTokenContext;
        this.authentication = authentication;
    }

    public UserWithRoles getUserWithRoles() {
        return authentication.getRoles(true);
    }

    public UserWithRoles getUserWithRolesWithoutTermsCheck() {
        return authentication.getRoles(false);
    }

    public Space getClientSpace(){
        return getUserWithRoles()!=null && getUserWithRoles().getClientId()!=null ? new Space(new SpaceName(getUserWithRoles().getClientId()),  false, true, false) : null;
    }

    public String getUserId(){
        UserWithRoles userWithRoles = getUserWithRoles();
        return userWithRoles == null || userWithRoles.getUser() ==null ? null : userWithRoles.getUser().getNativeId();
    }

    public SpaceName resolveSpaceName(String spaceName){
        SpaceName space = SpaceName.fromString(spaceName);
        if(space!=null){
            return space.getName().equals(SpaceName.PRIVATE_SPACE) ? getUserWithRoles().getPrivateSpace() : space;
        }
        return null;
    }

    public AuthTokens getAuthTokens(){
        return this.authTokenContext.getAuthTokens();
    }

}
