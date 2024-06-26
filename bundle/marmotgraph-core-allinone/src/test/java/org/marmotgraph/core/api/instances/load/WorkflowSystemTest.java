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

package org.marmotgraph.core.api.instances.load;

import org.marmotgraph.commons.IdUtils;
import org.marmotgraph.commons.jsonld.IndexedJsonLdDoc;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.ReleaseStatus;
import org.marmotgraph.commons.model.Result;
import org.marmotgraph.commons.params.ReleaseTreeScope;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.model.ExposedStage;
import org.marmotgraph.core.api.testutils.TestDataFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowSystemTest extends AbstractInstancesLoadTest {

    @Autowired
    private InstancesV3 instances;

    @Autowired
    private IdUtils idUtils;


    @Test
    void testReleaseAndUnreleaseAndReReleaseInstance() {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG);
        JsonLdId id = instance.getBody().getData().id();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        instances.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = instances.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        instances.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = instances.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        instances.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatusRerelease = instances.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatusRerelease.getBody().getData().getReleaseStatus());
    }

    @Test
    void testInsertAndDeleteInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG);
        JsonLdId id = instance.getBody().getData().id();

        //When
        ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(id));

        //Then
        assertEquals(HttpStatus.OK, resultResponseEntity.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());

    }

    @Test
    void testInsertAndUpdateInstance() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);

        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG);
        JsonLdId id = instance.getBody().getData().id();

        //When
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://marmotgraph.org/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), DEFAULT_RESPONSE_CONFIG);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://marmotgraph.org/fooE", String.class));
    }

    @Disabled("Failing")
    @Test
    void testFullCycle() throws IOException {
        //Given
        JsonLdDoc payload = TestDataFactory.createTestData(smallPayload, true, 0, null);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG);
        JsonLdId id = instance.getBody().getData().id();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        //Update
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://marmotgraph.org/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), DEFAULT_RESPONSE_CONFIG);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://marmotgraph.org/fooE", String.class));

        //When
        //Release
        instances.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = instances.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        //When
        //Unrelease
        instances.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = instances.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        //When
        //Delete
        ResponseEntity<Result<Void>> resultResponseEntityDeleted = instances.deleteInstance(idUtils.getUUID(id));
        //Then
        assertEquals(HttpStatus.OK, resultResponseEntityDeleted.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());
    }

}
