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

package org.marmotgraph.commons.permission;

import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.SpaceName;

import java.util.Objects;
import java.util.UUID;

/**
 * A functionality instance carries the functionality which can be executed either globally (neither space nor id) for a specific space or for a specific instance (id)
 */
public class FunctionalityInstance {
    private Functionality functionality;
    private SpaceName space;
    private UUID id;

    private FunctionalityInstance(){}

    public boolean appliesTo(SpaceName space, UUID id){
        if(space!=null && SpaceName.REVIEW_SPACE.equals(space.getName())){
            //The review space is special -> we only grant read access
            return this.functionality == Functionality.READ;
        }
        boolean global = this.space == null && this.id == null;
        boolean bySpace = this.space!=null && this.id == null && (this.space.equals(space) || (this.space.matchesWildcard(space)));
        boolean byInstance = this.id != null && this.id.equals(id);
        return global || bySpace || byInstance;
    }

    public FunctionalityInstance(Functionality functionality, SpaceName space, UUID id) {
        this.functionality = functionality;
        this.space = space;
        this.id = id;
    }

    public static String getRolePatternForSpace(SpaceName space) {
        return String.format("%s\\:.*", space.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionalityInstance that = (FunctionalityInstance) o;
        return functionality == that.functionality &&
                Objects.equals(space, that.space) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionality, space, id);
    }

    public Functionality getFunctionality() {
        return functionality;
    }

    public SpaceName getSpace() {
        return space;
    }

    public UUID getId() {
        return id;
    }

    public InstanceId getInstanceId() {
        if (id != null) {
            return new InstanceId(id, space);
        }
        return null;
    }

    public boolean matchesWildcard(FunctionalityInstance i){
        if(getSpace()!=null && i!=null && i.getFunctionality().equals(getFunctionality()) && i.getId() == null){
            return getSpace().matchesWildcard(i.getSpace());
        }
        return false;
    }

}
