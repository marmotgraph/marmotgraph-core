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

import os
from abc import ABC
from typing import Optional
from unittest import TestCase
from uuid import UUID

from marmotgraph.marmotgraph import MarmotGraph
from marmotgraph.models import JsonLdDocument, Stage, Instance
from marmotgraph.result import Result, EmptyResult


def _init_client() -> MarmotGraph.Client:
    client_id: Optional[str] = os.getenv("KG_TEST_CLIENT_ID")
    client_secret: Optional[str] = os.getenv("KG_TEST_CLIENT_SECRET")
    if client_id and client_secret:
        return MarmotGraph.client().host("core.kg-ppd.ebrains.eu").default_stage(Stage.IN_PROGRESS).with_credentials(client_id, client_secret).build()
    else:
        return MarmotGraph.client().host("core.kg-ppd.ebrains.eu").default_stage(Stage.IN_PROGRESS).build()


class MarmotTest(ABC, TestCase):
    _client: MarmotGraph.Client = _init_client()
    space: str = "myspace"
    target_type: str = "https://marmotgraph.org/tests/Test"
    instance_id: UUID = UUID("46f7efb4-fd4d-4b0b-9594-ffc913514e93")
    payload: JsonLdDocument = {
        "@id": _client.instance_id_from_uuid(instance_id),
        "@type": [target_type],
        "https://marmotgraph.org/foo": "bar"
    }

    def create_and_release_test_instance(self, space: str, payload: JsonLdDocument):
        successful = self._client.instances.get_by_id(self.instance_id).return_payload(False).invoke_in_stage(Stage.IN_PROGRESS).is_successful()
        if not successful:
            instance_result: Result[Instance] = self._client.instances.create_new_with_id(self.instance_id, space).invoke(payload)
            self.assertTrue(instance_result.is_successful())
            release_result: EmptyResult = self._client.instances.release(instance_result.instance.uuid).invoke()
            self.assertTrue(release_result.is_successful())
