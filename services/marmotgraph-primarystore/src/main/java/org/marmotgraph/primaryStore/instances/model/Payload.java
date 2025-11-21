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
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
@MappedSuperclass
@Getter
@Setter
public abstract class Payload<T extends TypeStructure> {

    @Id
    private UUID uuid;

    @Column(columnDefinition = "TEXT")
    private String jsonPayload;

    @ElementCollection
    private List<String> types;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid", referencedColumnName = "uuid")
    private InstanceInformation instanceInformation;

    public abstract void setTypeStructures(List<T> typeStructures);


    @Entity
    @Getter
    @Setter
    @Table(name="instances.inferred")
    public static class InferredPayload extends Payload<TypeStructure.InferredTypeStructure> {

        @Column(columnDefinition = "TEXT")
        private String alternative;

        @OneToMany(targetEntity = TypeStructure.InferredTypeStructure.class, cascade = CascadeType.ALL, mappedBy = "payload")
        private List<TypeStructure.InferredTypeStructure> typeStructures;

        @OneToMany(targetEntity = DocumentRelation.InferredDocumentRelation.class, mappedBy = "payload")
        private List<DocumentRelation.InferredDocumentRelation> documentRelations;

        @OneToMany(targetEntity = DocumentRelation.InferredDocumentRelation.class, mappedBy = "targetPayload", fetch = FetchType.LAZY)
        private List<DocumentRelation.InferredDocumentRelation> incomingRelations;

    }



    @Entity
    @Getter
    @Setter
    @Table(name="instances.released")
    public static class ReleasedPayload extends Payload<TypeStructure.ReleasedTypeStructure> {

        @OneToMany(targetEntity = TypeStructure.ReleasedTypeStructure.class, cascade = CascadeType.ALL, mappedBy = "payload")
        private List<TypeStructure.ReleasedTypeStructure> typeStructures;

        @OneToMany(targetEntity = DocumentRelation.ReleasedDocumentRelation.class, mappedBy = "payload")
        private List<DocumentRelation.ReleasedDocumentRelation> documentRelations;

        @OneToMany(targetEntity = DocumentRelation.ReleasedDocumentRelation.class, mappedBy = "targetPayload", fetch = FetchType.LAZY)
        private List<DocumentRelation.ReleasedDocumentRelation> incomingRelations;
    }


}
