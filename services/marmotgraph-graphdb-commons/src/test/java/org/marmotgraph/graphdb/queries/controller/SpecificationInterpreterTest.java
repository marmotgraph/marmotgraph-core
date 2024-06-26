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

package org.marmotgraph.graphdb.queries.controller;

import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.jsonld.NormalizedJsonLd;
import org.marmotgraph.graphdb.queries.model.spec.Specification;
import org.marmotgraph.test.JsonAdapter4Test;
import org.marmotgraph.test.Simpsons;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
public class SpecificationInterpreterTest {

    JsonAdapter jsonAdapter = new JsonAdapter4Test();

    @Test
    public void testReadSpecification(){
        //Given
        SpecificationInterpreter specificationInterpreter = new SpecificationInterpreter();
        NormalizedJsonLd query = jsonAdapter.fromJson(Simpsons.Queries.FAMILY_NAMES_NORMALIZED, NormalizedJsonLd.class);

        //When
        Specification specification = specificationInterpreter.readSpecification(query);

        //Then
        assertEquals(2, specification.getProperties().size());
        assertEquals("https://thesimpsons.com/FamilyMember", specification.getRootType().getName());
    }


}