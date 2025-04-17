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

package org.marmotgraph.commons.api;

import org.marmotgraph.commons.model.tenant.ColorScheme;
import org.marmotgraph.commons.model.tenant.Font;
import org.marmotgraph.commons.model.tenant.ImageResult;
import org.marmotgraph.commons.model.tenant.TenantDefinition;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface Tenants {

    interface Client extends Tenants {}

    void createTenant(String name, TenantDefinition tenantDefinition);

    TenantDefinition getTenant(String name);

    List<String> listTenants();

    void setFont(String name, Font font);

    void setColorScheme(String name, ColorScheme colorScheme);

    void setCustomCSS(String name, String css);

    String getCSS(String name);

    ImageResult getFavicon(String name);

    void setFavicon(String name, MultipartFile file);

    ImageResult getBackgroundImage(String name, boolean darkMode);

    void setBackgroundImage(String name, MultipartFile file, boolean darkMode);

    ImageResult getLogo(String name, boolean darkMode);

    void setLogo(String name, MultipartFile file, boolean darkMode);
}
