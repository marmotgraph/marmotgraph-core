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

package eu.ebrains.kg.core.serviceCall;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.ServiceCall;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GraphDB4TypesSvc {
    private final ServiceCall serviceCall;

    private final AuthContext authContext;

    private final String BASE_URL = "http://kg-graphdb-sync";

    public GraphDB4TypesSvc(ServiceCall serviceCall, AuthContext authContext) {
        this.serviceCall = serviceCall;
        this.authContext = authContext;
    }

    public JsonLdDoc get(DataStage stage, InstanceId instanceId) {
        return serviceCall.get(String.format(BASE_URL + "/%s/instances/%s", stage.name(), instanceId.serialize()), authContext.getAuthTokens(), JsonLdDoc.class);
    }

    public Map<String, Result<NormalizedJsonLd>> getTypesByNameList(List<String> types, DataStage stage, Space space, boolean withProperties) {
        return serviceCall.post(BASE_URL + String.format("/%s/%s?space=%s", stage.name(), withProperties ? "typesWithPropertiesByName": "typesByName",  space != null ? space.getName() : ""), types, authContext.getAuthTokens(), StringPayloadMapping.class);
    }

    public PaginatedResultOfDocuments getTypes(DataStage stage, Space space, boolean withProperties, PaginationParam paginationParam) {
        String relativeUrl = String.format("/%s/%s?space=%s", stage.name(),  withProperties ? "typesWithProperties" : "types", space != null ? space.getName() : "");
        if (paginationParam != null && paginationParam.getSize() != null) {
            relativeUrl = String.format("%s&from=%d&size=%d", relativeUrl, paginationParam.getFrom(), paginationParam.getSize());
        }
        return serviceCall.get(BASE_URL + relativeUrl, authContext.getAuthTokens(), PaginatedResultOfDocuments.class);
    }

}
