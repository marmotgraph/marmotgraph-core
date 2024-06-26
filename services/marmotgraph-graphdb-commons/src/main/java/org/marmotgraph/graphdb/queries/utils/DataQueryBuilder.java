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

package org.marmotgraph.graphdb.queries.utils;

import org.marmotgraph.arango.commons.aqlbuilder.*;
import org.marmotgraph.arango.commons.model.AQLQuery;
import org.marmotgraph.arango.commons.model.ArangoCollectionReference;
import org.marmotgraph.arango.commons.model.InternalSpace;
import org.marmotgraph.commons.jsonld.InstanceId;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.Type;
import org.marmotgraph.graphdb.queries.model.fieldFilter.Op;
import org.marmotgraph.graphdb.queries.model.fieldFilter.PropertyFilter;
import org.marmotgraph.graphdb.queries.model.spec.SpecProperty;
import org.marmotgraph.graphdb.queries.model.spec.SpecTraverse;
import org.marmotgraph.graphdb.queries.model.spec.Specification;
import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.marmotgraph.arango.commons.aqlbuilder.AQL.*;

public class DataQueryBuilder {

    private final Specification specification;
    private final PaginationParam pagination;
    private final AQL q;
    private final Map<String, String> filterValues;
    private final Map<String, Object> bindVars = new HashMap<>();

    private final List<ArangoCollectionReference> existingCollections;
    private final Map<String, Object> whiteListFilter;
    private final List<String> spaceRestriction;

    private final InstanceId idRestriction;

    public static ArangoAlias fromSpecField(SpecProperty specField) {
        return new ArangoAlias(String.format("%s_%d", specField.propertyName, specField.getAliasPostfix()));
    }

    private static ArangoCollectionReference fromSpecTraversal(SpecTraverse traverse) {
        return new ArangoCollectionReference(new ArangoKey(traverse.pathName).getValue(), true);
    }

    public AQLQuery build() {
        //Define the global parameters
        ArangoAlias rootAlias = new ArangoAlias("root");

        //Setup the root instance
        defineRootInstance();

        if(spaceRestriction!=null){
            bindVars.put("spaceRestriction", spaceRestriction);
        }

        addDocumentFilterWithWhitelistFilter(q, rootAlias.getArangoDocName(), whiteListFilter, spaceRestriction);

        //Define the complex fields (the ones with traversals)
        q.add(new TraverseBuilder(rootAlias, specification.getProperties()).getTraversedProperty());

        //Define filters
        q.add(new FilterBuilder(rootAlias, specification.getDocumentFilter(), specification.getProperties()).getFilter());

        //Define sorting
        q.addLine(new SortBuilder(rootAlias, specification.getProperties()).getSort());

        //Pagination
        if(pagination != null && idRestriction != null){
            //If the query is id restricted we might not need the size nor the total results
            if(pagination.getSize()==null || pagination.getSize() > 0) {
                pagination.setSize(null);
                pagination.setReturnTotalResults(false);
            }
        }
        q.addPagination(pagination);

        //Define return value
        q.add(new ReturnBuilder(rootAlias, null, specification.getProperties()).getReturnStructure());

        return new AQLQuery(q, bindVars);
    }

    public DataQueryBuilder(Specification specification, PaginationParam pagination, Map<String, Object> whitelistFilter, List<String> spaceRestriction, InstanceId idRestriction, Map<String, String> filterValues, List<ArangoCollectionReference> existingCollections) {
        this.q = new AQL();
        this.specification = specification;
        this.pagination = pagination;
        this.filterValues = filterValues == null ? Collections.emptyMap() : new HashMap<>(filterValues);
        this.existingCollections = existingCollections;
        this.whiteListFilter = whitelistFilter;
        this.spaceRestriction = spaceRestriction;
        this.idRestriction = idRestriction;
    }

    public void defineRootInstance() {
        if (whiteListFilter != null) {
            this.q.specifyWhitelist();
            this.bindVars.putAll(whiteListFilter);
        }
        if(idRestriction != null) {
            this.q.addLine(trust("LET root_doc = DOCUMENT(@@rootCollection, @rootId)"));
            this.bindVars.put("@rootCollection", ArangoCollectionReference.fromSpace(idRestriction.getSpace()).getCollectionName());
            this.bindVars.put("rootId",idRestriction.getUuid().toString());
        }
        else {
            this.q.addLine(trust("FOR root_doc IN 1..1 OUTBOUND DOCUMENT(@@typeCollection, @typeId) @@typeRelation"));
            ArangoCollectionReference collectionReference = ArangoCollectionReference.fromSpace(InternalSpace.TYPE_SPACE);
            this.bindVars.put("@typeCollection", collectionReference.getCollectionName());
            this.bindVars.put("@typeRelation", InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName());
            final Type rootType = this.specification.getRootType();
            this.bindVars.put("typeId", collectionReference.docWithStableId(rootType.getName()).getDocumentId().toString());
        }
        this.q.addLine(trust(""));
    }

