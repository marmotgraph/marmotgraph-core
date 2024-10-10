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

import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.config.openApiGroups.Tenants;
import org.marmotgraph.commons.model.tenant.ColorScheme;
import org.marmotgraph.commons.model.tenant.TenantDefinition;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping(Version.V3 +"/tenants")
//TODO move logic into controller and add to v3beta, implement the logic for dynamically storing the data
public class TenantsV3 {

    @Tenants
    @PutMapping("{name}")
    public void createTenant(@PathVariable String name, @RequestBody TenantDefinition tenantDefinition){
        if(name.equals("default")){
            throw new IllegalArgumentException("You can not update the \"default\" tenant");
        }
    }


    @Tenants
    @GetMapping("{name}")
    public TenantDefinition getTenant(@PathVariable String name){
        if(name.equals("default")){
            return TenantDefinition.defaultDefinition;
        }
        else{
            //TODO read from DB
            return null;
        }
    }


    @Tenants
    @GetMapping
    public List<String> listTenants(){
        //Read from DB
        return Collections.singletonList("default");
    }


    @Tenants
    @PutMapping("{name}/theme/colors")
    public void setColorScheme(@PathVariable String name, @RequestBody ColorScheme colorScheme){
        //Write to DB
    }

    @Tenants
    @PutMapping("{name}/theme/css")
    public void setCustomCSS(@PathVariable String name, @RequestBody String css){
        //Write to DB
    }


    @Tenants
    @GetMapping("{name}/theme/css")
    public String getCSS(@PathVariable String name){
        String css;
        ColorScheme colorScheme;
        String customCSS;
        if(name.equals("default")){
            //TODO add colorMap
            css = """
                    @import url(http://fonts.googleapis.com/css?family=Roboto:400,100,100italic,300,300italic,400italic,500,500italic,700,700italic,900italic,900);

                    html, body, html * {
                      font-family: 'Roboto', sans-serif;
                    }
                    """;
            colorScheme = ColorScheme.DEFAULT;
            customCSS = null;
        }
        else{
            //Load from DB
            css = null;
            colorScheme = null;
            customCSS = null;
        }
        if(css != null){
            if(colorScheme != null ) {
                css = css + "\n\n" + colorScheme.toCSS();
            }
            if(customCSS != null) {
                css = css + "\n\n" + customCSS;
            }
        }
        return css;
    }




    @Tenants
    @GetMapping("{name}/theme/favicon")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getFavicon(@PathVariable String name, @RequestParam(required = false) boolean darkMode){
        if(name.equals("default")){
            InputStream in = getClass().getResourceAsStream("/defaultTheme/images/favicon.ico");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/x-icon"))
                    .body(new InputStreamResource(in));
        }
        return null;
    }


    @Tenants
    @PutMapping("{name}/theme/favicon")
    @ResponseBody
    public ResponseEntity<InputStreamResource> setFavicon(@PathVariable String name, @RequestParam("file") MultipartFile file, @RequestParam(value = "darkMode", required = false) boolean darkMode){
        if(name.equals("default")){
            throw new IllegalArgumentException("You are not allowed to update the \"default\" tenant");
        }
        return null;
    }


    @Tenants
    @GetMapping("{name}/theme/background")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getBackgroundImage(@PathVariable String name, @RequestParam(required = false) boolean darkMode){
        if(name.equals("default")){
            InputStream in = getClass().getResourceAsStream("/defaultTheme/images/background.jpg");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/jpg"))
                    .body(new InputStreamResource(in));
        }
        return null;
    }


    @Tenants
    @PutMapping("{name}/theme/background")
    @ResponseBody
    public ResponseEntity<InputStreamResource> setBackgroundImage(@PathVariable String name, @RequestParam("file") MultipartFile file, @RequestParam(value = "darkMode", required = false) boolean darkMode){
        if(name.equals("default")){
            throw new IllegalArgumentException("You are not allowed to update the \"default\" tenant");
        }
        return null;
    }


    @Tenants
    @GetMapping("{name}/theme/logo")
    @ResponseBody
    public ResponseEntity<InputStreamResource> getLogo(@PathVariable String name, @RequestParam(required = false) boolean darkMode){
        if(name.equals("default")){
            InputStream in = getClass().getResourceAsStream("/defaultTheme/images/logo.svg");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/svg+xml"))
                    .body(new InputStreamResource(in));
        }
        return null;
    }


    @Tenants
    @PutMapping("{name}/theme/logo")
    @ResponseBody
    public ResponseEntity<InputStreamResource> setLogo(@PathVariable String name, @RequestParam("file") MultipartFile file, @RequestParam(value = "darkMode", required = false) boolean darkMode){
        if(name.equals("default")){
            throw new IllegalArgumentException("You are not allowed to update the \"default\" tenant");
        }
        return null;
    }


}
