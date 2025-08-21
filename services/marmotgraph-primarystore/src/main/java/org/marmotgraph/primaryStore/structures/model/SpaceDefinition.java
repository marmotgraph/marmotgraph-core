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

package org.marmotgraph.primaryStore.structures.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.model.external.spaces.SpaceInformation;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;

@Entity
@Getter
@Setter
public class SpaceDefinition {

    @Id
    private String name;
    private boolean autoRelease;
    private boolean deferCache;
    private boolean clientSpace;
    private boolean scopeRelevant;
    private boolean explicitlySpecified;


    public static SpaceDefinition fromSpaceSpecification(SpaceSpecification spec) {
        SpaceDefinition d = new SpaceDefinition();
        d.setName(spec.getName());
        d.setAutoRelease(spec.getAutoRelease() != null ? spec.getAutoRelease() : false);
        d.setClientSpace(spec.getClientSpace() != null ? spec.getClientSpace() : false);
        d.setDeferCache(spec.getDeferCache() != null ? spec.getDeferCache() : false);
        d.setScopeRelevant(spec.getScopeRelevant() != null ? spec.getScopeRelevant() : false);
        d.setExplicitlySpecified(true);
        return d;
    }

    public static SpaceDefinition fromReflection(String spaceName){
        SpaceDefinition d = new SpaceDefinition();
        d.setName(spaceName);
        return d;
    }

    public SpaceInformation toSpaceInformation(){
        SpaceInformation d = new SpaceInformation();
        d.setIdentifier(name);
        d.setName(name);
        return d;
    }

}