    static TrustedAqlValue getRepresentationOfField(ArangoAlias alias, SpecProperty field) {
        AQL representation = new AQL();
        if (field.isDirectChild()) {
            return representation.add(trust("${parentAlias}.`${originalKey}`")).setTrustedParameter("parentAlias", alias.getArangoDocName()).setParameter("originalKey", field.getLeafPath().pathName).build();
        } else if (field.hasGrouping()) {
            return representation.add(preventAqlInjection(fromSpecField(field).getArangoName() + "_grp")).build();
        } else {
            return representation.add(preventAqlInjection(fromSpecField(field).getArangoName())).build();
        }
    }


    private static void addDocumentFilterWithWhitelistFilter(AQL aql, TrustedAqlValue documentAlias, Map<String, Object> whitelistFilter, List<String> spaceRestriction) {
        if(whitelistFilter!=null){
            aql.addDocumentFilterWithWhitelistFilter(documentAlias);
        }
        if(spaceRestriction!=null) {
            aql.addLine(trust("FILTER " + documentAlias.getValue() + "." + ArangoVocabulary.COLLECTION + " IN @spaceRestriction"));
        }
    }


    private class TraverseBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias parentAlias;


        public TraverseBuilder(ArangoAlias parentAlias, List<SpecProperty> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }

        private List<SpecProperty> propertiesWithTraversal() {
            return fields.stream().filter(SpecProperty::needsTraversal).filter(f -> !f.isMerge()).collect(Collectors.toList());
        }

        TrustedAqlValue handleTraverse(boolean hasSort, SpecProperty.SingleItemStrategy singleItemStrategy, SpecTraverse traverse, ArangoAlias alias, Stack<ArangoAlias> aliasStack, boolean ensureOrder) {
            AQL aql = new AQL();
            aql.add(trust("LET ${alias} = "));
            if (singleItemStrategy != null) {
                switch (singleItemStrategy) {
                    case FIRST:
                        aql.add(trust("FIRST("));
                        break;
                    case CONCAT:
                        aql.add(trust("CONCAT_SEPARATOR(\", \", "));
                        break;
                }
            }
            if (aliasStack.size() == 1 && hasSort) {
                aql.add(trust("(FOR ${alias}_sort IN "));
            }
            aql.add(trust(ensureOrder ? "(" : "UNIQUE("));
            aql.add(trust("FLATTEN("));
            aql.indent().add(trust("FOR ${aliasDoc} "));
            boolean traverseExists = traverseExists(traverse);
            if (traverseExists) {
                if (ensureOrder) {
                    aql.add(trust(", ${aliasDoc}_e"));
                }
                aql.addLine(trust(" IN 1..1 ${direction} ${parentAliasDoc} `${edgeCollection}`"));
            } else {
                //TODO if the collection doesn't exist, the query could be simplified a lot - so there is quite some potential for optimization.
                aql.addLine(trust(" IN [] "));
            }
            addDocumentFilterWithWhitelistFilter(aql, alias.getArangoDocName(), whiteListFilter, spaceRestriction);
            if (traverse.typeRestrictions != null) {
                aql.addLine(trust(" FILTER "));
                for (int i = 0; i < traverse.typeRestrictions.size(); i++) {
                    aql.add(trust(" \"${aliasDoc_typefilter_" + i + "}\" IN ${aliasDoc}.`@type`"));
                    if (i < traverse.typeRestrictions.size() - 1) {
                        aql.add(trust(" OR "));
                    }
                    aql.setParameter("aliasDoc_typefilter_" + i, traverse.typeRestrictions.get(i).getName());
                }
            }
            if (traverseExists && ensureOrder) {
                aql.addLine(trust("SORT ${aliasDoc}_e." + ArangoVocabulary.ORDER_NUMBER + " ASC"));
            }
            aql.setTrustedParameter("alias", alias);
            aql.setTrustedParameter("aliasDoc", alias.getArangoDocName());
            aql.setParameter("direction", traverse.reverse ? "INBOUND" : "OUTBOUND");
            aql.setTrustedParameter("parentAliasDoc", aliasStack.peek().getArangoDocName());
            aql.setParameter("edgeCollection", fromSpecTraversal(traverse).getCollectionName());
            aql.setParameter("fieldPath", traverse.pathName);
            return aql.build();
        }


