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

package org.marmotgraph.tenants.api;

import org.apache.commons.codec.binary.Base64;
import org.marmotgraph.commons.api.Tenants;
import org.marmotgraph.commons.model.tenant.*;
import org.marmotgraph.tenants.controller.TenantsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class TenantsAPI implements Tenants.Client {

    private final TenantsRepository tenantsRepository;
    private final String namespace;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TenantsAPI(@Value("${org.marmotgraph.namespace}") String namespace, TenantsRepository tenantsRepository) {
        this.tenantsRepository = tenantsRepository;
        this.namespace = namespace;
    }

    @Override
    public void createTenant(String name, TenantDefinition tenantDefinition) {
        if (name.equals("default")) {
            throw new IllegalArgumentException("You can not update the \"default\" tenant");
        }
        tenantsRepository.upsertTenant(name, tenantDefinition);
    }

    @Override
    public TenantDefinitionWithIdNamespace getTenant(String name) {
        TenantDefinition tenantDefinition;
        if (name.equals("default")) {
            tenantDefinition = TenantDefinition.defaultDefinition;
        } else {
            tenantDefinition = tenantsRepository.getTenantDefinition(name);
            tenantDefinition = tenantDefinition == null ? TenantDefinition.defaultDefinition : tenantDefinition;
        }
        return TenantDefinitionWithIdNamespace.fromTenantDefinition(tenantDefinition, namespace);
    }

    @Override
    public List<String> listTenants() {
        List<String> result = new ArrayList<>();
        result.add("default");
        List<String> fromDB = tenantsRepository.listTenants();
        if(!CollectionUtils.isEmpty(fromDB)){
            result.addAll(fromDB);
        }
        return result;
    }

    @Override
    public void setFont(String name, Font font) {
        if (name.equals("default")) {
            throw new IllegalArgumentException("You can not update the \"default\" font");
        }
        tenantsRepository.upsertFont(name, font);
    }

    @Override
    public void setColorScheme(String name, ColorScheme colorScheme) {
        if (name.equals("default")) {
            throw new IllegalArgumentException("You can not update the \"default\" color scheme");
        }
        tenantsRepository.upsertColorScheme(name, colorScheme);
    }


    @Override
    public void setCustomCSS(String name, String css) {
        tenantsRepository.upsertCustomCSS(name, css);
        //Write to DB
    }

    @Override
    public String getCSS(String name) {
        Font font = Font.DEFAULT;
        ColorScheme colorScheme = ColorScheme.DEFAULT;
        String customCSS = "";
        if (!name.equals("default")) {
            Font fontFromDB = tenantsRepository.getFont(name);
            if(fontFromDB != null){
                font = fontFromDB;
            }
            ColorScheme colorSchemeFromDB = tenantsRepository.getColorScheme(name);
            if(colorSchemeFromDB!=null){
                colorScheme = colorSchemeFromDB;
            }
            String customCSSFromDB = tenantsRepository.getCustomCSS(name);
            if(customCSSFromDB!=null){
                customCSS = customCSSFromDB;
            }
        }
        return font.toCSS() + "\n\n" + colorScheme.toCSS() + "\n\n" + customCSS;
    }

    @Override
    public ImageResult getFavicon(String name) {
        if (name.equals("default")) {
            return getDefaultImage("marmotgraph_favicon.png", "image/png");
        }
        else{
            ImageResult favicon = tenantsRepository.getFavicon(name);
            return favicon != null ? favicon : getDefaultImage("marmotgraph_favicon.png", "image/png");
        }
    }

    @Override
    public void setFavicon(String name, MultipartFile file) {
        if (name.equals("default")) {
            throw new IllegalArgumentException("You are not allowed to update the \"default\" favicon");
        }
        try {
            byte[] base64bytes = Base64.encodeBase64(file.getBytes());
            tenantsRepository.upsertFavicon(name, new ImageDefinition(file.getOriginalFilename(), file.getContentType(), new String(base64bytes)));
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the favicon", e);
        }
    }


    @Override
    public ImageResult getBackgroundImage(String name, boolean darkMode) {
        if (name.equals("default")) {
            return getDefaultImage(darkMode ? "background_dark.svg" : "background_bright.svg", "image/svg+xml");
        }
        else{
            ImageResult backgroundImage = tenantsRepository.getBackgroundImage(name, darkMode);
            if(backgroundImage == null){
                //If there is no background image in this color mode, we fall back to the other
                backgroundImage = tenantsRepository.getBackgroundImage(name, !darkMode);
            }
            return backgroundImage != null ? backgroundImage : getDefaultImage(darkMode ? "background_dark.svg" : "background_bright.svg", "image/svg+xml");
        }
    }

    @Override
    public void setBackgroundImage(String name, MultipartFile file, boolean darkMode) {
        if (name.equals("default")) {
            throw new IllegalArgumentException("You are not allowed to update the \"default\" tenant");
        }
        try {
            byte[] base64bytes = Base64.encodeBase64(file.getBytes());
            tenantsRepository.upsertBackgroundImage(name, new ImageDefinition(file.getOriginalFilename(), file.getContentType(), new String(base64bytes)), darkMode);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the background image", e);
        }
    }

    @Override
    public ImageResult getLogo(String name, boolean darkMode) {
        if (name.equals("default")) {
            return getDefaultImage(darkMode ? "marmotgraph_dark.svg" : "marmotgraph_bright.svg", "image/svg+xml");
        }
        else{
            ImageResult logo = tenantsRepository.getLogo(name, darkMode);
            if(logo == null){
                //If there is no logo in this color mode, we fall back to the other
                logo = tenantsRepository.getLogo(name, !darkMode);
            }
            return logo != null ? logo : getDefaultImage(darkMode ? "marmotgraph_dark.svg" : "marmotgraph_bright.svg", "image/svg+xml");
        }
    }

    @Override
    public void setLogo(String name, MultipartFile file, boolean darkMode) {
        if (name.equals("default")) {
            throw new IllegalArgumentException("You are not allowed to update the \"default\" logo");
        }
        try {
            byte[] base64bytes = Base64.encodeBase64(file.getBytes());
            tenantsRepository.upsertLogo(name, new ImageDefinition(file.getOriginalFilename(), file.getContentType(), new String(base64bytes)), darkMode);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the logo", e);
        }
    }


    private ImageResult getDefaultImage(String fileName, String mediaType) {
        try (InputStream resourceAsStream = getClass().getResourceAsStream(String.format("/defaultTheme/images/%s", fileName))) {
            if (resourceAsStream != null) {
                byte[] value = resourceAsStream.readAllBytes();
                return new ImageResult(value, mediaType);
            }
        } catch (IOException e) {
            logger.error("Was not able to read default image {}", fileName, e);
        }
        return null;
    }
}
