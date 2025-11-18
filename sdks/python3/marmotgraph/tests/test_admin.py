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
#

from typing import Dict, Any

from marmotgraph.marmotgraph import MarmotGraph
from marmotgraph.result import EmptyResult
from marmotgraph.tests.marmot_test import MarmotTest


class TestAdmin(MarmotTest):
    """
    The test for the category Admin
    """

    def test_assign_type_to_space(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.AssignTypeToSpaceBuilder = self._client.admin.assign_type_to_space(self.space,
                                                                                                 self.target_type)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_calculate_instance_invitation_scope(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.CalculateInstanceInvitationScopeBuilder = self._client.admin.calculate_instance_invitation_scope(
            self.instance_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_create_space_definition(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.CreateSpaceDefinitionBuilder = self._client.admin.create_space_definition(self.space)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_create_type_definition(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.CreateTypeDefinitionBuilder = self._client.admin.create_type_definition(self.target_type)
        result: EmptyResult = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_define_property(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Admin.DefinePropertyBuilder = self._client.admin.define_property(property_name)
        result: EmptyResult = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_define_property_for_type(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Admin.DefinePropertyForTypeBuilder = self._client.admin.define_property_for_type(property_name,
                                                                                                         self.target_type)
        result: EmptyResult = builder.invoke(self.payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_deprecate_property(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Admin.DeprecatePropertyBuilder = self._client.admin.deprecate_property(property_name)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_deprecate_property_for_type(self):
        # GIVEN
        property_name: str = None

        # WHEN
        builder: MarmotGraph.Admin.DeprecatePropertyForTypeBuilder = self._client.admin.deprecate_property_for_type(
            property_name, self.target_type)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_all_role_definitions(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.GetAllRoleDefinitionsBuilder = self._client.admin.get_all_role_definitions()
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_available_checks(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.GetAvailableChecksBuilder = self._client.admin.get_available_checks()
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_claim_for_role(self):
        # GIVEN
        role: str = None

        # WHEN
        builder: MarmotGraph.Admin.GetClaimForRoleBuilder = self._client.admin.get_claim_for_role(role)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_get_report(self):
        # GIVEN
        name: str = None

        # WHEN
        builder: MarmotGraph.Admin.GetReportBuilder = self._client.admin.get_report(name)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_health_status(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.HealthStatusBuilder = self._client.admin.health_status()
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_remove_space_definition(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.RemoveSpaceDefinitionBuilder = self._client.admin.remove_space_definition(self.space)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_remove_type_definition(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.RemoveTypeDefinitionBuilder = self._client.admin.remove_type_definition()
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_remove_type_from_space(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.RemoveTypeFromSpaceBuilder = self._client.admin.remove_type_from_space(self.space,
                                                                                                     self.target_type)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_rerun_events(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.RerunEventsBuilder = self._client.admin.rerun_events(self.space)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_trigger_inference(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Admin.TriggerInferenceBuilder = self._client.admin.trigger_inference(self.space)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

    def test_update_claim_for_role(self):
        # GIVEN
        remove: bool = None
        role: str = None

        # WHEN
        payload: Dict[str, Any] = None
        builder: MarmotGraph.Admin.UpdateClaimForRoleBuilder = self._client.admin.update_claim_for_role(remove, role)
        result: EmptyResult = builder.invoke(payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.fail("Test has not been finalized yet")

# TESTS
