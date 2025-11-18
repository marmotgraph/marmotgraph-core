#  Copyright (c) 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
#  Copyright (c) 2021 - 2024 EBRAINS AISBL
#  Copyright (c) 2024 ETH Zurich
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0.
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  This open source software code was developed in part or in whole in the
#  Human Brain Project, funded from the European Union's Horizon 2020
#  Framework Programme for Research and Innovation under
#  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
#  (Human Brain Project SGA1, SGA2 and SGA3).
#
#  This open source software code was developed in part or in whole in the
#  Human Brain Project, funded from the European Union's Horizon 2020
#  Framework Programme for Research and Innovation under
#  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
#  (Human Brain Project SGA1, SGA2 and SGA3).
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0.
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  This open source software code was developed in part or in whole in the
#  Human Brain Project, funded from the European Union's Horizon 2020
#  Framework Programme for Research and Innovation under
#  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
#  (Human Brain Project SGA1, SGA2 and SGA3).

from typing import List
from uuid import UUID

from marmotgraph.marmotgraph import MarmotGraph
from marmotgraph.models import Instance, ReleaseTreeScope, ReleaseStatus, Scope, ListOfUUID
from marmotgraph.result import EmptyResult, Result, ResultsById, ResultPage
from marmotgraph.tests.marmot_test import MarmotTest


class TestInstances(MarmotTest):
    """
    The test for the category Instances
    """

    def test_contribute_to_full_replacement(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.ContributeToInstanceFullReplacementBuilder = self._client.instances.contribute_to_full_replacement(self.instance_id)
        result: Result[Instance] = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_contribute_to_partial_replacement(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.ContributeToInstancePartialReplacementBuilder = self._client.instances.contribute_to_partial_replacement(self.instance_id)
        result: Result[Instance] = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_create_new(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.CreateNewInstanceBuilder = self._client.instances.create_new(self.space)
        result: Result[Instance] = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_create_new_with_id(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.CreateNewInstanceWithIdBuilder = self._client.instances.create_new_with_id(self.instance_id, self.space)
        result: Result[Instance] = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_delete(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.DeleteInstanceBuilder = self._client.instances.delete(self.instance_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_by_id(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.GetInstanceByIdBuilder = self._client.instances.get_by_id(self.instance_id)
        result: Result[Instance] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_by_identifiers(self):
        # GIVEN

        # WHEN
        payload: List[str] = None
        builder: MarmotGraph.Instances.GetInstancesByIdentifiersBuilder = self._client.instances.get_by_identifiers()
        result: ResultsById[Instance] = builder.invoke(payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_by_ids(self):
        # GIVEN

        # WHEN
        payload: List[str] = None
        builder: MarmotGraph.Instances.GetInstancesByIdsBuilder = self._client.instances.get_by_ids()
        result: ResultsById[Instance] = builder.invoke(payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_incoming_links(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Instances.GetIncomingLinksBuilder = self._client.instances.get_incoming_links(self.instance_id, property_name, self.target_type)
        result: ResultPage[Instance] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_neighbors(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.GetNeighborsBuilder = self._client.instances.get_neighbors(self.instance_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_release_status(self):
        # GIVEN
        release_tree_scope: ReleaseTreeScope = None

        # WHEN
        builder: MarmotGraph.Instances.GetReleaseStatusBuilder = self._client.instances.get_release_status(self.instance_id, release_tree_scope)
        result: Result[ReleaseStatus] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_release_status_by_ids(self):
        # GIVEN
        release_tree_scope: ReleaseTreeScope = None

        # WHEN
        payload: List[UUID] = None
        builder: MarmotGraph.Instances.GetReleaseStatusByIdsBuilder = self._client.instances.get_release_status_by_ids(release_tree_scope)
        result: ResultsById[ReleaseStatus] = builder.invoke(payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_scope(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.GetInstanceScopeBuilder = self._client.instances.get_scope(self.instance_id)
        result: Result[Scope] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_suggested_links_for_property(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Instances.GetSuggestedLinksForPropertyBuilder = self._client.instances.get_suggested_links_for_property(self.instance_id, property_name)
        result: EmptyResult = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_suggested_links_for_property1(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Instances.GetSuggestedLinksForProperty1Builder = self._client.instances.get_suggested_links_for_property1(self.instance_id, property_name)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_invite_user_for(self):
        # GIVEN
        user_id: UUID = None

        # WHEN
        builder: MarmotGraph.Instances.InviteUserForInstanceBuilder = self._client.instances.invite_user_for(self.instance_id, user_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_list(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.ListInstancesBuilder = self._client.instances.list(self.target_type)
        result: ResultPage[Instance] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_list_invitations(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.ListInvitationsBuilder = self._client.instances.list_invitations(self.instance_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_list_with_invitations(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.ListInstancesWithInvitationsBuilder = self._client.instances.list_with_invitations()
        result: Result[ListOfUUID] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_move(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.MoveInstanceBuilder = self._client.instances.move(self.instance_id, self.space)
        result: Result[Instance] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_release(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.ReleaseInstanceBuilder = self._client.instances.release(self.instance_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_revoke_user_invitation(self):
        # GIVEN
        user_id: UUID = None

        # WHEN
        builder: MarmotGraph.Instances.RevokeUserInvitationBuilder = self._client.instances.revoke_user_invitation(self.instance_id, user_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_unrelease(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Instances.UnreleaseInstanceBuilder = self._client.instances.unrelease(self.instance_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

# TESTS
