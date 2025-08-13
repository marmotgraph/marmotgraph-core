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

package org.marmotgraph.graphdb.neo4j.api;

import org.apache.commons.lang3.NotImplementedException;
import org.marmotgraph.graphdb.neo4j.Neo4JProfile;
import org.marmotgraph.commons.api.GraphDBSpaces;
import org.marmotgraph.commons.model.Paginated;
import org.marmotgraph.commons.model.PaginationParam;
import org.marmotgraph.commons.model.SpaceName;
import org.marmotgraph.commons.model.external.spaces.SpaceSpecification;
import org.marmotgraph.commons.model.internal.spaces.Space;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("neo4j")
@Component
public class SpacesAPI implements GraphDBSpaces.Client {


    @Override
    public Space getSpace(SpaceName space) {
        throw new NotImplementedException();
    }

    @Override
    public Paginated<Space> listSpaces(PaginationParam paginationParam) {
        throw new NotImplementedException();

    }

    @Override
    public SpaceSpecification getSpaceSpecification(SpaceName spaceName) {
        throw new NotImplementedException();
    }

    @Override
    public void specifySpace(SpaceSpecification spaceSpecification) {
        throw new NotImplementedException();
    }

    @Override
    public void removeSpaceSpecification(SpaceName spaceName) {
        throw new NotImplementedException();
    }

    @Override
    public boolean checkTypeInSpace(SpaceName spaceName, String typeName) {
        throw new NotImplementedException();

    }

    @Override
    public void addTypeToSpace(SpaceName spaceName, String typeName) {
        throw new NotImplementedException();
    }

    @Override
    public void removeTypeFromSpace(SpaceName spaceName, String typeName) {
        throw new NotImplementedException();
    }

    private void checkOnSpaceSpecificationAdminOperations(SpaceName spaceName) {
        throw new NotImplementedException();
    }
}
