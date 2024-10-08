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

package org.marmotgraph.graphdb.structure.controller;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CollectionType;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.DocumentCreateOptions;
import org.marmotgraph.arango.commons.aqlbuilder.AQL;
import org.marmotgraph.arango.commons.aqlbuilder.ArangoVocabulary;
import org.marmotgraph.arango.commons.model.ArangoCollectionReference;
import org.marmotgraph.arango.commons.model.ArangoDatabaseProxy;
import org.marmotgraph.arango.commons.model.ArangoDocumentReference;
import org.marmotgraph.arango.commons.model.InternalSpace;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.cache.CacheConstant;
import org.marmotgraph.commons.exception.AmbiguousException;
import org.marmotgraph.commons.jsonld.DynamicJson;
import org.marmotgraph.commons.jsonld.JsonLdConsts;
import org.marmotgraph.commons.jsonld.JsonLdId;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.commons.model.DataStage;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.marmotgraph.commons.semantics.vocabularies.EBRAINSVocabulary;
import org.marmotgraph.commons.semantics.vocabularies.SchemaOrgVocabulary;
import org.marmotgraph.graphdb.commons.controller.ArangoDatabases;
import org.marmotgraph.graphdb.commons.controller.GraphDBArangoUtils;
import org.marmotgraph.graphdb.commons.model.ArangoEdge;
import org.marmotgraph.graphdb.structure.model.PropertyOfTypeInSpaceReflection;
import org.marmotgraph.graphdb.structure.model.TargetTypeReflection;
import org.marmotgraph.graphdb.structure.model.TypeWithInstanceCountReflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StructureRepository {

    private final ArangoDatabases arangoDatabases;
    private final JsonAdapter jsonAdapter;
    private final GraphDBArangoUtils graphDBArangoUtils;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public StructureRepository(ArangoDatabases arangoDatabases, JsonAdapter jsonAdapter, GraphDBArangoUtils graphDBArangoUtils) {
        this.arangoDatabases = arangoDatabases;
        this.jsonAdapter = jsonAdapter;
        this.graphDBArangoUtils = graphDBArangoUtils;
    }

    private final static ArangoCollectionReference SPACES = new ArangoCollectionReference("spaces", false);
    private final static ArangoCollectionReference TYPES = new ArangoCollectionReference("types", false);
    private final static ArangoCollectionReference PROPERTIES = new ArangoCollectionReference("properties", false);
    private final static ArangoCollectionReference TYPE_IN_SPACE = new ArangoCollectionReference("typeInSpace", true);
    private final static ArangoCollectionReference PROPERTY_IN_TYPE = new ArangoCollectionReference("propertyInType", true);

    public static void setupCollections(ArangoDatabaseProxy database) {
        database.createCollectionIfItDoesntExist(SPACES);
        database.createCollectionIfItDoesntExist(TYPES);
        database.createCollectionIfItDoesntExist(PROPERTIES);
        database.createCollectionIfItDoesntExist(TYPE_IN_SPACE);
        database.createCollectionIfItDoesntExist(PROPERTY_IN_TYPE);
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_REFLECTED_SPACES, sync = true)
    public List<SpaceName> reflectSpaces(DataStage stage) {
        logger.debug("Missing cache hit: Fetching space reflection from database");
        return doReflectSpaces(stage);
    }

    @CachePut(CacheConstant.CACHE_KEYS_REFLECTED_SPACES)
    public List<SpaceName> refreshReflectedSpacesCache(DataStage stage) {
        logger.debug("Change of data - refresh cache for reflected spaces");
        return doReflectSpaces(stage);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_REFLECTED_SPACES)
    public void evictReflectedSpacesCache(DataStage stage) {
        logger.debug("Cache evict: clearing cache for reflected spaces");
    }

    private List<SpaceName> doReflectSpaces(DataStage stage){
        final ArangoDatabase database = arangoDatabases.getByStage(stage);
        final List<CollectionEntity> spaces = database.getCollections(new CollectionsReadOptions().excludeSystem(true)).stream().filter(c -> c.getType() == CollectionType.DOCUMENT).filter(c -> !InternalSpace.INTERNAL_SPACENAMES.contains(c.getName())).distinct().collect(Collectors.toList());
        if(spaces.isEmpty()){
            return Collections.emptyList();
        }
        AQL query = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        query.addLine(AQL.trust("FOR space IN "));
        if(spaces.size()>1){
            query.addLine(AQL.trust("UNION_DISTINCT("));
        }
        for (int i = 0; i < spaces.size(); i++) {
            bindVars.put(String.format("@collection%d", i), spaces.get(i).getName());
            query.addLine(AQL.trust(String.format("(FOR e IN @@collection%d FILTER e.@space != NULL AND e.@space != [] LIMIT 0,1 RETURN DISTINCT e.@space)", i)));
            if (i < spaces.size() - 1) {
                query.add(AQL.trust(", "));
            }
            bindVars.put("space", EBRAINSVocabulary.META_SPACE);
        }
        if(spaces.size()>1){
            query.addLine(AQL.trust(")"));
        }
        query.addLine(AQL.trust(" RETURN space"));
        return database.query(query.build().getValue(), bindVars, String.class).asListRemaining().stream().map(SpaceName::fromString).collect(Collectors.toList());
    }



    @Cacheable(value = CacheConstant.CACHE_KEYS_SPACES, sync = true)
    public List<Space> getSpaces() {
        logger.debug("Missing cache hit: Fetching spaces from database");
        return doGetSpaces();
    }

    @CachePut(CacheConstant.CACHE_KEYS_SPACES)
    public List<Space> refreshSpacesCache(){
        logger.debug("Change of data: Fetching spaces from database");
        return doGetSpaces();
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_SPACES)
    public void evictSpacesCache() {
        logger.debug("Cache evict: clearing cache for spaces");
    }

    private List<Space> doGetSpaces(){
        AQL aql = new AQL();
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if(structureDB.collection(SPACES.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            aql.addLine(AQL.trust("FOR d IN @@collection"));
            bindVars.put("@collection", SPACES.getCollectionName());
            aql.addLine(AQL.trust(String.format("SORT d.`%s` ASC", SchemaOrgVocabulary.NAME)));
            aql.addLine(AQL.trust("RETURN KEEP(d, ATTRIBUTES(d, true))"));
            return Collections.unmodifiableList(structureDB.query(aql.build().getValue(), bindVars, Space.class).asListRemaining());
        }
        return Collections.emptyList();
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_SPACE_SPECIFICATIONS, sync = true)
    public List<SpaceSpecification> getSpaceSpecifications() {
        logger.debug("Missing cache hit: Fetching space specifications from database");
        return doGetSpaceSpecifications();
    }

    @CachePut(CacheConstant.CACHE_KEYS_SPACE_SPECIFICATIONS)
    public List<SpaceSpecification> refreshSpaceSpecificationsCache(){
        logger.debug("Change of data: Fetching space specifications from database");
        return doGetSpaceSpecifications();
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_SPACE_SPECIFICATIONS)
    public void evictSpaceSpecificationsCache() {
        logger.debug("Cache evict: clearing cache for space specifications");
    }

    private List<SpaceSpecification> doGetSpaceSpecifications(){
        AQL aql = new AQL();
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if(structureDB.collection(SPACES.getCollectionName()).exists()) {
            Map<String, Object> bindVars = new HashMap<>();
            aql.addLine(AQL.trust("FOR d IN @@collection"));
            bindVars.put("@collection", SPACES.getCollectionName());
            aql.addLine(AQL.trust(String.format("SORT d.`%s` ASC", SchemaOrgVocabulary.NAME)));
            aql.addLine(AQL.trust("RETURN KEEP(d, ATTRIBUTES(d, true))"));
            return Collections.unmodifiableList(structureDB.query(aql.build().getValue(), bindVars, SpaceSpecification.class).asListRemaining());
        }
        return Collections.emptyList();
    }


    @Cacheable(value = CacheConstant.CACHE_KEYS_TYPES_IN_SPACE_BY_SPEC, sync = true)
    public List<String> getTypesInSpaceBySpecification(SpaceName spaceName) {
        logger.debug(String.format("Missing cache hit: Fetching types in space %s specifications from database", spaceName.getName()));
        return doGetTypesInSpaceBySpecification(spaceName);
    }

    @CachePut(CacheConstant.CACHE_KEYS_TYPES_IN_SPACE_BY_SPEC)
    public List<String> refreshTypesInSpaceBySpecification(SpaceName spaceName) {
        logger.debug(String.format("Change of data: Fetching types in space %s specifications from database", spaceName.getName()));
        return doGetTypesInSpaceBySpecification(spaceName);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_TYPES_IN_SPACE_BY_SPEC)
    public void evictTypesInSpaceBySpecification(SpaceName spaceName) {
        logger.debug("Cache evict: clearing cache for type in space specifications");
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_TYPE_SPECIFICATION, sync = true)
    public DynamicJson getTypeSpecification(String typeName) {
        logger.debug(String.format("Missing cache hit: Fetching type specification for %s from database", typeName));
        return doGetTypeSpecification(typeName, TYPES);
    }

    @CachePut(CacheConstant.CACHE_KEYS_TYPE_SPECIFICATION)
    public DynamicJson refreshTypeSpecification(String typeName) {
        logger.debug(String.format("Change of data: Fetching type specification for %s from database", typeName));
        return doGetTypeSpecification(typeName, TYPES);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_TYPE_SPECIFICATION)
    public void evictTypeSpecification(String typeName) {
        logger.debug(String.format("Cache evict: clearing cache for type specification %s", typeName));
    }

    private List<String> doGetTypesInSpaceBySpecification(SpaceName spaceName){
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if(structureDB.collection(TYPE_IN_SPACE.getCollectionName()).exists()) {
            final UUID spaceUUID = spaceSpecificationRef(spaceName.getName());
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("FOR t IN @@collection FILTER t._from == @id"));
            bindVars.put("@collection", TYPE_IN_SPACE.getCollectionName());
            bindVars.put("id", String.format("%s/%s", SPACES.getCollectionName(), spaceUUID));
            query.addLine(AQL.trust(String.format("RETURN DOCUMENT(t._to).`%s`", SchemaOrgVocabulary.IDENTIFIER)));
            return structureDB.query(query.build().getValue(), bindVars, String.class).asListRemaining();
        }
        return Collections.emptyList();
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_TYPE_SPECIFICATION, sync = true)
    public  DynamicJson getClientSpecificTypeSpecification(String typeName, SpaceName clientSpaceName) {
        logger.debug(String.format("Missing cache hit: Fetching type specification for %s from database (client: %s)", typeName, clientSpaceName.getName()));
        return doGetTypeSpecification(typeName, clientTypesCollection(clientSpaceName.getName()));
    }

    @CachePut(CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_TYPE_SPECIFICATION)
    public DynamicJson refreshClientSpecificTypeSpecification(String typeName, SpaceName clientSpaceName) {
        logger.debug(String.format("Change of data: Fetching type specification for %s from database (client: %s)", typeName, clientSpaceName.getName()));
        return doGetTypeSpecification(typeName, clientTypesCollection(clientSpaceName.getName()));
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_TYPE_SPECIFICATION)
    public void evictClientSpecificTypeSpecification(String typeName, SpaceName clientSpaceName) {
        logger.debug(String.format("Cache evict: clearing cache for type specification %s (client: %s)", typeName, clientSpaceName.getName()));
    }

    private DynamicJson doGetTypeSpecification(String typeName, ArangoCollectionReference collectionReference) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if (structureDB.collection(collectionReference.getCollectionName()).exists()) {
            final UUID typeUUID = typeSpecificationRef(typeName);
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
            bindVars.put("id", String.format("%s/%s", TYPES.getCollectionName(), typeUUID));
            query.addLine(AQL.trust("LET result = DOCUMENT(@clientSpace, doc._key)"));
            query.addLine(AQL.trust("FILTER result != NULL"));
            bindVars.put("clientSpace", collectionReference.getCollectionName());
            query.addLine(AQL.trust("RETURN KEEP(result, ATTRIBUTES(result, True))"));
            return getSingleResult(structureDB.query(query.build().getValue(), bindVars, DynamicJson.class).asListRemaining(), typeUUID);
        }
        return null;
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_TYPES_IN_SPACE, sync = true)
    public List<TypeWithInstanceCountReflection> reflectTypesInSpace(DataStage stage, SpaceName name) {
        logger.debug(String.format("Missing cache hit: Reflecting types in space %s (stage %s)", name, stage.name()));
        return doReflectTypesInSpace(stage, name);
    }

    @CachePut(CacheConstant.CACHE_KEYS_TYPES_IN_SPACE)
    public List<TypeWithInstanceCountReflection> refreshTypesInSpaceCache(DataStage stage, SpaceName name) {
        logger.debug(String.format("Change of data: Reflecting types in space %s (stage %s)", name, stage.name()));
        return doReflectTypesInSpace(stage, name);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_TYPES_IN_SPACE)
    public void evictTypesInSpaceCache(DataStage stage, SpaceName name) {
        logger.debug(String.format("Cache evict: clearing cache for types in space %s (stage %s)", name, stage.name()));
    }

    private List<TypeWithInstanceCountReflection> doReflectTypesInSpace(DataStage stage, SpaceName name){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("LET typeGroups=(FOR i in @@collection"));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(name).getCollectionName());
        aql.addLine(AQL.trust(String.format("FILTER i.`%s` != NULL", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust(String.format("COLLECT t=i.`%s` WITH COUNT INTO length", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust("RETURN {\"name\": t, \"occurrences\": length})"));
        aql.addLine(AQL.trust("LET types = UNIQUE(typeGroups[**].name[**])"));
        aql.addLine(AQL.trust("FOR t IN types"));
        aql.addLine(AQL.trust("LET countsInGroup = SUM(FOR g IN typeGroups FILTER t IN g.name RETURN g.occurrences)"));
        aql.addLine(AQL.trust("RETURN { \"name\": t, \"occurrences\": countsInGroup }"));
        return Collections.unmodifiableList(arangoDatabases.getByStage(stage).query(aql.build().getValue(), bindVars, TypeWithInstanceCountReflection.class).asListRemaining());
    }


    @Cacheable(value = CacheConstant.CACHE_KEYS_PROPERTY_SPECIFICATION, sync = true)
    public DynamicJson getPropertyBySpecification(String propertyName) {
        logger.debug(String.format("Missing cache hit: Reflecting property specification %s", propertyName));
        return doGetPropertyBySpecification(propertyName, PROPERTIES);
    }

    @CachePut(CacheConstant.CACHE_KEYS_PROPERTY_SPECIFICATION)
    public DynamicJson refreshPropertySpecificationCache(String propertyName) {
        logger.debug(String.format("Change of data: Reflecting property specification %s", propertyName));
        return doGetPropertyBySpecification(propertyName, PROPERTIES);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_PROPERTY_SPECIFICATION)
    public void evictPropertySpecificationCache(String propertyName) {
        logger.debug(String.format("Cache evict: clearing cache for property %s", propertyName));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_PROPERTY_SPECIFICATION, sync = true)
    public DynamicJson getClientSpecificPropertyBySpecification(String propertyName, SpaceName clientSpaceName) {
        logger.debug(String.format("Missing cache hit: Reflecting property specification %s (client: %s)", propertyName, clientSpaceName.getName()));
        return doGetPropertyBySpecification(propertyName, clientTypesCollection(clientSpaceName.getName()));
    }

    @CachePut(CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_PROPERTY_SPECIFICATION)
    public DynamicJson refreshClientSpecificPropertySpecificationCache(String propertyName, SpaceName clientSpaceName) {
        logger.debug(String.format("Change of data: Reflecting property specification %s (client: %s)", propertyName, clientSpaceName.getName()));
        return doGetPropertyBySpecification(propertyName, clientTypesCollection(clientSpaceName.getName()));
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_PROPERTY_SPECIFICATION)
    public void evictClientSpecificPropertySpecificationCache(String propertyName, SpaceName clientSpaceName) {
        logger.debug(String.format("Cache evict: clearing cache for property %s (client: %s)", propertyName, clientSpaceName.getName()));
    }

    private DynamicJson doGetPropertyBySpecification(String propertyName, ArangoCollectionReference collectionReference) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if (structureDB.collection(collectionReference.getCollectionName()).exists()) {
            final UUID propertyUUID = propertySpecificationRef(propertyName);
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("LET doc = DOCUMENT(@id)"));
            bindVars.put("id", String.format("%s/%s", collectionReference.getCollectionName(), propertyUUID));
            query.addLine(AQL.trust("RETURN KEEP(doc, ATTRIBUTES(doc, True))"));
            return getSingleResult(structureDB.query(query.build().getValue(), bindVars, DynamicJson.class).asListRemaining(), propertyUUID);
        }
        return null;
    }


    @Cacheable(value = CacheConstant.CACHE_KEYS_PROPERTIES_IN_TYPE_SPECIFICATION, sync = true)
    public List<DynamicJson> getPropertiesInTypeBySpecification(String type) {
        logger.debug(String.format("Missing cache hit: Reflecting properties for type %s", type));
        return doGetPropertiesInTypeBySpecification(type, PROPERTY_IN_TYPE);
    }

    @CachePut(CacheConstant.CACHE_KEYS_PROPERTIES_IN_TYPE_SPECIFICATION)
    public List<DynamicJson> refreshPropertiesInTypeBySpecificationCache(String type) {
        logger.debug(String.format("Change of data: Reflecting properties for type %s", type));
        return doGetPropertiesInTypeBySpecification(type, PROPERTY_IN_TYPE);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_PROPERTIES_IN_TYPE_SPECIFICATION)
    public void evictPropertiesInTypeBySpecificationCache(String type) {
        logger.debug(String.format("Cache evict: clearing cache for properties in type %s", type));
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_PROPERTIES_IN_TYPE_SPECIFICATION, sync = true)
    public List<DynamicJson> getClientSpecificPropertiesInTypeBySpecification(String type, SpaceName clientSpaceName) {
        logger.debug(String.format("Missing cache hit: Reflecting properties for type %s (client: %s)", type, clientSpaceName.getName()));
        return doGetPropertiesInTypeBySpecification(type, clientPropertyInTypeCollection(clientSpaceName.getName()));
    }

    @CachePut(CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_PROPERTIES_IN_TYPE_SPECIFICATION)
    public List<DynamicJson> refreshClientSpecificPropertiesInTypeBySpecificationCache(String type, SpaceName clientSpaceName) {
        logger.debug(String.format("Change of data: Reflecting properties for type %s (client: %s)", type, clientSpaceName.getName()));
        return doGetPropertiesInTypeBySpecification(type, clientPropertyInTypeCollection(clientSpaceName.getName()));
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_CLIENT_SPECIFIC_PROPERTIES_IN_TYPE_SPECIFICATION)
    public void evictClientSpecificPropertiesInTypeBySpecificationCache(String type, SpaceName clientSpaceName) {
        logger.debug(String.format("Cache evict: clearing cache for properties in type %s (client: %s)", type, clientSpaceName.getName()));
    }

    private List<DynamicJson> doGetPropertiesInTypeBySpecification(String type, ArangoCollectionReference collectionReference) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        if (structureDB.collection(collectionReference.getCollectionName()).exists()) {
            final UUID typeUUID = typeSpecificationRef(type);
            AQL query = new AQL();
            Map<String, Object> bindVars = new HashMap<>();
            query.addLine(AQL.trust("FOR t IN @@collection FILTER t._from == @id"));
            bindVars.put("@collection", collectionReference.getCollectionName());
            bindVars.put("id", String.format("%s/%s", TYPES.getCollectionName(), typeUUID));
            query.addLine(AQL.trust("LET doc = DOCUMENT(t._to)"));
            query.addLine(AQL.trust("FILTER doc != NULL"));
            query.addLine(AQL.trust(String.format("RETURN MERGE(KEEP(doc, [\"%s\"]), KEEP(t, ATTRIBUTES(t, True)))", SchemaOrgVocabulary.IDENTIFIER)));
            return structureDB.query(query.build().getValue(), bindVars, DynamicJson.class).asListRemaining();
        }
        return Collections.emptyList();
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_PROPERTIES_OF_TYPE_IN_SPACE, sync = true)
    public List<PropertyOfTypeInSpaceReflection> reflectPropertiesOfTypeInSpace(DataStage stage, SpaceName spaceName, String type) {
        logger.debug(String.format("Missing cache hit: Reflecting properties of type %s in space %s (stage %s)", type, spaceName, stage.name()));
        return doReflectPropertiesOfTypeInSpace(stage, spaceName, type);
    }

    @CachePut(CacheConstant.CACHE_KEYS_PROPERTIES_OF_TYPE_IN_SPACE)
    public List<PropertyOfTypeInSpaceReflection> refreshPropertiesOfTypeInSpaceCache(DataStage stage, SpaceName spaceName, String type) {
        logger.debug(String.format("Change of data: Reflecting properties of type %s in space %s (stage %s)", type, spaceName, stage.name()));
        return doReflectPropertiesOfTypeInSpace(stage, spaceName, type);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_PROPERTIES_OF_TYPE_IN_SPACE)
    public void evictPropertiesOfTypeInSpaceCache(DataStage stage, SpaceName spaceName, String type) {
        logger.debug(String.format("Cache evict: clearing cache for properties of type %s in space %s (stage %s)", type, spaceName, stage.name()));
    }

    private List<PropertyOfTypeInSpaceReflection> doReflectPropertiesOfTypeInSpace(DataStage stage, SpaceName spaceName, String type){
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust("LET attGroups = (FOR d IN @@collection"));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(spaceName).getCollectionName());
        aql.addLine(AQL.trust(String.format("FILTER @type IN d.`%s`", JsonLdConsts.TYPE)));
        bindVars.put("type", type);
        aql.addLine(AQL.trust("COLLECT attGroup =  ATTRIBUTES(d, true) WITH COUNT INTO length"));
        aql.addLine(AQL.trust("RETURN {\"attributes\": attGroup, \"count\": length})"));
        aql.addLine(AQL.trust("LET attributes = UNIQUE(attGroups[**].attributes[**])"));
        aql.addLine(AQL.trust("FOR att IN attributes"));
        aql.addLine(AQL.trust("LET countsInGroup = SUM(FOR g IN attGroups FILTER att IN g.attributes RETURN g.count)"));
        aql.addLine(AQL.trust("RETURN { \"name\": att, \"occurrences\": countsInGroup }"));
        return Collections.unmodifiableList(arangoDatabases.getByStage(stage).query(aql.build().getValue(), bindVars, PropertyOfTypeInSpaceReflection.class).asListRemaining());
    }

    @Cacheable(value = CacheConstant.CACHE_KEYS_TARGET_TYPES, sync = true)
    public List<TargetTypeReflection> reflectTargetTypes(DataStage stage, SpaceName spaceName, String type, String property) {
        logger.debug(String.format("Missing cache hit: Reflecting target type of property %s of type %s in space %s (stage %s)", property, type, spaceName, stage.name()));
        return doReflectTargetTypes(stage, spaceName, type, property);
    }

    @CachePut(CacheConstant.CACHE_KEYS_TARGET_TYPES)
    public List<TargetTypeReflection> refreshTargetTypesCache(DataStage stage, SpaceName spaceName, String type, String property) {
        logger.debug(String.format("Change of data:  Reflecting target type of property %s of type %s in space %s (stage %s)", property, type, spaceName, stage.name()));
        return doReflectTargetTypes(stage, spaceName, type, property);
    }

    @CacheEvict(CacheConstant.CACHE_KEYS_TARGET_TYPES)
    public void evictTargetTypesCache(DataStage stage, SpaceName spaceName, String type, String property) {
        logger.debug(String.format("Cache evict: clearing cache for target type of property %s of type %s in space %s (stage %s)", property, type, spaceName, stage.name()));
    }

    private List<TargetTypeReflection> doReflectTargetTypes(DataStage stage, SpaceName spaceName, String type, String property){
        final ArangoCollectionReference edgeCollection = new ArangoCollectionReference(property, true);
        //It's a property which actually does have target types
        AQL aql = new AQL();
        Map<String, Object> bindVars = new HashMap<>();
        aql.addLine(AQL.trust(String.format("FOR i IN @@collection FILTER @type IN i.`%s` AND i.@property != NULL", JsonLdConsts.TYPE)));
        bindVars.put("@collection", ArangoCollectionReference.fromSpace(spaceName).getCollectionName());
        bindVars.put("type", type);
        bindVars.put("property", property);
        aql.addLine(AQL.trust("LET targetType = FLATTEN("));
        aql.addLine(AQL.trust("       FOR target IN 1..1 OUTBOUND i @@propertyEdge"));
        bindVars.put("@propertyEdge", edgeCollection.getCollectionName());
        aql.addLine(AQL.trust("FILTER target != NULL AND target.`@type` != NULL"));
        aql.addLine(AQL.trust(String.format("FOR type IN TO_ARRAY(target.`%s`)", JsonLdConsts.TYPE)));
        aql.addLine(AQL.trust(String.format("COLLECT t = type, s=target.`%s` WITH COUNT INTO length", EBRAINSVocabulary.META_SPACE)));
        aql.addLine(AQL.trust("RETURN {"));
        aql.addLine(AQL.trust("    \"type\": t,"));
        aql.addLine(AQL.trust("    \"space\": s,"));
        aql.addLine(AQL.trust("    \"count\": length"));
        aql.addLine(AQL.trust("}"));
        aql.addLine(AQL.trust(" )"));
        aql.addLine(AQL.trust(" FOR t IN targetType"));
        aql.addLine(AQL.trust(" COLLECT type = t.type, space = t.space AGGREGATE count = SUM(t.count)"));
        aql.addLine(AQL.trust(" RETURN {"));
        aql.addLine(AQL.trust("\"name\": type,"));
        aql.addLine(AQL.trust("\"space\": space,"));
        aql.addLine(AQL.trust("\"occurrences\": count"));
        aql.addLine(AQL.trust("}"));
        return Collections.unmodifiableList(arangoDatabases.getByStage(stage).query(aql.build().getValue(), bindVars, TargetTypeReflection.class).asListRemaining());
    }

    private final List<String> EDGE_BLACKLIST = Arrays.asList(
            new ArangoCollectionReference(EBRAINSVocabulary.META_ALTERNATIVE, true).getCollectionName(),
            new ArangoCollectionReference(EBRAINSVocabulary.META_USER, true).getCollectionName(),
            InternalSpace.DOCUMENT_ID_EDGE_COLLECTION.getCollectionName(),
            ArangoCollectionReference.fromSpace(InternalSpace.INFERENCE_OF_SPACE).getCollectionName(),
            InternalSpace.RELEASE_STATUS_EDGE_COLLECTION.getCollectionName(),
            InternalSpace.TYPE_EDGE_COLLECTION.getCollectionName(),
            ArangoCollectionReference.fromSpace(InternalSpace.UNRESOLVED_SPACE).getCollectionName());

    public List<String> getAllRelevantEdges(DataStage stage) {
        final ArangoDatabase db = arangoDatabases.getByStage(stage);
        final Collection<CollectionEntity> collections = db.getCollections(new CollectionsReadOptions().excludeSystem(true));
        return collections.stream().filter(c -> c.getType().equals(CollectionType.EDGES)).map(CollectionEntity::getName).filter(c -> !EDGE_BLACKLIST.contains(c)).collect(Collectors.toList());
    }


    private ArangoCollectionReference createClientCollection(String ref, String client, boolean edge) {
        return new ArangoCollectionReference(String.format("%s_%s", client, ref), edge);
    }

    private ArangoCollectionReference clientTypesCollection(String client) {
        return createClientCollection("types", client, false);
    }

    private ArangoCollectionReference clientPropertiesCollection(String client) {
        return createClientCollection("properties", client, false);
    }

    private ArangoCollectionReference clientPropertyInTypeCollection(String client) {
        return createClientCollection("propertyInType", client, true);
    }

    private UUID spaceSpecificationRef(String spaceName) {
        return UUID.nameUUIDFromBytes((String.format("spaces/%s", spaceName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID typeSpecificationRef(String typeName) {
        return UUID.nameUUIDFromBytes((String.format("types/%s", typeName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID propertySpecificationRef(String propertyName) {
        return UUID.nameUUIDFromBytes((String.format("properties/%s", propertyName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID typeInSpaceSpecificationRef(String spaceName, String typeName) {
        return UUID.nameUUIDFromBytes((String.format("space/%s/types/%s", spaceName, typeName)).getBytes(StandardCharsets.UTF_8));
    }

    private UUID propertyInTypeSpecificationRef(String typeName, String propertyName) {
        return UUID.nameUUIDFromBytes((String.format("types/%s/properties/%s", typeName, propertyName)).getBytes(StandardCharsets.UTF_8));
    }

    public void createOrUpdateSpaceDocument(SpaceSpecification spaceSpecification) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        final ArangoCollection spaces = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, SPACES);
        final DynamicJson arangoDoc = jsonAdapter.fromJson(jsonAdapter.toJson(spaceSpecification), DynamicJson.class);
        arangoDoc.put(ArangoVocabulary.KEY, spaceSpecificationRef(spaceSpecification.getName()));
        spaces.insertDocument(arangoDoc, new DocumentCreateOptions().overwrite(true));
    }

    public void removeSpaceDocument(SpaceName spaceName) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        final ArangoCollection spaces = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, SPACES);
        final String id = spaceSpecificationRef(spaceName.getName()).toString();
        if (spaces.documentExists(id)) {
            spaces.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, spaces.name()));
        }
    }

    public void addLinkBetweenSpaceAndType(SpaceName spaceName, String type) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoEdge edge = new ArangoEdge();
        edge.setFrom(new ArangoDocumentReference(SPACES, spaceSpecificationRef(spaceName.getName())));
        edge.setTo(new ArangoDocumentReference(TYPES, typeSpecificationRef(type)));
        edge.redefineId(new ArangoDocumentReference(TYPE_IN_SPACE, typeInSpaceSpecificationRef(spaceName.getName(), type)));
        final ArangoCollection typeInSpace = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, TYPE_IN_SPACE);
        typeInSpace.insertDocument(jsonAdapter.toJson(edge), new DocumentCreateOptions().overwrite(true));
    }

    public void removeLinkBetweenSpaceAndType(SpaceName spaceName, String type) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        final ArangoCollection typeInSpace = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, TYPE_IN_SPACE);
        final String id = typeInSpaceSpecificationRef(spaceName.getName(), type).toString();
        if (typeInSpace.documentExists(id)) {
            typeInSpace.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, typeInSpace.name()));
        }
    }

    public void createOrUpdateTypeDocument(JsonLdId typeName, NormalizedJsonLd typeSpecification, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? TYPES : clientTypesCollection(clientSpace.getName());
        final ArangoCollection types = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        typeSpecification.put(ArangoVocabulary.KEY, typeSpecificationRef(typeName.getId()));
        typeSpecification.put(SchemaOrgVocabulary.IDENTIFIER, typeName.getId());
        types.insertDocument(typeSpecification, new DocumentCreateOptions().overwrite(true));
    }

    public void removeTypeDocument(JsonLdId typeName, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? TYPES : clientTypesCollection(clientSpace.getName());
        final ArangoCollection types = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        final String id = typeSpecificationRef(typeName.getId()).toString();
        if (types.documentExists(id)) {
            types.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, types.name()));
        }
    }


    public void createOrUpdatePropertyDocument(JsonLdId propertyName, NormalizedJsonLd propertySpecification, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? PROPERTIES : clientPropertiesCollection(clientSpace.getName());
        final ArangoCollection properties = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        propertySpecification.put(ArangoVocabulary.KEY, propertySpecificationRef(propertyName.getId()));
        propertySpecification.put(SchemaOrgVocabulary.IDENTIFIER, propertyName.getId());
        properties.insertDocument(propertySpecification, new DocumentCreateOptions().overwrite(true));
    }

    public void removePropertyDocument(JsonLdId propertyName, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? PROPERTIES : clientPropertiesCollection(clientSpace.getName());
        final ArangoCollection properties = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        final String id = propertySpecificationRef(propertyName.getId()).toString();
        if (properties.documentExists(id)) {
            properties.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, properties.name()));
        }

    }

    public void addLinkBetweenTypeAndProperty(String type, String property, NormalizedJsonLd payload, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        payload.put(ArangoVocabulary.FROM, new ArangoDocumentReference(TYPES, typeSpecificationRef(type)).getId());
        payload.put(ArangoVocabulary.TO, new ArangoDocumentReference(PROPERTIES, propertySpecificationRef(property)).getId());
        payload.put(ArangoVocabulary.KEY, propertyInTypeSpecificationRef(type, property));
        payload.remove(ArangoVocabulary.ID);
        ArangoCollectionReference collection = clientSpace == null ? PROPERTY_IN_TYPE : clientPropertyInTypeCollection(clientSpace.getName());
        final ArangoCollection propertyInType = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        propertyInType.insertDocument(payload, new DocumentCreateOptions().overwrite(true));
    }

    public void removeLinkBetweenTypeAndProperty(String type, String property, SpaceName clientSpace) {
        final ArangoDatabase structureDB = arangoDatabases.getStructureDB();
        ArangoCollectionReference collection = clientSpace == null ? PROPERTY_IN_TYPE : clientPropertyInTypeCollection(clientSpace.getName());
        final ArangoCollection propertyInType = graphDBArangoUtils.getOrCreateArangoCollection(structureDB, collection);
        final String id = propertyInTypeSpecificationRef(type, property).toString();
        if (propertyInType.documentExists(id)) {
            propertyInType.deleteDocument(id);
        } else {
            logger.info(String.format("Was trying to remove document %s but it doesn't exist in collection %s", id, propertyInType.name()));
        }
    }


    private DynamicJson getSingleResult(List<DynamicJson> dynamicJsons, UUID id) {
        if (dynamicJsons.isEmpty()) {
            return null;
        }
        if (dynamicJsons.size() > 1) {
            throw new AmbiguousException(String.format("The lookup for %s resulted in too many results", id));
        }
        return dynamicJsons.get(0);
    }

}
