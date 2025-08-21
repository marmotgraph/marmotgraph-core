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

package org.marmotgraph.authentication.config;

import jakarta.annotation.PostConstruct;
import org.marmotgraph.authentication.service.PermissionsService;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.permission.roles.RoleMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InitializeAuthentication {

    private final String adminGroup;
    private final String testDataUploader;
    private final PermissionsService permissionsService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public InitializeAuthentication(@Value("${org.marmotgraph.authentication.adminGroup}") String adminGroup, @Value("${org.marmotgraph.authentication.testDataUploader:null}") String testDataUploader, PermissionsService permissionsService) {
        this.adminGroup = adminGroup;
        this.testDataUploader = testDataUploader;
        this.permissionsService = permissionsService;
    }

    @PostConstruct
    public void setup(){
        if(adminGroup != null){
            logger.info("Initializing authentication admin group {}", adminGroup);
            permissionsService.addClaimToRole(RoleMapping.ADMIN.toRole(null), Map.of("groups", List.of(adminGroup)));
        }
        if(testDataUploader != null){
            logger.info("Initializing demo data uploader");
            permissionsService.addClaimToRole(RoleMapping.ADMIN.toRole(SpaceName.fromString("demo")), Map.of("preferred_username", List.of(String.format("service-account-%s", testDataUploader))));
        }
    }

}
