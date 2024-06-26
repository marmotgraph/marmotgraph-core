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

package org.marmotgraph.core.api.queries.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.PaginatedStreamResult;
import org.marmotgraph.core.api.AbstractTest;
import org.marmotgraph.core.api.instances.TestContext;
import org.marmotgraph.core.api.testutils.TestDataFactory;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.api.v3.QueriesV3;
import org.marmotgraph.core.model.ExposedStage;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("java:S2187") //We don't add "tests" to these classes because they are test abstractions and are used in other tests
public class TestSimpleQueryTest extends AbstractTest {

    private final QueriesV3 queries;
    private final InstancesV3 instances;
    private final boolean release;
    public NormalizedJsonLd instanceA;
    public NormalizedJsonLd instanceArelated;
    public NormalizedJsonLd instanceB;
    public ExposedStage stage;

    public String relationToArelated = "http://test/aRelated";
    public String relationToB = "http://test/b";

    public JsonLdDoc query;

    public PaginatedStreamResult<? extends Map<?,?>> response;
    public final String nameOfRoot = "http://test/nameOfRoot";
    public final String nameOfARel = "http://test/nameOfArelated";
    public final String nameOfB = "http://test/nameOfB";

    public String testQuery = "{\n" +
            "  \"@context\": {\n" +
            "    \"query\": \"https://core.kg.ebrains.eu/vocab/query/\",\n" +
            "    \"schema\": \"http://schema.org/\"\n" +
            "  },\n" +
            "  \"query:meta\": {\n" +
            "    \"query:name\": \"Test Query\",\n" +
            "    \"query:alias\": \"testQuery\",\n" +
            "    \"query:type\": \""+ TestDataFactory.TEST_TYPE+"\"\n" +
            "  },\n" +
            "  \"query:structure\": [\n" +
            "    {\n" +
            "      \"query:propertyName\": {\"@id\":  \""+nameOfRoot+"\"},\n"+
            "      \"query:path\": {\n" +
            "        \"@id\": \"schema:name\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"query:propertyName\": {\"@id\":  \""+nameOfARel+"\"},\n"+
            "      \"query:singleValue\": \"FIRST\",\n" +
            "      \"query:path\": [" +
            "      {\n" +
            "        \"@id\": \""+relationToArelated+"\"\n" +
            "      },\n" +
            "      {\n" +
            "\"@id\": \"schema:name\"\n" +
            "      }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"query:propertyName\": {\"@id\":  \""+nameOfB+"\"},\n"+
            "      \"query:singleValue\": \"FIRST\",\n" +
            "      \"query:path\": [" +
            "      {\n" +
            "        \"@id\": \""+relationToB+"\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"@id\": \"schema:name\"\n" +
            "      }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";


    public TestSimpleQueryTest(TestContext testContext, QueriesV3 queries, InstancesV3 instances, ExposedStage stage, boolean release)  throws IOException
    {
        super(testContext);
        this.instances = instances;
        this.queries = queries;
        this.query = new JsonLdDoc(new ObjectMapper().readValue(this.testQuery, LinkedHashMap.class));
        this.stage = stage;
        this.release = release;
    }

    private NormalizedJsonLd createTestInstance(JsonLdDoc doc, String space){
        return instances.createNewInstance(doc, space, defaultResponseConfiguration).getBody().getData();
    }

    @Override
    protected void setup() {
        JsonLdDoc docB = TestDataFactory.createTestData(smallPayload, "b", true);
        instanceB = createTestInstance(docB, "b");
        JsonLdDoc docArelated = TestDataFactory.createTestData(smallPayload, "aRelated", true);
        instanceArelated = createTestInstance(docArelated, "a");
        JsonLdDoc docA = TestDataFactory.createTestData(smallPayload, "a", true);
        docA.put(relationToArelated, instanceArelated.id());
        docA.put(relationToB, instanceB.id());
        instanceA = createTestInstance(docA, "a");
        if(release) {
            instances.releaseInstance(testContext.getIdUtils().getUUID(instanceA.id()), null);
            instances.releaseInstance(testContext.getIdUtils().getUUID(instanceB.id()), null);
            instances.releaseInstance(testContext.getIdUtils().getUUID(instanceArelated.id()), null);
        }
    }

    @Override
    protected void run() {
         response = queries.runDynamicQuery(this.query, defaultPaginationParam, stage, null, null, new HashMap<>());
    }
}
