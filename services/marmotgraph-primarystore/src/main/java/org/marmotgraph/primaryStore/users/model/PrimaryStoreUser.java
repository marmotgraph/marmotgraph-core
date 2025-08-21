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

package org.marmotgraph.primaryStore.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.model.ReducedUserInformation;
import org.marmotgraph.commons.model.User;

import java.util.Collections;
import java.util.UUID;

@Entity
@Table(name="users")
@Getter @Setter
public class PrimaryStoreUser {

    @Id
    private UUID uuid;
    private String alternateName;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    @Column(unique=true)
    private String nativeId;

    public User toUser(){
        return new User(getAlternateName(), getName(), getEmail(), getGivenName(), getFamilyName(), getNativeId());
    }

    public ReducedUserInformation toUserWithLimitedInformation(){
        // We don't expose the e-mail address in this context.
        return new ReducedUserInformation(getName(), getAlternateName(), Collections.singletonList(getNativeId()), uuid.toString());
    }

    public static PrimaryStoreUser fromUser(User user, UUID uuid){
        if(user==null){
            return null;
        }
        PrimaryStoreUser result = new PrimaryStoreUser();
        result.setUuid(uuid);
        result.setAlternateName(user.getUserName());
        result.setName(user.getDisplayName());
        result.setEmail(user.getEmail());
        result.setGivenName(user.getGivenName());
        result.setFamilyName(user.getFamilyName());
        result.setNativeId(user.getNativeId());
        return result;
    }

}
