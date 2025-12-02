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

package org.marmotgraph.primaryStore.structures.api;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.api.primaryStore.Types;
import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.external.types.TypeInformation;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.primaryStore.structures.service.TypesService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class TypesAPI implements Types.Client {

    private static final String NO_RIGHTS_TO_DEFINE_TYPES = "You don't have the required rights to define types";

    private final TypesService typesService;
    private final Permissions permissions;
    private final AuthContext authContext;


    @Override
    public Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties,
                                                boolean withIncomingLinks, PaginationParam paginationParam) {
        return typesService.listTypes(stage, space, withProperties, withIncomingLinks, paginationParam);

    }

    @Override
    public Map<String, Result<TypeInformation>> getTypesByName(List<String> types, DataStage stage, String space,
                                                               boolean withProperties, boolean withIncomingLinks) {
        return typesService.getByName(types, stage, space, withProperties, withIncomingLinks);
    }

    private void canManageTypesAndPropertiesOrThrow(){
        if(!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.DEFINE_TYPES_AND_PROPERTIES)){
            throw new ForbiddenException(NO_RIGHTS_TO_DEFINE_TYPES);
        }
    }


    @Override
    public DynamicJson getSpecifyType(String type, boolean global) {
        canManageTypesAndPropertiesOrThrow();
        return typesService.getTypeSpecification(type, getClientId(global));
    }


    private String getClientId(boolean global) {
        String clientId = "";
        if (!global) {
            clientId = authContext.getUserWithRoles().getClientId();
            if (clientId == null) {
                throw new IllegalArgumentException("You need to be logged in with a client for non-global specifications");
            }
        }
        return clientId;
    }


    @Override
    public void specifyType(JsonLdId typeName, NormalizedJsonLd normalizedJsonLd, boolean global) {
        canManageTypesAndPropertiesOrThrow();
        typesService.specifyType(typeName, normalizedJsonLd, getClientId(global));
    }

    @Override
    public void removeTypeSpecification(JsonLdId typeName, boolean global) {
        canManageTypesAndPropertiesOrThrow();
        typesService.removeType(typeName, getClientId(global));
    }


    @Override
    public DynamicJson getPropertySpecification(String propertyName, boolean global) {
        canManageTypesAndPropertiesOrThrow();
        return typesService.getPropertySpecification(propertyName, getClientId(global));
    }

    @Override
    public void specifyProperty(JsonLdId propertyName, NormalizedJsonLd normalizedJsonLd, boolean global) {
        canManageTypesAndPropertiesOrThrow();
        typesService.specifyProperty(propertyName, normalizedJsonLd, getClientId(global));
    }

    @Override
    public void removePropertySpecification(JsonLdId propertyName, boolean global) {
        canManageTypesAndPropertiesOrThrow();
        typesService.removePropertySpecification(propertyName, getClientId(global));
    }

    @Override
    public boolean checkPropertyInType(String typeName, String propertyName, boolean global) {
        throw new NotImplementedException();
    }

    @Override
    public void addOrUpdatePropertyToType(String typeName, String propertyName, NormalizedJsonLd payload,
                                          boolean global) {
        throw new NotImplementedException();
    }

    @Override
    public void removePropertyFromType(String typeName, String propertyName, boolean global) {
        throw new NotImplementedException();
    }
}
