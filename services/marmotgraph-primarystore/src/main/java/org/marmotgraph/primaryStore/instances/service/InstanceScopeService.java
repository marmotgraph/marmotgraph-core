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

package org.marmotgraph.primaryStore.instances.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.ScopeElement;
import org.marmotgraph.commons.model.external.types.TypeInformation;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.graphdb.GraphDB;
import org.marmotgraph.primaryStore.instances.model.InstanceInformation;
import org.marmotgraph.primaryStore.instances.model.InstanceScope;
import org.marmotgraph.primaryStore.instances.model.TypeStructure;
import org.marmotgraph.primaryStore.structures.service.TypesService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Component
public class InstanceScopeService {

    private final InstanceScopeRepository repository;
    private final GraphDB graphDB;
    private final EntityManager em;
    private final InstanceInformationRepository instanceInformationRepository;
    private final InferredTypeStructureRepository inferredTypeStructureRepository;
    private final SpaceRepository spaceRepository;


    public void calculateInstanceScope(UUID id) {
//        fetchScopeRelevantQueries()
//
//
//        List<TypeStructure.InferredTypeStructure> instances = inferredTypeStructureRepository.findByCompositeIdEmbeddedIdentifier(id.toString());
//        Set<String> types = instances.stream().map(i -> i.getCompositeId().getType()).collect(Collectors.toSet());
//        Set<String> relevantSpaces = spaceRepository.getSpacesByScopeRelevant(true).stream().map(org.marmotgraph.primaryStore.instances.model.Space::getName).collect(Collectors.toSet());
//        inferredTypeStructureRepository.findByCompositeIdType(EBRAINSVocabulary.META_QUERY_TYPE)
//
//        instances.forEach(f -> {});

        //
//        Optional<InstanceInformation> instanceInformationOptional = this.instanceInformationRepository.findById(id);
//        if(instanceInformationOptional.isPresent()){
//            InstanceInformation instanceInformation = instanceInformationOptional.get();
//            Stream<NormalizedJsonLd> typeQueries = .types().stream().map(type -> queries.getQueriesByRootType(stage, null, null, false, false, type).getData()).flatMap(Collection::stream);
//
//
//            final ScopeElement scopeForInstance = graphDB.getScopeForInstance(instanceInformation.getSpaceName(), instanceInformation.getUuid(), DataStage.IN_PROGRESS, false);
//            final Set<UUID> uuids = collectIds(scopeForInstance, new HashSet<>());
//            repository.saveAndFlush(new InstanceScope(id, new HashSet<>(uuids)));
//        }
    }

    private List<UUID> fetchScopeRelevantQueries(){
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<UUID> query = criteriaBuilder.createQuery(UUID.class);
        Root<? extends InstanceInformation> root = query.from(InstanceInformation.class);
        query.select(root.get("uuid")).distinct(true);
        Join<Object, Object> typeStructures = root.join("typeStructures", JoinType.LEFT);
        Join<Object, Object> space = root.join("space", JoinType.LEFT);
        query.where(criteriaBuilder.and(criteriaBuilder.isTrue(space.get("scopeRelevant")), criteriaBuilder.equal(typeStructures.get("compositeId").get("type"), EBRAINSVocabulary.META_QUERY_TYPE)));
        return em.createQuery(query).getResultList();
    }


    public Set<UUID> getRelatedIds(UUID id){
        return repository.findById(id).map(InstanceScope::getRelatedIds).orElse(Collections.emptySet());
    }

    public List<UUID> getRelatedIds(List<UUID> ids){
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<UUID> query = criteriaBuilder.createQuery(UUID.class);
        Root<InstanceScope> root = query.from(InstanceScope.class);
        Join<InstanceScope, UUID> r = root.join("relatedIds");
        query.select(r).distinct(true);
        query.where(root.get("uuid").in(ids));
        return em.createQuery(query).getResultList();
    }

    private Set<UUID> collectIds(ScopeElement s, Set<UUID> collector) {
        collector.add(s.getId());
        if (s.getChildren() != null) {
            for (ScopeElement c : s.getChildren()) {
                collectIds(c, collector);
            }
        }
        return collector;
    }

}
