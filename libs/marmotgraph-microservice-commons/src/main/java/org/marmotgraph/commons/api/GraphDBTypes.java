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

package org.marmotgraph.commons.api;

import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.external.types.TypeInformation;

import java.util.List;
import java.util.Map;

public interface GraphDBTypes {

    interface Client extends GraphDBTypes {}

    Paginated<TypeInformation> listTypes(DataStage stage, String space, boolean withProperties, boolean withIncomingLinks, PaginationParam paginationParam);

    Map<String, Result<TypeInformation>> getTypesByName(List<String> types, DataStage stage, String space, boolean withProperties, boolean withIncomingLinks);

    DynamicJson getSpecifyType(String type, boolean global);

    void specifyType(JsonLdId typeName, NormalizedJsonLd normalizedJsonLd, boolean global);

    void removeTypeSpecification(JsonLdId typeName, boolean global);

    DynamicJson getSpecifyProperty(String property, boolean global);

    void specifyProperty(JsonLdId propertyName, NormalizedJsonLd normalizedJsonLd, boolean global);

    void removePropertySpecification(JsonLdId propertyName, boolean global);

    boolean checkPropertyInType(String typeName, String propertyName, boolean global);

    void addOrUpdatePropertyToType(String typeName, String propertyName, NormalizedJsonLd payload, boolean global);

    void removePropertyFromType(String typeName, String propertyName, boolean global);

}
