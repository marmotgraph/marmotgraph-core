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

package org.marmotgraph.graphdb.queries.controller;

import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionType;
import org.marmotgraph.arango.commons.ArangoQueries;
import org.marmotgraph.arango.commons.model.AQLQuery;
import org.marmotgraph.arango.commons.model.ArangoCollectionReference;
import org.marmotgraph.arango.commons.model.InternalSpace;
import org.marmotgraph.commons.Tuple;
import org.marmotgraph.commons.exception.LimitExceededException;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.QueryResult;
import org.marmotgraph.commons.model.StreamedQueryResult;
import org.marmotgraph.commons.models.UserWithRoles;
import org.marmotgraph.commons.query.KgQuery;
import org.marmotgraph.graphdb.commons.controller.ArangoDatabases;
import org.marmotgraph.graphdb.commons.controller.GraphDBArangoUtils;
import org.marmotgraph.graphdb.commons.controller.PermissionsController;
import org.marmotgraph.graphdb.queries.model.spec.Specification;
import org.marmotgraph.graphdb.queries.utils.DataQueryBuilder;
import org.marmotgraph.graphdb.queries.utils.SpecificationToScopeQueryAdapter;
import org.marmotgraph.graphdb.structure.controller.MetaDataController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class QueryController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SpecificationInterpreter specificationInterpreter;

    private final PermissionsController permissionsController;

    private final ArangoDatabases arangoDatabases;

    private final GraphDBArangoUtils graphDBArangoUtils;

    private final MetaDataController metaDataController;

    private final Double maxMemoryForQuery;

    public QueryController(SpecificationInterpreter specificationInterpreter, ArangoDatabases arangoDatabases, PermissionsController permissionsController, GraphDBArangoUtils graphDBArangoUtils, @Value("${org.marmotgraph.arango.maxMemory:#{null}}") Double maxMemoryForQuery, MetaDataController metaDataController) {
        this.specificationInterpreter = specificationInterpreter;
        this.arangoDatabases = arangoDatabases;
        this.graphDBArangoUtils = graphDBArangoUtils;
        this.permissionsController = permissionsController;
        this.maxMemoryForQuery = maxMemoryForQuery;
        this.metaDataController = metaDataController;
    }


    public QueryResult query(UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        ArangoDatabase database = arangoDatabases.getByStage(query.getStage());
        final Tuple<AQLQuery, Specification> q = query(database, userWithRoles, query, paginationParam, filterValues, scopeMode);
        try {
            return new QueryResult(ArangoQueries.queryDocuments(database, q.getA(), maxMemoryForQuery), q.getB().getResponseVocab());
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query: %s", q.getA()));
            // Test if the exception error num is for "Query use more memory than allowed"
            if (ex.getErrorNum()!=null && ex.getErrorNum() == 32) {
                throw new LimitExceededException(String.format("%s - Bandwidth Limit Exceeded - %s", HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value(), ex.getErrorNum()));
            } else {
                throw ex;
            }
        }
    }

    public StreamedQueryResult queryToStream(UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        ArangoDatabase database = arangoDatabases.getByStage(query.getStage());
        final Tuple<AQLQuery, Specification> q = query(database, userWithRoles, query, paginationParam, filterValues, scopeMode);
        try {
            return new StreamedQueryResult(ArangoQueries.queryDocumentsAsStream(database, q.getA(), maxMemoryForQuery), q.getB().getResponseVocab());
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query: %s", q.getA()));
            throw ex;
        }
    }


    private Tuple<AQLQuery, Specification> query(ArangoDatabase database, UserWithRoles userWithRoles, KgQuery query, PaginationParam paginationParam, Map<String, String> filterValues, boolean scopeMode) {
        Specification specification = specificationInterpreter.readSpecification(query.getPayload());
        Map<String, Object> whitelistFilter;
        if(scopeMode){
            specification = new SpecificationToScopeQueryAdapter(specification).translate();
            // In scope mode, we don't apply the whitelist filter since we're only exposing ids and it is important
            // that we have the full scope of an instance
            whitelistFilter = null;
        }
        else {
            whitelistFilter = permissionsController.whitelistFilterForReadInstances(metaDataController.getSpaceNames(query.getStage(), userWithRoles), userWithRoles, query.getStage());
        }
        graphDBArangoUtils.getOrCreateArangoCollection(database, ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE));
        graphDBArangoUtils.getOrCreateArangoCollection(database, InternalSpace.TYPE_EDGE_COLLECTION);
        final List<String> spaceRestrictions = query.getRestrictToSpaces() == null ? null : query.getRestrictToSpaces().stream().filter(Objects::nonNull).map(ArangoCollectionReference::fromSpace).map(ArangoCollectionReference::getCollectionName).collect(Collectors.toList());
        AQLQuery aql = new DataQueryBuilder(specification, paginationParam, whitelistFilter, spaceRestrictions, query.getIdRestriction(), filterValues, database.getCollections().stream().map(c -> new ArangoCollectionReference(c.getName(), c.getType() == CollectionType.EDGES)).collect(Collectors.toList())).build();
        return new Tuple<>(aql, specification);
    }



}
