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

import com.google.gson.Gson;
import eu.ebrains.kg.KgCoreAllInOne;
import eu.ebrains.kg.commons.AuthContext;
import eu.ebrains.kg.commons.IdUtils;
import eu.ebrains.kg.commons.jsonld.IndexedJsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdDoc;
import eu.ebrains.kg.commons.jsonld.JsonLdId;
import eu.ebrains.kg.commons.jsonld.NormalizedJsonLd;
import eu.ebrains.kg.commons.model.*;
import eu.ebrains.kg.commons.params.ReleaseTreeScope;
import eu.ebrains.kg.commons.serviceCall.ToAuthentication;
import eu.ebrains.kg.core.model.ExposedStage;
import eu.ebrains.kg.metrics.TestInformation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = KgCoreAllInOne.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {"eu.ebrains.kg.arango.pwd=changeMe", "eu.ebrains.kg.arango.port=9111", "eu.ebrains.kg.develop=true"})
public class PerformanceTest {

    private static Path averageNoLink;
    private static Path averageNoLinkUnnormalized;
    private static Path smallNoLink;
    private static Path bigNoLink;

    private static int smallBatchInsertion = 10;
    private static int batchInsertion = 100;

    @MockBean
    AuthContext authContext;

    @MockBean
    TestInformation testInformation;

    @Autowired
    private Instances instances;

    @Autowired
    private Extra extra;

    @Autowired
    private Releases releases;

    @Autowired
    private IdUtils idUtils;

    @Autowired
    private ToAuthentication authenticationSvc;

    String testRunId;

    @Before
    public void setup() {
        Mockito.doReturn(authenticationSvc.getUserWithRoles()).when(authContext).getUserWithRoles();
        testRunId = UUID.randomUUID().toString();
        Mockito.doReturn(testRunId).when(testInformation).getRunId();
    }

    PaginationParam EMPTY_PAGINATION = new PaginationParam();
    ResponseConfiguration DEFAULT_RESPONSE_CONFIG = new ResponseConfiguration();
    IngestConfiguration DEFAULT_INGEST_CONFIG = new IngestConfiguration().setNormalizePayload(false);

    String type = "https://core.kg.ebrains.eu/TestPayload";

    @BeforeClass
    public static void loadResources() throws URISyntaxException {
        averageNoLink = Paths.get(PerformanceTest.class.getClassLoader().getResource("average_size_payload_no_link.json").toURI());
        averageNoLinkUnnormalized = Paths.get(PerformanceTest.class.getClassLoader().getResource("average_size_payload_no_link_unnormalized.json").toURI());
        smallNoLink = Paths.get(PerformanceTest.class.getClassLoader().getResource("small_size_payload_no_link.json").toURI());
        bigNoLink = Paths.get(PerformanceTest.class.getClassLoader().getResource("big_size_payload_no_link.json").toURI());
    }

    private JsonLdDoc getDoc(Path path) throws IOException {
        return new JsonLdDoc(new Gson().fromJson(Files.lines(path).collect(Collectors.joining("\n")), LinkedHashMap.class));
    }

    private List<NormalizedJsonLd> getAllInstancesFromInProgress(ExposedStage stage) {
        return this.instances.getInstances(stage, type, null, DEFAULT_RESPONSE_CONFIG, EMPTY_PAGINATION).getData();
    }

