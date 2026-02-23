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

package org.marmotgraph.core.controller;

import lombok.AllArgsConstructor;
import org.marmotgraph.tenants.api.TenantsAPI;
import org.marmotgraph.tenants.model.ColorScheme;
import org.marmotgraph.tenants.model.Font;
import org.marmotgraph.tenants.model.TenantDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AllArgsConstructor
@Component
public class CoreTenantsController {

    private final TenantsAPI client;

    public void createTenant(String name, TenantDefinition tenantDefinition) {
        this.client.createTenant(name, tenantDefinition);
    }

    public TenantDefinition getTenant(String name) {
        return this.client.getTenant(name);
    }

    public List<String> listTenants() {
        return this.client.listTenants();
    }

    public void setFont(String name, Font font) {
        this.client.setFont(name, font);
    }
    public void setColorScheme(String name, ColorScheme colorScheme) {
        this.client.setColorScheme(name, colorScheme);
    }

    public void setCustomCSS(String name, String css) {
        this.client.setCustomCSS(name, css);
    }

    public String getCSS(String name) {
        return this.client.getCSS(name);
    }

    public ResponseEntity<Resource> getFavicon(String name) {
        TenantsAPI.ImageResult favicon = this.client.getFavicon(name);
        if(favicon != null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(favicon.mediaType())).body(new ByteArrayResource(favicon.image()));
        }
        return ResponseEntity.notFound().build();
    }

    public void setFavicon(String name, MultipartFile file) {
        this.client.setFavicon(name, file);
    }

    public ResponseEntity<Resource> getBackgroundImage(String name, boolean darkMode) {
        TenantsAPI.ImageResult backgroundImage = this.client.getBackgroundImage(name, darkMode);
        if(backgroundImage!=null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(backgroundImage.mediaType())).body(new ByteArrayResource(backgroundImage.image()));
        }
        return ResponseEntity.notFound().build();
    }

    public void setBackgroundImage(String name, MultipartFile file, boolean darkMode) {
        this.client.setBackgroundImage(name, file, darkMode);
    }

    public ResponseEntity<Resource> getLogo(String name, boolean darkMode) {
        TenantsAPI.ImageResult logo = this.client.getLogo(name, darkMode);
        if(logo!=null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.mediaType())).body(new ByteArrayResource(logo.image()));
        }
        return ResponseEntity.notFound().build();
    }

    public void setLogo(String name, MultipartFile file, boolean darkMode) {
        this.client.setLogo(name, file, darkMode);
    }
}
