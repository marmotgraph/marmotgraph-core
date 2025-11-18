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

from enum import Enum
from typing import Optional, Any, Dict, Generic, TypeVar, List, Callable, Iterator, Self
from uuid import UUID
import http.client

from pydantic import BaseModel, Field
from requests import Response

from .models import JsonLdDocument, Instance

ResponseType = TypeVar("ResponseType")


class Error(BaseModel):
    code: int
    message: Optional[str] = None
    uuid: Optional[UUID] = Field(alias="instanceId")


class ResponseObjectConstructor(Generic[ResponseType]):
    @staticmethod
    def init_response_object(constructor: ResponseType, data: Any, id_namespace: Any) -> ResponseType:
        if issubclass(constructor, Enum):
            return constructor[data]
        elif issubclass(constructor, Instance):
            return constructor(id_namespace, **data)
        else:
            return constructor(**data)


class EmptyResult(object):

    def __init__(self, message: Optional[str] = None, start_time: Optional[int] = None, duration_in_ms: Optional[int] = None,
                 transaction_id: Optional[int] = None, error: Optional[Error] = None):
        self.message = message
        self.start_time = start_time
        self.duration_in_ms = duration_in_ms
        self.transaction_id = transaction_id
        self.error = error

    def is_successful(self) -> bool:
        return self.error is None


def translate_empty_result(response: Response) -> EmptyResult:
    payload, wrapper = _parse_response(response, False)
    return EmptyResult(**wrapper)


class EmptyResultPage(EmptyResult):

    def __init__(self, message: Optional[str], start_time: Optional[int], duration_in_ms: Optional[int],
                 transaction_id: Optional[int], error: Optional[Error], total: Optional[int], size: Optional[int],
                 from_item: Optional[int]):
        super().__init__(message, start_time, duration_in_ms, transaction_id, error)
        self.total = total
        self.size = size
        self.from_item = from_item

    def __str__(self):
        return f"{super.__str__(self)} - status: {self.error.code if self.error else 'success'}"


class Result(EmptyResult, Generic[ResponseType]):

    def __init__(self, instance: Optional[ResponseType] = None, message: Optional[str] = None, start_time: Optional[int] = None,
                 duration_in_ms: Optional[int] = None,
                 transaction_id: Optional[int] = None, error: Optional[Error] = None):
        super().__init__(message, start_time, duration_in_ms, transaction_id, error)
        self.instance = instance

    def is_successful(self) -> bool:
        return super().is_successful() and self.instance


def _process_response(parsed_response: Optional[Dict[str, Any]], add_list_properties: bool) -> (Optional[Any], Dict[str, Any]):
    if parsed_response:
        wrapper = {
            "message": parsed_response["message"] if "message" in parsed_response else None,
            "start_time": parsed_response["startTime"] if "startTime" in parsed_response else None,
            "duration_in_ms": parsed_response["durationInMs"] if "durationInMs" in parsed_response else None,
            "transaction_id": parsed_response["transactionId"] if "transactionId" in parsed_response else None
        }
        if add_list_properties:
            wrapper["total"] = parsed_response["total"] if "total" in parsed_response else None
            wrapper["size"] = parsed_response["size"] if "size" in parsed_response else None
            wrapper["from_item"] = parsed_response["from"] if "from" in parsed_response else None
    else:
        wrapper = {}
    if (parsed_response and "error" in parsed_response and parsed_response["error"] and
            not isinstance(parsed_response["error"], str)):
        wrapper["error"] = Error(**parsed_response["error"])
    return parsed_response["data"] if parsed_response and "data" in parsed_response else None, wrapper


def _parse_response(response: Response, add_list_properties: bool) -> (Optional[Any], Dict[str, Any]):
    result, wrapper = _process_response(response.json() if response.content else None, add_list_properties)
    if 400 <= response.status_code < 200:
        wrapper["error"] = Error(code=response.status_code, message=http.client.responses[response.status_code])
    return result, wrapper


def _translate_inner_result(id_namespace: str, parsed_response: Dict[str, Any], constructor: Callable[..., ResponseType]) -> Result[ResponseType]:
    payload, wrapper = _process_response(parsed_response, False)
    if payload:
        wrapper["instance"] = ResponseObjectConstructor.init_response_object(constructor, payload, id_namespace)
    return Result(**wrapper)


