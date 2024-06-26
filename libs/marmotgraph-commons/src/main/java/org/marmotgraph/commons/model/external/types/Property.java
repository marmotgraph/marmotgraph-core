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

package org.marmotgraph.commons.model.external.types;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.List;
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Property extends DynamicJson {

    @JsonGetter(SchemaOrgVocabulary.IDENTIFIER)
    public String getIdentifier() {
        return getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.IDENTIFIER)
    public void setIdentifier(String identifier) {
        put(SchemaOrgVocabulary.IDENTIFIER, identifier);
    }

    @JsonGetter(SchemaOrgVocabulary.DESCRIPTION)
    public String getDescription() {
        return getAs(SchemaOrgVocabulary.DESCRIPTION, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.DESCRIPTION)
    public void setDescription(String description) {
        put(SchemaOrgVocabulary.DESCRIPTION, description);
    }

    @JsonGetter(SchemaOrgVocabulary.NAME)
    public String getName() {
        return getAs(SchemaOrgVocabulary.NAME, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.NAME)
    public void setName(String name) {
        put(SchemaOrgVocabulary.NAME, name);
    }

    @JsonGetter(EBRAINSVocabulary.META_OCCURRENCES)
    public Integer getOccurrences() {
        return getAs(EBRAINSVocabulary.META_OCCURRENCES, Integer.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_OCCURRENCES)
    public void setOccurrences(Integer occurrences) {
        put(EBRAINSVocabulary.META_OCCURRENCES, occurrences);
    }

    @JsonGetter(EBRAINSVocabulary.META_NAME_REVERSE_LINK)
    public String getNameForReverseLink() {
        return getAs(EBRAINSVocabulary.META_NAME_REVERSE_LINK, String.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_NAME_REVERSE_LINK)
    public void setNameForReverseLink(String nameForReverseLink) {
        put(EBRAINSVocabulary.META_NAME_REVERSE_LINK, nameForReverseLink);
    }

    @JsonGetter(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES)
    public List<TargetType> getTargetTypes() {
        return getAsListOf(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, TargetType.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES)
    public void setTargetTypes(List<TargetType> targetTypes) {
        put(EBRAINSVocabulary.META_PROPERTY_TARGET_TYPES, targetTypes);
    }

}
