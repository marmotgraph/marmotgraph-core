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

package org.marmotgraph.core.api.v3;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.marmotgraph.commons.Version;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.config.openApiGroups.Admin;
import org.marmotgraph.commons.markers.WritesData;
import org.marmotgraph.core.api.examples.InstancesExamples;
import org.marmotgraph.core.controller.CoreCacheController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Version.V3 +"/cache")
public class CacheV3 {

    private final CoreCacheController cacheController;

    public CacheV3(CoreCacheController cacheController) {
        this.cacheController = cacheController;
    }

    @Operation(summary = "Get all keys")
    @GetMapping("/keys")
    @Admin
    public List<String> getCacheKeys() {
        return this.cacheController.getKeys();
    }

    @Operation(summary = "Flush keys")
    @PostMapping("/keys/flush")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
            @ExampleObject(name = "All keys", description = "Flush all keys", value = "[\"all\"]"),
            @ExampleObject(name = "List of keys", description = "List of key to flush", value = "[\"" + CacheConstant.CACHE_KEYS_SPACES + "\", \"" + CacheConstant.CACHE_KEYS_SPACE_SPECIFICATIONS + "\"]"),
    }))
    @Admin
    public List<String> flushCache(@RequestBody List<String> keys)
    {
        if (keys.contains("*") || keys.isEmpty() || keys.contains("all")) {
            return this.cacheController.clearAllKeys();
        }

        return this.cacheController.clearKeys(keys);
    }


}
