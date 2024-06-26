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

package org.marmotgraph.core.api.v3;

import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.api.GraphDBHealth;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.core.model.ExposedStage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(Version.V3 +"/health")
public class HealthV3{

    private final GraphDBHealth.Client graphDBHealth;

    public HealthV3(GraphDBHealth.Client graphDBHealth) {
        this.graphDBHealth = graphDBHealth;
    }


    @Admin
    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void healthStatus(){
        graphDBHealth.analyzeHealthStatus();
    }

    @Admin
    @GetMapping("{name}")
    public List<DynamicJson> getReport(@RequestParam("stage") ExposedStage stage, @PathVariable("name") String name){
        return graphDBHealth.getReport(stage.getStage(), name);
    }

    @Admin
    @GetMapping
    public List<String> getAvailableChecks(){
        return graphDBHealth.getAvailableChecks();
    }


}
