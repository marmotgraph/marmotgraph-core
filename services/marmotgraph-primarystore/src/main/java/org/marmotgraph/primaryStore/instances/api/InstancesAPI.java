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

package org.marmotgraph.primaryStore.instances.api;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.commons.AuthContext;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.api.primaryStore.Instances;
import org.marmotgraph.commons.exception.AmbiguousException;
import org.marmotgraph.commons.exception.AmbiguousIdException;
import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.exception.InstanceNotFoundException;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.markers.ExposesData;
import org.marmotgraph.commons.markers.ExposesMinimalData;
import org.marmotgraph.commons.markers.ExposesQuery;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.model.query.QuerySpecification;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.params.ReleaseTreeScope;
import org.marmotgraph.commons.permission.Functionality;
import org.marmotgraph.commons.permissions.controller.Permissions;
import org.marmotgraph.commons.query.MarmotGraphQuery;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.graphdb.GraphDB;
import org.marmotgraph.primaryStore.instances.model.InstanceInformation;
import org.marmotgraph.primaryStore.instances.service.InstanceInformationRepository;
import org.marmotgraph.primaryStore.instances.service.InstanceScopeService;
import org.marmotgraph.primaryStore.instances.service.PayloadService;
import org.marmotgraph.primaryStore.instances.service.SpaceService;
import org.marmotgraph.primaryStore.queries.service.QueryPermissions;
import org.marmotgraph.primaryStore.queries.service.SpecificationInterpreter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
public class InstancesAPI implements Instances.Client {

    private final PayloadService payloadService;
    private final InstanceInformationRepository globalInstanceInformationRepository;
    private final Permissions permissions;
    private final AuthContext authContext;
    private final InstanceScopeService scopes;
    private final GraphDB graphDB;
    private final IdUtils idUtils;
    private final SpecificationInterpreter specificationInterpreter;
    private final JsonAdapter jsonAdapter;
    private final QueryPermissions queryPermissions;
    private final SpaceService spaceService;


    @Override
    public Map<UUID, InstanceId> resolveIds(List<IdWithAlternatives> idWithAlternatives, DataStage stage) throws AmbiguousIdException {
        return payloadService.resolveIds(idWithAlternatives, stage);
    }

    @Override
    public InstanceId findInstanceByIdentifiers(UUID uuid, List<String> identifiers, DataStage dataStage) throws AmbiguousException {
        return globalInstanceInformationRepository.findById(uuid)
                .map(i -> new InstanceId(i.getUuid(), SpaceName.fromString(i.getSpaceName())))
                .orElseGet(() -> resolveIds(List.of(new IdWithAlternatives(uuid, null, new HashSet<>(identifiers))), dataStage).get(uuid));
    }

    @Override
    public NormalizedJsonLd getInstanceById(UUID id, DataStage stage, boolean removeInternalProperties) {
        Optional<NormalizedJsonLd> instanceById = payloadService.getInstanceById(id, stage, true,false, authContext.getUserWithRoles());
        if(instanceById.isPresent()) {
            NormalizedJsonLd normalizedJsonLd = instanceById.get();
            if(removeInternalProperties){
                normalizedJsonLd.removeAllInternalProperties();
            }
            return normalizedJsonLd;
        }
        return null;
    }

    @Override
    public Tuple<NormalizedJsonLd, String> getNativeInstanceById(UUID id, String userId) {
        Tuple<NormalizedJsonLd, String> normalizedJsonLd = payloadService.getNativeInstanceById(id, userId).orElse(null);
        if(normalizedJsonLd != null && normalizedJsonLd.getA()!=null) {
            normalizedJsonLd.getA().removeAllInternalProperties();
        }
        return normalizedJsonLd;
    }


