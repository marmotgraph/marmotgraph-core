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
from kg_core.request import Stage
from kg_core.kg import kg

skip_until = ""
skip_types = []


endpoint = os.getenv("CORE_ENDPOINT")
sa_client_id = os.getenv("UPLOADER_SERVICE_ACCOUNT_CLIENT_ID")
sa_client_secret = os.getenv("UPLOADER_SERVICE_ACCOUNT_CLIENT_SECRET")
oidc_client = os.getenv("OIDC_CLIENT")

target_kg = kg(host=endpoint).with_credentials(sa_client_id, sa_client_secret).build()
ebrains_kg = kg(host="core.kg-ppd.ebrains.eu").build()
skip = True
for t in ebrains_kg.types.list(stage=Stage.IN_PROGRESS).items():
    if t.identifier == skip_until:
        skip = False
    do_skip = skip or t.identifier in skip_types
    print(f"Skipping {t}" if do_skip else f"Process {t}")
    if not do_skip:
        counter = 0
        for i in ebrains_kg.instances.list(t.identifier, stage=Stage.IN_PROGRESS).items():
            counter += 1
            if counter%100==0:
                print(f"{counter} instances handled")
            space = i.get('https://core.kg.ebrains.eu/vocab/meta/space', None)
            if space:
                for k in set(i.keys()):
                    if k.startswith("https://core.kg.ebrains.eu/vocab/meta") or k == '@id':
                        del i[k]
                target_kg.instances.create_new_with_id(i, i.uuid, space)