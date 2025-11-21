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

package org.marmotgraph.primaryStore.instances.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode
public abstract class TypeStructure {

    @EmbeddedId
    private TypeStructure.CompositeId compositeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    private InstanceInformation instanceInformation;

    @ElementCollection
    private List<String> properties;

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompositeId implements Serializable {
        private UUID uuid;
        private String type;
        //This can vary if the property originates from an embedded object
        private String embeddedIdentifier;
    }

    @Entity
    @Getter
    @Setter
    @Table(name="structure.inferred.type")
    @EqualsAndHashCode(callSuper = true)
    public static class InferredTypeStructure extends TypeStructure{

    }


    @Entity
    @Getter
    @Setter
    @Table(name="structure.released.type")
    @EqualsAndHashCode(callSuper = true)
    public static class ReleasedTypeStructure extends TypeStructure{
    }
}
