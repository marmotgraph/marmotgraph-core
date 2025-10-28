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

package org.marmotgraph.core.controller;

import lombok.AllArgsConstructor;
import org.marmotgraph.commons.api.primaryStore.Instances;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.query.MarmotGraphQuery;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The query controller contains the orchestration logic for the query operations
 */
@AllArgsConstructor
@Component
public class CoreQueryController {

    private final Instances.Client instances;
    private final CoreInstanceController instanceController;

    public ResultWithExecutionDetails<NormalizedJsonLd> createNewQuery(NormalizedJsonLd query, UUID queryId, SpaceName space) {
        return instanceController.createNewInstance(query, queryId, space, new ExtendedResponseConfiguration());
    }

    public ResultWithExecutionDetails<NormalizedJsonLd> updateQuery(NormalizedJsonLd query, InstanceId instanceId) {
        return instanceController.contributeToInstance(query, instanceId, false, new ExtendedResponseConfiguration());
    }

    public Paginated<NormalizedJsonLd> listQueries(String search, PaginationParam paginationParam) {
        return instances.getQueriesByType(DataStage.IN_PROGRESS, search, false, false, paginationParam, null);
    }

    public Paginated<NormalizedJsonLd> listQueriesPerRootType(String search, Type type, PaginationParam paginationParam) {
        return instances.getQueriesByType(DataStage.IN_PROGRESS, search, false, false, paginationParam, type.getName());
    }

    public NormalizedJsonLd fetchQueryById(InstanceId instanceId) {
        if (instanceId != null) {
            return instances.getQueryById(instanceId.getSpace().getName(), instanceId.getUuid());
        }
        return null;
    }

    public PaginatedStream<? extends JsonLdDoc> executeQuery(MarmotGraphQuery query, Map<String, String> params, PaginationParam paginationParam) {
        StreamedQueryResult paginatedQueryResult = instances.executeQuery(query, params, paginationParam);
        if (paginatedQueryResult != null) {
            if (paginatedQueryResult.getResponseVocab() != null) {
                final String responseVocab = paginatedQueryResult.getResponseVocab();
                final Stream<NormalizedJsonLd> stream = paginatedQueryResult.getStream().getStream().peek(s -> s.applyVocab(responseVocab));
                return new PaginatedStream<>(stream, paginatedQueryResult.getStream().getTotalResults(), paginatedQueryResult.getStream().getSize(), paginatedQueryResult.getStream().getFrom());
            }
            return paginatedQueryResult.getStream();
        }
        return null;
    }

    public void deleteQuery(UUID id) {
        instanceController.deleteInstance(id);
    }

    public boolean isInvited(NormalizedJsonLd normalizedJsonLd) {
       return instanceController.isInvited(normalizedJsonLd);
    }
}
