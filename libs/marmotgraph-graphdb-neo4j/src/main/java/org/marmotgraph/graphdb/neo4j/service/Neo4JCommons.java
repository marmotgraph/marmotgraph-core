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

package org.marmotgraph.graphdb.neo4j.service;

import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;

import java.net.URI;
import java.net.URISyntaxException;

public class Neo4JCommons {

    static boolean isValidURI(String uriStr) {
        if (uriStr == null || uriStr.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(uriStr);
            return uri.getScheme() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }


    static String sanitizeLabel(String label){
        if(label!=null) {
            while (label.startsWith("_")) {
                label = label.substring(1);
            }
            if(label.equals("@id")){
                return "_id";
            }
            return label.replace("-", "_").replaceAll("[^a-zA-Z0-9_]", "");
        }
        return null;
    }

    static String getStageLabel(DataStage stage, String prefix) {
        return String.format("%s_STG_%s", prefix, stage.getAbbreviation());
    }


    static String getSpaceLabel(SpaceName spaceName, String prefix, String defaultValue){
        return spaceName != null && !spaceName.getName().isBlank() ? String.format("%s_SPC_%s", prefix, Neo4JCommons.sanitizeLabel(spaceName.getName())) : defaultValue;
    }


}
