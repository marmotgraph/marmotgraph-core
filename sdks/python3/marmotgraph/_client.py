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


import time
from uuid import UUID
import requests
from abc import ABC, abstractmethod
from typing import Optional, Self, Callable, Dict, Any

from .models import Stage


class Translator(object):

    @staticmethod
    def from_instance_id(instance_id: str, id_namespace: str) -> Optional[UUID]:
        if instance_id and instance_id.startswith(id_namespace):
            return UUID(instance_id[len(id_namespace):])
        return None

    @staticmethod
    def to_instance_id(uuid: UUID, id_namespace: str) -> Optional[str]:
        if uuid and id_namespace:
            return f"{id_namespace}{str(uuid)}"
        return None


class OIDCConfig(object):

    def __init__(self, well_known: str, device_auth_endpoint: str, token_endpoint: str):
        self._well_known = well_known
        self.device_auth_endpoint = device_auth_endpoint
        self.token_endpoint = token_endpoint


def fetch_by_open_id_config(well_known: str) -> Optional[OIDCConfig]:
    well_known_config = requests.get(well_known)
    if well_known_config and 200 <= well_known_config.status_code < 300 and well_known_config.json():
        well_known_config = well_known_config.json()
        token_endpoint = well_known_config["token_endpoint"] if "token_endpoint" in well_known_config else None
        device_auth_endpoint = well_known_config["device_authorization_endpoint"] \
            if "device_authorization_endpoint" in well_known_config else None
        if token_endpoint and device_auth_endpoint:
            return OIDCConfig(well_known, device_auth_endpoint, token_endpoint)
    return None


class TokenHandler(ABC):

    def __init__(self):
        self.token: Optional[str] = None

    def get_token(self, force_fetch: bool) -> Optional[str]:
        if not self.token or force_fetch:
            self.token = self._fetch_token()
        return self.token

    @abstractmethod
    def _fetch_token(self) -> Optional[str]:
        pass


class SimpleToken(TokenHandler):

    def __init__(self, token: str):
        super(SimpleToken, self).__init__()
        self._token = token

    def _fetch_token(self) -> Optional[str]:
        return self._token


class ClientCredentials(TokenHandler):

    def __init__(self, client_id: str, client_secret: str, oidc_config: OIDCConfig):
        super(ClientCredentials, self).__init__()
        self._client_id = client_id
        self._client_secret = client_secret
        self._oidc_config = oidc_config

    def _fetch_token(self) -> Optional[str]:
        if self._oidc_config:
            payload = {
                "grant_type": "client_credentials",
                "client_id": self._client_id,
                "client_secret": self._client_secret
            }
            response = requests.post(self._oidc_config.token_endpoint, data=payload)
            if response.status_code == 200:
                response = response.json()
                if response and "access_token" in response and response["access_token"]:
                    return response["access_token"]
        return None


class CallableTokenHandler(TokenHandler):
    def __init__(self, callable_handler: Callable[[], str]):
        super(CallableTokenHandler, self).__init__()
        self._callable = callable_handler

    def _fetch_token(self) -> Optional[str]:
        return self._callable()


