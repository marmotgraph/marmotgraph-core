/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
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

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.api.Authentication;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.model.TermsOfUse;
import eu.ebrains.kg.commons.permission.roles.RoleMapping;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(Version.API+"/setup")
public class Setup {

    private final Authentication.Client authentication;

    public Setup(Authentication.Client authentication) {
        this.authentication = authentication;
    }

    @Operation(hidden = true)
    @PutMapping("/termsOfUse")
    @Admin
    @Deprecated(forRemoval = true)
    public void registerTermsOfUse(@RequestBody TermsOfUse termsOfUse){
        authentication.registerTermsOfUse(termsOfUse);
    }

    @PatchMapping("/permissions/{role}")
    @Admin
    public JsonLdDoc updateClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space, @RequestBody Map<?, ?> claimPattern, @RequestParam("remove") boolean removeClaim) {
        return authentication.updateClaimForRole(role, space, claimPattern, removeClaim);
    }

    @GetMapping("/permissions/{role}")
    @Admin
    public JsonLdDoc getClaimForRole(@PathVariable("role") RoleMapping role, @RequestParam(value = "space", required = false) String space) {
        return authentication.getClaimForRole(role, space);
    }

    @GetMapping("/permissions")
    @Admin
    public List<JsonLdDoc> getAllRoleDefinitions() {
        return authentication.getAllRoleDefinitions();
    }
}
