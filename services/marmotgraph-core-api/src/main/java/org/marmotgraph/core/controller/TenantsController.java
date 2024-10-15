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

package org.marmotgraph.core.controller;

import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.api.Tenants;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.model.tenant.ColorScheme;
import org.marmotgraph.commons.model.tenant.Font;
import org.marmotgraph.commons.model.tenant.ImageResult;
import org.marmotgraph.commons.model.tenant.TenantDefinition;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class TenantsController {
    private final Tenants.Client client;
    private final AuthContext authContext;
    private final Permissions permissions;

    public TenantsController(Tenants.Client client, AuthContext authContext, Permissions permissions) {
        this.client = client;
        this.authContext = authContext;
        this.permissions = permissions;
    }

    public void createTenant(String name, TenantDefinition tenantDefinition) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to create tenants.");
        }
        this.client.createTenant(name, tenantDefinition);
    }

    public TenantDefinition getTenant(String name) {
        return this.client.getTenant(name);
    }

    public List<String> listTenants() {
        return this.client.listTenants();
    }

    public void setFont(String name, Font font) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to define fonts for a tenant.");
        }
        this.client.setFont(name, font);
    }
    public void setColorScheme(String name, ColorScheme colorScheme) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to define the color scheme for a tenant.");
        }
        this.client.setColorScheme(name, colorScheme);
    }

    public void setCustomCSS(String name, String css) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to define the custom css for a tenant.");
        }
        this.client.setCustomCSS(name, css);
    }

    public String getCSS(String name) {
        return this.client.getCSS(name);
    }

    public ResponseEntity<Resource> getFavicon(String name) {
        ImageResult favicon = this.client.getFavicon(name);
        if(favicon != null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(favicon.getMediaType())).body(new ByteArrayResource(favicon.getImage()));
        }
        return ResponseEntity.notFound().build();
    }

    public void setFavicon(String name, MultipartFile file) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to define the favicon for a tenant.");
        }
        this.client.setFavicon(name, file);
    }

    public ResponseEntity<Resource> getBackgroundImage(String name, boolean darkMode) {
        ImageResult backgroundImage = this.client.getBackgroundImage(name, darkMode);
        if(backgroundImage!=null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(backgroundImage.getMediaType())).body(new ByteArrayResource(backgroundImage.getImage()));
        }
        return ResponseEntity.notFound().build();
    }

    public void setBackgroundImage(String name, MultipartFile file, boolean darkMode) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to define the background image for a tenant.");
        }
        this.client.setBackgroundImage(name, file, darkMode);
    }

    public ResponseEntity<Resource> getLogo(String name, boolean darkMode) {
        ImageResult logo = this.client.getLogo(name, darkMode);
        if(logo!=null){
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(logo.getMediaType())).body(new ByteArrayResource(logo.getImage()));
        }
        return ResponseEntity.notFound().build();
    }

    public void setLogo(String name, MultipartFile file, boolean darkMode) {
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to define the logo for a tenant.");
        }
        this.client.setLogo(name, file, darkMode);
    }
}
