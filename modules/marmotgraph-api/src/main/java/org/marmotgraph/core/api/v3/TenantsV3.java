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

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.AllArgsConstructor;
import org.marmotgraph.core.api.NoAuthentication;
import org.marmotgraph.core.api.Version;
import org.marmotgraph.core.api.config.openApi.Tenants;
import org.marmotgraph.core.controller.CoreTenantsController;
import org.marmotgraph.tenants.model.ColorScheme;
import org.marmotgraph.tenants.model.Font;
import org.marmotgraph.tenants.model.TenantDefinition;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping(Version.V3 +"/tenants")
@AllArgsConstructor
public class TenantsV3 {

    private final CoreTenantsController controller;

    @Tenants
    @PutMapping("{name}")
    public void createTenant(@PathVariable String name, @RequestBody TenantDefinition tenantDefinition){
        this.controller.createTenant(name, tenantDefinition);
    }


    @Tenants
    @GetMapping("{name}")
    @SecurityRequirements
    @NoAuthentication
    public TenantDefinition getTenant(@PathVariable String name){
        return this.controller.getTenant(name);
    }


    @Tenants
    @GetMapping
    @SecurityRequirements
    @NoAuthentication
    public List<String> listTenants(){
        return this.controller.listTenants();
    }


    @Tenants
    @PutMapping("{name}/theme/font")
    public void setFont(@PathVariable String name, @RequestBody Font font){
        this.controller.setFont(name, font);
    }

    @Tenants
    @PutMapping("{name}/theme/colors")
    public void setColorScheme(@PathVariable String name, @RequestBody ColorScheme colorScheme){
        this.controller.setColorScheme(name, colorScheme);
    }

    @Tenants
    @PutMapping(value = "{name}/theme/customCss", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void setCustomCSS(@PathVariable String name, @RequestBody String css){
        this.controller.setCustomCSS(name, css);
    }


    @Tenants
    @GetMapping(value = "{name}/theme/css", produces = "text/css")
    @SecurityRequirements
    @NoAuthentication
    public String getCSS(@PathVariable String name){
       return this.controller.getCSS(name);
    }


    @Tenants
    @GetMapping("{name}/theme/favicon")
    @ResponseBody
    @SecurityRequirements
    @NoAuthentication
    public ResponseEntity<Resource> getFavicon(@PathVariable String name){
       return this.controller.getFavicon(name);
    }


    @Tenants
    @PutMapping(value = "{name}/theme/favicon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void setFavicon(@PathVariable String name, @RequestBody MultipartFile file){
        this.controller.setFavicon(name, file);
    }


    @Tenants
    @GetMapping("{name}/theme/background")
    @ResponseBody
    @SecurityRequirements
    @NoAuthentication
    public ResponseEntity<Resource> getBackgroundImage(@PathVariable String name, @RequestParam(defaultValue = "true") boolean darkMode){
        return this.controller.getBackgroundImage(name, darkMode);
    }


    @Tenants
    @PutMapping(value= "{name}/theme/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void setBackgroundImage(@PathVariable String name, @RequestBody MultipartFile file, @RequestParam(defaultValue = "true") boolean darkMode){
        this.controller.setBackgroundImage(name, file, darkMode);
    }


    @Tenants
    @GetMapping("{name}/theme/logo")
    @ResponseBody
    @SecurityRequirements
    @NoAuthentication
    public ResponseEntity<Resource> getLogo(@PathVariable String name, @RequestParam(defaultValue = "true") boolean darkMode){
       return this.controller.getLogo(name, darkMode);
    }


    @Tenants
    @PutMapping(value = "{name}/theme/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void setLogo(@PathVariable String name, @RequestBody MultipartFile file, @RequestParam(defaultValue = "true") boolean darkMode){
        this.controller.setLogo(name, file, darkMode);
    }


}