        TrustedAqlValue finalizeTraversalWithSubfields(SpecProperty field, Stack<ArangoAlias> aliasStack) {
            AQL aql = new AQL();
            aql.addLine(new TraverseBuilder(aliasStack.peek(), field.property).getTraversedProperty());
            return aql.build();
        }

        boolean traverseExists(SpecTraverse traverse) {
            return existingCollections.contains(fromSpecTraversal(traverse));
        }


        TrustedAqlValue getTraversedProperty() {
            List<SpecProperty> traversalProperties = propertiesWithTraversal();
            if (!traversalProperties.isEmpty()) {
                AQL properties = new AQL();
                for (SpecProperty traversalProperty : traversalProperties) {
                    ArangoAlias traversalFieldAlias = fromSpecField(traversalProperty);
                    ArangoAlias alias = traversalFieldAlias;
                    Stack<ArangoAlias> aliasStack = new Stack<>();
                    aliasStack.push(parentAlias);
                    SpecProperty.SingleItemStrategy singleItemStrategy = traversalProperty.singleItem;
                    for (SpecTraverse traverse : traversalProperty.path) {
                        boolean lastTraversal;
                        if (!traversalProperty.hasSubProperties()) {
                            lastTraversal = traversalProperty.path.size() < 2 || traverse == traversalProperty.path.get(traversalProperty.path.size() - 2);
                        } else {
                            lastTraversal = traverse == traversalProperty.path.get(traversalProperty.path.size() - 1);
                        }
                        properties.addLine(handleTraverse(traversalProperty.isSort(), singleItemStrategy, traverse, alias, aliasStack, traversalProperty.ensureOrder));
                        singleItemStrategy = null;
                        aliasStack.push(alias);
                        if (lastTraversal) {
                            if (traversalProperty.hasSubProperties()) {
                                properties.add(finalizeTraversalWithSubfields(traversalProperty, aliasStack));
                            }
                        }
                        if (lastTraversal) {
                            break;
                        }
                        alias = alias.increment();
                    }

                    properties.add(new FilterBuilder(alias, traversalProperty.propertyFilter, traversalProperty.property).getFilter());
                    //fields.add(new SortBuilder(alias, traversalField.fields).getSort());
                    properties.addLine(new ReturnBuilder(alias, traversalProperty, traversalProperty.property).getReturnStructure());
                    while (aliasStack.size() > 1) {
                        ArangoAlias a = aliasStack.pop();
                        properties.addLine(trust("))"));

                        if (aliasStack.size() > 1) {
                            AQL returnStructure = new AQL();
                            returnStructure.addLine(trust("RETURN DISTINCT ${traverseField}"));
                            returnStructure.setParameter("traverseField", a.getArangoName());
                            properties.addLine(returnStructure.build());
                        } else if (aliasStack.size() == 1) {
                            if (traversalProperty.isSort()) {
                                AQL sortStructure = new AQL();
                                sortStructure.indent().addLine(trust("SORT ${traverseField}_sort ASC"));
                                sortStructure.addLine(trust("RETURN ${traverseField}_sort")).outdent();
                                sortStructure.addLine(trust(")"));
                                sortStructure.setParameter("traverseField", a.getArangoName());
                                properties.addLine(sortStructure.build());
                            }
                            if (traversalProperty.singleItem != null) {
                                properties.addLine(trust(")"));
                            }
                        }
                    }
                    if (traversalProperty.hasGrouping()) {
                        handleGrouping(properties, traversalProperty, traversalFieldAlias);
                    }

                }
                return properties.build();
            } else {
                //return new ReturnBuilder(parentAlias, fields).getReturnStructure();
                return null;
            }
        }

