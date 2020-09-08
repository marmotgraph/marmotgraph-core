/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.graphdb.scopes.controller;

import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.InstanceId;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.query.KgQuery;
import eu.ebrains.kg.graphdb.instances.api.GraphDBInstancesAPI;
import eu.ebrains.kg.graphdb.instances.controller.ArangoRepositoryInstances;
import eu.ebrains.kg.graphdb.queries.controller.QueryController;
import eu.ebrains.kg.graphdb.types.controller.ArangoRepositoryTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ScopesController {

    private final GraphDBInstancesAPI graphDBInstancesAPI;
    private final QueryController queryController;
    private final AuthContext authContext;
    private final IdUtils idUtils;
    private final ArangoRepositoryTypes typesRepo;
    private final ArangoRepositoryInstances instancesRepo;

    public ScopesController(GraphDBInstancesAPI graphDBInstancesAPI, QueryController queryController, AuthContext authContext, IdUtils idUtils, ArangoRepositoryTypes typesRepo, ArangoRepositoryInstances instancesRepo) {
        this.graphDBInstancesAPI = graphDBInstancesAPI;
        this.queryController = queryController;
        this.authContext = authContext;
        this.idUtils = idUtils;
        this.typesRepo = typesRepo;
        this.instancesRepo = instancesRepo;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ScopeElement getScopeForInstance(Space space, UUID id, DataStage stage, boolean fetchLabels){
        //get instance
        NormalizedJsonLd instance = graphDBInstancesAPI.getInstanceById(space.getName(), id, stage, false, false, false);
        //get scope relevant queries
        //TODO filter user defined queries (only take client queries into account)
        Stream<NormalizedJsonLd> typeQueries = instance.getTypes().stream().map(type -> graphDBInstancesAPI.getQueriesByType(stage, null, false, false, null, type)
                .getData()).flatMap(Collection::stream);
        List<NormalizedJsonLd> results = typeQueries.map(q -> queryController.query(authContext.getUserWithRoles(), new KgQuery(q, stage).setIdRestrictions(Collections.singletonList(new EntityId(id.toString()))), null, null, true).getData()).flatMap(Collection::stream).collect(Collectors.toList());
        return translateResultToScope(results, stage, fetchLabels);
    }

    private ScopeElement handleSubElement(NormalizedJsonLd data, Map<String, Set<ScopeElement>> typeToUUID){
        String id = data.getAs("id", String.class);
        UUID uuid = idUtils.getUUID(new JsonLdId(id));
        List<ScopeElement> children = data.keySet().stream().filter(k -> k.startsWith("dependency_")).map(k ->
                data.getAsListOf(k, NormalizedJsonLd.class).stream().map(d -> handleSubElement(d, typeToUUID)).collect(Collectors.toList())
        ).flatMap(Collection::stream).collect(Collectors.toList());
        List<String> type = data.getAsListOf("type", String.class);
        ScopeElement element = new ScopeElement(uuid, type, children.isEmpty() ? null : children, data.getAs("internalId", String.class));
        type.forEach(t -> {
            typeToUUID.computeIfAbsent(t, x -> new HashSet<>()).add(element);
        });
        return element;
    }

    private ScopeElement translateResultToScope(List<NormalizedJsonLd> data, DataStage stage, boolean fetchLabels){
        final Map<String, Set<ScopeElement>> typeToUUID = new HashMap<>();
        ScopeElement element = data.stream().map(d -> handleSubElement(d, typeToUUID)).findFirst().orElse(null);
        if(fetchLabels) {
            List<Type> affectedTypes = typesRepo.getTypeInformation(authContext.getUserWithRoles().getClientId(), stage, typeToUUID.keySet().stream().map(Type::new).collect(Collectors.toList()));
            Set<InstanceId> instances = typeToUUID.values().stream().flatMap(Collection::stream).map(s -> InstanceId.deserialize(s.getInternalId())).collect(Collectors.toSet());
            Map<UUID, String> labelsForInstances = instancesRepo.getLabelsForInstances(stage, instances, affectedTypes);
            typeToUUID.values().stream().distinct().parallel().flatMap(Collection::stream).forEach(e -> {
                if(e.getLabel()==null) {
                    String label = labelsForInstances.get(e.getId());
                    if (label != null){
                        e.setLabel(label);
                    }
                }
            });
        }
        return element;
    }




}