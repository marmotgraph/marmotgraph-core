/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2025 ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.commons.services;

import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class JsonAdapter {

    private final JsonMapper jsonMapper;

    public JsonAdapter() {
        this.jsonMapper = tools.jackson.databind.json.JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .build();
    }

    public String toJson(Object object) {
        try {
            if (object == null) {
                return null;
            } else if (object instanceof String string) {
                return string;
            }
            return jsonMapper.writer().writeValueAsString(object);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T fromJson(String payload, Class<T> clazz) {
        try {
            if (payload == null) {
                return null;
            }
            return jsonMapper.readValue(payload, clazz);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

}
