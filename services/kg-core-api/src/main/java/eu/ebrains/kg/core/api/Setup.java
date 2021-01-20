/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.core.api;

import eu.ebrains.kg.arango.commons.model.InternalSpace;
import eu.ebrains.kg.commons.Version;
import eu.ebrains.kg.commons.config.openApiGroups.Admin;
import eu.ebrains.kg.commons.model.Credential;
import eu.ebrains.kg.commons.model.Space;
import eu.ebrains.kg.core.controller.CoreSpaceController;
import eu.ebrains.kg.core.serviceCall.CoreToAuthentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Version.API+"/setup")
public class Setup {

    private final CoreSpaceController spaceController;
    private final CoreToAuthentication coreToAuthentication;

    public Setup(CoreSpaceController spaceController, CoreToAuthentication coreToAuthentication) {
        this.spaceController = spaceController;
        this.coreToAuthentication = coreToAuthentication;
    }

    @PutMapping("/database")
    @Admin
    public void setupDatabaseStructures(){
        //Create global spec space
        Space space = new Space(InternalSpace.GLOBAL_SPEC, false, false);
        space.setInternalSpace(true);
        spaceController.createSpaceDefinition(space, true);
    }

    @PutMapping("/authentication")
    @Admin
    public void setupAuthentication(@RequestBody Credential credential){
        coreToAuthentication.setupAuthentication(credential);
    }
}