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

package org.marmotgraph.primaryStore.instances.service;

import lombok.AllArgsConstructor;
import org.marmotgraph.commons.jsonld.*;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The reconciliation mechanism allows to unify documents talking about the same entity. The tricky part is the detection
 * of which instances are contributing to the same entity. This is solved by known-semantics. The system can think of
 * these information sources:
 * 1. Linking via shared identifiers ({@link SchemaOrgVocabulary#IDENTIFIER})
 * <p>
 * The reconciliation of stable links is rather straight forward: If the linking information is available already at the
 * creation time of an instance and is not manipulated anymore, it will just never get its own id in the inferred space.
 * <p>
 * But since this information is encoded in the payloads, it can always be subject of change (in case 1) or depends on the lifecycle of an additional resource (case 2).
 * It can therefore happen, that instances are temporarily reconciled and/or reconciled in a later step.
 * <p>
 * Here is a small documentation of possible cases:
 * <p>
 * SETUP
 * ======
 * bar.identifier="bar"
 * INSERT bar -&gt;Reconcile "bar" (with info of bar)
 * <p>
 * <p>
 * LINK KNOWN AT INSERTION TIME
 * ============================
 * foo.identifier=["foo", "bar"]
 * <p>
 * INSERT foo -&gt; Reconcile "bar" (with info of bar and foo)
 * UPDATE foo
 * -&gt; foo.identifier==["foo", "bar"] -&gt; Reconcile "bar" (with info of bar and foo)
 * -&gt; foo.identifier==["foo"] -&gt; Reconcile "foo" (with info of foo) and "bar" (with info of bar)
 * DELETE foo -&gt; Reconcile bar (with info of bar)
 * <p>
 * MERGE
 * ===============
 * foo.identifier=["foo"]
 * INSERT foo -&gt; Reconcile "foo" (with info of foo)
 * UPDATE foo -&gt; foo.identifier==["foo", "bar"] -&gt; Reconcile "fooBar" (with identifiers &amp; 302 redirects for "foo" and "bar") (MERGE)
 * <p>
 * MERGE &amp; DELETE
 * ==============
 * foo.identifier=["foo"]
 * INSERT foo -&gt; Reconcile "foo" (with info of foo)
 * UPDATE foo -&gt; foo.identifier==["foo", "bar"] -&gt; Reconcile "fooBar" (with identifiers &amp; 302 redirects for "foo" and "bar") (MERGE)
 * DELETE foo -&gt; Reconcile fooBar (with info of bar, answer queries to "fooBar" with 302 redirect to "bar" since there is only one left)
 * <p>
 * Reconciliation is currently only possible within the same space (since it would mess with the access permissions otherwise)
 * <p>
 * Currently, the reconcile mechanism has the following restrictions:
 * - It only supports instances in the same space (materialized merging of instances across spaces messes up access permissions).
 */
@AllArgsConstructor
@Component
public class Reconcile {


    public InferredJsonLdDoc reconcile(List<NormalizedJsonLd> sourceDocuments) {
        return merge(sourceDocuments);
    }

    private JsonLdDoc createAlternative(String key, Object value, boolean selected, List<JsonLdId> users) {
        if (!DynamicJson.isInternalKey(key) && !JsonLdConsts.isJsonLdConst(key) && !SchemaOrgVocabulary.IDENTIFIER.equals(key) && !EBRAINSVocabulary.META_USER.equals(key) && !EBRAINSVocabulary.META_SPACE.equals(key) && !EBRAINSVocabulary.META_PROPERTYUPDATES.equals(key)) {
            JsonLdDoc alternative = new JsonLdDoc();
            alternative.put(EBRAINSVocabulary.META_SELECTED, selected);
            //We always save the users of an alternative as string only to prevent links to be created - the resolution happens lazily.
            alternative.put(EBRAINSVocabulary.META_USER, users.stream().filter(Objects::nonNull).map(JsonLdId::getId).collect(Collectors.toList()));
            alternative.put(EBRAINSVocabulary.META_VALUE, value);
            return alternative;
        }
        return null;
    }

    private InferredJsonLdDoc merge(List<NormalizedJsonLd> originalInstances) {
        if (CollectionUtils.isEmpty(originalInstances)) {
            return null;
        }
        InferredJsonLdDoc inferredDocument = InferredJsonLdDoc.create();
        Set<String> keys = originalInstances.stream().map(LinkedHashMap::keySet).flatMap(Set::stream).filter(k -> !DynamicJson.isInternalKey(k)).collect(Collectors.toSet());
        JsonLdDoc alternatives = new JsonLdDoc();
        inferredDocument.setAlternatives(alternatives);
        for (String key : keys) {
            List<NormalizedJsonLd> documentsForKey = originalInstances.stream().filter(i -> i.containsKey(key) || i.getAs(EBRAINSVocabulary.META_PROPERTYUPDATES, Map.class, Collections.emptyMap()).containsKey(key)).collect(Collectors.toList());
            if (documentsForKey.size() == 1) {
                //Single occurrence - the merge is easy. :)
                NormalizedJsonLd doc = documentsForKey.getFirst();
                final Object value = doc.get(key);
                //We only add the property to the inferred document if it is not-null.
                if (value != null) {
                    inferredDocument.asIndexed().getDoc().addProperty(key, value);
                }
                JsonLdDoc alternative = createAlternative(key, doc.get(key), true, Collections.singletonList(doc.getAs(EBRAINSVocabulary.META_USER, JsonLdId.class)));
                if (alternative != null) {
                    alternatives.put(key, Collections.singletonList(alternative));
                }
            } else if (documentsForKey.size() > 1) {
                sortByFieldChangeDate(key, documentsForKey);
                NormalizedJsonLd firstDoc = documentsForKey.getFirst();
                switch (key) {
                    case JsonLdConsts.ID:
                        List<JsonLdId> distinctIds = documentsForKey.stream().map(JsonLdDoc::id).distinct().toList();
                        if (distinctIds.size() == 1) {
                            inferredDocument.asIndexed().getDoc().setId(distinctIds.getFirst());
                        }
                        //We don't handle the ID merging - if there are conflicting ids, we create a new one - but this is in the responsibility of the event generation process.
                        break;
                    case SchemaOrgVocabulary.IDENTIFIER:
                        Set<String> identifiers = documentsForKey.stream().map(JsonLdDoc::identifiers).flatMap(Collection::stream).collect(Collectors.toSet());
                        inferredDocument.asIndexed().getDoc().put(SchemaOrgVocabulary.IDENTIFIER, identifiers);
                        break;
                    default:
                        final Object propertyValue = firstDoc.get(key);
                        //We only add the property to the inferred document if it is not-null.
                        if (propertyValue != null) {
                            inferredDocument.asIndexed().getDoc().addProperty(key, propertyValue);
                        }
                        Object nullGroup = new Object();
                        Map<Object, List<NormalizedJsonLd>> documentsByValue = documentsForKey.stream().collect(Collectors.groupingBy(d -> d.getOrDefault(key, nullGroup)));
                        final List<JsonLdDoc> alternativePayloads = documentsByValue.keySet().stream().map(value -> {
                            List<NormalizedJsonLd> docs = documentsByValue.get(value);
                            return createAlternative(key, value == nullGroup ? null : value, docs.contains(firstDoc), docs.stream().filter(d -> d.getAs(EBRAINSVocabulary.META_USER, NormalizedJsonLd.class) != null).map(doc -> doc.getAs(EBRAINSVocabulary.META_USER, NormalizedJsonLd.class).id()).distinct().collect(Collectors.toList()));
                        }).filter(Objects::nonNull).collect(Collectors.toList());
                        if (!CollectionUtils.isEmpty(alternativePayloads)) {
                            alternatives.put(key, alternativePayloads);
                        } else {
                            //FIXME do we want to remove this code? It is cleaning up some data and therefore is rather defensive
                            alternatives.remove(key);
                        }
                        break;
                }
            }
        }
        return inferredDocument;
    }

    private void sortByFieldChangeDate(String key, List<NormalizedJsonLd> documentsForKey) {
        documentsForKey.sort((o1, o2) -> {
            ZonedDateTime dateTime1 = o1 != null && o1.getFieldUpdateTimes() != null ? o1.getFieldUpdateTimes().get(key) : null;
            ZonedDateTime dateTime2 = o2 != null && o2.getFieldUpdateTimes() != null ? o2.getFieldUpdateTimes().get(key) : null;
            if (dateTime1 != null) {
                return dateTime2 == null || dateTime1.isBefore(dateTime2) ? 1 : dateTime1.equals(dateTime2) ? 0 : -1;
            }
            return dateTime2 == null ? 0 : -1;
        });
    }
}
