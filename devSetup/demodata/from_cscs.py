#  Copyright 2024 - 2025 ETH Zurich
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0.
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#    limitations under the License.
#
#   This open source software code was developed in part or in whole in the
#   Human Brain Project, funded from the European Union's Horizon 2020
#   Framework Programme for Research and Innovation under
#   Specific Grant Agreements No. 720270, No. 785907, and No. 945539
#   (Human Brain Project SGA1, SGA2 and SGA3).
import os

from kg_core.kg import kg
from kg_core.request import Stage, Pagination
from marmotgraph.marmotgraph import MarmotGraph

skip_until = ""
skip_types = []

sa_client_id = os.getenv("UPLOADER_SERVICE_ACCOUNT_CLIENT_ID")
sa_client_secret = os.getenv("UPLOADER_SERVICE_ACCOUNT_CLIENT_SECRET")

target_kg = MarmotGraph.client("core.kg-v4.tds.cscs.ch").with_credentials(sa_client_id, sa_client_secret).build(Stage.IN_PROGRESS)
cscs_kg = MarmotGraph.client("core.kg-dev.tds.cscs.ch").build(Stage.IN_PROGRESS)
skip = True

for t in cscs_kg.types.list().invoke():
    if not skip_until or t.identifier == skip_until:
        skip = False
    do_skip = skip or t.identifier in skip_types
    print(f"Skipping {t}" if do_skip else f"Process {t}")
    if not do_skip:
        counter = 0
        page = cscs_kg.instances.list(t.identifier).size(200).invoke()
        while page and page.instances:
            bulk_update = MarmotGraph.Instances.BulkUpdate()
            for i in page.instances:
                space = i.get('https://core.kg.ebrains.eu/vocab/meta/space', None)
                if space:
                    for k in set(i.keys()):
                        if k.startswith("https://core.kg.ebrains.eu/vocab/meta") or k == '@id':
                            del i[k]
                    bulk_update.add(target_kg.instances.create_new_with_id(i.uuid, space).update_if_exists().skip_if_unchanged().return_payload(False), i)
            result = bulk_update.invoke(max_workers=10)
            page = page.next_page()
