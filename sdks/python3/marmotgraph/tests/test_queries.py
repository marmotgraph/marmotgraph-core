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

from uuid import UUID

from marmotgraph.marmotgraph import MarmotGraph

from marmotgraph.models import JsonLdDocument, Instance
from marmotgraph.result import EmptyResult, Result, ResultPage
from marmotgraph.tests.marmot_test import MarmotTest


class TestQueries(MarmotTest):
    """
    The test for the category Queries
    """
    query_id = UUID("b85629ce-dd0e-436f-982a-5eea06e8fca4")

    query = JsonLdDocument({
        "@context": {
            "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
            "query": "https://schema.hbp.eu/myQuery/",
            "propertyName": {
                "@id": "propertyName",
                "@type": "@id"
            },
            "path": {
                "@id": "path",
                "@type": "@id"
            }
        },
        "meta": {
            "type": "https://marmotgraph.org/tests/Test",
            "responseVocab": "https://schema.hbp.eu/myQuery/"
        },
        "structure": [
            {
                "propertyName": "query:id",
                "path": "@id"
            },
            {
                "propertyName": "query:foo",
                "path": "https://marmotgraph.org/foo"
            }
        ]
    })

    def setUp(self):
        super().setUp()
        self.create_and_release_test_instance(self.space, self.payload)

    def test_execute_query_by_id(self):
        # GIVEN
        self.test_save_query()

        # WHEN
        builder: Marmot.Queries.ExecuteQueryByIdBuilder = self._client.queries.execute_query_by_id(self.query_id)
        result: ResultPage[JsonLdDocument] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.assertFalse(len(result.instances) == 0)
        self.assertTrue("foo" in result.instances[0])

    def test_get_query_specification(self):
        # GIVEN
        self.test_save_query()

        # WHEN
        builder: MarmotGraph.Queries.GetQuerySpecificationBuilder = self._client.queries.get_query_specification(self.query_id)
        result: Result[Instance] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.assertEqual(self.query_id, result.instance.uuid)

    def test_list_per_root_type(self):
        # GIVEN
        self.test_save_query()

        # WHEN
        builder: MarmotGraph.Queries.ListQueriesPerRootTypeBuilder = self._client.queries.list_per_root_type()
        builder.target_type(self.target_type)
        result: ResultPage[Instance] = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())
        self.assertFalse(len(result.instances) == 0)

    def test_remove_query(self):
        # GIVEN
        self.test_save_query()

        # WHEN
        builder: MarmotGraph.Queries.RemoveQueryBuilder = self._client.queries.remove_query(self.query_id)
        result: EmptyResult = builder.invoke()

        # THEN
        self.assertTrue(result.is_successful())

    def test_run_dynamic_query(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Queries.RunDynamicQueryBuilder = self._client.queries.run_dynamic_query()
        result: ResultPage[JsonLdDocument] = builder.invoke(self.query)

        # THEN
        self.assertTrue(result.is_successful())
        self.assertFalse(len(result.instances) == 0)
        self.assertTrue("foo" in result.instances[0])

    def test_save_query(self):
        # GIVEN

        # WHEN
        builder: MarmotGraph.Queries.SaveQueryBuilder = self._client.queries.save_query(self.query_id)
        builder.space(self.space)
        result: Result[Instance] = builder.invoke(self.query)

        # THEN
        self.assertTrue(result.is_successful())
        self.assertEqual(self.query_id, result.instance.uuid)

# TESTS
