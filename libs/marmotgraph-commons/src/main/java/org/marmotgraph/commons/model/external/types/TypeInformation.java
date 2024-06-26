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

import com.fasterxml.jackson.annotation.*;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;

import java.util.List;
@JsonPropertyOrder(alphabetic=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TypeInformation extends DynamicJson {

    @JsonGetter(SchemaOrgVocabulary.DESCRIPTION)
    public String getDescription() {
        return getAs(SchemaOrgVocabulary.DESCRIPTION, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.DESCRIPTION)
    public void setDescription(String description) {
        put(SchemaOrgVocabulary.DESCRIPTION, description);
    }

    @JsonGetter(SchemaOrgVocabulary.IDENTIFIER)
    public String getIdentifier() {
        return getAs(SchemaOrgVocabulary.IDENTIFIER, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.IDENTIFIER)
    public void setIdentifier(String identifier) {
        put(SchemaOrgVocabulary.IDENTIFIER, identifier);
    }

    @JsonGetter(SchemaOrgVocabulary.NAME)
    public String getName() {
        return getAs(SchemaOrgVocabulary.NAME, String.class);
    }

    @JsonSetter(SchemaOrgVocabulary.NAME)
    public void setName(String name) {
        put(SchemaOrgVocabulary.NAME, name);
    }

    @JsonGetter(EBRAINSVocabulary.META_INCOMING_LINKS)
    public List<IncomingLink> getIncomingLinks() {
        return getAsListOf(EBRAINSVocabulary.META_INCOMING_LINKS, IncomingLink.class);
    }
    @JsonSetter(EBRAINSVocabulary.META_INCOMING_LINKS)
    public void setIncomingLinks(List<IncomingLink> incomingLinks) {
        put(EBRAINSVocabulary.META_INCOMING_LINKS, incomingLinks);
    }

    @JsonGetter(EBRAINSVocabulary.META_OCCURRENCES)
    public Integer getOccurrences() {
        return getAs(EBRAINSVocabulary.META_OCCURRENCES, Integer.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_OCCURRENCES)
    public void setOccurrences(Integer occurrences) {
        put(EBRAINSVocabulary.META_OCCURRENCES, occurrences);
    }

    @JsonGetter(EBRAINSVocabulary.META_PROPERTIES)
    public List<Property> getProperties() {
        return getAsListOf(EBRAINSVocabulary.META_PROPERTIES, Property.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_PROPERTIES)
    public void setProperties(List<Property> properties) {
        put(EBRAINSVocabulary.META_PROPERTIES, properties);
    }

    @JsonGetter(EBRAINSVocabulary.META_SPACES)
    public List<SpaceTypeInformation> getSpaces() {
        return getAsListOf(EBRAINSVocabulary.META_SPACES, SpaceTypeInformation.class);
    }

    @JsonSetter(EBRAINSVocabulary.META_SPACES)
    public void setSpaces(List<SpaceTypeInformation> spaces) {
        put(EBRAINSVocabulary.META_SPACES, spaces);
    }

    @JsonIgnore
    public void clearSpaces(){
        remove(EBRAINSVocabulary.META_SPACES);
    }


    @JsonIgnore
    public void clearProperties(){
        remove(EBRAINSVocabulary.META_PROPERTIES);
    }

}
