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

package org.marmotgraph.tenants.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.exception.InstanceNotFoundException;
import org.marmotgraph.commons.model.tenant.ColorScheme;
import org.marmotgraph.commons.model.tenant.Font;
import org.marmotgraph.commons.model.tenant.TenantDefinition;
import org.marmotgraph.tenants.model.Image;
import org.marmotgraph.tenants.model.Tenant;
import org.marmotgraph.tenants.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@AllArgsConstructor
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;
    private final JsonAdapter jsonAdapter;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @CacheEvict(value = {CacheConstant.CACHE_KEYS_TENANTDEFINITION}, key="{#name}")
    public void upsertTenantDefinition(String name, TenantDefinition tenantDefinition){
        tenantRepository.saveAndFlush(Tenant.fromTenantDefinition(name, tenantDefinition, tenantRepository.findById(name)));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_TENANTDEFINITION, key="{#name}")
    public TenantDefinition getTenantDefinition(String name){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_TENANTDEFINITION);
        return tenantRepository.findById(name).map(Tenant::toTenantDefinition).orElse(null);
    }

    public List<String> listTenants(){
        List<String> result = new ArrayList<>();
        result.add("default");
        List<String> fromDB = entityManager.createQuery("select t.name from Tenant t", String.class).getResultList();
        if(!CollectionUtils.isEmpty(fromDB)){
            result.addAll(fromDB);
        }
        return result;
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_COLOR_SCHEME, key="{#name}")
    public void updateColorScheme(String name, ColorScheme colorScheme){
        partialUpdate(name, t -> t.setColorScheme(jsonAdapter.toJson(colorScheme)));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_COLOR_SCHEME, key="{#name}")
    public ColorScheme getColorScheme(String name){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_COLOR_SCHEME);
        return tenantRepository.findById(name).map(tenant -> jsonAdapter.fromJson(tenant.getColorScheme(), ColorScheme.class)).orElse(null);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_FONT, key="{#name}")
    public void updateFont(String name, Font font){
        partialUpdate(name, t -> t.setFont(jsonAdapter.toJson(font)));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_FONT, key="{#name}")
    public Font getFont(String name){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_FONT);
        return tenantRepository.findById(name).map(tenant -> jsonAdapter.fromJson(tenant.getFont(), Font.class)).orElse(null);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_CUSTOM_CSS, key="{#name}")
    public void updateCustomCSS(String name, String css){
        partialUpdate(name, t -> t.setCustomCSS(css));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_CUSTOM_CSS, key="{#name}")
    public String getCustomCSS(String name){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_CUSTOM_CSS);
        return tenantRepository.findById(name).map(Tenant::getCustomCSS).orElse(null);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_FAVICONS, key="{#name}")
    public void updateFavicon(String name, Image favicon){
        partialUpdate(name, t -> t.setFavIcon(favicon));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_FAVICONS, key="{#name}")
    public Image getFavicon(String name){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_FAVICONS);
        return tenantRepository.findById(name).map(Tenant::getFavIcon).orElse(null);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_LOGOS, key="{#name, #darkMode}")
    public void updateLogo(String name, Image logo, boolean darkMode){
        if(darkMode) {
            partialUpdate(name, t -> t.setDarkLogo(logo));
        }
        else{
            partialUpdate(name, t-> t.setBrightLogo(logo));
        }
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_LOGOS, key="{#name, #darkMode}")
    public Image getLogo(String name, boolean darkMode){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_LOGOS);
        return tenantRepository.findById(name).map(darkMode ? Tenant::getDarkLogo : Tenant::getBrightLogo).orElse(null);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_BACKGROUND_IMAGES, key="{#name, #darkMode}")
    public void updateBackgroundImage(String name, Image backgroundImage, boolean darkMode){
        if(darkMode) {
            partialUpdate(name, t -> t.setDarkBgImage(backgroundImage));
        }
        else{
            partialUpdate(name, t-> t.setBrightBgImage(backgroundImage));
        }
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_BACKGROUND_IMAGES, key="{#name, #darkMode}")
    public Image getBackgroundImage(String name, boolean darkMode){
        logCacheMiss(name, CacheConstant.CACHE_KEYS_BACKGROUND_IMAGES);
        return tenantRepository.findById(name).map(darkMode ? Tenant::getDarkBgImage : Tenant::getBrightBgImage).orElse(null);
    }

    private void logCacheMiss(String name, String cache){
        logger.info("Cache miss for {} of tenant {}", cache, name);
    }

    private void partialUpdate(String name, Consumer<Tenant> consumer){
        Optional<Tenant> existing = tenantRepository.findById(name);
        if(existing.isPresent()){
            Tenant existingTenant = existing.get();
            consumer.accept(existingTenant);
            tenantRepository.saveAndFlush(existingTenant);
        }
        else{
            throw new InstanceNotFoundException(String.format("Tenant with name %s not found", name));
        }
    }
}
