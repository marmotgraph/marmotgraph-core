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

package eu.ebrains.kg.graphdb.types.api;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.graphdb.types.controller.ArangoRepositoryTypes;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/graphdb/{stage}")
public class GraphDBTypesAPI {

    private final AuthContext authContext;

    private final ArangoRepositoryTypes repositoryTypes;

    public GraphDBTypesAPI(AuthContext authContext, ArangoRepositoryTypes repositoryTypes) {
        this.authContext = authContext;
        this.repositoryTypes = repositoryTypes;
    }

    @GetMapping("/types")
    public Paginated<NormalizedJsonLd> getTypes(@PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space, PaginationParam paginationParam) {
        return getTypes(stage, space, false, false, false, paginationParam);
    }

    private Paginated<NormalizedJsonLd> getTypes(DataStage stage, String space, boolean withProperties, boolean withIncomingLinks, boolean withCount, PaginationParam paginationParam) {
        if (space != null && !space.isEmpty()) {
            return repositoryTypes.getTypesForSpace(authContext.getUserWithRoles().getClientId(), stage, new SpaceName(space), withProperties, withIncomingLinks,  withCount, paginationParam);
        } else {
            return repositoryTypes.getAllTypes(authContext.getUserWithRoles().getClientId(), stage, withProperties, withIncomingLinks, withCount, paginationParam);
        }
    }

    @GetMapping("/typesWithProperties")
    public Paginated<NormalizedJsonLd> getTypesWithProperties(@PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space, @RequestParam(value = "withCounts", required = false, defaultValue = "true") boolean withCounts, @RequestParam(value = "withIncomingLinks", required = false, defaultValue = "true") boolean withIncomingLinks, PaginationParam paginationParam) {
        return getTypes(stage, space, true, withIncomingLinks, withCounts, paginationParam);
    }

    @PostMapping("/typesByName")
    public Map<String, Result<NormalizedJsonLd>> getTypesByName(@RequestBody List<String> types, @PathVariable("stage") DataStage stage, @RequestParam(value = "space", required = false) String space) {
        return getTypesByName(types, stage, space, false, false, false);
    }

    @PostMapping("/typesWithPropertiesByName")
    public Map<String, Result<NormalizedJsonLd>> getTypesWithPropertiesByName(@RequestBody List<String> types, @PathVariable("stage") DataStage stage, @RequestParam(value = "withCounts", required = false, defaultValue = "true") boolean withCounts, @RequestParam(value = "withIncomingLinks", required = false, defaultValue = "true") boolean withIncomingLinks, @RequestParam(value = "space", required = false) String space) {
        return getTypesByName(types, stage, space, true, withIncomingLinks, withCounts);
    }

    private Map<String, Result<NormalizedJsonLd>> getTypesByName(List<String> types, DataStage stage, String space, boolean withProperties, boolean withIncomingLinks, boolean withCounts) {
        List<Type> typeList = types.stream().map(Type::new).collect(Collectors.toList());
        List<NormalizedJsonLd> typeObjects;
        if (space != null && !space.isBlank()) {
            typeObjects = repositoryTypes.getTypesForSpace(authContext.getUserWithRoles().getClientId(), stage, new SpaceName(space), typeList, withProperties, withIncomingLinks, withCounts);
        } else {
            typeObjects = repositoryTypes.getTypes(authContext.getUserWithRoles().getClientId(), stage, typeList, withProperties, withIncomingLinks,  withCounts);
        }
        Map<String, Result<NormalizedJsonLd>> type2Map = typeObjects.stream().map(NormalizedJsonLd::removeAllInternalProperties).collect(Collectors.toMap(JsonLdDoc::primaryIdentifier, Result::ok));
        for (String type : types) {
            if(!type2Map.containsKey(type)){
                type2Map.put(type, Result.nok(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase()));
            }
        }
        return type2Map;
    }
}
