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

package org.marmotgraph.core.api.v3beta;

import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.api.GraphDBTypes;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.config.openApiGroups.Advanced;
import org.marmotgraph.commons.config.openApiGroups.Simple;
import org.marmotgraph.commons.exception.InstanceNotFoundException;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesType;
import org.marmotgraph.commons.markers.WritesData;
import org.marmotgraph.commons.model.PaginatedResult;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.types.TypeInformation;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.core.model.ExposedStage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * The types API allows to get information about the available types of instances including statistical values
 */
@RestController
@RequestMapping(Version.V3_BETA)
public class TypesV3Beta {

    private final GraphDBTypes.Client graphDBTypes;

    public TypesV3Beta(GraphDBTypes.Client graphDBTypes) {
        this.graphDBTypes = graphDBTypes;
    }

    @Operation(summary = "Returns the types available - either with property information or without")
    @GetMapping("/types")
    @ExposesType
    @Simple
    public PaginatedResult<TypeInformation> listTypes(@RequestParam("stage") ExposedStage stage, @RequestParam(value = "space", required = false) @Parameter(description = "The space by which the types should be filtered or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "withIncomingLinks", defaultValue = "false") boolean withIncomingLinks, @ParameterObject PaginationParam paginationParam) {
        return PaginatedResult.ok(graphDBTypes.listTypes(stage.getStage(), space, withProperties, withIncomingLinks, paginationParam));
    }

    @Operation(summary = "Returns the types according to the list of names - either with property information or without")
    @PostMapping("/typesByName")
    @ExposesType
    @Advanced
    public Result<Map<String, Result<TypeInformation>>> getTypesByName(@RequestBody List<String> typeNames, @RequestParam("stage") ExposedStage stage, @RequestParam(value = "withProperties", defaultValue = "false") boolean withProperties, @RequestParam(value = "withIncomingLinks", defaultValue = "false") boolean withIncomingLinks, @RequestParam(value = "space", required = false) @Parameter(description = "The space by which the types should be filtered or \"" + SpaceName.PRIVATE_SPACE + "\" for your private space.") String space) {
        return Result.ok(graphDBTypes.getTypesByName(typeNames, stage.getStage(), space, withProperties, withIncomingLinks));
    }

    @Operation(summary = "Get type specification")
    @GetMapping("/types/specification")
    @ExposesType
    @Admin
    public DynamicJson getTypeSpecification(
            @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)")
            @RequestParam(value = "global", required = false) boolean global,
            @RequestParam("type") String type
    ) {
        DynamicJson informations = graphDBTypes.getSpecifyType(type, global);

        if (informations != null) {
            return informations;
        }
        throw new InstanceNotFoundException(String.format("Type %s was not found", type));
    }

    @Operation(summary = "Specify a type")
    //In theory, this could also go into /types only. But since Swagger doesn't allow the discrimination of groups with the same path (there is already the same path registered as GET for simple), we want to discriminate it properly
    @PutMapping("/types/specification")
    @WritesData
    @Admin
    public void createTypeDefinition(@RequestBody NormalizedJsonLd payload, @Parameter(description = "By default, the specification is only valid for the current client. If this flag is set to true (and the client/user combination has the permission), the specification is applied for all clients (unless they have defined something by themselves)") @RequestParam(value = "global", required = false) boolean global, @RequestParam("type") String type) {
        JsonLdId typeFromPayload = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        if(typeFromPayload!=null){
            throw new IllegalArgumentException("You are not supposed to provide a @type in the payload of the type specifications to avoid ambiguity");
        }
        graphDBTypes.specifyType(new JsonLdId(decodedType), payload, global);
    }


    @Operation(summary = "Remove a type definition", description = "Allows to deprecate a type specification")
    @DeleteMapping("/types/specification")
    @WritesData
    @Admin
    public void removeTypeDefinition(@RequestParam(value = "type", required = false) String type,  @RequestParam(value = "global", required = false) boolean global) {
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        graphDBTypes.removeTypeSpecification(new JsonLdId(decodedType), global);
    }
}
