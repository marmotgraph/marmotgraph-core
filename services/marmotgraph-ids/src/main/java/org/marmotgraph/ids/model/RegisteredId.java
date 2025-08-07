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

package org.marmotgraph.ids.model;

import jakarta.persistence.*;
import lombok.*;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "ids")
public class RegisteredId {

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompositeId implements Serializable {
        private UUID uuid;
        private DataStage stage;
    }

    @EmbeddedId
    CompositeId compositeId;

    @ElementCollection
    private Set<String> alternativeIds;

    private String space;

    public static RegisteredId fromPersistedId(PersistedId persistedId, DataStage stage){
        if(persistedId==null){
            return null;
        }
        RegisteredId result = new RegisteredId();
        result.setCompositeId(new CompositeId(persistedId.getUUID(), stage));
        result.setAlternativeIds(persistedId.getAlternativeIds());
        result.setSpace(persistedId.getSpace().toString());
        return result;
    }

    public PersistedId toPersistedId() {
        PersistedId result = new PersistedId();
        result.setUUID(compositeId.getUuid());
        result.setSpace(SpaceName.fromString(getSpace()));
        result.setAlternativeIds(getAlternativeIds());
        return result;
    }

    public InstanceId toInstanceId(){
        return new InstanceId(compositeId.getUuid(), SpaceName.fromString(getSpace()));
    }

}