        private void handleGrouping(AQL fields, SpecProperty traversalField, ArangoAlias traversalFieldAlias) {
            AQL group = new AQL();
            group.addLine(trust("LET ${traverseField}_grp = (FOR ${traverseField}_grp_inst IN ${traverseField}"));
            group.indent().add(trust("COLLECT "));

            List<SpecProperty> groupByFields = traversalField.property.stream().filter(SpecProperty::isGroupBy).collect(Collectors.toList());
            for (SpecProperty groupByField : groupByFields) {
                AQL groupField = new AQL();
                groupField.add(trust("`${field}` = ${traverseField}_grp_inst.`${field}`"));
                if (groupByField != groupByFields.get(groupByFields.size() - 1)) {
                    groupField.addComma();
                }
                groupField.setParameter("field", groupByField.propertyName);
                group.addLine(groupField.build());
            }
            group.addLine(trust("INTO ${traverseField}_group"));
            group.addLine(trust("LET ${traverseField}_instances = ( FOR ${traverseField}_group_el IN ${traverseField}_group"));
            List<SpecProperty> notGroupedByFields = traversalField.property.stream().filter(f -> !f.isGroupBy()).collect(Collectors.toList());

            sortNotGroupedFields(group, notGroupedByFields);

            group.addLine(trust("RETURN {"));

            for (SpecProperty notGroupedByField : notGroupedByFields) {
                AQL notGroupedField = new AQL();
                notGroupedField.add(trust("\"${field}\": ${traverseField}_group_el.${traverseField}_grp_inst.`${field}`"));
                if (notGroupedByField != notGroupedByFields.get(notGroupedByFields.size() - 1)) {
                    notGroupedField.addComma();
                }
                notGroupedField.setParameter("field", notGroupedByField.propertyName);
                group.addLine(notGroupedField.build());
            }

            group.addLine(trust("})"));

            sortGroupedFields(group, groupByFields);

            group.addLine(trust("RETURN {"));

            for (SpecProperty groupByField : groupByFields) {
                AQL groupField = new AQL();
                groupField.add(trust("\"${field}\": `${field}`"));
                groupField.addComma();
                groupField.setParameter("field", groupByField.propertyName);
                group.addLine(groupField.build());
            }
            group.addLine(trust("\"${collectField}\": ${traverseField}_instances"));
            group.addLine(trust("})"));
            group.setParameter("traverseField", traversalFieldAlias.getArangoName());
            group.setParameter("collectField", traversalField.groupedInstances);
            fields.addLine(group.build());
        }

        private void sortGroupedFields(AQL group, List<SpecProperty> groupByFields) {
            List<SpecProperty> groupedSortFields = groupByFields.stream().filter(SpecProperty::isSort).collect(Collectors.toList());
            if (!groupedSortFields.isEmpty()) {
                group.add(trust("SORT "));
                for (SpecProperty specField : groupedSortFields) {
                    AQL groupSort = new AQL();
                    groupSort.add(trust("`${field}`"));
                    if (specField != groupedSortFields.get(groupedSortFields.size() - 1)) {
                        groupSort.addComma();
                    }
                    groupSort.setParameter("field", specField.propertyName);
                    group.add(groupSort.build());
                }
                group.add(trust(" ASC"));
            }
        }