    // INSERTION
    @Test
    public void testInsertSingleAverageNoLink() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(averageNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            results.add(instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertSingleSmallNoLink() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(smallNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            results.add(instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertSingleSmallNoLinkInferenceDeferredNormalize() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(smallNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            results.add(instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, new IngestConfiguration().setDeferInference(true), null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

        System.out.println("Trigger inference");
        extra.triggerDeferredInference(true, "test");
        System.out.println("Inference handled");

    }

    @Test
    public void testInsertSingleSmallNoLinkInferenceDeferred() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(smallNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            results.add(instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, new IngestConfiguration().setDeferInference(true).setNormalizePayload(false), null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }
        Instant start = Instant.now();
        System.out.println("Trigger inference");
        extra.triggerDeferredInference(true, "test");
        Instant end = Instant.now();
        System.out.println(String.format("Inference handled in %d ms", Duration.between(start, end).toMillis()));
    }




    @Test
    public void testInsertSingleAverageNoLinkUnnormalized() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(averageNoLinkUnnormalized);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            results.add(instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertBigNoLink() throws IOException {

        //Given
        JsonLdDoc payload = getDoc(bigNoLink);

        //When
        List<ResponseEntity<Result<NormalizedJsonLd>>> results = new ArrayList<>();
        for (int i = 0; i < batchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            results.add(instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null));
        }

        //Then
        for (int i = 0; i < results.size(); i++) {
            System.out.println(String.format("Result %d: %d ms", i, results.get(i).getBody().getDurationInMs()));
        }

    }

    @Test
    public void testInsertBigNoLinkFullParallelism() throws IOException, InterruptedException {

        //Given
        JsonLdDoc payload = getDoc(bigNoLink);

        ExecutorService executorService = Executors.newFixedThreadPool(6);
        //When
        for (int i = 0; i < batchInsertion; i++) {
            executorService.execute(() -> {
                ResponseEntity<Result<NormalizedJsonLd>> result = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);
                System.out.println(String.format("Result: %d ms", result.getBody().getDurationInMs()));
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

    @Test
    public void testInsertSingleSmallNoLinkFullParallelism() throws IOException, InterruptedException {

        //Given
        JsonLdDoc payload = getDoc(smallNoLink);

        ExecutorService executorService = Executors.newFixedThreadPool(6);
        //When
        for (int i = 0; i < batchInsertion; i++) {
            executorService.execute(() -> {
                ResponseEntity<Result<NormalizedJsonLd>> result = instances.createNewInstance(payload, "test",DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
                System.out.println(String.format("Result: %d ms", result.getBody().getDurationInMs()));
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

    // DELETION
    @Test
    public void testDeleteSingleAverageNoLink() throws IOException {
        //Given
        testInsertSingleAverageNoLink();
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()), null);
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
    }

    @Test
    public void testDeleteSingleAverageNoLinkFullParallelism() throws IOException, InterruptedException {
        //Given
        testInsertSingleAverageNoLink();
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);

        //When
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            int finalI = i;
            executorService.execute(() -> {
                ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).getId()), null);
                System.out.println(String.format("Result %d: %d ms", finalI, resultResponseEntity.getBody().getDurationInMs()));
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
    }

    // RELEASING
    @Test
    public void testReleaseSingleAverageNoLink() throws IOException {
        //Given
        testInsertSingleAverageNoLink();
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            IndexedJsonLdDoc from = IndexedJsonLdDoc.from(allInstancesFromInProgress.get(i));
            ResponseEntity<Result<Void>> resultResponseEntity = releases.releaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()), from.getRevision());
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d releases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

    @Test
    public void testReleaseSingleAverageNoLinkfullParallelism() throws IOException, InterruptedException {
        //Given
        testInsertSingleAverageNoLink();
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);

        //When
        long startTime = new Date().getTime();
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            int finalI = i;
            executorService.execute(() -> {
                IndexedJsonLdDoc from = IndexedJsonLdDoc.from(allInstancesFromInProgress.get(finalI));
                ResponseEntity<Result<Void>> resultResponseEntity = releases.releaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(finalI).getId()), from.getRevision());
                System.out.println(String.format("Result %d: %d ms", finalI, resultResponseEntity.getBody().getDurationInMs()));
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.HOURS);
        System.out.println(String.format("Total time for %d releases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }


    @Test
    public void testReleaseSingleBigNoLink() throws IOException {
        //Given
        JsonLdDoc payload = getDoc(bigNoLink);
        List<ResponseEntity<Result<NormalizedJsonLd>>> l = new ArrayList<>();
        for (int i = 0; i < smallBatchInsertion; i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);
            l.add(instance);
        }

        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < l.size(); i++) {
            JsonLdId id = l.get(i).getBody().getData().getId();
            IndexedJsonLdDoc from = IndexedJsonLdDoc.from(l.get(i).getBody().getData());
            ResponseEntity<Result<Void>> resultResponseEntity = releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d releases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

    @Test
    public void testUnReleaseSingleAverageNoLink() throws IOException {
        //Given
        testInsertSingleAverageNoLink();
        List<NormalizedJsonLd> allInstancesFromInProgress = getAllInstancesFromInProgress(ExposedStage.IN_PROGRESS).subList(0, batchInsertion);
        //When
        long startTime = new Date().getTime();
        for (int i = 0; i < allInstancesFromInProgress.size(); i++) {
            Mockito.doReturn(i).when(testInformation).getExecutionNumber();
            ResponseEntity<Result<Void>> resultResponseEntity = releases.unreleaseInstance(idUtils.getUUID(allInstancesFromInProgress.get(i).getId()));
            System.out.println(String.format("Result %d: %d ms", i, resultResponseEntity.getBody().getDurationInMs()));
        }
        System.out.println(String.format("Total time for %d unreleases: %d ms", batchInsertion, new Date().getTime() - startTime));
    }

    @Test
    public void testRealeaseAndUnreleaseAndReReleaseInstance() throws IOException {
        //Given
        JsonLdDoc payload = getDoc(smallNoLink);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);
        JsonLdId id = instance.getBody().getData().getId();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        releases.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatusRerelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatusRerelease.getBody().getData().getReleaseStatus());
    }

    @Test
    public void testInsertAndDeleteInstance() throws  IOException {
        //Given
        JsonLdDoc payload = getDoc(smallNoLink);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);
        JsonLdId id = instance.getBody().getData().getId();

        //When
        ResponseEntity<Result<Void>> resultResponseEntity = instances.deleteInstance(idUtils.getUUID(id), null);

        //Then
        assertEquals(HttpStatus.OK, resultResponseEntity.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());

    }

    @Test
    public void testInsertAndUpdateInstance() throws IOException {
        //Given
        JsonLdDoc payload = getDoc(smallNoLink);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG, null);
        JsonLdId id = instance.getBody().getData().getId();

        //When
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://core.kg.ebrains.eu/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), false, DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://core.kg.ebrains.eu/fooE", String.class));
    }

    @Ignore("Failing")
    @Test
    public void testFullCycle() throws  IOException {
        //Given
        JsonLdDoc payload = getDoc(smallNoLink);
        ResponseEntity<Result<NormalizedJsonLd>> instance = instances.createNewInstance(payload, "test", DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);
        JsonLdId id = instance.getBody().getData().getId();
        IndexedJsonLdDoc from = IndexedJsonLdDoc.from(instance.getBody().getData());

        //When
        //Update
        JsonLdDoc doc = new JsonLdDoc();
        doc.addProperty("https://core.kg.ebrains.eu/fooE", "fooEUpdated");
        ResponseEntity<Result<NormalizedJsonLd>> resultResponseEntity = instances.contributeToInstancePartialReplacement(doc, idUtils.getUUID(id), false, DEFAULT_RESPONSE_CONFIG, DEFAULT_INGEST_CONFIG,  null);

        //Then
        assertEquals("fooEUpdated", resultResponseEntity.getBody().getData().getAs("https://core.kg.ebrains.eu/fooE", String.class));

        //When
        //Release
        releases.releaseInstance(idUtils.getUUID(id), from.getRevision());
        ResponseEntity<Result<ReleaseStatus>> releaseStatus = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.RELEASED.getReleaseStatus(), releaseStatus.getBody().getData().getReleaseStatus());

        //When
        //Unrelease
        releases.unreleaseInstance(idUtils.getUUID(id));
        ResponseEntity<Result<ReleaseStatus>> releaseStatusAfterUnrelease = releases.getReleaseStatus(idUtils.getUUID(id), ReleaseTreeScope.TOP_INSTANCE_ONLY);

        //Then
        assertEquals(ReleaseStatus.UNRELEASED.getReleaseStatus(), releaseStatusAfterUnrelease.getBody().getData().getReleaseStatus());

        //When
        //Delete
        ResponseEntity<Result<Void>> resultResponseEntityDeleted = instances.deleteInstance(idUtils.getUUID(id), null);
        //Then
        assertEquals(HttpStatus.OK, resultResponseEntityDeleted.getStatusCode());

        ResponseEntity<Result<NormalizedJsonLd>> instanceById = instances.getInstanceById(idUtils.getUUID(id), ExposedStage.IN_PROGRESS, DEFAULT_RESPONSE_CONFIG);

        assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode());
    }

}
