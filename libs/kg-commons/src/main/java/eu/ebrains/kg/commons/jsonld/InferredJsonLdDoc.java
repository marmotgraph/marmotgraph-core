/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ebrains.kg.commons.jsonld;

import eu.ebrains.kg.commons.semantics.vocabularies.EBRAINSVocabulary;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InferredJsonLdDoc {
    //FOR INFERENCE
    public final static String INFERENCE_OF = "_inferenceOf";

    private final IndexedJsonLdDoc indexedJsonLdDoc;

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

    public List<JsonLdId> getInferenceOf(){
        List list = indexedJsonLdDoc.getDoc().getAs(INFERENCE_OF, List.class);
        if(list!=null){
            return (List<JsonLdId>)list.stream().map(r -> new JsonLdId((String)r)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void setInferenceOf(List<String> jsonLdIds){
        indexedJsonLdDoc.getDoc().addProperty(INFERENCE_OF, jsonLdIds);
    }

    public void setAlternatives(JsonLdDoc alternatives){
        indexedJsonLdDoc.getDoc().addProperty(EBRAINSVocabulary.META_ALTERNATIVE, alternatives);
    }

    public boolean hasTypes(){
        List<String> types = asIndexed().getDoc().getTypes();
        return types !=null && !types.isEmpty();
    }



}
