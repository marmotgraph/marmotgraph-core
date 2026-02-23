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
import org.marmotgraph.commons.model.ReleaseStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Table(name="instances.information")
@Entity
@Getter
@Setter
public class InstanceInformation {

    @Id
    private UUID uuid;

    private Long createdAt;

    private Long firstRelease;

    private Long lastRelease;

    private String spaceName;

    @Enumerated(EnumType.STRING)
    private ReleaseStatus releaseStatus = ReleaseStatus.UNRELEASED;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="instances.information.alternative_ids", indexes = @Index(name="alternativeIdLookup", unique = true, columnList = "alternative_ids"))
    @Column(length = 2000)
    private Set<String> alternativeIds;

    @OneToMany(targetEntity=TypeStructure.InferredTypeStructure.class, mappedBy = "instanceInformation", fetch = FetchType.LAZY)
    private List<TypeStructure.InferredTypeStructure> typeStructures;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spaceName", referencedColumnName = "name", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Space space;
}
