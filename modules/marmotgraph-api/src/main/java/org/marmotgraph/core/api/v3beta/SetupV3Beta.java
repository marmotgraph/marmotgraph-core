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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.AllArgsConstructor;
import org.marmotgraph.auth.models.roles.RoleMapping;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.model.ResultWithExecutionDetails;
import org.marmotgraph.commons.model.TermsOfUse;
import org.marmotgraph.core.api.NoAuthentication;
import org.marmotgraph.core.api.Version;
import org.marmotgraph.core.api.config.openApi.Admin;
import org.marmotgraph.core.api.config.openApi.Simple;
import org.marmotgraph.core.api.v3.SetupV3;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping(Version.V3_BETA +"/setup")
public class SetupV3Beta {
    private final SetupV3 setupV3;

    @Operation(hidden = true)
    @PutMapping("/termsOfUse")
    @Deprecated(forRemoval = true)
    @Admin
    public void registerTermsOfUse(@RequestBody TermsOfUse termsOfUse){
        //We no longer support the registration of the terms of use - to ensure compatibility, we just don't do anything when called
    }

    @PatchMapping("/permissions/{role}")
    @Admin
    public JsonLdDoc updateClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space, @RequestBody Map<String, Object> claimPattern, @RequestParam("remove") boolean removeClaim) {
        return setupV3.updateClaimForRole(role, space, claimPattern, removeClaim);
    }

    @GetMapping("/permissions/{role}")
    @Admin
    public JsonLdDoc getClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space) {
        return setupV3.getClaimForRole(role, space);
    }

    @GetMapping("/permissions")
    @Admin
    public List<JsonLdDoc> getAllRoleDefinitions() {
        return setupV3.getAllRoleDefinitions();
    }

    @Operation(summary = "Get the endpoint of the configured openid configuration")
    @GetMapping(value = "/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @NoAuthentication
    @Simple
    public ResultWithExecutionDetails<JsonLdDoc> getOpenIdConfigUrl() {
        return setupV3.getOpenIdConfigUrl();
    }

}
