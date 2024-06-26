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
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesData;
import org.marmotgraph.commons.markers.ExposesMinimalData;
import org.marmotgraph.commons.markers.ExposesQuery;
import org.marmotgraph.commons.markers.ExposesReleaseStatus;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.params.ReleaseTreeScope;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface GraphDBInstances {

    interface Client extends GraphDBInstances {}

    @ExposesData
    Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam);

    @ExposesData
    NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties);

    @ExposesData
    NormalizedJsonLd getInstanceByIdWithoutPayload(String space, UUID id, DataStage stage, boolean returnIncomingLinks, Long incomingLinksPageSize);

    @ExposesData
    NormalizedJsonLd getQueryById(String space, UUID id);

    @ExposesData
    Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String type, String space, String searchByLabel, String filterLabel, String filterValue, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam);

    @ExposesQuery
    Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType);

    @ExposesData
    Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> instanceIds, DataStage stage, String typeRestriction, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize);

    @ExposesMinimalData
    Map<UUID, String> getLabels(List<String> instanceIds, DataStage stage);

    @ExposesData
    List<NormalizedJsonLd> getInstancesByIdentifier(String identifier, String space, DataStage stage);

    @ExposesData
    List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives);

    @ExposesData
    List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean useOriginalTo, boolean returnEmbedded, boolean returnAlternatives);
//
//    @ExposesData
//    List<NormalizedJsonLd> getDocumentWithOutgoingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean returnEmbedded, boolean returnAlternatives);

    @ExposesMinimalData
    GraphEntity getNeighbors(String space, UUID id, DataStage stage);

    @ExposesReleaseStatus
    ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope treeScope);

    @ExposesReleaseStatus
    Map<UUID, ReleaseStatus> getIndividualReleaseStatus(List<InstanceId> instanceIds, ReleaseTreeScope releaseTreeScope);

    @ExposesMinimalData
    SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam);
}
