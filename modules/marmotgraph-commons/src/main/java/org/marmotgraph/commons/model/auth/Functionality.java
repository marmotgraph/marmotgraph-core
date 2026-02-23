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

package org.marmotgraph.commons.model.auth;

import lombok.Getter;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Event;
import org.marmotgraph.commons.constants.EBRAINSVocabulary;

import java.util.Arrays;
import java.util.List;

public enum Functionality {

    //The minimal read permission allows to receive the label as well as type and space information. This is e.g. useful whenever users are required to link to instances of other spaces, which they are not allowed to see everything.
    MINIMAL_READ(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),
    MINIMAL_READ_RELEASED(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),

    //Space mgmt
    MANAGE_SPACE(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.SPACES, null, Event.Type.INSERT, EBRAINSVocabulary.META_SPACEDEFINITION_TYPE),
    RERUN_EVENTS_FOR_SPACE(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.SPACES, null, null, null),

    //Invitations
    INVITE_FOR_REVIEW(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    INVITE_FOR_SUGGESTION(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    LIST_INVITATIONS(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.INSTANCE, null, null, null),
    UPDATE_INVITATIONS(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE,  DataStage.IN_PROGRESS, null, null),

    //Instances
    READ_RELEASED(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.RELEASED, null, null),
    READ(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    RELEASE_STATUS(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    SUGGEST(PermissionForFunctionality.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    WRITE(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    CREATE(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    RELEASE(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    UNRELEASE(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.RELEASED, null, null),
    DELETE(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),

    //Users
    LIST_USERS(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.USERS, DataStage.NATIVE, null, null),
    LIST_USERS_LIMITED(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.USERS, DataStage.NATIVE, null, null),

    //Types
    DEFINE_TYPES_AND_PROPERTIES(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.TYPES, null, null, null),

    // Client mgmt
    READ_CLIENT(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),
    READ_CLIENT_PERMISSION(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),
    CREATE_CLIENT_PERMISSION(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),
    DELETE_CLIENT_PERMISSION(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),

    //Permission management
    READ_PERMISSION(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),
    CREATE_PERMISSION(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),
    DELETE_PERMISSION(PermissionForFunctionality.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),

    DEFINE_TERMS_OF_USE(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.ADMIN, null, null, null),
    DEFINE_PUBLIC_SPACE(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.ADMIN, null, null, null),
    DEFINE_SCOPE_RELEVANT_SPACE(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.ADMIN, null, null, null),

    CHECK_HEALTH_STATUS(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.ADMIN, null, null, null),

    //Cache mgmt
    CACHE_FLUSH(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.ADMIN, null, null, null),

    //Tenant mgmt
    TENANT_MANAGEMENT(PermissionForFunctionality.Level.GLOBAL_ONLY, FunctionalityGroup.ADMIN, null, null, null);

    @Getter
    private final List<PermissionForFunctionality.Level> allowedPermissionLevels;
    @Getter
    private final FunctionalityGroup functionalityGroup;
    @Getter
    private final DataStage stage;
    private final Event.Type type;
    private final String semantic;

    Functionality(List<PermissionForFunctionality.Level> allowedPermissionLevels, FunctionalityGroup functionalityGroup, DataStage stage, Event.Type type, String semantic) {
        this.allowedPermissionLevels = allowedPermissionLevels;
        this.functionalityGroup = functionalityGroup;
        this.stage = stage;
        this.type = type;
        this.semantic = semantic;
    }

    public static Functionality withSemanticsForOperation(List<String> semantics, Event.Type type, Functionality fallback) {
        return Arrays.stream(values()).filter(v -> v.type == type && v.semantic != null && semantics.contains(v.semantic)).findFirst().orElse(fallback);
    }

    public enum FunctionalityGroup {
        INSTANCE, CLIENT, PERMISSIONS, SPACES, USERS, TYPES, ADMIN
    }
}
