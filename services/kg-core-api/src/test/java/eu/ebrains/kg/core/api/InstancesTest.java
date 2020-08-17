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

package eu.ebrains.kg.core.api;

import com.netflix.discovery.EurekaClient;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.IngestConfiguration;
import eu.ebrains.kg.commons.model.ResponseConfiguration;
import eu.ebrains.kg.commons.model.Result;
import eu.ebrains.kg.docker.SpringDockerComposeRunner;
import eu.ebrains.kg.test.TestObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111"})
public class InstancesTest {
    @Autowired
    EurekaClient discoveryClient;

    @Autowired
    Instances instances;

    @Before
    public void setup() {
        new SpringDockerComposeRunner(discoveryClient, Arrays.asList("arango"), "kg-ids", "kg-jsonld", "kg-primarystore", "kg-permissions", "kg-indexing", "kg-graphdb-sync").start();
    }

    @Test
    public void createNewInstance() {
        //Given
        NormalizedJsonLd homer = TestObjectFactory.createJsonLd(TestObjectFactory.SIMPSONS, "homer.json");

        //When
        ResponseEntity<Result<NormalizedJsonLd>> result = instances.createNewInstance(homer, TestObjectFactory.SIMPSONS.getName(), new ResponseConfiguration(), new IngestConfiguration(), null);

        //Then
        Assert.assertNotNull(result);
    }
}