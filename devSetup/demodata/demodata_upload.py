#  Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
#  Copyright 2021 - 2024 EBRAINS AISBL
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
import glob
import json
import os
from time import sleep
from requests.exceptions import ConnectionError

import requests
from kg_core.kg import kg

endpoint = os.getenv("CORE_ENDPOINT")
sa_client_id = os.getenv("UPLOADER_SERVICE_ACCOUNT_CLIENT_ID")
sa_client_secret = os.getenv("UPLOADER_SERVICE_ACCOUNT_CLIENT_SECRET")
oidc_client = os.getenv("OIDC_CLIENT")

def upload():
    client = kg(host=endpoint).with_credentials(sa_client_id, sa_client_secret).build()
    for testdata in glob.glob("**/*.jsonld"):
        with open(testdata, "r") as testdata_file:
            data = json.load(testdata_file)
            result = client.instances.create_new(data, "demo")
            if result.error and result.error.code == 409:
                result = client.instances.contribute_to_full_replacement(data, result.error.uuid)
            if result.error:
                print(f"Wasn't able to upload the instance {testdata} - error: {result.error}")
            else:
                print(f"Upload of {testdata} was successful", flush=True)
                error = client.instances.release(result.data.uuid)
                if error:
                    print(f"Wasn't able to release the instance {testdata} - error: {error}")
                else:
                    print(f"Release of {testdata} was successful", flush=True)

endpoint_with_protocol = f"http://{endpoint}" if endpoint.startswith("172.") or endpoint.startswith("localhost") else f"https://{endpoint}"
number_of_retries = 30
current_try = 0
success = False
while current_try<number_of_retries:
    try:
        response = requests.get(endpoint_with_protocol)
        if response.status_code==200:
            print(f"I've found KG at {endpoint_with_protocol}! I'm going to upload the demo data now!", flush=True)
            upload()
            success = True
            break
    except ConnectionError as e:
        pass
    current_try += 1
    print(f"Wasn't able to connect to KG yet. I'm expecting it at {endpoint_with_protocol}. Did you forget to start your local server? I'm waiting for {current_try*2} seconds...", flush=True)
    sleep(current_try*2)
if not success:
    print("Wasn't able to connect to KG in time", flush=True)

