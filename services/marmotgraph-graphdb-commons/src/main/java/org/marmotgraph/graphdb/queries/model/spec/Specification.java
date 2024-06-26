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

package org.marmotgraph.graphdb.queries.model.spec;

import org.marmotgraph.commons.model.Type;
import org.marmotgraph.graphdb.queries.model.fieldFilter.PropertyFilter;

import java.util.Collections;
import java.util.List;

public class Specification {
    private final List<SpecProperty> properties;
    private final PropertyFilter documentFilter;
    private final Type rootType;
    private final String responseVocab;


    public List<SpecProperty> getProperties() {
        return properties;
    }

    public Specification(List<SpecProperty> properties, PropertyFilter documentFilter, Type rootType, String responseVocab) {
        this.properties = properties ==null ? Collections.emptyList() : Collections.unmodifiableList(addUniqueAliasPostfixToProperties(properties, 0));
        this.documentFilter = documentFilter;
        this.rootType = rootType;
        this.responseVocab = responseVocab;
    }

    private List<SpecProperty> addUniqueAliasPostfixToProperties(List<SpecProperty> properties, Integer counter){
        for (SpecProperty property : properties) {
            property.setAliasPostfix(counter++);
            if(property.property!=null){
                addUniqueAliasPostfixToProperties(property.property, counter);
            }
        }
        return properties;
    }


    public String getResponseVocab() {
        return responseVocab;
    }

    public PropertyFilter getDocumentFilter() {
        return documentFilter;
    }

    public Type getRootType() {
        return rootType;
    }
}
