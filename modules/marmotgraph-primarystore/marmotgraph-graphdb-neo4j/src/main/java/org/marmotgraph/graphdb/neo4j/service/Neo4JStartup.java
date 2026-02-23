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
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.graphdb.neo4j.Neo4J;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Neo4J
@Component
@AllArgsConstructor
public class Neo4JStartup {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Driver driver;
    private final int maxRetries = 20;
    private final int sleepTimeInSecs = 2;

    @Bean
    public ApplicationRunner checkNeo4jAtStartup() {
        return args -> {
            boolean connected = false;
            int retries = 0;
            while (!connected && retries < maxRetries) {
                try (Session session = driver.session()) {
                    session.run("RETURN 1").consume();
                    connected = true;
                } catch (Exception e) {
                    retries++;
                    logger.warn("Neo4j not available yet – retrying {} more times ", maxRetries-retries);
                    Thread.sleep(sleepTimeInSecs*1000);
                }
            }
            if (!connected) {
                throw new IllegalStateException("Neo4j not reachable - giving up");
            }
            logger.info("Neo4j is available!");
            createConstraints();
        };
    }


    private void createConstraints() {
        try (Session session = driver.session()) {
            for (DataStage stage : DataStage.values()) {
                if (stage != DataStage.NATIVE) {
                    String stageLabel = Neo4JCommons.getStageLabel(stage, "");
                    session.run(String.format("CREATE INDEX %s_id_index IF NOT EXISTS FOR (i:%s) ON (i._id)", stageLabel.toLowerCase(), stageLabel));
                    session.run(String.format("CREATE INDEX %s_lifecycleId_index IF NOT EXISTS FOR (i:%s) ON (i._lifecycleId)", stageLabel.toLowerCase(), stageLabel));
                }
            }
        }
    }
}