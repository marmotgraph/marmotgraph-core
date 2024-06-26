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

package org.marmotgraph.core.api.types.test;

import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.ExtendedResponseConfiguration;
import org.marmotgraph.commons.model.PaginatedResult;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.model.external.types.TypeInformation;
import org.marmotgraph.core.api.AbstractTest;
import org.marmotgraph.core.api.instances.TestContext;
import org.marmotgraph.core.api.testutils.TestDataFactory;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.api.v3.TypesV3;
import org.marmotgraph.core.model.ExposedStage;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class GetTypesForInvitation extends AbstractTest {
    private final TypesV3 types;
    private final InstancesV3 instances;
    private final boolean withProperties;
    private final boolean withIncomingLinks;
    public PaginatedResult<TypeInformation> response;
    private final String space;

    public GetTypesForInvitation(TestContext testContext, boolean withProperties, boolean withIncomingLinks, TypesV3 types, String space, InstancesV3 instances) {
        super(testContext);
        this.instances = instances;
        this.types = types;
        this.withProperties = withProperties;
        this.withIncomingLinks = withIncomingLinks;
        this.space = space;
    }

    @Override
    protected void setup() {
        // We create a new instance so the type is implicitly created.
        final ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(TestDataFactory.createTestData(smallPayload, 0, true), "functionalityTest", new ExtendedResponseConfiguration());
        instances.inviteUserForInstance(testContext.getIdUtils().getUUID(instance.getBody().getData().id()), USER_ID);
    }

    @Override
    protected void run() {
        response = this.types.listTypes(ExposedStage.IN_PROGRESS, space, withProperties, withIncomingLinks, new PaginationParam());
    }
}
