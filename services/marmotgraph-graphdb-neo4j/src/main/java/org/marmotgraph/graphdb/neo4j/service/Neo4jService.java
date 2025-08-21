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

package org.marmotgraph.graphdb.neo4j.service;

import lombok.AllArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.marmotgraph.commons.exception.InvalidRequestException;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class Neo4jService {

    private transient final Logger logger = LoggerFactory.getLogger(getClass());
    private final PayloadSplitter payloadSplitter;
    private final Neo4jClient neo4jClient;

    public final static String UUID_ALIAS = "mg_uuid";
    public final static String LIFECYCLE_ALIAS = "mg_lifecycleId";

    private String prefix(DataStage stage) {
        return String.format("mg_%s", stage.getAbbreviation());
    }

    private boolean isValidURI(String uriStr) {
        if (uriStr == null || uriStr.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(uriStr);
            return uri.getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private String handleType(NormalizedJsonLd json){
        return json.types().stream().filter(this::isValidURI).map(t -> {
            String[] splitted = t.split("/");
            String lastItem = splitted[splitted.length-1];
            splitted[splitted.length-1] = null;
            //We only allow default characters and numbers for the type as passed
            // into the DB but no special characters nor spaces or similar. This should be enough for
            // preventing query injection
            if(lastItem.matches("[a-zA-Z0-9]*")){
                String nameSpace = Arrays.stream(splitted).filter(Objects::nonNull).collect(Collectors.joining("/"));
                return lastItem; //TODO add namespace prefix
            }
            else{
                return null;
            }
        }).collect(Collectors.joining(":"));
    }

    public void upsert(UUID uuid, DataStage stage, NormalizedJsonLd json) {
        String type = handleType(json);
        if(type.isBlank()){
            throw new InvalidRequestException("The payload doesn't contain any valid @type");
        }
        PayloadSplitter.PayloadSplit splitEntity = payloadSplitter.createEntities(json, uuid);
        Neo4jClient.RunnableSpec query = buildUpsertCypherQuery(splitEntity, stage);
        ResultSummary summary = query.run();
    }

    public void delete(UUID uuid, DataStage stage){
        String template = "MATCH (n:${stage} {%{lifecycleIdAlias}: '${lifecycleId}'}) DELETE n";
        String query = StringSubstitutor.replace(template,
                Map.of(
                        "stage", stage.getAbbreviation(),
                        "lifecycleIdAlias", LIFECYCLE_ALIAS,
                        "lifecycleId", uuid.toString()
                ));
        neo4jClient.query(query).run();
    }


    Neo4jClient.RunnableSpec buildUpsertCypherQuery(PayloadSplitter.PayloadSplit splittedEntity, DataStage stage) {
        StringBuilder builder = new StringBuilder();
        String template = "CREATE (${alias}${extra}:${stage} $payload${index})\n";
        Map<String, Object> bindParams = new HashMap<>();
        for (int i = 0; i < splittedEntity.getInstances().size(); i++) {
            PayloadSplitter.Instance instance = splittedEntity.getInstances().get(i);
            Map<String, Object> params = Map.of(
                    "alias", instance.getAlias(),
                    "stage", prefix(stage),
                    "index", i,
                    "extra", instance.isUnresolved() ? ":unresolved" : instance.isEmbedded() ? ":embedded" : ""
            );
            builder.append(new StringSubstitutor(params).replace(template));
            bindParams.put(String.format("payload%s", i), instance.getPayload());
        }
        String queryString = builder.toString();
        logger.info(queryString);
        Neo4jClient.RunnableSpec query = neo4jClient.query(queryString);
        for (String key : bindParams.keySet()) {
             query = query.bind(bindParams.get(key)).to(key);
        }
        return query;
    }


}
