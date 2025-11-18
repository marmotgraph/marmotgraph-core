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

package org.marmotgraph.graphdb.arango.ingestion.controller;

import org.junit.jupiter.api.Disabled;
import org.marmotgraph.graphdb.arango.model.ArangoDocumentReference;
import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.TypeUtils;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.graphdb.arango.commons.model.ArangoInstance;
import org.marmotgraph.test.JsonAdapter4Test;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@Disabled
class StructureSplitterTest {

    @Test
    void extractRelations() {
        JsonAdapter jsonAdapter = new JsonAdapter4Test();
        String testJson = """
                {
                   "@id": "https://kg.ebrains.eu/api/instances/minds/helloWorld", 
                   "https://schema.hbp.eu/foo": "bar",
                   "https://schema.hbp.eu/embedded": {
                      "https://schema.hbp.eu/embeddedKey": "embeddedValue"
                   }, 
                   "https://schema.hbp.eu/link": {
                      "@id": "https://kg.ebrains.eu/api/instances/minds/related"
                   }
                }
                """;
        NormalizedJsonLd jsonld = jsonAdapter.fromJson(testJson, NormalizedJsonLd.class);
        StructureSplitter structureSplitter = new StructureSplitter(new IdUtils("https://kg.ebrains.eu/api/instances/"), new TypeUtils(jsonAdapter));
        List<ArangoInstance> indexedNormalizedJsonLdDocs = structureSplitter.extractRelations(ArangoDocumentReference.fromArangoId("minds/"+ UUID.randomUUID(), false), jsonld);
        System.out.println(indexedNormalizedJsonLdDocs);
    }
}