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

package org.marmotgraph.tenants.api;

import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.api.tenants.Tenants;
import org.marmotgraph.commons.exception.UnauthorizedException;
import org.marmotgraph.commons.model.tenant.*;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.tenants.model.Image;
import org.marmotgraph.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

@Component
public class TenantsAPI implements Tenants.Client {

    private final TenantService tenantService;
    private final Permissions permissions;
    private final AuthContext authContext;
    private final String namespace;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TenantsAPI(TenantService tenantService, Permissions permissions, AuthContext authContext, @Value("${org.marmotgraph.namespace}") String namespace) {
        this.tenantService = tenantService;
        this.permissions = permissions;
        this.authContext = authContext;
        this.namespace = namespace;
    }

    @Override
    public void createTenant(String name, TenantDefinition tenantDefinition) {
        canManageTenantOrThrowException(name);
        tenantService.upsertTenantDefinition(name, tenantDefinition);
    }

    @Override
    public TenantDefinitionWithIdNamespace getTenant(String name) {
        return TenantDefinitionWithIdNamespace.fromTenantDefinition(fallback(name, () -> tenantService.getTenantDefinition(name), TenantDefinition.defaultDefinition), namespace);
    }

    @Override
    public List<String> listTenants() {
        return tenantService.listTenants();
    }

    @Override
    public void setFont(String name, Font font) {
        canManageTenantOrThrowException(name);
        tenantService.updateFont(name, font);
    }

    @Override
    public void setColorScheme(String name, ColorScheme colorScheme) {
        canManageTenantOrThrowException(name);
        tenantService.updateColorScheme(name, colorScheme);
    }


    @Override
    public void setCustomCSS(String name, String css) {
        canManageTenantOrThrowException(name);
        tenantService.updateCustomCSS(name, css);
    }

    @Override
    public String getCSS(String name) {
        ColorScheme colorScheme = fallback(name, () -> tenantService.getColorScheme(name), ColorScheme.DEFAULT);
        Font font = fallback(name, () -> tenantService.getFont(name), Font.DEFAULT);
        String customCSS = fallback(name, () -> tenantService.getCustomCSS(name), "");
        return font.toCSS() + "\n\n" + colorScheme.toCSS() + "\n\n" + customCSS;
    }

    @Override
    public ImageResult getFavicon(String name) {
        Image favicon = fallback(name, () -> tenantService.getFavicon(name), getDefaultImage("marmotgraph_favicon.png", "image/png"));
        return favicon != null ? favicon.toImageResult() : null;
    }

    @Override
    public void setFavicon(String name, MultipartFile file) {
        try {
            canManageTenantOrThrowException(name);
            tenantService.updateFavicon(name, new Image(file.getOriginalFilename(), file.getContentType(), file.getBytes()));
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the favicon", e);
        }
    }

    @Override
    public ImageResult getBackgroundImage(String name, boolean darkMode) {
        Image backgroundImage = fallback(name, () -> tenantService.getBackgroundImage(name, darkMode), getDefaultImage(darkMode ? "background_dark.svg" : "background_bright.svg", "image/svg+xml"));
        return backgroundImage != null ? backgroundImage.toImageResult() : null;
    }

    @Override
    public void setBackgroundImage(String name, MultipartFile file, boolean darkMode) {
        try {
            canManageTenantOrThrowException(name);
            tenantService.updateBackgroundImage(name, new Image(file.getOriginalFilename(), file.getContentType(), file.getBytes()), darkMode);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the background image", e);
        }
    }

    @Override
    public ImageResult getLogo(String name, boolean darkMode) {
        Image logo = fallback(name, () -> tenantService.getLogo(name, darkMode), getDefaultImage(darkMode ? "marmotgraph_dark.svg" : "marmotgraph_bright.svg", "image/svg+xml"));
        return logo != null ? logo.toImageResult() : null;
    }

    @Override
    public void setLogo(String name, MultipartFile file, boolean darkMode) {
        try {
            canManageTenantOrThrowException(name);
            tenantService.updateLogo(name, new Image(file.getOriginalFilename(), file.getContentType(), file.getBytes()), darkMode);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the logo", e);
        }
    }


    private void canManageTenantOrThrowException(String name){
        if (name.equals("default")) {
            throw new IllegalArgumentException("You can not update the \"default\" tenant");
        }
        if (!permissions.hasGlobalPermission(authContext.getUserWithRoles(), Functionality.TENANT_MANAGEMENT)) {
            throw new UnauthorizedException("You don't have the right to manage tenants.");
        }
    }

    private Image getDefaultImage(String fileName, String mediaType) {
        try (InputStream resourceAsStream = getClass().getResourceAsStream(String.format("/defaultTheme/images/%s", fileName))) {
            if (resourceAsStream != null) {
                byte[] value = resourceAsStream.readAllBytes();
                return new Image(fileName, mediaType, value);
            }
        } catch (IOException e) {
            logger.error("Was not able to read default image {}", fileName, e);
        }
        return null;
    }

    private <T> T fallback(String name, Supplier<T> supplier, T fallback){
        if (name.equals("default")) {
            return fallback;
        } else {
            try {
                T result = supplier.get();
                return result == null ? fallback : result;
            }
            catch (Exception e) {
                logger.error(e.getMessage(), e);
                return fallback;
            }
        }
    }
}
