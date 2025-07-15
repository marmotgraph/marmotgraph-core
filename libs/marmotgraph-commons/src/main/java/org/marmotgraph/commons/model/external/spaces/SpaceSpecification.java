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

package org.marmotgraph.commons.model.external.spaces;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.marmotgraph.commons.semantics.vocabularies.MarmotGraphVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;


public class SpaceSpecification {

    @JsonProperty(SchemaOrgVocabulary.NAME)
    private String name;

    @JsonProperty(SchemaOrgVocabulary.IDENTIFIER)
    private String identifier;

    @JsonProperty(MarmotGraphVocabulary.META_AUTORELEASE_SPACE)
    private Boolean autoRelease;

    @JsonProperty(MarmotGraphVocabulary.META_DEFER_CACHE_SPACE)
    private Boolean deferCache;

    @JsonProperty(MarmotGraphVocabulary.META_CLIENT_SPACE)
    private Boolean clientSpace;

    @JsonProperty(MarmotGraphVocabulary.META_SCOPE_RELEVANT_SPACE)
    private Boolean scopeRelevant;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Boolean getAutoRelease() {
        return autoRelease;
    }

    public void setAutoRelease(Boolean autoRelease) {
        this.autoRelease = autoRelease;
    }

    public Boolean getClientSpace() {
        return clientSpace;
    }

    public void setClientSpace(Boolean clientSpace) {
        this.clientSpace = clientSpace;
    }

    public Boolean getDeferCache() {
        return deferCache;
    }

    public void setDeferCache(Boolean deferCache) {
        this.deferCache = deferCache;
    }

    public Boolean getScopeRelevant() {
        return scopeRelevant;
    }

    public void setScopeRelevant(Boolean scopeRelevant) {
        this.scopeRelevant = scopeRelevant;
    }

    @JsonIgnore
    public Space toSpace(){
       return new Space(new SpaceName(name), autoRelease != null && autoRelease, false, deferCache != null && deferCache, scopeRelevant != null && scopeRelevant);
    }

}
