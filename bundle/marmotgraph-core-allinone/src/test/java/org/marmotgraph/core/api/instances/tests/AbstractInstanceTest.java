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

package org.marmotgraph.core.api.instances.tests;

import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.core.api.AbstractTest;
import org.marmotgraph.core.api.instances.TestContext;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.api.testutils.TestDataFactory;
import org.springframework.http.ResponseEntity;

public abstract class AbstractInstanceTest extends AbstractTest {

    protected final InstancesV3 instances;

    public AbstractInstanceTest(TestContext testContext, InstancesV3 instances) {
        super(testContext);
        this.instances = instances;
    }

    protected NormalizedJsonLd createInstanceWithServerDefinedUUID(int iteration) {
        JsonLdDoc originalDoc = TestDataFactory.createTestData(smallPayload, iteration, true);
        ResponseEntity<Result<NormalizedJsonLd>> response = instances.createNewInstance(originalDoc, "functionalityTest", defaultResponseConfiguration);
        return response.getBody().getData();
    }



}
