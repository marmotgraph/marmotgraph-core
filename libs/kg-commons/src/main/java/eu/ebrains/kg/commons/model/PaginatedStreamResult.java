/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.commons.model;

import java.util.stream.Stream;

public class PaginatedStreamResult<T> extends Result<Stream<T>> {
    private Long total;
    private long size;
    private long from;

    public static <T> PaginatedStreamResult<T> ok(PaginatedStream<T> data) {
        return ok(data, null);
    }

    public static <T> PaginatedStreamResult<T> ok(PaginatedStream<T> data, String message) {
        PaginatedStreamResult<T> result = new PaginatedStreamResult<>();
        if (data == null) {
            data = new PaginatedStream<>();
            result.data = Stream.of();
        }
        else {
            result.data = data.getStream();
        }
        result.total = data.getTotalResults();
        result.from = data.getFrom();
        result.size = data.getSize();
        return result;
    }

    public Long getTotal() {
        return total;
    }

    public long getSize() {
        return size;
    }

    public long getFrom() {
        return from;
    }
}
