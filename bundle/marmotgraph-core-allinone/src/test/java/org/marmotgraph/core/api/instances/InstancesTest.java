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

package org.marmotgraph.core.api.instances;

import org.marmotgraph.commons.exception.ForbiddenException;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.*;
import org.marmotgraph.commons.permission.roles.RoleMapping;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.marmotgraph.core.api.instances.tests.*;
import org.marmotgraph.core.api.v3.InstancesV3;
import org.marmotgraph.core.model.ExposedStage;
import org.marmotgraph.core.api.testutils.AbstractFunctionalityTest;
import org.marmotgraph.core.api.testutils.TestDataFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class InstancesTest extends AbstractFunctionalityTest {

    @Autowired
    InstancesV3 instances;

    private static final RoleMapping[] WRITE_ROLES = {RoleMapping.ADMIN, RoleMapping.OWNER, RoleMapping.EDITOR};
    private static final RoleMapping[] NON_WRITE_ROLES = RoleMapping.getRemainingUserRoles(WRITE_ROLES);

    private static final RoleMapping[] READ_IN_PROGRESS_ROLES = {RoleMapping.ADMIN, RoleMapping.OWNER, RoleMapping.EDITOR, RoleMapping.REVIEWER};
    private static final RoleMapping[] NON_READ_IN_PROGRESS_ROLES = RoleMapping.getRemainingUserRoles(READ_IN_PROGRESS_ROLES);

    private static final RoleMapping[] OWNER_ROLES = {RoleMapping.ADMIN, RoleMapping.OWNER};
    private static final RoleMapping[] NON_OWNER_ROLES = RoleMapping.getRemainingUserRoles(OWNER_ROLES);

    private static final RoleMapping[] NON_RELEASE_STATUS_ROLES = {null, RoleMapping.CONSUMER};
    private static final RoleMapping[] RELEASE_STATUS_ROLES = RoleMapping.getRemainingUserRoles(NON_RELEASE_STATUS_ROLES);

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void createInstanceOk() {
        //Given
        CreateInstanceTest test = new CreateInstanceTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(
                //Then
                () -> test.assureValidPayloadIncludingId(test.response)
        );
    }


    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void createInstanceForbidden() {
        //Given
        CreateInstanceTest test = new CreateInstanceTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void createInstanceWithSpecifiedUUIDOk() {
        //Given
        CreateInstanceWithSpecifiedUUIDTest test = new CreateInstanceWithSpecifiedUUIDTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            assertEquals(test.clientSpecifiedUUID, idUtils.getUUID(document.id()));
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void createInstanceWithSpecifiedUUIDOkForbidden() {
        //Given
        CreateInstanceWithSpecifiedUUIDTest test = new CreateInstanceWithSpecifiedUUIDTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void createInstanceWithNewSpaceOk() {
        //Given
        CreateInstanceWithNewSpaceTest test = new CreateInstanceWithNewSpaceTest(ctx(OWNER_ROLES), instances);

        //When
        test.execute(
                //Then
                () -> test.assureValidPayloadIncludingId(test.response)
        );
    }

    /**
     * Only an administrator can create an instance in a not-yet existing space (since space creation rights are required)
     */
    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void createInstanceWithNewSpaceForbidden() {
        //Given
        CreateInstanceWithNewSpaceTest test = new CreateInstanceWithNewSpaceTest(ctx(NON_OWNER_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    void contributeToInstanceFullReplacementOk() {
        //Given
        ContributeToInstanceFullReplacementTest test = new ContributeToInstanceFullReplacementTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            test.originalInstance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).forEach(k -> {
                assertNotNull(document.get(k));
                assertNotEquals(test.originalInstance.get(k), document.get(k), "The dynamic properties should change when doing the update");
            });
            test.originalInstance.keySet().stream().filter(k -> !k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX) && !k.startsWith(EBRAINSVocabulary.META)).forEach(k -> {
                assertNotNull(document.get(k));
                assertEquals(test.originalInstance.get(k), document.get(k), "The non-dynamic properties should remain the same when doing a contribution");
            });
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void contributeToInstanceFullReplacementForbidden() {
        //Given
        ContributeToInstanceFullReplacementTest test = new ContributeToInstanceFullReplacementTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    void contributeToInstancePartialReplacementOk() {
        //Given
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            test.originalInstance.keySet().stream().filter(k -> k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX)).forEach(k -> {
                if (k.equals(TestDataFactory.DYNAMIC_FIELD_PREFIX + "0")) {
                    assertNotNull(document.get(k));
                    assertNotEquals(test.originalInstance.get(k), document.get(k), "The dynamic property should change when doing the update");
                } else {
                    assertNotNull(document.get(k));
                    assertEquals(test.originalInstance.get(k), document.get(k), "All other dynamic properties should remain the same after a partial update");
                }
            });
            test.originalInstance.keySet().stream().filter(k -> !k.startsWith(TestDataFactory.DYNAMIC_FIELD_PREFIX) && !k.startsWith(EBRAINSVocabulary.META)).forEach(k -> {
                assertNotNull(document.get(k));
                assertEquals(test.originalInstance.get(k), document.get(k), "The non-dynamic properties should remain the same when doing a contribution");
            });
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void contributeToInstanceAlternatives() {
        //Given
        final ExtendedResponseConfiguration extendedResponseConfiguration = new ExtendedResponseConfiguration();
        extendedResponseConfiguration.setReturnAlternatives(true);
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(ctx(WRITE_ROLES), instances, extendedResponseConfiguration);

        //When
        test.execute(() -> {
            NormalizedJsonLd document = test.assureValidPayloadIncludingId(test.response);
            final NormalizedJsonLd alternative = document.getAs(EBRAINSVocabulary.META_ALTERNATIVE, NormalizedJsonLd.class);
            assertNotNull(alternative);

            final List<NormalizedJsonLd> alternativeOfManipulatedProperty = alternative.getAsListOf(ContributeToInstancePartialReplacementTest.MANIPULATED_PROPERTY, NormalizedJsonLd.class);
            assertEquals(2, alternativeOfManipulatedProperty.size());

            final NormalizedJsonLd selectedAlternative = alternativeOfManipulatedProperty.stream().filter(a -> a.getAs(EBRAINSVocabulary.META_SELECTED, Boolean.class)).findFirst().orElseThrow();
            final NormalizedJsonLd userOfSelectedAlternative = selectedAlternative.getAs(EBRAINSVocabulary.META_USER, NormalizedJsonLd.class);
            assertEquals("Alice", userOfSelectedAlternative.getAs(SchemaOrgVocabulary.NAME, String.class));

            final NormalizedJsonLd notSelectedAlternative = alternativeOfManipulatedProperty.stream().filter(a -> !a.getAs(EBRAINSVocabulary.META_SELECTED, Boolean.class)).findFirst().orElseThrow();
            final NormalizedJsonLd userOfNotSelectedAlternative = notSelectedAlternative.getAs(EBRAINSVocabulary.META_USER, NormalizedJsonLd.class);
            assertEquals("Admin", userOfNotSelectedAlternative.getAs(SchemaOrgVocabulary.NAME, String.class));

        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void contributeToInstancePartialReplacementForbidden() {
        //Given
        ContributeToInstancePartialReplacementTest test = new ContributeToInstancePartialReplacementTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    void getInstanceByIdOk() {
        //Given
        GetInstanceByIdTest test = new GetInstanceByIdTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            NormalizedJsonLd normalizedJsonLd = test.assureValidPayloadIncludingId(test.response);
            assertEquals(test.originalInstance, normalizedJsonLd);
        });
    }

    @Disabled("this doesn't return a forbidden exception anymore but rather minimal metadata")
    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void getInstanceByIdForbidden() {
        //Given
        GetInstanceByIdTest test = new GetInstanceByIdTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //TODO update assertion
        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    void getInstanceScopeSimpleOk() {
        //Given
        GetInstanceScopeSimpleTest test = new GetInstanceScopeSimpleTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ScopeElement scopeElement = test.assureValidPayload(test.response);
            assertNotNull(scopeElement.getId());
            assertNotNull(scopeElement.getTypes());
            assertNotNull(scopeElement.getSpace());
            assertNull(scopeElement.getChildren());
        });
    }


    @Disabled("this doesn't return a forbidden exception anymore but rather minimal metadata")
    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void getInstanceScopeSimpleForbidden() {
        //Given
        GetInstanceScopeSimpleTest test = new GetInstanceScopeSimpleTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);
        //TODO update assertion
        test.execute(ForbiddenException.class);
    }

    @Test
    void getInstanceNeighborsSimpleOk() {

        //Given
        GetInstanceNeighborsSimpleTest test = new GetInstanceNeighborsSimpleTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            GraphEntity neighborsGraph = test.assureValidPayload(test.response);
            assertNotNull(neighborsGraph.getId());
            assertNotNull(neighborsGraph.getTypes());
            assertNotNull(neighborsGraph.getSpace());
            assertTrue(neighborsGraph.getInbound().isEmpty());
            assertTrue(neighborsGraph.getOutbound().isEmpty());

        });

    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void getInstanceNeighborsSimpleForbidden() {

        //Given
        GetInstanceNeighborsSimpleTest test = new GetInstanceNeighborsSimpleTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    void getInstancesOk() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesWithExistingType(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            assertEquals(0, test.response.getFrom());
            assertEquals(2, test.response.getSize());
            assertEquals(Long.valueOf(2), test.response.getTotal());
            List<NormalizedJsonLd> data = test.response.getData();
            assertNotNull(data);
            assertEquals(2, data.size());
            assertTrue(data.contains(test.originalInstanceA));
            assertTrue(data.contains(test.originalInstanceB));

        });
    }

    @Test
    void getInstancesForbidden() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesWithExistingType(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    void getInstancesWithNonExistingType() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesByType(ctx(READ_IN_PROGRESS_ROLES), instances, "http://aTypeThatDoesntExist");

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    void getInstancesWithNonExistingTypeInExistingSpace() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesByTypeAndSpace(ctx(READ_IN_PROGRESS_ROLES), instances, "http://aTypeThatDoesntExist", "functionalityTest");

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }

    @Test
    void getInstancesWithNonExistingTypeInNonExistingSpace() {
        //Given
        GetInstancesTest test = GetInstancesTest.getInstancesByTypeAndSpace(ctx(READ_IN_PROGRESS_ROLES), instances, "http://aTypeThatDoesntExist", "aSpaceThatDoesntExist");

        //When
        test.execute(() -> {
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertTrue(test.response.getData().isEmpty());
        });
    }


    @Test
    void getInstancesByIdsOk() {
        //Given
        GetInstancesByIdsTest test = new GetInstancesByIdsTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertEquals(2, test.response.getData().size());
            test.ids.forEach(id -> {
                assertTrue(test.response.getData().containsKey(id.toString()));
                NormalizedJsonLd original = test.originalInstances.get(id);
                Result<NormalizedJsonLd> result = test.response.getData().get(id.toString());
                assertNotNull(result.getData());
                assertEquals(result.getData(), original);
            });

        });

    }

    @Test
    @Disabled("this doesn't return a forbidden exception anymore but rather minimal metadata")
    void getInstancesByIdsForbidden() {
        //Given
        GetInstancesByIdsTest test = new GetInstancesByIdsTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //TODO update assertion

        //When
        test.execute(() -> {
            for (UUID id : test.ids) {
                Result.Error error = test.response.getData().get(id.toString()).getError();
                assertNotNull(error);
                assertEquals(403, error.getCode());
            }
        });
    }

    @Test
    void getInstancesByIdentifiersOk() {
        //Given
        GetInstancesByIdentifiersTest test = new GetInstancesByIdentifiersTest(ctx(READ_IN_PROGRESS_ROLES), instances);

        //When
        test.execute(() -> {

            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertEquals(1, test.response.getData().size());
            assertTrue(test.response.getData().containsKey(test.identifier));
            Result<NormalizedJsonLd> result = test.response.getData().get(test.identifier);
            assertNotNull(result.getData());
            assertEquals(result.getData(), Objects.requireNonNull(test.updateResult.getBody()).getData());

        });

    }


    @Test
    @Disabled("this doesn't return a forbidden exception anymore but rather minimal metadata")
    void getInstancesByIdentifiersForbidden() {
        //Given
        GetInstancesByIdentifiersTest test = new GetInstancesByIdentifiersTest(ctx(NON_READ_IN_PROGRESS_ROLES), instances);

        //TODO update assertion
        //When
        test.execute(() -> {

            //Then
            Result.Error error = test.response.getData().get(test.identifier).getError();
            assertNotNull(error);
            assertEquals(403, error.getCode());
        });
    }


    @Test
    void deleteInstanceOk() {
        //Given
        DeleteInstanceTest test = new DeleteInstanceTest(ctx(WRITE_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> instanceById = test.fetchInstance();
            assertEquals(HttpStatus.NOT_FOUND, instanceById.getStatusCode(), "We expect a 404 to be returned from instanceById");
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void deleteInstanceForbidden() {
        //Given
        DeleteInstanceTest test = new DeleteInstanceTest(ctx(NON_WRITE_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    void releaseInstanceOk() {
        //Given
        ReleaseInstanceTest test = new ReleaseInstanceTest(ctx(OWNER_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> instanceById = test.fetchInstance();
            NormalizedJsonLd releasedInstance = test.assureValidPayloadIncludingId(instanceById);
            final String firstRelease = releasedInstance.getAs(EBRAINSVocabulary.META_FIRST_RELEASED_AT, String.class);
            assertNotNull(firstRelease);
            final String lastRelease = releasedInstance.getAs(EBRAINSVocabulary.META_LAST_RELEASED_AT, String.class);
            assertNotNull(lastRelease);
            assertEquals(firstRelease, lastRelease);
            releasedInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META);
            test.originalInstance.removeAllFieldsFromNamespace(EBRAINSVocabulary.META);
            assertEquals(releasedInstance, test.originalInstance);

            //Release again and ensure the release dates are correct
            instances.releaseInstance(test.getInstanceUUID(), null);
            NormalizedJsonLd releasedInstanceAfterSecondRelease = test.assureValidPayloadIncludingId(test.fetchInstance());
            final String firstReleaseAfterSecondRelease = releasedInstanceAfterSecondRelease.getAs(EBRAINSVocabulary.META_FIRST_RELEASED_AT, String.class);
            assertNotNull(firstRelease);
            final String lastReleaseAfterSecondRelease = releasedInstanceAfterSecondRelease.getAs(EBRAINSVocabulary.META_LAST_RELEASED_AT, String.class);
            assertNotNull(lastRelease);
            assertEquals(firstRelease, firstReleaseAfterSecondRelease);
            assertNotEquals(lastRelease, lastReleaseAfterSecondRelease);
            assertNotEquals(firstReleaseAfterSecondRelease, lastReleaseAfterSecondRelease);
        });
    }



    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void releaseInstanceForbidden() {
        //Given
        ReleaseInstanceTest test = new ReleaseInstanceTest(ctx(NON_OWNER_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    void unreleaseInstanceOk() {
        //Given
        UnreleaseInstanceTest test = new UnreleaseInstanceTest(ctx(OWNER_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            ResponseEntity<Result<NormalizedJsonLd>> releasedInstanceById = test.fetchInstance(ExposedStage.RELEASED);
            assertEquals(HttpStatus.NOT_FOUND, releasedInstanceById.getStatusCode(), "We expect a 404 to be returned from instanceById in released scope");

            //Just to be sure - we want to check if the instance is still available in the inferred space.
            ResponseEntity<Result<NormalizedJsonLd>> inferredInstanceById = test.fetchInstance(ExposedStage.IN_PROGRESS);
            test.assureValidPayloadIncludingId(inferredInstanceById);
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void unreleaseInstanceForbidden() {
        //Given
        UnreleaseInstanceTest test = new UnreleaseInstanceTest(ctx(NON_OWNER_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }


    @Test
    void getReleaseStatusOk() {
        //Given
        GetReleaseStatusTest test = new GetReleaseStatusTest(ctx(RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            assertEquals(ReleaseStatus.UNRELEASED, test.releaseStatusAfterCreation);
            assertEquals(ReleaseStatus.RELEASED, test.releaseStatusAfterReleasing);
            assertEquals(ReleaseStatus.HAS_CHANGED, test.releaseStatusAfterChange);
            assertEquals(ReleaseStatus.UNRELEASED, test.releaseStatusAfterUnreleasing);
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void getReleaseStatusForbidden() {
        //Given
        GetReleaseStatusTest test = new GetReleaseStatusTest(ctx(NON_RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(ForbiddenException.class);
    }

    @Test
    void getReleaseStatusByIdsOk() {
        //Given
        GetReleaseStatusByIdsTest test = new GetReleaseStatusByIdsTest(ctx(RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
            assertNotNull(test.response.getData());
            assertNotNull(test.response.getData().get(test.uuidA));
            assertNotNull(test.response.getData().get(test.uuidB));
            assertEquals(ReleaseStatus.RELEASED, test.response.getData().get(test.uuidA).getData());
            assertEquals(ReleaseStatus.UNRELEASED, test.response.getData().get(test.uuidB).getData());
        });
    }

    @Test
    @SuppressWarnings("java:S2699") //The assertion is handled within the "execution" part.
    void getReleaseStatusByIdsForbidden() {
        //Given
        GetReleaseStatusByIdsTest test = new GetReleaseStatusByIdsTest(ctx(NON_RELEASE_STATUS_ROLES), instances);

        //When, then
        test.execute(ForbiddenException.class);
    }


    @Test
    void getSuggestions() {
        //Given
        GetSuggestionsTest test = new GetSuggestionsTest(ctx(NON_RELEASE_STATUS_ROLES), instances);

        //When
        test.execute(() -> {
            //Then
            assertNotNull(test.response);
        });
    }
}
