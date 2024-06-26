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

package org.marmotgraph.ids.api;
import org.marmotgraph.commons.api.Ids;
import org.marmotgraph.commons.exception.AmbiguousException;
import org.marmotgraph.commons.exception.AmbiguousIdException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.IdWithAlternatives;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.ids.controller.IdRepository;
import org.marmotgraph.ids.model.PersistedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class IdsAPI implements Ids.Client {

    private final IdRepository idRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public IdsAPI(IdRepository idRepository) {
        this.idRepository = idRepository;
    }

    @Override
    public void createOrUpdateId(IdWithAlternatives idWithAlternatives,  DataStage stage) {
        if (idWithAlternatives != null && idWithAlternatives.getId() != null) {
            logger.debug(String.format("Updating id %s%s", idWithAlternatives.getId(), idWithAlternatives.getAlternatives() != null ? "with alternatives " + String.join(", ", idWithAlternatives.getAlternatives()) : ""));
            PersistedId persistedId = new PersistedId();
            persistedId.setUUID(idWithAlternatives.getId());
            persistedId.setSpace(new SpaceName(idWithAlternatives.getSpace()));
            persistedId.setAlternativeIds(idWithAlternatives.getAlternatives());
            idRepository.upsert(stage, persistedId);
        }
        else {
            throw new IllegalArgumentException("Invalid payload");
        }
    }

    @Override
    public void removeId(DataStage stage, UUID id) {
        PersistedId foundId = idRepository.getId(id, stage);
        if(foundId!=null) {
            idRepository.remove(stage, foundId);
        }
    }

    @Override
    public Map<UUID, InstanceId> resolveId(List<IdWithAlternatives> idWithAlternatives, DataStage stage) throws AmbiguousIdException {
        if (idWithAlternatives == null || idWithAlternatives.isEmpty()) {
            return Collections.emptyMap();
        }
        return idRepository.resolveIds(stage, idWithAlternatives);
    }

    @Override
    public InstanceId findInstanceByIdentifiers(UUID uuid, List<String> identifiers, DataStage stage) throws AmbiguousException {
        return idRepository.findInstanceByIdentifiers(stage, uuid, identifiers);
    }

}
