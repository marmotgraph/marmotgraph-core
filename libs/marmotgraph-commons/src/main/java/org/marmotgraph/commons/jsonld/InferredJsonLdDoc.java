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

package org.marmotgraph.commons.jsonld;

import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.util.List;
import java.util.Objects;

public class InferredJsonLdDoc {
    //FOR INFERENCE
    public static final String INFERENCE_OF = "_inferenceOf";

    private final IndexedJsonLdDoc indexedJsonLdDoc;

    @Setter
    @Getter
    private JsonLdDoc alternatives;

    @Getter
    @Setter
    private List<String> inferenceOf;

    protected InferredJsonLdDoc(IndexedJsonLdDoc document) {
        this.indexedJsonLdDoc = document;
    }

    public static InferredJsonLdDoc create(){
        return new InferredJsonLdDoc(IndexedJsonLdDoc.create());
    }

    public static InferredJsonLdDoc from(IndexedJsonLdDoc indexedJsonLdDoc){
        return new InferredJsonLdDoc(indexedJsonLdDoc);
    }

    public static InferredJsonLdDoc from(NormalizedJsonLd document){
        return new InferredJsonLdDoc(IndexedJsonLdDoc.from(document));
    }

    public IndexedJsonLdDoc asIndexed(){
        return indexedJsonLdDoc;
    }

    public static boolean isInferenceOfKey(String key){
        return INFERENCE_OF.equals(key);
    }

    public boolean hasTypes(){
        List<String> types = asIndexed().getDoc().types();
        return types !=null && !types.isEmpty();
    }



}
