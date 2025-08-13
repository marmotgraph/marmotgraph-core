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

package org.marmotgraph.graphdb.neo4j.api;

import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.graphdb.neo4j.Neo4JProfile;
import org.marmotgraph.commons.api.GraphDBInstances;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesData;
import org.marmotgraph.commons.markers.ExposesMinimalData;
import org.marmotgraph.commons.markers.ExposesQuery;
import org.marmotgraph.commons.markers.ExposesReleaseStatus;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.params.ReleaseTreeScope;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Profile("neo4j")
@Component
public class InstancesAPI implements GraphDBInstances.Client {


    @Override
    public Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam) {
        throw new NotImplementedException();
    }

    @Override
    public NormalizedJsonLd getInstanceById(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties) {
        throw new NotImplementedException();

    }

    @Override
    public NormalizedJsonLd getInstanceByIdWithoutPayload(String space, UUID id, DataStage stage, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesQuery
    public NormalizedJsonLd getQueryById(String space, UUID id) {
        throw new NotImplementedException();

    }

    @Override
    @ExposesData
    public Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String typeName, String space, String searchByLabel, String filterProperty, String filterValue, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<String> ids, DataStage stage, String typeRestriction, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesMinimalData
    public Map<UUID, String> getLabels(List<String> ids, DataStage stage) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getInstancesByIdentifier(String identifier, String space, DataStage stage) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithRelatedInstancesByIdentifiers(String space, UUID id, DataStage stage, boolean returnEmbedded, boolean returnAlternatives) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesData
    public List<NormalizedJsonLd> getDocumentWithIncomingRelatedInstances(String space, UUID id, DataStage stage, String relation, boolean useOriginalTo, boolean returnEmbedded, boolean returnAlternatives) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesMinimalData
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesReleaseStatus
    public ReleaseStatus getReleaseStatus(String space, UUID id, ReleaseTreeScope treeScope) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesReleaseStatus
    public Map<UUID, ReleaseStatus> getIndividualReleaseStatus(List<InstanceId> instanceIds, ReleaseTreeScope releaseTreeScope) {
        throw new NotImplementedException();
    }


    @Override
    @ExposesMinimalData
    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam) {
        throw new NotImplementedException();

    }


}
