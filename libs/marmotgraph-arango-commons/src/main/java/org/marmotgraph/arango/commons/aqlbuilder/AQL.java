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

package org.marmotgraph.arango.commons.aqlbuilder;

import com.arangodb.model.AqlQueryOptions;
import org.marmotgraph.commons.model.PaginationParam;
import org.apache.commons.text.StringSubstitutor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AQL is a query builder which allows to build complex AQL queries including several conventions (such as document filters) and applies the {@link TrustedAqlValue} - a mechanism to prevent injection logic to be inserted into the queries.
 */
public class AQL {

    public static final String READ_ACCESS_BY_SPACE = "readAccessBySpace";
    public static final String READ_ACCESS_BY_INVITATION = "readAccessByInvitation";

    private final static ArangoKey WHITELIST_ALIAS = new ArangoKey("whitelist");
    private final static ArangoKey INVITATION_ALIAS = new ArangoKey("invitation");

    final Map<String, String> parameters = new TreeMap<>();
    private final StringBuilder query = new StringBuilder();
    private int indent = 0;
    private final AqlQueryOptions queryOptions = new AqlQueryOptions();
    private PaginationParam paginationParam;

    /**
     * With applying this method to a string, you state that you have checked, the provided string does not contain any unchecked "dynamic" part (such as user inputs).
     * <p>
     * The string should consist only of string constants, other trusted values and concatenations out of them. Dynamic parts can be defined with property placeholders - such as ${foo} since they are checked before insertion for their trustworthiness.
     * <p>
     * Please be careful when executing your checks! Passing non-validated strings to this method can introduce vulnerabilities to the system!!!
     */
    public static TrustedAqlValue trust(String trustedString) {
        return new TrustedAqlValue(trustedString);
    }

    public void specifyWhitelist() {
        addLine(trust("LET " + WHITELIST_ALIAS + "=@" + READ_ACCESS_BY_SPACE));
        addLine(trust("LET " + INVITATION_ALIAS + "=@" + READ_ACCESS_BY_INVITATION));
    }

    public AQL indent() {
        this.indent++;
        return this;
    }

    public AQL outdent() {
        this.indent = Math.max(this.indent - 1, 0);
        return this;
    }

    public AQL setParameter(String key, String value) {
        setTrustedParameter(key, preventAqlInjection(value));
        return this;
    }

    /**
     * Use with caution! The passed parameter will not be further checked for AQL injection but rather immediately added to the query!
     */
    public AQL setTrustedParameter(String key, TrustedAqlValue trustedAqlValue) {
        parameters.put(key, trustedAqlValue != null ? trustedAqlValue.getValue() : null);
        return this;
    }


    public AQL addLine(TrustedAqlValue... queryLine) {
        if (queryLine != null) {
            query.append('\n');
            query.append(createIndent());
            add(queryLine);
        }
        return this;
    }

    public AQL add(TrustedAqlValue... trustedAqlValue) {
        if (trustedAqlValue != null) {
            for (TrustedAqlValue aqlValue : trustedAqlValue) {
                if (aqlValue != null) {
                    query.append(aqlValue.getValue());
                }
            }
        }
        return this;
    }

    public void addComma() {
        query.append(", ");
    }


    private <T> TrustedAqlValue listValuesWithQuote(Character quote, Collection<T> values, Function<T, String> transform) {
        if (values != null && !values.isEmpty()) {
            String q = quote != null ? String.valueOf(quote) : "";
            return new TrustedAqlValue(q + String.join(q + "," + q, values.stream().map(transform).map(v -> preventAqlInjection(v).getValue()).collect(Collectors.toSet())) + q);
        } else {
            return new TrustedAqlValue("");
        }
    }

    public static TrustedAqlValue preventAqlInjection(String value) {
        return value != null ? new TrustedAqlValue(value.replaceAll("[^A-Za-z0-9\\-_:.#/@" + "]", "")) : null;
    }

    public TrustedAqlValue build() {
        return new TrustedAqlValue(StringSubstitutor.replace(query.toString(), parameters));
    }

    private String createIndent() {
        return "   ".repeat(Math.max(0, this.indent));
    }

    public void addDocumentFilter(TrustedAqlValue documentAlias) {
        addLine(new TrustedAqlValue("FILTER " + documentAlias.getValue() + " != NULL"));
    }

    public void addDocumentFilterWithWhitelistFilter(TrustedAqlValue documentAlias) {
        addDocumentFilter(documentAlias);
        addLine(trust("FILTER " + documentAlias.getValue() + "." + ArangoVocabulary.COLLECTION + " IN " + WHITELIST_ALIAS + " OR HAS(" + INVITATION_ALIAS+", "+documentAlias.getValue() + "." + ArangoVocabulary.KEY + ")"));
    }


    public void addPagination(PaginationParam paginationParam) {
        this.paginationParam = paginationParam;
        if (paginationParam != null && paginationParam.getSize() != null ) {
            queryOptions.fullCount(paginationParam.isReturnTotalResults());
            addLine(AQL.trust(String.format("LIMIT %d, %d", paginationParam.getFrom(), paginationParam.getSize())));
        } else {
            queryOptions.count(true);
        }
    }

    public PaginationParam getPaginationParam() {
        return paginationParam;
    }

    public AqlQueryOptions getQueryOptions() {
        return queryOptions;
    }

    public String buildSimpleDebugQuery(Map<String, Object> bindVars) {
        String aql = query.toString();
        for (Map.Entry<String,Object> entry : bindVars.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueRep;
            if(value == null){
                valueRep = "null";
            }
            else {
                if (value instanceof Collection) {
                    valueRep = "[\"" + ((Collection<?>) value).stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining("\",\"")) + "\"]";
                } else if (key.startsWith("@")) {
                    valueRep = "`" + value + "`";
                } else {
                    valueRep = "\"" + value + "\"";
                }
            }
            aql = aql.replaceAll(String.format("@%s(?=[^a-zA-Z])", key), valueRep);
        }
        return aql;
    }

}
