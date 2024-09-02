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
import org.marmotgraph.core.api.AbstractTest;
import org.marmotgraph.core.api.instances.TestContext;
import org.marmotgraph.core.api.v3.TypesV3;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class DefineTypeTest extends AbstractTest {

    private final TypesV3 types;
    public final NormalizedJsonLd typeDefinition = createTypeDefinition();

    public DefineTypeTest(TestContext testContext, TypesV3 types) {
        super(testContext);
        this.types = types;
    }

    public final String typeName = "https://marmotgraph.org/Test";

    NormalizedJsonLd createTypeDefinition() {
        NormalizedJsonLd payload = new NormalizedJsonLd();
        payload.addProperty("http://foo", "bar");
        return payload;
    }

    @Override
    protected void setup() {

    }

    @Override
    protected void run() {
        types.createTypeDefinition(typeDefinition, true, typeName);
    }
}