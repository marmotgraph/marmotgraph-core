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

import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.core.api.instances.TestContext;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.model.ExposedStage;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class GetInstanceByIdTest extends AbstractInstanceTest {

    public ResponseEntity<Result<NormalizedJsonLd>> response;
    public NormalizedJsonLd originalInstance;

    public GetInstanceByIdTest(TestContext testContext, InstancesV3 instances) {
        super(testContext, instances);
    }

    @Override
    protected void setup() {
        originalInstance = createInstanceWithServerDefinedUUID(0);
    }

    protected void run(){
        response = instances.getInstanceById(testContext.getIdUtils().getUUID(originalInstance.id()), ExposedStage.IN_PROGRESS, defaultResponseConfiguration);
    }

}
