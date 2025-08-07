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

import org.marmotgraph.commons.api.Tenants;
import org.marmotgraph.commons.model.tenant.*;
import org.marmotgraph.tenants.controller.TenantsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class TenantsAPI implements Tenants.Client {

    private final TenantsController tenantsRepository;
    private final String namespace;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TenantsAPI(@Value("${org.marmotgraph.namespace}") String namespace, TenantsController tenantsRepository) {
        this.tenantsRepository = tenantsRepository;
        this.namespace = namespace;
    }

    @Override
    public void createTenant(String name, TenantDefinition tenantDefinition) {
        tenantsRepository.upsertTenant(name, tenantDefinition);
    }

    @Override
    public TenantDefinitionWithIdNamespace getTenant(String name) {
        return TenantDefinitionWithIdNamespace.fromTenantDefinition(tenantsRepository.getTenantDefinition(name), namespace);
    }

    @Override
    public List<String> listTenants() {
        return tenantsRepository.listTenants();
    }

    @Override
    public void setFont(String name, Font font) {
        tenantsRepository.upsertFont(name, font);
    }

    @Override
    public void setColorScheme(String name, ColorScheme colorScheme) {
        tenantsRepository.upsertColorScheme(name, colorScheme);
    }


    @Override
    public void setCustomCSS(String name, String css) {
        tenantsRepository.upsertCustomCSS(name, css);
    }

    @Override
    public String getCSS(String name) {
        return tenantsRepository.getCSS(name);
    }

    @Override
    public ImageResult getFavicon(String name) {
        return tenantsRepository.getFavicon(name);
    }

    @Override
    public void setFavicon(String name, MultipartFile file) {
        try {
            tenantsRepository.upsertFavicon(name, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the favicon", e);
        }
    }


    @Override
    public ImageResult getBackgroundImage(String name, boolean darkMode) {
        return tenantsRepository.getBackgroundImage(name, darkMode);
    }

    @Override
    public void setBackgroundImage(String name, MultipartFile file, boolean darkMode) {
        try {
            tenantsRepository.upsertBackgroundImage(name, file.getOriginalFilename(), file.getContentType(), file.getBytes(), darkMode);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the background image", e);
        }
    }

    @Override
    public ImageResult getLogo(String name, boolean darkMode) {
        return tenantsRepository.getLogo(name, darkMode);
    }

    @Override
    public void setLogo(String name, MultipartFile file, boolean darkMode) {
        try {
            tenantsRepository.upsertLogo(name, file.getOriginalFilename(), file.getContentType(), file.getBytes(), darkMode);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Was not able to upload the logo", e);
        }
    }

}
