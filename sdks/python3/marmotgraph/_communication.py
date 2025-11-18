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

from abc import ABC
from typing import Any, Dict, Optional

import requests
from requests import Response

from .models import Stage
from ._client import Config


class Request(object):

    def __init__(self, path: str, payload: Any, query_parameters: Dict[str, Any], method: str):
        self.path = path
        self.payload = payload
        self.query_parameters = query_parameters
        self.method = method

def create_endpoint(host:str, path:str):
    if host.lower().startswith("localhost"):
        return f"http://{host}/{path}"
    else:
        return f"https://{host}/{path}"


class Communication(ABC):

    def __init__(self, config: Config):
        self.config = config

    def _get_default_stage(self) -> Stage:
        return self.config.default_stage

    def _evaluate_headers(self, force_token_fetch) -> Dict[str, Any]:
        headers = {}
        if self.config.token_handler:
            token: Optional[str] = self.config.token_handler.get_token(force_token_fetch)
            if token:
                headers["Authorization"] = f"Bearer {token}"
            if self.config.client_token_handler:
                client_token: Optional[str] = self.config.client_token_handler.get_token(force_token_fetch)
                if client_token:
                    headers["Client-Authorization"] = f"Bearer {client_token}"
        return headers

    def _execute_request(self, request_definition: Request, force_token_fetch: bool) -> Response:
        headers = self._evaluate_headers(force_token_fetch)
        return requests.request(
            method=request_definition.method,
            url=create_endpoint(self.config.endpoint, request_definition.path),
            headers=headers,
            json=request_definition.payload,
            params=request_definition.query_parameters
        )

    def _do_request(self, request_definition: Request) -> Response:
        response = self._execute_request(request_definition, False)
        if response.status_code == 401:
            response = self._execute_request(request_definition, True)
        return response

    def get_request(self, path: str, query_parameters: Dict[str, Any]) -> Response:
        return self._do_request(Request(path, None, query_parameters, "GET"))

    def put_request(self, path: str, payload: Any, query_parameters: Dict[str, Any]) -> Response:
        return self._do_request(Request(path, payload, query_parameters, "PUT"))

    def post_request(self, path: str, payload: Any, query_parameters: Dict[str, Any]) -> Response:
        return self._do_request(Request(path, payload, query_parameters, "POST"))

    def patch_request(self, path: str, payload: Any, query_parameters: Dict[str, Any]) -> Response:
        return self._do_request(Request(path, payload, query_parameters, "PATCH"))

    def delete_request(self, path: str, query_parameters: Dict[str, Any]):
        return self._do_request(Request(path, None, query_parameters, "DELETE"))

    def _get_id_namespace(self) -> Optional[str]:
        return self.config.id_namespace if self.config else None
