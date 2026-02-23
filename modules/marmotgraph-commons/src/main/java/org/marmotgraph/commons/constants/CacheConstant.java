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

package org.marmotgraph.commons.constants;

public class CacheConstant {

    private CacheConstant() {
    }

    // keys
    public static final String CACHE_KEYS_IDS_COLLECTIONS = "idsCollection";
    public static final String CACHE_KEYS_PRIMARY_STORE_COLLECTION = "primaryStoreCollection";
    public static final String CACHE_KEYS_ARANGO_COLLECTION = "arangoCollection";

    public static final String CACHE_KEYS_BACKGROUND_IMAGES = "backgroundImages";
    public static final String CACHE_KEYS_LOGOS = "logos";
    public static final String CACHE_KEYS_FAVICONS = "favicons";
    public static final String CACHE_KEYS_CUSTOM_CSS = "customCSS";
    public static final String CACHE_KEYS_COLOR_SCHEME = "colorScheme";
    public static final String CACHE_KEYS_FONT = "font";
    public static final String CACHE_KEYS_TENANTDEFINITION = "tenantDefinition";


    public static final String CACHE_KEYS_REFLECTED_SPACES = "reflectedSpaces";
    public static final String CACHE_KEYS_TERMS_OF_USE = "termsOfUse";
    public static final String CACHE_KEYS_TERMS_OF_USE_BY_USER= "termsOfUseByUser";
    public static final String CACHE_KEYS_USER_ROLE_MAPPINGS = "userRoleMappings";

    public static final String CACHE_KEYS_TARGET_TYPES = "targetTypes";

    public static final String CACHE_KEYS_TYPES_IN_SPACE = "typesInSpace";
    public static final String CACHE_KEYS_SPACES = "spaces";
    public static final String CACHE_KEYS_SPACE_SPECIFICATIONS = "spaceSpecifications";
    public static final String CACHE_KEYS_TYPES_IN_SPACE_BY_SPEC = "typesInSpaceBySpec";
    public static final String CACHE_KEYS_PROPERTIES_OF_TYPE_IN_SPACE = "propertiesOfTypeInSpace";

    public static final String CACHE_KEYS_TYPE_SPECIFICATION = "typeSpecification";
    public static final String CACHE_KEYS_PROPERTY_SPECIFICATION = "propertySpecification";
    public static final String CACHE_KEYS_PROPERTIES_IN_TYPE_SPECIFICATION = "propertiesInTypeSpecification";

    public static final String CACHE_KEYS_CLIENT_SPECIFIC_TYPE_SPECIFICATION = "clientSpecificTypeSpecification";
    public static final String CACHE_KEYS_CLIENT_SPECIFIC_PROPERTY_SPECIFICATION = "clientSpecificPropertySpecification";
    public static final String CACHE_KEYS_CLIENT_SPECIFIC_PROPERTIES_IN_TYPE_SPECIFICATION = "clientSpecificPropertiesInTypeSpecification";

    public static final String[] CACHE_KEYS_IN_MEMORY = {
            CACHE_KEYS_IDS_COLLECTIONS,
            CACHE_KEYS_PRIMARY_STORE_COLLECTION,
            CACHE_KEYS_ARANGO_COLLECTION,
            CACHE_KEYS_LOGOS,
            CACHE_KEYS_FAVICONS,
            CACHE_KEYS_CUSTOM_CSS,
            CACHE_KEYS_COLOR_SCHEME,
            CACHE_KEYS_FONT,
            CACHE_KEYS_TENANTDEFINITION,
            CACHE_KEYS_BACKGROUND_IMAGES
    };

    public static final String[] CACHE_KEYS_ALL = {
            CACHE_KEYS_IDS_COLLECTIONS,
            CACHE_KEYS_PRIMARY_STORE_COLLECTION,
            CACHE_KEYS_ARANGO_COLLECTION,
            CACHE_KEYS_REFLECTED_SPACES,
            CACHE_KEYS_TERMS_OF_USE,
            CACHE_KEYS_TERMS_OF_USE_BY_USER,
            CACHE_KEYS_USER_ROLE_MAPPINGS,
            CACHE_KEYS_TARGET_TYPES,
            CACHE_KEYS_TYPES_IN_SPACE,
            CACHE_KEYS_SPACES,
            CACHE_KEYS_SPACE_SPECIFICATIONS,
            CACHE_KEYS_TYPES_IN_SPACE_BY_SPEC,
            CACHE_KEYS_PROPERTIES_OF_TYPE_IN_SPACE,
            CACHE_KEYS_TYPE_SPECIFICATION,
            CACHE_KEYS_PROPERTY_SPECIFICATION,
            CACHE_KEYS_PROPERTIES_IN_TYPE_SPECIFICATION,
            CACHE_KEYS_CLIENT_SPECIFIC_TYPE_SPECIFICATION,
            CACHE_KEYS_CLIENT_SPECIFIC_PROPERTY_SPECIFICATION,
            CACHE_KEYS_CLIENT_SPECIFIC_PROPERTIES_IN_TYPE_SPECIFICATION,
            CACHE_KEYS_LOGOS,
            CACHE_KEYS_FAVICONS,
            CACHE_KEYS_CUSTOM_CSS,
            CACHE_KEYS_COLOR_SCHEME,
            CACHE_KEYS_FONT,
            CACHE_KEYS_TENANTDEFINITION,
            CACHE_KEYS_BACKGROUND_IMAGES
    };

}
