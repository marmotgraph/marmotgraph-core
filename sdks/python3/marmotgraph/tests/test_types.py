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
from unittest import TestCase

from typing import List, Dict, Any
from marmotgraph.marmotgraph import MarmotGraph
from marmotgraph.result import EmptyResult, Result, ResultsById, ResultPage
from uuid import UUID
from marmotgraph.models import JsonLdDocument, Instance, ReleaseTreeScope, ReleaseStatus, Scope, ListOfUUID, SpaceInformation, TypeInformation, UserWithRoles, User
from marmotgraph.tests.marmot_test import MarmotTest


class TestTypes(MarmotTest):
    """
    The test for the category Types
    """

    def test_get_by_name(self):
        # GIVEN
        self.create_and_release_test_instance(self.space, self.payload)

        # WHEN
        payload: List[str] = [self.target_type]
        builder: MarmotGraph.Types.GetTypesByNameBuilder = self._client.types.get_by_name()
        result: ResultsById[TypeInformation] = builder.invoke(payload)

        # THEN
        self.assertTrue(result.is_successful())
        self.assertIsNotNone(result.instances_by_id[self.target_type])
        self.assertIsNotNone(result.instances_by_id[self.target_type].instance)
        self.assertEqual(self.target_type, result.instances_by_id[self.target_type].instance.identifier)

    def test_list(self):
        # GIVEN
        self.create_and_release_test_instance(self.space, self.payload)

        # WHEN
        builder: MarmotGraph.Types.ListTypesBuilder = self._client.types.list()
        result: ResultPage[TypeInformation] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.assertFalse(len(result.instances) == 0)
        self.assertEqual(TypeInformation, type(result.instances[0]))

# TESTS
