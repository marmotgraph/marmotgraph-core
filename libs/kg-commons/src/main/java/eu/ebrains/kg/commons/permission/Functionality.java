/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.commons.permission;

import eu.ebrains.kg.commons.model.DataStage;
import eu.ebrains.kg.commons.model.Event;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Functionality {

    READ_PERMISSION(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),
    CREATE_PERMISSION(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null ),
    DELETE_PERMISSION(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.PERMISSIONS, null, null, null),

    //Space mgmt
    READ_SPACE(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.SPACES, null, null, null),
    CREATE_SPACE(Permission.Level.GLOBAL_ONLY, FunctionalityGroup.SPACES, null, Event.Type.INSERT, EBRAINSVocabulary.META_SPACEDEFINITION_TYPE),
    DELETE_SPACE(Permission.Level.GLOBAL_ONLY, FunctionalityGroup.SPACES, null, Event.Type.DELETE, EBRAINSVocabulary.META_SPACEDEFINITION_TYPE),

    //Invitations
    INVITE_FOR_REVIEW(Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    INVITE_FOR_SUGGESTION(Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),

    //Instances
    READ_RELEASED(Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.RELEASED, null, null),
    READ(Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    SUGGEST(Permission.Level.ALL_LEVELS, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    WRITE(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    CREATE( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    RELEASE(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),
    UNRELEASE( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.RELEASED, null, null),
    DELETE( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.INSTANCE, DataStage.IN_PROGRESS, null, null),


    // Client mgmt
    READ_CLIENT(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),
    CONFIGURE_CLIENT(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, Event.Type.UPDATE, EBRAINSVocabulary.META_CLIENTCONFIGURATION_TYPE),
    CREATE_CLIENT(Permission.Level.GLOBAL_ONLY, FunctionalityGroup.CLIENT, null, Event.Type.INSERT, EBRAINSVocabulary.META_CLIENTCONFIGURATION_TYPE),
    DELETE_CLIENT( Permission.Level.GLOBAL_ONLY, FunctionalityGroup.CLIENT, null, Event.Type.DELETE, EBRAINSVocabulary.META_CLIENTCONFIGURATION_TYPE),
    READ_CLIENT_PERMISSION(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),
    CREATE_CLIENT_PERMISSION( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),
    DELETE_CLIENT_PERMISSION(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.CLIENT, null, null, null),

    //Query
    READ_QUERY( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null, null, null),
    CREATE_QUERY( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null, Event.Type.INSERT, KgQuery.getKgQueryType()),
    EXECUTE_QUERY( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null, null, null),
    EXECUTE_SYNC_QUERY( Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null, null, null),
    DELETE_QUERY(Permission.Level.GLOBAL_AND_SPACE, FunctionalityGroup.QUERY, null, Event.Type.DELETE, KgQuery.getKgQueryType());


    private final List<Permission.Level> allowedPermissionLevels;
    private final FunctionalityGroup functionalityGroup;
    private final DataStage stage;
    private final Event.Type type;
    private final String semantic;

    Functionality(List<Permission.Level> allowedPermissionLevels, FunctionalityGroup functionalityGroup, DataStage stage, Event.Type type, String semantic) {
        this.allowedPermissionLevels = allowedPermissionLevels;
        this.functionalityGroup = functionalityGroup;
        this.stage = stage;
        this.type = type;
        this.semantic = semantic;
    }

    public static Functionality withSemanticsForOperation(List<String> semantics, Event.Type type, Functionality fallback){
        return Arrays.stream(values()).filter(v -> v.type == type && v.semantic != null && semantics.contains(v.semantic)).findFirst().orElse(fallback);
    }

    public FunctionalityGroup getFunctionalityGroup(){
        return functionalityGroup;
    }

    public List<Permission.Level> getAllowedPermissionLevels() {
        return allowedPermissionLevels;
    }

    public static List<Functionality> getGlobalFunctionality() {
        return Arrays.stream(values()).filter(f -> f.getAllowedPermissionLevels().contains(Permission.Level.GLOBAL)).collect(Collectors.toList());
    }

    public DataStage getStage() {
        return stage;
    }

    public enum FunctionalityGroup{
        INSTANCE, CLIENT, QUERY, PERMISSIONS, SPACES
    }
}