def translate_result(id_namespace: str, response: Response, constructor: Callable[..., ResponseType]) \
        -> Result[ResponseType]:
    payload, wrapper = _parse_response(response, False)
    if payload:
        wrapper["instance"] = ResponseObjectConstructor.init_response_object(constructor, payload, id_namespace)
    return Result(**wrapper)


class ResultPageBuilderContext(object):

    def __init__(self, builder, invocation_function, **invocation_params):
        self.builder = builder
        self.invocation_function = invocation_function
        self.invocation_params = invocation_params


class ResultPage(EmptyResultPage, Generic[ResponseType]):

    def __init__(self, builder_context: ResultPageBuilderContext, instances: Optional[List[ResponseType]] = None,
                 message: Optional[str] = None, start_time: Optional[int] = None, duration_in_ms: Optional[int] = None,
                 transaction_id: Optional[int] = None, error: Optional[Error] = None, total: Optional[int] = None,
                 size: Optional[int] = None,
                 from_item: Optional[int] = None):
        super().__init__(message, start_time, duration_in_ms, transaction_id, error, total, size, from_item)
        self.instances = instances
        self._builder_context = builder_context

    def is_successful(self) -> bool:
        return super().is_successful() and (self.size == 0 or self.instances is not None)

    def __iter__(self) -> Iterator[ResponseType]:
        # We're iterating through the instances until the end
        # we don't need the total results (this speeds up the query)
        self._builder_context.builder.return_total_results(False)
        self._current_result = self._builder_context.invocation_function(**self._builder_context.invocation_params)
        if self._current_result.is_successful():
            self._current_item_in_page = 0
        else:
            raise ValueError(f"Was not able to execute query: {self._current_result.error.message}")
        return self

    def next_page(self) -> Optional[Self]:
        self._builder_context.builder.params["from"] = (self._builder_context.builder.params["from"] +
                                                        self._builder_context.builder.params["size"])
        next_page = self._builder_context.invocation_function(**self._builder_context.invocation_params)
        if next_page.is_successful():
            return next_page
        else:
            raise ValueError(f"Was not able to execute query: {next_page.error.message}")

    def __next__(self):
        if len(self._current_result.instances) == 0:
            raise StopIteration
        if self._current_item_in_page < len(self._current_result.instances):
            instance = self._current_result.instances[self._current_item_in_page]
            self._current_item_in_page += 1
            return instance
        else:
            self._current_result = self.next_page()
            if self._current_result.is_successful():
                self._current_item_in_page = 0
                if self._current_result.size > 0 and len(self._current_result.instances) > 0:
                    return self._current_result.instances[self._current_item_in_page]
                else:
                    raise StopIteration
            else:
                raise ValueError(f"Was not able to execute query: {self._current_result.error.message}")


def translate_result_page(builder_context: ResultPageBuilderContext, id_namespace: str, response: Response,
                          constructor: Callable[..., ResponseType]) \
        -> ResultPage[ResponseType]:
    payload, wrapper = _parse_response(response, True)
    instances = [ResponseObjectConstructor.init_response_object(constructor, d, id_namespace)
                 for d in payload] if payload else None
    return ResultPage(builder_context=builder_context, instances=instances, **wrapper)


class ResultsById(EmptyResult, Generic[ResponseType]):

    def __init__(self, instances_by_id: Optional[Dict[str, Result[ResponseType]]] = None, message: Optional[str] = None,
                 start_time: Optional[int] = None, duration_in_ms: Optional[int] = None,
                 transaction_id: Optional[int] = None, error: Optional[Error] = None):
        super().__init__(message, start_time, duration_in_ms, transaction_id, error)
        self.instances_by_id = instances_by_id

    def is_successful(self) -> bool:
        return super().is_successful() and self.instances_by_id


def translate_results_by_id(id_namespace: str, response: Response, constructor: Callable[..., ResponseType]) \
        -> ResultsById[ResponseType]:
    payload, wrapper = _parse_response(response, False)
    if payload:
        wrapper["instances_by_id"] = {k: _translate_inner_result(id_namespace, payload[k], constructor) for k in payload.keys()}
    return ResultsById(**wrapper)
