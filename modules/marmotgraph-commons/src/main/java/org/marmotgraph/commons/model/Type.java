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

package org.marmotgraph.commons.model;

import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.model.external.types.SpaceTypeInformation;
import org.marmotgraph.commons.model.external.types.TypeInformation;
import org.marmotgraph.commons.constants.EBRAINSVocabulary;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Type {

    @Getter
    private String name;
    @Setter
    @Getter
    private transient Boolean ignoreIncomingLinks;
    private transient Set<SpaceName> spaces;

    public Type() {
    }

    public Type(String name) {
        this.name = URLDecoder.decode(name, StandardCharsets.UTF_8);
    }

    public Set<SpaceName> getSpaces() {
        return spaces == null ? new HashSet<>() : spaces;
    }

    public Set<SpaceName> getSpacesForInternalUse(SpaceName privateSpaceName){
        return spaces!=null ? spaces.stream().map(s -> SpaceName.getInternalSpaceName(s, privateSpaceName)).collect(Collectors.toSet()) : new HashSet<>() ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return Objects.equals(name, type.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static Type fromPayload(TypeInformation payload) {
        Type targetType = new Type(payload.getIdentifier());
        targetType.setIgnoreIncomingLinks(payload.getAs(EBRAINSVocabulary.META_IGNORE_INCOMING_LINKS, Boolean.class));
        final List<SpaceTypeInformation> spaces = payload.getSpaces();
        if(spaces!=null){
            targetType.spaces = spaces.stream().map(s -> SpaceName.fromString(s.getSpace())).collect(Collectors.toSet());
        }
        return targetType;
    }

    public static String labelFromName(String fullyQualifiedName) {
        if (fullyQualifiedName != null) {
            if (fullyQualifiedName.startsWith("@")) {
                return fullyQualifiedName.replace("@", "");
            }
            if (fullyQualifiedName.lastIndexOf("#") > -1) {
                return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf("#") + 1);
            }
            try {
                URI uri = new URI(fullyQualifiedName);
                return uri.getPath() != null ? uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1) : null;
            } catch (URISyntaxException e) {
                return fullyQualifiedName;
            }
        }
        return null;
    }


}
