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

package org.marmotgraph.auth.models;

import lombok.Getter;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.auth.Functionality;
import org.marmotgraph.commons.model.SpaceName;

import java.util.Objects;
import java.util.UUID;

/**
 * A functionality instance carries the functionality which can be executed either globally (neither space nor id) for a specific space or for a specific instance (id)
 */
public record FunctionalityInstance(Functionality functionality, SpaceName space, UUID id) {

    public boolean appliesTo(SpaceName space, UUID id) {
        if (space != null && SpaceName.REVIEW_SPACE.equals(space.getName())) {
            //The review space is special -> we only grant read access
            return this.functionality == Functionality.READ;
        }
        boolean global = this.space == null && this.id == null;
        boolean bySpace = this.space != null && this.id == null && (this.space.equals(space) || (this.space.matchesWildcard(space)));
        boolean byInstance = this.id != null && this.id.equals(id);
        return global || bySpace || byInstance;
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

    public InstanceId getInstanceId() {
        if (id != null) {
            return new InstanceId(id, space);
        }
        return null;
    }

    public boolean matchesWildcard(FunctionalityInstance i) {
        if (space() != null && i != null && i.functionality().equals(functionality()) && i.id() == null) {
            return space().matchesWildcard(i.space());
        }
        return false;
    }

}
