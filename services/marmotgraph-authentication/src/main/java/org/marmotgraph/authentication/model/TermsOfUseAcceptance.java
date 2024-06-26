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

package org.marmotgraph.authentication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.marmotgraph.arango.commons.aqlbuilder.ArangoVocabulary;

import java.util.List;

public class TermsOfUseAcceptance {

    @JsonProperty(ArangoVocabulary.KEY)
    private String key;

    private String userId;

    private List<AcceptedTermsOfUse> acceptedTermsOfUse;

    public TermsOfUseAcceptance() {
    }

    public TermsOfUseAcceptance(String key, String userId, List<AcceptedTermsOfUse> acceptedTermsOfUse) {
        this.key = key;
        this.userId = userId;
        this.acceptedTermsOfUse = acceptedTermsOfUse;
    }

    public String getKey() {
        return key;
    }

    public String getUserId() {
        return userId;
    }

    public List<AcceptedTermsOfUse> getAcceptedTermsOfUse() {
        return acceptedTermsOfUse;
    }
}
