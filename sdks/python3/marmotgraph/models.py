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

from __future__ import annotations
import uuid
from collections import UserList
from enum import Enum
from typing import Any, Iterable, Optional, Dict, List
from uuid import UUID

from pydantic import BaseModel, Field, Extra


class ReleaseStatus(str, Enum):
    RELEASED = "RELEASED"
    UNRELEASED = "UNRELEASED"
    HAS_CHANGED = "HAS_CHANGED"


class JsonLdDocument(Dict[str, Any]):
    pass


def _evaluate_uuid(id_namespace:str, instance_id: str) -> Optional[UUID]:
    if id_namespace and instance_id and instance_id.startswith(id_namespace):
        return uuid.UUID(instance_id[len(id_namespace):])
    else:
        return None


class Instance(JsonLdDocument):
    uuid: Optional[UUID] = None

    def __init__(self, id_namespace: str, **kwargs):
        super().__init__(**kwargs)
        self.instance_id = self["@id"] if "@id" in self else None
        self.uuid = _evaluate_uuid(id_namespace, self.instance_id)

    def __str__(self):
        return f"Instance {self.uuid if self.uuid else 'unknown'}"


class Stage(str):
    IN_PROGRESS = "IN_PROGRESS"
    RELEASED = "RELEASED"


class ReleaseTreeScope(str):
    TOP_INSTANCE_ONLY = "TOP_INSTANCE_ONLY"
    CHILDREN_ONLY = "CHILDREN_ONLY"


class Scope(BaseModel):
    uuid: Optional[UUID] = Field(alias="id", default=None)
    label: Optional[str] = Field(default=None)
    space: Optional[str] = Field(default=None)
    types: Optional[List[str]] = Field(default=None)
    children: Optional[List[Scope]] = Field(default=None)
    permissions: Optional[List[str]] = Field(default=None)


class SpaceInformation(BaseModel):
    identifier: Optional[str] = Field(alias="http://schema.org/identifier", default=None)
    name: Optional[str] = Field(alias="http://schema.org/name", default=None)
    permissions: Optional[List[str]] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/permissions", default=None)


class TargetSpaceInformation(BaseModel):
    occurrences: Optional[int] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/occurrences", default=None)
    space: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/space", default=None)


class TargetTypeInformation(BaseModel):
    occurrences: Optional[int] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/occurrences", default=None)
    type: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/type", default=None)
    spaces: Optional[List[TargetSpaceInformation]] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/spaces", default=None)


class PropertyInformation(BaseModel):
    identifier: Optional[str] = Field(alias="http://schema.org/identifier", default=None)
    occurrences: Optional[int] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/occurrences", default=None)
    description: Optional[str] = Field(alias="http://schema.org/description", default=None)
    instruction: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/instruction", default=None)
    required: Optional[bool] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/required", default=False)
    order_number: Optional[int] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/orderNumber", default=None)
    name: Optional[str] = Field(alias="http://schema.org/name", default=None)
    name_for_reverse_link: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/nameForReverseLink", default=None)
    target_types: Optional[List[TargetTypeInformation]] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/targetTypes", default=None)

    # This will catch all extra unmapped fields
    additional_definitions: Optional[Dict[str, Any]] = Field(default=None, alias='additional_definitions')

    class Config:
        # Allow extra fields in the model
        extra = Extra.allow

    def __init__(self, **data):
        super().__init__(**data)
        # Capture extra fields in extra_fields
        aliases = [f.alias for f in self.__fields__.values()]
        extra_fields = { k: v for k, v in data.items() if k not in aliases}
        self.additional_definitions = extra_fields if extra_fields else None


class TypeSpaceInformation(BaseModel):
    space: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/space", default=None)
    occurrences: Optional[int] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/occurrences", default=None)
    properties: Optional[List[PropertyInformation]] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/properties", default=None)


class TypeInformation(BaseModel):
    identifier: Optional[str] = Field(alias="http://schema.org/identifier", default=None)
    description: Optional[str] = Field(dalias="http://schema.org/description", default=None)
    name: Optional[str] = Field(alias="http://schema.org/name", default=None)
    color: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/color", default=None)
    label_property: Optional[str] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/type/labelProperty", default=None)
    # TODO incoming_links
    occurrences: Optional[int] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/occurrences", default=None)
    properties: Optional[List[PropertyInformation]] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/properties", default=None)
    spaces: Optional[List[TypeSpaceInformation]] = Field(alias="https://core.kg.ebrains.eu/vocab/meta/spaces", default=None)

    # This will catch all extra unmapped fields
    additional_definitions: Optional[Dict[str, Any]] = Field(default=None, alias='additional_definitions')

    class Config:
        # Allow extra fields in the model
        extra = Extra.allow

    def __init__(self, **data):
        super().__init__(**data)
        # Capture extra fields in extra_fields
        aliases = [f.alias for f in self.__fields__.values()]
        extra_fields = { k: v for k, v in data.items() if k not in aliases}
        self.additional_definitions = extra_fields if extra_fields else None


class User(BaseModel):
    alternate_name: Optional[str] = Field(alias="http://schema.org/alternateName", default=None)
    name: Optional[str] = Field(alias="http://schema.org/name", default=None)
    email: Optional[str] = Field(alias="http://schema.org/email", default=None)
    given_name: Optional[str] = Field(alias="http://schema.org/givenName", default=None)
    family_name: Optional[str] = Field(alias="http://schema.org/familyName", default=None)
    identifiers: Optional[List[str]] = Field(alias="http://schema.org/identifier", default=None)


class UserWithRoles(BaseModel):
    user: User
    client_roles: Optional[List[str]] = Field(alias="clientRoles", default=None)
    user_roles: Optional[List[str]] = Field(alias="userRoles", default=None)
    invitations: Optional[List[str]] = Field(default=None)
    client_id: Optional[str] = Field(alias="clientId", default=None)


class ListOfUUID(UserList[UUID]):
    def __init__(self, seq: Iterable[UUID] = ()):
        super(ListOfUUID, self).__init__(seq)
