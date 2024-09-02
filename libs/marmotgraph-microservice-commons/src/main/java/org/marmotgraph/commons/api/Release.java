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

package org.marmotgraph.commons.api;

import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.ReleaseStatus;
import org.marmotgraph.commons.params.ReleaseTreeScope;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface Release {

    interface Client extends Release {}

    void releaseInstance(String space, UUID id, String revision);

    void unreleaseInstance(String space, UUID id);

    ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope releaseTreeScope);

    Map<UUID, ReleaseStatus> getIndividualReleaseStatus(List<InstanceId> instanceIds, ReleaseTreeScope releaseTreeScope);
}