        private void sortNotGroupedFields(AQL group, List<SpecProperty> notGroupedByFields) {
            List<SpecProperty> notGroupedSortFields = notGroupedByFields.stream().filter(SpecProperty::isSort).collect(Collectors.toList());
            if (!notGroupedSortFields.isEmpty()) {
                group.add(trust("SORT "));
                for (SpecProperty notGroupedSortField : notGroupedSortFields) {
                    AQL notGroupedSort = new AQL();
                    notGroupedSort.add(trust("${traverseField}_group_el.${traverseField}_grp_inst.`${field}`"));
                    if (notGroupedSortField != notGroupedSortFields.get(notGroupedSortFields.size() - 1)) {
                        notGroupedSort.addComma();
                    }
                    notGroupedSort.setParameter("field", notGroupedSortField.propertyName);
                    group.add(notGroupedSort.build());
                }
                group.addLine(trust(" ASC"));
            }
        }

    }


    private static class ReturnBuilder {

        private final List<SpecProperty> properties;
        private final ArangoAlias parentAlias;
        private final SpecProperty parentProperty;

        public ReturnBuilder(ArangoAlias parentAlias, SpecProperty parentProperty, List<SpecProperty> properties) {
            this.properties = properties;
            this.parentAlias = parentAlias;
            this.parentProperty = parentProperty;
        }

        TrustedAqlValue getReturnStructure() {
            AQL aql = new AQL();

            if (this.properties == null || this.properties.isEmpty()) {
                if (this.parentProperty != null) {
                    aql.addLine(trust("FILTER ${parentAliasDoc}.`${field}` != NULL"));
                }
            } else if (this.parentProperty != null) {
                aql.add(trust("FILTER "));
                for (SpecProperty field : properties) {
                    AQL fieldResult = new AQL();
                    fieldResult.add(trust("(${fieldRepresentation} != NULL AND ${fieldRepresentation} != [])"));
                    if (field != properties.get(properties.size() - 1)) {
                        fieldResult.add(trust(" OR "));
                    }
                    fieldResult.setTrustedParameter("fieldRepresentation", getRepresentationOfField(parentAlias, field));
                    aql.addLine(fieldResult.build());
                }
            }

            aql.addLine(trust("RETURN "));
            if (parentProperty != null) {
                aql.add(trust("DISTINCT "));
            }

            if (this.properties == null || this.properties.isEmpty()) {
                if (this.parentProperty != null) {
                    aql.add(trust("${parentAliasDoc}.`${field}`"));
                    aql.setTrustedParameter("parentAliasDoc", parentAlias.getArangoDocName());
                    aql.setParameter("field", parentProperty.getLeafPath().pathName);
                }
            } else {
                aql.indent();
                aql.add(trust("{"));
                for (SpecProperty field : properties) {
                    AQL fieldResult = new AQL();
                    fieldResult.add(new TrustedAqlValue("\"${fieldName}\": ${fieldRepresentation}"));
                    fieldResult.setParameter("fieldName", field.propertyName);
                    fieldResult.setTrustedParameter("fieldRepresentation", getRepresentationOfField(parentAlias, field));
                    if (field != properties.get(properties.size() - 1)) {
                        fieldResult.addComma();
                    }
                    aql.addLine(fieldResult.build());
                }
                aql.outdent();
                aql.addLine(trust("}"));
            }
            return aql.build();
        }


    }


    private static class SortBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias parentAlias;

        public SortBuilder(ArangoAlias parentAlias, List<SpecProperty> fields) {
            this.fields = fields;
            this.parentAlias = parentAlias;
        }


        private List<SpecProperty> fieldsWithSort() {
            return fields.stream().filter(SpecProperty::isSort).collect(Collectors.toList());
        }

        TrustedAqlValue getSort() {
            List<SpecProperty> sortFields = fieldsWithSort();
            if (!sortFields.isEmpty()) {
                AQL aql = new AQL();
                aql.add(trust("SORT "));
                for (SpecProperty sortField : sortFields) {
                    AQL sort = new AQL();
                    if (sortField != sortFields.get(0)) {
                        sort.addComma();
                    }
                    sort.add(trust("${field}"));
                    sort.setTrustedParameter("field", getRepresentationOfField(parentAlias, sortField));
                    aql.add(sort.build());
                }
                aql.addLine(trust(" ASC"));
                return aql.build();
            }
            return null;
        }


    }


    private class FilterBuilder {

        private final List<SpecProperty> fields;
        private final ArangoAlias alias;
        private final PropertyFilter parentFilter;

        public FilterBuilder(ArangoAlias alias, PropertyFilter parentFilter, List<SpecProperty> fields) {
            this.fields = fields;
            this.alias = alias;
            this.parentFilter = parentFilter;
        }


        private List<SpecProperty> fieldsWithFilter() {
            return fields.stream().filter(f -> f.isRequired() || (f.propertyFilter != null && f.propertyFilter.getOp() != null && !f.propertyFilter.getOp().isInstanceFilter())).collect(Collectors.toList());
        }

        TrustedAqlValue getFilter() {
            AQL filter = new AQL();
            filter.addDocumentFilter(alias.getArangoDocName());
            if (parentFilter != null && parentFilter.getOp() != null && parentFilter.getOp().isInstanceFilter()) {
                filter.addLine(createInstanceFilter(parentFilter));
            }
            List<SpecProperty> fieldsWithFilter = fieldsWithFilter();
            if (!fieldsWithFilter.isEmpty()) {
                for (SpecProperty specField : fieldsWithFilter) {
                    filter.addLine(createFilter(specField));
                }
            }
            return filter.build();
        }

        private TrustedAqlValue createInstanceFilter(PropertyFilter filter) {
            AQL aql = new AQL();
            if (filter != null && filter.getOp().isInstanceFilter()) {
                TrustedAqlValue fieldFilter = createFieldFilter(filter);
                if (fieldFilter != null) {
                    aql.addLine(trust("AND ${fieldFilter}"));
                    aql.setTrustedParameter("fieldFilter", fieldFilter);
                    aql.setTrustedParameter("document", alias.getArangoDocName());
                }
            }
            return aql.build();
        }


        private TrustedAqlValue createFilter(SpecProperty field) {
            AQL aql = new AQL();
            if (field.isRequired()) {
                aql.addLine(trust("AND ${field} !=null"));
                aql.addLine(trust("AND ${field} !=\"\""));
                aql.addLine(trust("AND ${field} !=[]"));
            }
            if (field.propertyFilter != null && field.propertyFilter.getOp() != null && !field.propertyFilter.getOp().isInstanceFilter()) {
                if (field.propertyFilter.getOp() == Op.IS_EMPTY) {
                    aql.addLine(trust("AND (${field} ==null"));
                    aql.addLine(trust("OR ${field} ==\"\""));
                    aql.addLine(trust("OR ${field} == [])"));
                } else {
                    TrustedAqlValue fieldFilter = createFieldFilter(field.propertyFilter);
                    if (fieldFilter != null) {
                        aql.addLine(trust("AND (IS_ARRAY(${field}) ? ${field}[* FILTER LOWER(CURRENT)${fieldFilter}] != [] : LOWER(${field})${fieldFilter})"));
                        aql.setTrustedParameter("fieldFilter", fieldFilter);
                    }
                }
            }
            aql.setTrustedParameter("field", getRepresentationOfField(alias, field));
            return aql.build();
        }


        private TrustedAqlValue createAqlForFilter(PropertyFilter fieldFilter, boolean prefixWildcard, boolean postfixWildcard) {
            String value = null;
            String key;
            if (fieldFilter.getParameter() != null) {
                key = fieldFilter.getParameter().getName();
            } else {
                key = "staticFilter" + DataQueryBuilder.this.bindVars.size();
            }
            if (DataQueryBuilder.this.filterValues.containsKey(key)) {
                Object fromMap = DataQueryBuilder.this.filterValues.get(key);
                value = fromMap != null ? URLDecoder.decode(fromMap.toString(), StandardCharsets.UTF_8) : null;
            }
            if (value == null && fieldFilter.getValue() != null) {
                value = fieldFilter.getValue().getValue();
            }
            if (StringUtils.isNotBlank(value) && key != null) {
                if (prefixWildcard && !value.startsWith("%")) {
                    value = "%" + value;
                }
                if (postfixWildcard && !value.endsWith("%")) {
                    value = value + "%";
                }
                if(!DataQueryBuilder.this.bindVars.containsKey(key)) {
                    DataQueryBuilder.this.bindVars.put(key, value);
                }
                AQL aql = new AQL();
                if (fieldFilter.getOp().isInstanceFilter()) {
                    aql.add(trust("@${field}"));
                } else {
                    aql.add(trust("LOWER(@${field})"));
                }
                aql.setParameter("field", key);
                return aql.build();
            }
            return null;
        }


        private TrustedAqlValue createFieldFilter(PropertyFilter fieldFilter) {
            AQL aql = new AQL();

            TrustedAqlValue value;
            switch (fieldFilter.getOp()) {
                case REGEX:
                case EQUALS:
                case TYPE:
                case MBB:
                case ID:
                    value = createAqlForFilter(fieldFilter, false, false);
                    break;
                case STARTS_WITH:
                    value = createAqlForFilter(fieldFilter, false, true);
                    break;
                case ENDS_WITH:
                    value = createAqlForFilter(fieldFilter, true, false);
                    break;
                case CONTAINS:
                    value = createAqlForFilter(fieldFilter, true, true);
                    break;
                default:
                    value = null;
            }
            if (value != null) {
                switch (fieldFilter.getOp()) {
                    case EQUALS:
                        aql.add(trust(" == " + value.getValue()));
                        break;
                    case STARTS_WITH:
                    case ENDS_WITH:
                    case CONTAINS:
                        aql.add(trust(" LIKE " + value.getValue()));
                        break;
                    case REGEX:
                        aql.add(trust(" =~ " + value.getValue()));
                        break;
                    case MBB:
                    case ID:
                        aql.add(trust("${document}._id IN " + value.getValue() + " "));
                        break;
                    case TYPE:
                        aql.add(trust(value.getValue() + " IN ${document}._type"));
                }
                return aql.build();
            }
            return null;
        }
    }

}
