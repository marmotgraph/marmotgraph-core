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

package org.marmotgraph.tenants.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.model.DocumentCreateOptions;
import com.arangodb.model.OverwriteMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.codec.binary.Base64;
import org.marmotgraph.arango.commons.aqlbuilder.AQL;
import org.marmotgraph.arango.commons.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.arango.commons.model.ArangoCollectionReference;
import org.marmotgraph.arango.commons.model.ArangoDatabaseProxy;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.model.tenant.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class TenantsRepository {
    private final ArangoDatabaseProxy arangoDatabase;
    private final TenantsDBUtils tenantsDBUtils;

    private static final String TENANTS = "tenants";
    private static final String COLOR_SCHEMES = "colorSchemes";
    private static final String CUSTOM_CSS = "customCSS";
    private static final String FONT = "font";
    private static final String FAVICON = "favicon";
    private static final String LOGO = "logo";

    @PostConstruct
    public void setup() {
        arangoDatabase.createIfItDoesntExist();
        arangoDatabase.createCollectionIfItDoesntExist(TENANTS);
        arangoDatabase.createCollectionIfItDoesntExist(COLOR_SCHEMES);
        arangoDatabase.createCollectionIfItDoesntExist(CUSTOM_CSS);
        arangoDatabase.createCollectionIfItDoesntExist(FONT);
        arangoDatabase.createCollectionIfItDoesntExist(FAVICON);
        arangoDatabase.createCollectionIfItDoesntExist(LOGO);
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

    public void upsertTenant(String name, TenantDefinition tenantDefinition){
        ArangoCollection tenantDefinitions = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(TENANTS, false));
        tenantDefinitions.insertDocument(new ArangoWrapper<>(name, tenantDefinition), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    public TenantDefinition getTenantDefinition(String name){
        ArangoCollection tenantDefinitions = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(TENANTS, false));
        return tenantDefinitions.getDocument(name, TenantDefinition.class);
    }

    public List<String> listTenants(){
        AQL aql = new AQL();
        aql.add(AQL.trust(String.format("FOR i in %s return i._key", TENANTS)));
        return arangoDatabase.get().query(aql.build().getValue(), String.class).asListRemaining();
    }

    public void upsertFont(String name, Font font){
        ArangoCollection colorSchemes = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FONT, false));
        colorSchemes.insertDocument(new ArangoWrapper<>(name, font), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    public Font getFont(String name){
        ArangoCollection fonts = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FONT, false));
        return fonts.getDocument(name, Font.class);
    }

    public void upsertColorScheme(String name, ColorScheme colorScheme){
        ArangoCollection colorSchemes = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(COLOR_SCHEMES, false));
        colorSchemes.insertDocument(new ArangoWrapper<>(name, colorScheme), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    public ColorScheme getColorScheme(String name){
        ArangoCollection colorSchemes = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(COLOR_SCHEMES, false));
        return colorSchemes.getDocument(name, ColorScheme.class);
    }

    public void upsertCustomCSS(String name, String css){
        ArangoCollection customCSS = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(CUSTOM_CSS, false));
        customCSS.insertDocument(new CustomCSS(name, css), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    public String getCustomCSS(String name){
        ArangoCollection customCSS = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(CUSTOM_CSS, false));
        CustomCSS document = customCSS.getDocument(name, CustomCSS.class);
        return document!=null ? document.getCss() : null;
    }

    public void upsertFavicon(String name, ImageDefinition image){
        ArangoCollection favicons = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FAVICON, false));
        favicons.insertDocument(new ArangoWrapper<>(name, image), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    public ImageResult getFavicon(String name){
        ArangoCollection customCSS = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(FAVICON, false));
        ImageDefinition definition = customCSS.getDocument(name, ImageDefinition.class);
        return definition != null ? new ImageResult(Base64.decodeBase64(definition.getBase64()), definition.getMimeType()) : null;
    }


    private String postfix(boolean darkMode){
        return darkMode ? "_dark" : "_bright";
    }

    public void upsertLogo(String name, ImageDefinition image, boolean darkMode){
        ArangoCollection favicons = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(LOGO, false));
        favicons.insertDocument(new ArangoWrapper<>(name+postfix(darkMode), image), new DocumentCreateOptions().overwriteMode(OverwriteMode.replace));
    }

    public ImageResult getLogo(String name, boolean darkMode){
        ArangoCollection customCSS = tenantsDBUtils.getOrCreateArangoCollection(arangoDatabase.getOrCreate(), new ArangoCollectionReference(LOGO, false));
        ImageDefinition definition = customCSS.getDocument(name+postfix(darkMode), ImageDefinition.class);
        return definition != null ? new ImageResult(Base64.decodeBase64(definition.getBase64()), definition.getMimeType()) : null;
    }

}