class DeviceAuthenticationFlow(TokenHandler):

    def __init__(self, client_id: str, oidc_config: OIDCConfig):
        super(DeviceAuthenticationFlow, self).__init__()
        self._client_id: str = client_id
        self._oidc_config: OIDCConfig = oidc_config
        self._poll_interval_in_secs: int = 1
        self._refresh_token: Optional[str] = None

    def _poll_for_token(self, device_code: str) -> Optional[Dict[str, Any]]:
        payload = {
            "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
            "client_id": self._client_id,
            "device_code": device_code
        }
        response = requests.post(self._oidc_config.token_endpoint, data=payload)
        if response.status_code == 400:
            response = response.json()
            error = response["error"] if "error" in response else None
            if error == "expired_token":
                return None
            elif error == "slow_down":
                self._poll_interval_in_secs += 1
            time.sleep(self._poll_interval_in_secs)
            return self._poll_for_token(device_code)
        elif response.status_code == 200:
            return response.json()
        else:
            return None

    def _get_token_by_refresh_token(self) -> Optional[Dict[str, Any]]:
        response = requests.post(self._oidc_config.token_endpoint, data={
            "grant_type": "refresh_token",
            "client_id": self._client_id,
            "refresh_token": self._refresh_token
        })
        if response.status_code == 200:
            return response.json()
        else:
            if response.status_code == 401:
                # Reset the refresh token
                self.__refresh_token = None
            return None

    def _device_flow(self) -> Optional[Dict[str, Any]]:
        payload = {"client_id": self._client_id}
        response = requests.post(self._oidc_config.device_auth_endpoint, data=payload)
        if response.status_code == 200:
            response = response.json()
            verification_code_uri_complete = response["verification_uri_complete"]
            device_code = response["device_code"]
            if verification_code_uri_complete and device_code:
                print("************************************************************************")
                print(f"To continue, you need to authenticate. To do so, please visit {verification_code_uri_complete}")
                print("*************************************************************************")
                return self._poll_for_token(device_code=device_code)
        return None

    def _find_tokens(self) -> Dict[str, Any]:
        result = None
        if self._refresh_token:
            result = self._get_token_by_refresh_token()
        if not result:
            if not self._client_id or not self._oidc_config:
                raise ValueError("Configuration for device authentication flow is incomplete")
            else:
                result = self._device_flow()
                if result:
                    print(f"You are successfully authenticated! Thank you very much!")
                    print("*************************************************************************")
        if not result:
            print(f"Unfortunately, the authentication didn't succeed in time - please try again")
            print("*************************************************************************")
            result = self._find_tokens()
        return result

    def _fetch_token(self) -> Optional[str]:
        result = self._find_tokens()
        if result:
            refresh_token = result["refresh_token"] if "refresh_token" in result else None
            if refresh_token:
                self._refresh_token = refresh_token
            return result["access_token"] if "access_token" in result else None
        else:
            return None


class Config(object):

    def __init__(self, endpoint: str, token_handler: Optional[TokenHandler],
                 client_token_handler: Optional[TokenHandler],
                 id_namespace: Optional[str], enable_profiling: bool, default_stage: Stage):
        self.endpoint: str = endpoint
        self.token_handler: Optional[TokenHandler] = token_handler
        self.client_token_handler: Optional[TokenHandler] = client_token_handler
        self.id_namespace: Optional[str] = id_namespace
        self.enable_profiling = enable_profiling
        self.default_stage = default_stage


class AbstractClientBuilder(ABC):

    def __init__(self, host_name: str, id_namespace: str, login_client_id:Optional[str], oidc_config: Optional[OIDCConfig]):
        self._host_name: str = host_name
        self._login_client_id=login_client_id
        self._id_namespace: str = id_namespace
        self._enable_profiling: bool = False
        self._token_handler: Optional[TokenHandler] = None
        self._client_token_handler: Optional[TokenHandler] = None
        self._oidc_config: Optional[OIDCConfig] = oidc_config

    def _resolve_token_handler(self) -> Optional[TokenHandler]:
        if not self._token_handler:
            self.with_device_flow(self._login_client_id)
        return self._token_handler

    def _get_oidc_config(self) -> Optional[OIDCConfig]:
        return self._oidc_config

    def with_device_flow(self, client_id) -> Self:
        self._token_handler = DeviceAuthenticationFlow(client_id, self._oidc_config)
        return self

    def with_token(self, token: str) -> Self:
        self._token_handler = SimpleToken(token)
        return self

    def with_credentials(self, client_id: str, client_secret: str) -> Self:
        self._token_handler = ClientCredentials(client_id, client_secret, self._oidc_config)
        return self

    def with_custom_token_provider(self, token_provider: Callable[[], str]) -> Self:
        self._token_handler = CallableTokenHandler(token_provider)
        return self

    def add_client_authentication(self, client_id: str, client_secret: str) -> Self:
        self._client_token_handler = ClientCredentials(client_id, client_secret, self._oidc_config)
        return self

    def _create_config(self, default_stage: Stage) -> Config:
        return Config(self._host_name, self._resolve_token_handler(), self._client_token_handler,
                      self._id_namespace, self._enable_profiling, default_stage)

    def host(self, host_name: str) -> Self:
        self._host_name = host_name
        return self

    def id_namespace(self, id_namespace: str) -> Self:
        self._id_namespace = id_namespace
        return self