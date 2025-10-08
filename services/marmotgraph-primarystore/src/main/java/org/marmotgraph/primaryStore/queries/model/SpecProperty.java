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

package org.marmotgraph.primaryStore.queries.model;

import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.primaryStore.queries.model.fieldFilter.PropertyFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpecProperty {

    public enum SingleItemStrategy{
        FIRST, CONCAT
    }

    public final SingleItemStrategy singleItem;
    public String propertyName;
    public final List<SpecProperty> property;
    public final List<SpecTraverse> path;
    @Setter
    @Getter
    private transient Integer aliasPostfix;
    @Getter
    public boolean required;
    @Getter
    public boolean sort;
    @Getter
    public boolean groupBy;
    public boolean ensureOrder;
    public final String groupedInstances;
    public PropertyFilter propertyFilter;
    public final Map<String, Object> customDirectives;

    public SpecProperty(String fieldName, List<SpecProperty> property, List<SpecTraverse> path, String groupedInstances, boolean required, boolean sort, boolean groupBy, boolean ensureOrder, PropertyFilter propertyFilter, SingleItemStrategy singleItem) {
        this(fieldName, property, path, groupedInstances, required, sort, groupBy, ensureOrder, propertyFilter, singleItem, null);
    }

    public SpecProperty(String fieldName, List<SpecProperty> property, List<SpecTraverse> path, String groupedInstances, boolean required, boolean sort, boolean groupBy, boolean ensureOrder, PropertyFilter propertyFilter, SingleItemStrategy singleItem, Map<String, Object> customDirectives) {
        this.propertyName = fieldName;
        this.required = required;
        this.property = property != null ? new ArrayList<>(property) : new ArrayList<>();
        this.path = path ==null ? Collections.emptyList() : Collections.unmodifiableList(path);
        this.sort = sort;
        this.groupBy = groupBy;
        this.groupedInstances = groupedInstances;
        this.ensureOrder = ensureOrder;
        this.propertyFilter = propertyFilter;
        this.customDirectives = customDirectives;
        this.singleItem = singleItem;
    }

    public boolean isDirectChild(){
        return !hasSubProperties() && path.size()<2;
    }


    public boolean hasSubProperties(){
        //TODO check how to handle merges
        return property !=null && !property.isEmpty();
    }

    public boolean isLeaf(){
        return property.isEmpty();
    }

    public boolean isMerge(){
        return this.path.isEmpty() && !this.property.isEmpty();
    }

    public boolean needsTraversal(){
        return !property.isEmpty() || this.path.size()>1;
    }


    public SpecTraverse getLeafPath(){
        if(isLeaf() && !path.isEmpty()){
            return path.getLast();
        }
        return null;
    }

    public boolean hasGrouping(){
        if(groupedInstances!=null && !groupedInstances.isEmpty() && property !=null && !property.isEmpty()){
            for (SpecProperty field : property) {
                if(field.isGroupBy()){
                    return true;
                }
            }
        }
        return false;
    }

}
