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

package org.marmotgraph.arango.commons;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import org.marmotgraph.arango.commons.aqlbuilder.AQL;
import org.marmotgraph.arango.commons.model.AQLQuery;
import org.marmotgraph.commons.exception.InvalidRequestException;
import org.marmotgraph.commons.exception.LimitExceededException;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginatedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ArangoQueries {

    private ArangoQueries() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ArangoQueries.class);


    public static <T> PaginatedStream<T> queryDocuments(ArangoDatabase db, AQLQuery aqlQuery, Function<NormalizedJsonLd, T> mapper, Double maxMemoryForQuery) {
        try {
            AQL aql = aqlQuery.getAql();
            if (logger.isTraceEnabled()) {
                logger.trace(aql.buildSimpleDebugQuery(aqlQuery.getBindVars()));
            }
            String value = aql.build().getValue();
            long launch = new Date().getTime();
            if (maxMemoryForQuery != null) {
                aql.getQueryOptions().memoryLimit(maxMemoryForQuery.longValue());
            }
            aql.getQueryOptions().count(true);
            try (ArangoCursor<NormalizedJsonLd> result = db.query(value, NormalizedJsonLd.class, aqlQuery.getBindVars(), aql.getQueryOptions())) {
                logger.debug("Received {} results from Arango in {}ms", result.getCount(), new Date().getTime() - launch);
                Long count = result.getCount() != null ? result.getCount().longValue() : null;
                Long totalCount;
                if (aql.getPaginationParam() != null && aql.getPaginationParam().getSize() != null) {
                    totalCount = aql.getPaginationParam().isReturnTotalResults() ? result.getStats().getFullCount() : null;
                } else {
                    totalCount = count;
                }

                logger.debug("Start parsing the results after {}ms", new Date().getTime() - launch);
                final Stream<NormalizedJsonLd> stream = StreamSupport.stream(result.spliterator(), false);
                Stream<T> resultStream = stream.map(Objects.requireNonNullElseGet(mapper, () -> s -> (T) s));
                logger.debug("Done processing the Arango result - received {} results in {}ms total", count, new Date().getTime() - launch);
                if (aql.getPaginationParam() != null && aql.getPaginationParam().getSize() == null && (int) aql.getPaginationParam().getFrom() > 0 && count != null && (int) aql.getPaginationParam().getFrom() < count) {
                    //Arango doesn't allow to request from a specific offset to infinite. To achieve this, we load everything and we cut the additional instances in Java
                    resultStream = resultStream.skip((int) aql.getPaginationParam().getFrom());
                    return new PaginatedStream<>(resultStream, totalCount, count - aql.getPaginationParam().getFrom(), aql.getPaginationParam() != null ? aql.getPaginationParam().getFrom() : 0);
                }
                return new PaginatedStream<>(resultStream, totalCount, count != null ? count : -1L, aql.getPaginationParam() != null ? aql.getPaginationParam().getFrom() : 0);
            } catch (IOException e) {
                logger.error(String.format("Was not able to execute query : %s", aqlQuery), e);
                throw new ArangoDBException(e.getMessage());
            }
        } catch (ArangoDBException ex) {
            logger.error(String.format("Was not able to execute query : %s", aqlQuery), ex);
            switch (ex.getErrorNum()) {
                case 32 -> throw new LimitExceededException("Query size limit exceeded");
                case 1501 -> throw new InvalidRequestException("Invalid query");
                default -> throw ex;
            }
        }
    }

    public static Paginated<NormalizedJsonLd> queryDocuments(ArangoDatabase db, AQLQuery aqlQuery, Double maxMemoryForQuery) {
        return new Paginated<>(queryDocuments(db, aqlQuery, null, maxMemoryForQuery));
    }

    public static PaginatedStream<NormalizedJsonLd> queryDocumentsAsStream(ArangoDatabase db, AQLQuery aqlQuery, Double maxMemoryForQuery) {
        return queryDocuments(db, aqlQuery, null, maxMemoryForQuery);
    }
}
