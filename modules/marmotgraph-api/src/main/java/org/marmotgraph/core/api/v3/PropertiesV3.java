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

package org.marmotgraph.core.api.v3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.constants.EBRAINSVocabulary;
import org.marmotgraph.commons.exceptions.InstanceNotFoundException;
import org.marmotgraph.commons.exceptions.NoContentException;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.external.types.PropertyInType;
import org.marmotgraph.core.api.Version;
import org.marmotgraph.core.api.config.openApi.Admin;
import org.marmotgraph.primaryStore.api.TypesAPI;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * The property API allows to add meta information about semantic properties either globally or by type for the requesting client.
 */
@RestController
@RequestMapping(Version.V3)
@AllArgsConstructor
public class PropertiesV3 {
    private final TypesAPI graphDBTypes;

    @Operation(summary = "Get a property specification")
    @GetMapping("/properties")
    @Admin
    public DynamicJson getPropertySpecification(@RequestParam(value = "property", required = false) String property) {
        DynamicJson propertySpecification = graphDBTypes.getPropertySpecification(property);
        if (propertySpecification != null) {
            return propertySpecification;
        }
        throw new InstanceNotFoundException(String.format("Property specification %s was not found", property));

    }

    @Operation(summary = "Upload a property specification")
    @PutMapping("/properties")
    @Admin
    public void specifyProperty(@RequestBody NormalizedJsonLd payload, @RequestParam("property") String property) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        graphDBTypes.specifyProperty(new JsonLdId(decodedProperty), payload);
    }

    @Operation(summary = "Deprecate a property specification")
    @DeleteMapping("/properties")
    @Admin
    public void deprecateProperty(@RequestParam("property") String property) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        graphDBTypes.removePropertySpecification(new JsonLdId(decodedProperty));
    }

    @Operation(summary = "Check type for a specific property")
    @GetMapping("/propertiesForType")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Relation between type and property", content = {@Content(mediaType = "application/json", schema = @Schema(implementation = PropertyInType.class))}),
            @ApiResponse(responseCode = "404", description = "Type not found", content = @Content),
            @ApiResponse(responseCode = "204", description = "No relation", content = @Content)})

    @Admin
    public PropertyInType getPropertyForType(
            @RequestParam("property") String property,
            @RequestParam("type") String type) {
        if (graphDBTypes.checkPropertyInType(type, property)) {
            return new PropertyInType(property, type);
        } else {
            throw new NoContentException("No Content");
        }
    }

    @Operation(summary = "Define a property specification")
    @PutMapping("/propertiesForType")
    @Admin
    public void definePropertyForType(@RequestBody NormalizedJsonLd payload, @RequestParam("property") String property, @RequestParam("type") String type) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        JsonLdId typeFromPayload = payload.getAs(EBRAINSVocabulary.META_TYPE, JsonLdId.class);
        if (typeFromPayload != null) {
            throw new IllegalArgumentException("You are not supposed to provide a @type in the payload of the type specifications to avoid ambiguity");
        }
        graphDBTypes.addOrUpdatePropertyToType(decodedType, decodedProperty, payload);
    }

    @Operation(summary = "Deprecate a property specification")
    @DeleteMapping("/propertiesForType")
    @Admin
    public void deprecatePropertyForType(@RequestParam("property") String property, @RequestParam("type") String type) {
        String decodedProperty = URLDecoder.decode(property, StandardCharsets.UTF_8);
        String decodedType = URLDecoder.decode(type, StandardCharsets.UTF_8);
        graphDBTypes.removePropertyFromType(decodedType, decodedProperty);
    }

}
