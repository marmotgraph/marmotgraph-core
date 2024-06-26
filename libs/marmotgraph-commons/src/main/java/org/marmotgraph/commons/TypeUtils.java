/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2022 EBRAINS AISBL
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

package org.marmotgraph.commons;

import java.util.ArrayList;
import java.util.List;

public class TypeUtils {

    private final JsonAdapter jsonAdapter;

    public TypeUtils(JsonAdapter jsonAdapter){
        this.jsonAdapter = jsonAdapter;
    }

    public <T> T translate(Object o, Class<T> clazz){
        return jsonAdapter.fromJson(jsonAdapter.toJson(o), clazz);
    }

    public static <T> List<List<T>> splitList(List<T> list, int chunkSize) {
        int numberOfChunks = (list.size() / chunkSize) + 1;
        List<List<T>> result = new ArrayList<>(numberOfChunks);
        for (int i = 0; i < numberOfChunks; i++) {
            List<T> subL = list.subList(i * chunkSize, Math.min((i + 1) * chunkSize, list.size()));
            if(!subL.isEmpty()) {
                result.add(subL);
            }
        }
        return result;
    }

}
