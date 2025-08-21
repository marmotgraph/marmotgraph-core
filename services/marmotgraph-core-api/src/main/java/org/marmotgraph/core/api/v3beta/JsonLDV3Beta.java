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

package org.marmotgraph.core.api.v3beta;

import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.api.jsonld.JsonLd;
import org.marmotgraph.commons.config.openApiGroups.Simple;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesInputWithoutEnrichedSensitiveData;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The spaces API provides information about existing MarmotGraph spaces
 */
@RestController
@RequestMapping(Version.V3_BETA + "/jsonld")
public class JsonLDV3Beta {
    private final JsonLd.Client jsonLd;

    public JsonLDV3Beta(JsonLd.Client jsonLd) {
        this.jsonLd = jsonLd;
    }

    @Operation(summary = "Normalizes the passed payload according to the MarmotGraph conventions")
    @PostMapping("/normalizedPayload")
    @ExposesInputWithoutEnrichedSensitiveData
    @Simple
    public NormalizedJsonLd normalizePayload(@RequestBody JsonLdDoc payload) {
        return jsonLd.normalize(payload, true);
    }

}