    @Override
    public ReleaseStatus getReleaseStatus(UUID id, ReleaseTreeScope releaseTreeScope){
        Optional<InstanceInformation> byId = globalInstanceInformationRepository.findById(id);
        if(byId.isPresent()) {
            if(!permissions.hasPermission(authContext.getUserWithRoles(), Functionality.RELEASE_STATUS, SpaceName.fromString(byId.get().getSpaceName()), id)){
                throw new ForbiddenException();
            }
            ReleaseStatus topInstanceReleaseStatus = payloadService.getReleaseStatus(id);
            switch (releaseTreeScope){
                case TOP_INSTANCE_ONLY -> {return topInstanceReleaseStatus; }
                case CHILDREN_ONLY, CHILDREN_ONLY_RESTRICTED -> {
                    scopes.calculateInstanceScope(id); //TODO check if we can somehow avoid to call this method everytime (since it might be expensive)
                    Set<UUID> relatedIds = this.scopes.getRelatedIds(id);
                    List<ReleaseStatus> distinctReleaseStatus = payloadService.getDistinctReleaseStatus(relatedIds);
                    if (distinctReleaseStatus.contains(null) || distinctReleaseStatus.contains(ReleaseStatus.UNRELEASED)) {
                        return ReleaseStatus.UNRELEASED;
                    } else if (distinctReleaseStatus.contains(ReleaseStatus.HAS_CHANGED)) {
                        return ReleaseStatus.HAS_CHANGED;
                    } else {
                        return ReleaseStatus.RELEASED;
                    }
                }
            }
            throw new RuntimeException("Release tree scope unknown");
        }
        throw new InstanceNotFoundException(String.format("Instance %s not found", id));
    }


    @Override
    public Map<UUID, ReleaseStatus> getReleaseStatus(List<UUID> ids, ReleaseTreeScope releaseTreeScope){
        if(releaseTreeScope == ReleaseTreeScope.TOP_INSTANCE_ONLY){
            // For the top instance only, we can optimize the way we request the release status...
            Map<InstanceId, ReleaseStatus> releaseStatus = payloadService.getReleaseStatus(ids);
            final UserWithRoles userWithRoles = authContext.getUserWithRoles();
            Set<SpaceName> permittedSpaces = new HashSet<>();
            return releaseStatus.keySet().stream().filter(k -> {
                if (permittedSpaces.contains(k.getSpace())) {
                    return true;
                } else {
                    if (permissions.hasPermission(userWithRoles, Functionality.RELEASE_STATUS, k.getSpace())) {
                        permittedSpaces.add(k.getSpace());
                        return true;
                    } else {
                        return permissions.hasPermission(userWithRoles, Functionality.RELEASE_STATUS, k.getSpace(), k.getUuid());
                    }
                }
            }).collect(Collectors.toMap(InstanceId::getUuid, releaseStatus::get));
        }
        else{
            // whilst for the other states, we just run the standard evaluation in the stream.
            return ids.stream().collect(Collectors.toMap(k -> k, v -> getReleaseStatus(v, releaseTreeScope)));
        }
    }


    @Override
    @ExposesMinimalData
    public Map<UUID, String> getLabels(List<UUID> ids, DataStage stage) {
        throw new NotImplementedException();
    }


    @Override
    @ExposesMinimalData
    public SuggestionResult getSuggestedLinksForProperty(NormalizedJsonLd payload, DataStage stage, String space, UUID id, String propertyName, String sourceType, String targetType, String search, PaginationParam paginationParam) {
        throw new NotImplementedException();

    }

    @Override
    public NormalizedJsonLd getInstanceById(UUID id, DataStage stage, boolean returnPayload, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize, boolean removeInternalProperties) {
        Optional<NormalizedJsonLd> instanceById = payloadService.getInstanceById(id, stage, returnPayload && returnEmbedded, returnPayload && returnAlternatives, authContext.getUserWithRoles());
        if(instanceById.isEmpty()){
            return null;
        }
        NormalizedJsonLd result = instanceById.get();
        if(returnPayload){
            if(removeInternalProperties){
                result.removeAllInternalProperties();
            }

        }
        else{
            String space = result.getAs(EBRAINSVocabulary.META_SPACE, String.class);
            result = new NormalizedJsonLd();
            result.setId(id.toString());
            result.put(EBRAINSVocabulary.META_SPACE, space);
        }
        return result;
    }


