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

package org.marmotgraph.tenants.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.OverwriteMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.binary.Base64;
import org.marmotgraph.arango.commons.aqlbuilder.AQL;
import org.marmotgraph.arango.commons.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.arango.commons.model.ArangoCollectionReference;
import org.marmotgraph.arango.commons.model.ArangoDatabaseProxy;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.model.tenant.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantsRepository {
    private final ArangoDatabaseProxy arangoDatabase;
    private final TenantsDBUtils tenantsDBUtils;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String TENANTS = "tenants";
    private static final String COLOR_SCHEMES = "colorSchemes";
    private static final String CUSTOM_CSS = "customCSS";
    private static final String FONT = "font";
    private static final String FAVICON = "favicon";
    private static final String LOGO = "logo";
    private static final String BACKGROUND_IMAGE = "backgroundImage";

    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist(TENANTS);
        arangoDatabase.createCollectionIfItDoesntExist(COLOR_SCHEMES);
        arangoDatabase.createCollectionIfItDoesntExist(CUSTOM_CSS);
        arangoDatabase.createCollectionIfItDoesntExist(FONT);
        arangoDatabase.createCollectionIfItDoesntExist(FAVICON);
        arangoDatabase.createCollectionIfItDoesntExist(LOGO);
        arangoDatabase.createCollectionIfItDoesntExist(BACKGROUND_IMAGE);
    }

    public TenantsRepository(@Qualifier("tenantsDB") ArangoDatabaseProxy arangoDatabase, TenantsDBUtils tenantsDBUtils) {
        this.arangoDatabase = arangoDatabase;
        this.tenantsDBUtils = tenantsDBUtils;
    }

    public static class ArangoWrapper<T> {
        @JsonProperty(ArangoVocabulary.KEY)
        private String key;

        @JsonUnwrapped
        private T wrapped;

        public ArangoWrapper() {
        }

        public ArangoWrapper(String key, T wrapped) {
            this.key = key;
            this.wrapped = wrapped;
        }

        public String getKey() {
            return key;
        }
    }

    public static class CustomCSS {
        public CustomCSS(String key, String css) {
            this.key = key;
            this.css = css;
        }

        public CustomCSS() {
        }

        @JsonProperty(ArangoVocabulary.KEY)
        private String key;

        private String css;

        public String getKey() {
            return key;
        }

        public String getCss() {
            return css;
        }

    }
    @CacheEvict(value = {CacheConstant.CACHE_KEYS_TENANTDEFINITION}, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertTenant(String name, TenantDefinition tenantDefinition){
        ArangoCollection tenantDefinitions = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(TENANTS, false));
        tenantDefinitions.insertDocument(new ArangoWrapper<>(name, tenantDefinition), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_TENANTDEFINITION, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public TenantDefinition getTenantDefinition(String name){
        logger.info("Cache miss for tenant definition of tenant {}", name);
        ArangoCollection tenantDefinitions = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(TENANTS, false));
        return tenantDefinitions.getDocument(name, TenantDefinition.class);
    }

    public List<String> listTenants(){
        AQL aql = new AQL();
        aql.add(AQL.trust(String.format("FOR i in %s return i._key", TENANTS)));
        return arangoDatabase.get().query(aql.build().getValue(), String.class).asListRemaining();
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_FONT, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertFont(String name, Font font){
        logger.info("Cache miss for tenant definition of tenant {}", name);
        ArangoCollection colorSchemes = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FONT, false));
        colorSchemes.insertDocument(new ArangoWrapper<>(name, font), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_FONT, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public Font getFont(String name){
        logger.info("Cache miss for font of tenant {}", name);
        ArangoCollection fonts = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FONT, false));
        return fonts.getDocument(name, Font.class);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_COLOR_SCHEME, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertColorScheme(String name, ColorScheme colorScheme){
        ArangoCollection colorSchemes = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(COLOR_SCHEMES, false));
        colorSchemes.insertDocument(new ArangoWrapper<>(name, colorScheme), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_COLOR_SCHEME, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public ColorScheme getColorScheme(String name){
        logger.info("Cache miss for color scheme of tenant {}", name);
        ArangoCollection colorSchemes = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(COLOR_SCHEMES, false));
        return colorSchemes.getDocument(name, ColorScheme.class);
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_CUSTOM_CSS, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertCustomCSS(String name, String css){
        ArangoCollection customCSS = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(CUSTOM_CSS, false));
        customCSS.insertDocument(new CustomCSS(name, css), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_CUSTOM_CSS, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public String getCustomCSS(String name){
        logger.info("Cache miss for custom css tenant {}", name);
        ArangoCollection customCSS = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(CUSTOM_CSS, false));
        CustomCSS document = customCSS.getDocument(name, CustomCSS.class);
        return document!=null ? document.getCss() : null;
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_FAVICONS, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertFavicon(String name, ImageDefinition image){
        ArangoCollection favicons = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FAVICON, false));
        favicons.insertDocument(new ArangoWrapper<>(name, image), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_FAVICONS, key="{#name}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public ImageResult getFavicon(String name){
        logger.info("Cache miss for favicon of tenant {}", name);
        ArangoCollection favicons = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FAVICON, false));
        ImageDefinition definition = favicons.getDocument(name, ImageDefinition.class);
        return definition != null ? new ImageResult(Base64.decodeBase64(definition.getBase64()), definition.getMimeType()) : null;
    }


    private String postfix(boolean darkMode){
        return darkMode ? "_dark" : "_bright";
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_LOGOS, key="{#name, #darkMode}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertLogo(String name, ImageDefinition image, boolean darkMode){
        ArangoCollection logos = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(LOGO, false));
        logos.insertDocument(new ArangoWrapper<>(name+postfix(darkMode), image), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_LOGOS, key="{#name, #darkMode}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public ImageResult getLogo(String name, boolean darkMode){
        logger.info("Cache miss for logo of tenant {}", name);
        ArangoCollection logos = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(LOGO, false));
        ImageDefinition definition = logos.getDocument(name+postfix(darkMode), ImageDefinition.class);
        return definition != null ? new ImageResult(Base64.decodeBase64(definition.getBase64()), definition.getMimeType()) : null;
    }

    @CacheEvict(value = CacheConstant.CACHE_KEYS_BACKGROUND_IMAGES, key="{#name, #darkMode}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public void upsertBackgroundImage(String name, ImageDefinition image, boolean darkMode){
        ArangoCollection backgroundImages = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(BACKGROUND_IMAGE, false));
        backgroundImages.insertDocument(new ArangoWrapper<>(name+postfix(darkMode), image), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_BACKGROUND_IMAGES, key="{#name, #darkMode}", cacheManager=CacheConstant.CACHE_MANAGER_IN_MEMORY)
    public ImageResult getBackgroundImage(String name, boolean darkMode){
        logger.info("Cache miss for background image of tenant {}", name);
        ArangoCollection backgroundImages = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(BACKGROUND_IMAGE, false));
        ImageDefinition definition = backgroundImages.getDocument(name+postfix(darkMode), ImageDefinition.class);
        return definition != null ? new ImageResult(Base64.decodeBase64(definition.getBase64()), definition.getMimeType()) : null;
    }

}
