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

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.marmotgraph.commons.exception.InvalidRequestException;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.relations.IncomingRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class Neo4jService {

    private transient final Logger logger = LoggerFactory.getLogger(getClass());
    private final PayloadSplitter payloadSplitter;
    private final Neo4jClient neo4jClient;

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

    private String handleType(NormalizedJsonLd json) {
        return json.types().stream().filter(this::isValidURI).map(t -> {
            String[] splitted = t.split("/");
            String lastItem = splitted[splitted.length - 1];
            splitted[splitted.length - 1] = null;
            //We only allow default characters and numbers for the type as passed
            // into the DB but no special characters nor spaces or similar. This should be enough for
            // preventing query injection
            if (lastItem.matches("[a-zA-Z0-9]*")) {
                String nameSpace = Arrays.stream(splitted).filter(Objects::nonNull).collect(Collectors.joining("/"));
                return lastItem; //TODO add namespace prefix
            } else {
                return null;
            }
        }).collect(Collectors.joining(":"));
    }

    @Transactional
    public void upsert(UUID uuid, DataStage stage, SpaceName spaceName, NormalizedJsonLd json, Set<IncomingRelation> incomingRelations) {
        String type = handleType(json);
        if (type.isBlank()) {
            throw new InvalidRequestException("The payload doesn't contain any valid @type");
        }
        PayloadSplitter.PayloadSplit splitEntity = payloadSplitter.createEntities(json, uuid);
        Stream<Neo4jClient.RunnableSpec> queries = buildUpsertCypherQuery(uuid, spaceName, splitEntity, stage, incomingRelations);
        queries.forEach(Neo4jClient.RunnableSpec::run);
    }

    public void delete(UUID uuid, DataStage stage) {
        String template = """
        MATCH (n${stage} {${lifecycleIdAlias}: $lifecycleId})
        DETACH DELETE n
        """;
        String query = StringSubstitutor.replace(template,
                Map.of(
                        "stage", getStageLabel(stage),
                        "lifecycleIdAlias", NormalizedJsonLd.LIFECYCLE_ALIAS
                ));
        logger.info("Preparing Neo4j query to delete all instances in {} with lifecycle id {}", stage.name(), uuid);
        neo4jClient.query(query).bind(uuid.toString()).to("lifecycleId").run();
    }

    Stream<Neo4jClient.RunnableSpec> buildUpsertCypherQuery(UUID lifeCycleId, SpaceName spaceName, PayloadSplitter.PayloadSplit splitEntity, DataStage stage, Set<IncomingRelation> incomingRelations) {
        StringSubstitutor stringSubstitutor = new StringSubstitutor(Map.of(
                "stage", getStageLabel(stage),
                "lifecycleIdAlias", NormalizedJsonLd.LIFECYCLE_ALIAS
        ));
        Neo4jClient.RunnableSpec clearInstances = neo4jClient.query(stringSubstitutor.replace( """
                MATCH (e${stage} {${lifecycleIdAlias}: $lifecycleId})
                DETACH DELETE e
         """));

        Neo4jClient.RunnableSpec clearRelations = neo4jClient.query(stringSubstitutor.replace("""
                MATCH ()-[r${stage} {${lifecycleIdAlias}: $lifecycleId}]->()
                DELETE r;
                """));


        Stream<Neo4jClient.RunnableSpec> clearStream = Stream.of(
                clearInstances.bind(lifeCycleId.toString()).to("lifecycleId"),
                clearRelations.bind(lifeCycleId.toString()).to("lifecycleId")
        );
        return Stream.concat(clearStream, Stream.concat(instancesStream(splitEntity, spaceName, stage), Stream.concat(incomingRelationStream(incomingRelations, stage), outgoingRelationStream(splitEntity, spaceName, stage))));
    }

    private Stream<Neo4jClient.RunnableSpec> incomingRelationStream(Set<IncomingRelation> incomingRelations, DataStage stage) {
        final String template = new StringSubstitutor(Map.of(
                "idAlias", NormalizedJsonLd.ID_ALIAS,
                "lifecycleIdAlias", NormalizedJsonLd.LIFECYCLE_ALIAS,
                "stage", getStageLabel(stage)
        )).replace("""
                    MATCH (a${stage} {${idAlias}: $sourceId})
                    MATCH (b${stage} {${idAlias}: $targetId})
                    MERGE (a)-[a_b${stage} {orderNumber: $orderNumber, ${lifecycleIdAlias}: $lifecycleId, property: $property}]->(b)
                """);
        return incomingRelations.stream().map(r -> {
            Neo4jClient.RunnableSpec query = neo4jClient.query(template);
            query.bindAll(Map.of(
                    "sourceId", r.from(),
                    "targetId", r.to().toString(),
                    "lifecycleId", r.lifecycleId().toString(),
                    "orderNumber", r.orderNumber(),
                    "property", r.property()
            ));
            return query;
        });
    }



    private Stream<Neo4jClient.RunnableSpec> outgoingRelationStream(PayloadSplitter.PayloadSplit splitEntity, SpaceName spaceName, DataStage stage) {
        final String relationTemplate = """
                    MATCH (a${stage} {${idAlias}: $sourceId})
                    MATCH (b${stage} {${idAlias}: $targetId})
                    CREATE (a)-[a_b${stage} {orderNumber: $orderNumber, ${lifecycleIdAlias}: $lifecycleId, property: $property}]->(b)
                    """;
        return splitEntity.getRelations().stream().map(relation -> {
            String q = new StringSubstitutor(Map.of(
                    "idAlias", NormalizedJsonLd.ID_ALIAS,
                    "lifecycleIdAlias", NormalizedJsonLd.LIFECYCLE_ALIAS,
                    "stage", getStageLabel(stage)
            )).replace(relationTemplate);
            Neo4jClient.RunnableSpec query = neo4jClient.query(q);
            logger.info("Preparing Neo4j query (relation {} from {} to {}): {}", relation.getRelationName(), relation.getFrom(), relation.getTo(), q);
            return query.bindAll(Map.of(
                    "sourceId", relation.getFrom(),
                    "targetId", relation.getTo(),
                    "lifecycleId", relation.getLifecycleId().toString(),
                    "orderNumber", relation.getOrderNumber(),
                    "property", relation.getRelationName()
            ));
        });
    }

    private String sanitizeLabel(String label){
        if(label!=null) {
            while (label.startsWith("_")) {
                label = label.substring(1);
            }
            return label.replace("-", "_").replaceAll("[^a-zA-Z0-9_]", "");
        }
        return null;
    }

    private String getStageLabel(DataStage stage) {
        return String.format(":_STG_%s", stage.getAbbreviation());
    }

    private Stream<Neo4jClient.RunnableSpec> instancesStream(PayloadSplitter.PayloadSplit splitEntity, SpaceName spaceName, DataStage stage) {
        final String template = """
                  CREATE (i${extra}${stage}${spaceName}${types} $payload)
        """;
        return splitEntity.getInstances().stream().map(instance -> {
            Collection<String> types = instance.types();
            String q = new StringSubstitutor(Map.of(
                    "stage", getStageLabel(stage),
                    "spaceName", spaceName != null && !spaceName.getName().isBlank() ? String.format(":_SPC_%s", sanitizeLabel(spaceName.getName())) : "",
                    "extra", instance.isEmbedded() ? ":embedded" : "",
                    "types", types.isEmpty() ? "" : String.format(":%s", types.stream().map(this::sanitizeLabel).collect(Collectors.joining(":")))
            )).replace(template);
            logger.info("Preparing Neo4j query (instance {}): {}", instance.getId(), q);
            Neo4jClient.RunnableSpec query = neo4jClient.query(q);
            Map<String, Object> payload = instance.getPayload();
            payload.put(NormalizedJsonLd.ID_ALIAS, instance.getId());
            payload.put(NormalizedJsonLd.LIFECYCLE_ALIAS, instance.getLifecycleId().toString());
            payload.put(NormalizedJsonLd.SPACE_ALIAS, spaceName!=null ? spaceName.getName() : null);
            return query.bind(payload).to("payload").bind(instance.getId()).to("id");
        });
    }


}
