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

package org.marmotgraph.core.api.v3;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.config.openApiGroups.Tenants;
import org.marmotgraph.commons.model.tenant.ColorScheme;
import org.marmotgraph.commons.model.tenant.Font;
import org.marmotgraph.commons.model.tenant.TenantDefinition;
import org.marmotgraph.core.controller.TenantsController;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping(Version.V3 +"/tenants")
public class TenantsV3 {

    private final TenantsController controller;

    public TenantsV3(TenantsController controller) {
        this.controller = controller;
    }

    @Tenants
    @PutMapping("{name}")
    public void createTenant(@PathVariable String name, @RequestBody TenantDefinition tenantDefinition){
        this.controller.createTenant(name, tenantDefinition);
    }


    @Tenants
    @GetMapping("{name}")
    @SecurityRequirements
    public TenantDefinition getTenant(@PathVariable String name){
        return this.controller.getTenant(name);
    }


    @Tenants
    @GetMapping
    @SecurityRequirements
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
    @GetMapping("{name}/theme/css")
    @SecurityRequirements
    public String getCSS(@PathVariable String name){
       return this.controller.getCSS(name);
    }


    @Tenants
    @GetMapping("{name}/theme/favicon")
    @ResponseBody
    @SecurityRequirements
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
    public ResponseEntity<Resource> getBackgroundImage(@PathVariable String name, @RequestParam(required = false) boolean darkMode){
        return this.controller.getBackgroundImage(name, darkMode);
    }


    @Tenants
    @PutMapping(value= "{name}/theme/background", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void setBackgroundImage(@PathVariable String name, @RequestBody MultipartFile file, @RequestParam(value = "darkMode", required = false) boolean darkMode){
        this.controller.setBackgroundImage(name, file, darkMode);
    }


    @Tenants
    @GetMapping("{name}/theme/logo")
    @ResponseBody
    @SecurityRequirements
    public ResponseEntity<Resource> getLogo(@PathVariable String name, @RequestParam(required = false) boolean darkMode){
       return this.controller.getLogo(name, darkMode);
    }


    @Tenants
    @PutMapping(value = "{name}/theme/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void setLogo(@PathVariable String name, @RequestBody MultipartFile file, @RequestParam(value = "darkMode", required = false) boolean darkMode){
        this.controller.setLogo(name, file, darkMode);
    }


}
