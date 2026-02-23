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

package org.marmotgraph.graphdb.neo4j.service;

public class TestQueries {

    public static final String COUNTRIES_WITH_LANGUAGES= """
            {
                "https://core.kg.ebrains.eu/vocab/query/meta": {
                  "https://core.kg.ebrains.eu/vocab/query/type": "https://marmotgraph.org/types/Country",
                  "https://core.kg.ebrains.eu/vocab/query/responseVocab": "https://schema.hbp.eu/myQuery/"
                },
                "https://core.kg.ebrains.eu/vocab/query/structure": [
                  {
                    "https://core.kg.ebrains.eu/vocab/query/propertyName": {
                      "@id": "https://schema.hbp.eu/myQuery/id"
                    },
                    "https://core.kg.ebrains.eu/vocab/query/path": {
                      "@id": "@id"
                    }
                  },
                  {
                    "https://core.kg.ebrains.eu/vocab/query/propertyName": {
                      "@id": "https://schema.hbp.eu/myQuery/name"
                    },
                    "https://core.kg.ebrains.eu/vocab/query/path": {
                      "@id": "https://marmotgraph.org/properties/name"
                    }
                  },
                  {
                    "https://core.kg.ebrains.eu/vocab/query/propertyName": {
                      "@id": "https://schema.hbp.eu/myQuery/officialLanguages"
                    },
                    "https://core.kg.ebrains.eu/vocab/query/path": {
                      "@id": "https://marmotgraph.org/properties/officialLanguages"
                    },
                    "https://core.kg.ebrains.eu/vocab/query/structure": [
                      {
                        "https://core.kg.ebrains.eu/vocab/query/propertyName": {
                          "@id": "https://schema.hbp.eu/myQuery/name"
                        },
                        "https://core.kg.ebrains.eu/vocab/query/path": {
                          "@id": "https://marmotgraph.org/properties/name"
                        }
                      },
                      {
                        "https://core.kg.ebrains.eu/vocab/query/propertyName": {
                          "@id": "https://schema.hbp.eu/myQuery/dialects"
                        },
                        "https://core.kg.ebrains.eu/vocab/query/path": [
                          {
                            "@id": "https://marmotgraph.org/properties/dialectOf",
                            "https://core.kg.ebrains.eu/vocab/query/reverse": true
                          },
                          {
                            "@id": "https://marmotgraph.org/properties/name"
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "https://core.kg.ebrains.eu/vocab/query/propertyName": {
                      "@id": "https://schema.hbp.eu/myQuery/hosts"
                    },
                    "https://core.kg.ebrains.eu/vocab/query/path": [
                      {
                        "@id": "https://marmotgraph.org/properties/countries",
                        "https://core.kg.ebrains.eu/vocab/query/reverse": true
                      },
                      {
                        "@id": "https://marmotgraph.org/properties/name"
                      }
                    ]
                  }
                ]
              }
            """;

}
