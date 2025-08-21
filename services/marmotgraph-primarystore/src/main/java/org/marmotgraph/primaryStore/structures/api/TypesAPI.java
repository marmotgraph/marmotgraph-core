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

import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.commons.api.primaryStore.Types;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.external.types.TypeInformation;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Profile("neo4j")
@Component
public class TypesAPI implements Types.Client {

    @Override
    public Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties,
                                                boolean withIncomingLinks, PaginationParam paginationParam) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, Result<TypeInformation>> getTypesByName(List<String> types, DataStage stage, String space,
                                                               boolean withProperties, boolean withIncomingLinks) {
        throw new NotImplementedException();
    }

    @Override
    public DynamicJson getSpecifyType(String type, boolean global) {
        throw new NotImplementedException();
    }

    @Override
    public void specifyType(JsonLdId typeName, NormalizedJsonLd normalizedJsonLd, boolean global) {
        throw new NotImplementedException();
    }

    @Override
    public void removeTypeSpecification(JsonLdId typeName, boolean global) {
        throw new NotImplementedException();
    }


    @Override
    public DynamicJson getSpecifyProperty(String propertyName, boolean global) {
        throw new NotImplementedException();
    }

    @Override
    public void specifyProperty(JsonLdId propertyName, NormalizedJsonLd normalizedJsonLd, boolean global) {
        throw new NotImplementedException();
    }

    @Override
    public void removePropertySpecification(JsonLdId propertyName, boolean global) {
        throw new NotImplementedException();
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