    @Override
    @ExposesQuery
    public NormalizedJsonLd getQueryById(String space, UUID id) {
        throw new NotImplementedException();

    }

    @Override
    @ExposesData
    public Paginated<NormalizedJsonLd> getInstancesByType(DataStage stage, String typeName, String space, String searchByLabel, String filterProperty, String filterValue, boolean returnPayload, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam) {
        return payloadService.getInstancesByType(stage, typeName, space, searchByLabel, filterProperty, filterValue, returnPayload, returnAlternatives, returnEmbedded, paginationParam);
    }

    @Override
    @ExposesQuery
    public Paginated<NormalizedJsonLd> getQueriesByType(DataStage stage, String searchByLabel, boolean returnAlternatives, boolean returnEmbedded, PaginationParam paginationParam, String rootType) {
        throw new NotImplementedException();
    }

    @Override
    @ExposesData
    public Map<UUID, Result<NormalizedJsonLd>> getInstancesByIds(List<UUID> ids, DataStage stage, String typeRestriction, boolean returnPayload, boolean returnEmbedded, boolean returnAlternatives, boolean returnIncomingLinks, Long incomingLinksPageSize) {
        return payloadService.getInstancesByIds(ids, stage, typeRestriction, returnPayload, returnEmbedded, returnAlternatives, returnIncomingLinks, incomingLinksPageSize);
    }


    @Override
    @ExposesMinimalData
    public GraphEntity getNeighbors(String space, UUID id, DataStage stage) {
        throw new NotImplementedException();
    }

    @Override
    public Paginated<NormalizedJsonLd> getIncomingLinks(String space, UUID id, DataStage stage, String property, String type, PaginationParam paginationParam) {
        throw new NotImplementedException();
    }

    @Override
    public ScopeElement getScopeForInstance(String space, UUID id, DataStage stage, boolean applyRestrictions) {
        return graphDB.getScopeForInstance(space, id, stage, applyRestrictions);
    }

    @Override
    public StreamedQueryResult executeQuery(MarmotGraphQuery query, Map<String, String> params, PaginationParam paginationParam) {
        //FIXME evict the intermediate step once it works properly
        NormalizedJsonLd.recursiveVisitOfProperties(query.getPayload(), Collections.emptyList(), query.getPayload(),
                (name, value, path, parentMap, orderNumber) -> {
                    String queryPrefix = "https://core.kg.ebrains.eu/vocab/query/";
                    if(name.startsWith(queryPrefix)){
                        parentMap.put(name.replace(queryPrefix, ""), value);
                        parentMap.remove(name);
                    }
                    return null;
                }, null, null);
        QuerySpecification querySpecification = jsonAdapter.fromJson(jsonAdapter.toJson(query.getPayload()), QuerySpecification.class);
        payloadService.applyCURIEPrefixes(querySpecification);
        Tuple<Set<SpaceName>, Set<UUID>> accessFilter = queryPermissions.queryFilter(query.getRestrictToSpaces(), authContext.getUserWithRoles(), query.getStage());

        Collection<NormalizedJsonLd> normalizedJsonLds;
        Long totalResults;
        if(accessFilter != null && accessFilter.getA().isEmpty() && accessFilter.getB().isEmpty()){
            //The user doesn't have access to anything - let's fast-track this.
            totalResults = 0L;
            normalizedJsonLds = Collections.emptyList();
        }
        else {
            Tuple<Collection<NormalizedJsonLd>, Long> result = graphDB.executeQuery(querySpecification, query.getStage(), params, paginationParam, accessFilter);
            normalizedJsonLds = result.getA();
            totalResults = result.getB();
        }
        return new StreamedQueryResult(new PaginatedStream<>(normalizedJsonLds.stream(), totalResults, normalizedJsonLds.size(), paginationParam.getFrom()), querySpecification.getMeta().getResponseVocab());
    }

}
