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

package org.marmotgraph.authorization.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.marmotgraph.authorization.models.Permission;
import org.marmotgraph.commons.JsonAdapter;
import org.marmotgraph.commons.jsonld.JsonLdDoc;
import org.marmotgraph.commons.permission.roles.Role;
import org.springframework.stereotype.Component;

import java.util.*;

@AllArgsConstructor
@Component
public class PermissionsService {

    private final PermissionsRepository permissionsRepository;
    private final EntityManager em;
    private final JsonAdapter jsonAdapter;


    public List<Permission> getAllRoleDefinitions(){
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Permission> criteriaQuery = criteriaBuilder.createQuery(Permission.class);
        Root<Permission> root = criteriaQuery.from(Permission.class);
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("id")));
        return em.createQuery(criteriaQuery).getResultList();
    }

    public JsonLdDoc getClaimForRole(Role role) {
        Optional<Permission> byId = permissionsRepository.findById(role.getName());
        return byId.map(permission -> jsonAdapter.fromJson(permission.getClaims(), JsonLdDoc.class)).orElse(null);
    }

    // Although we might be able to return an empty JsonLdDoc, we think it's more readable to return null
    public JsonLdDoc addClaimToRole(Role role, Map<String, Object> claimPattern) {
        Optional<Permission> byId = permissionsRepository.findById(role.getName());
        Permission p;
        JsonLdDoc document;
        if(byId.isEmpty()) {
            p = new Permission();
            p.setId(role.getName());
            document = new JsonLdDoc();
        }
        else{
            p = byId.get();
            document = jsonAdapter.fromJson(p.getClaims(), JsonLdDoc.class);
        }
        boolean empty = synchronizeMaps(role.getName(), claimPattern, document, false);
        if (empty) {
            permissionsRepository.deleteById(role.getName());
            return null;
        } else {
            p.setClaims(jsonAdapter.toJson(document));
            permissionsRepository.saveAndFlush(p);
            return document;
        }
    }


    // Although we might be able to return an empty JsonLdDoc, we think it's more readable to return null
    public JsonLdDoc removeClaimFromRole(Role role, Map<String, Object> claimPattern) {
        Optional<Permission> byId = permissionsRepository.findById(role.getName());
        if (byId.isEmpty()) {
            //The document doesn't exist - nothing to remove, nothing to do...
            return null;
        }
        Permission p = byId.get();
        JsonLdDoc jsonLdDoc = jsonAdapter.fromJson(byId.get().getClaims(), JsonLdDoc.class);
        boolean empty = synchronizeMaps(role.getName(), claimPattern, jsonLdDoc, true);
        if (empty) {
            permissionsRepository.deleteById(role.getName());
            return null;
        } else {
            p.setClaims(jsonAdapter.toJson(jsonLdDoc));
            permissionsRepository.saveAndFlush(p);
            return jsonLdDoc;
        }
    }

    private <K, V> boolean synchronizeMaps(String role, Map<K, V> source, Map<K, V> target, boolean remove) {
        Set<K> toBeRemoved = new HashSet<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (!target.containsKey(entry.getKey())) {
                if (remove) {
                    //We want to remove, but it doesn't exist - so we're good.
                } else {
                    //It doesn't exist yet, so we can just attach it
                    target.put(entry.getKey(), entry.getValue());
                }
            } else {
                final Object existingValue = target.get(entry.getKey());
                if (entry.getValue() instanceof Map entryMap) {
                    if (existingValue instanceof Map existingMap) {
                        boolean empty = synchronizeMaps(role, entryMap, existingMap, remove);
                        if (empty) {
                            target.remove(entry.getKey());
                        }
                    } else {
                        throw new RuntimeException(String.format(
                                "There is a problem with the structure of the permission map for the role %s. It seems like there are incompatible levels.", role));
                    }
                } else if (entry.getValue() instanceof List entryList) {
                    if (existingValue instanceof List existingList) {
                        for (Object e : entryList) {
                            if (remove && existingList.contains(e)) {
                                existingList.remove(e);
                            } else if (!remove && !existingList.contains(e)) {
                                existingList.add(e);
                            }
                        }
                        if (existingList.isEmpty()) {
                            toBeRemoved.add(entry.getKey());
                        }
                    } else {
                        throw new RuntimeException(String.format(
                                "There is a problem with the structure of the permission map for the role %s. It seems like there are incompatible levels.", role));
                    }
                } else if (existingValue != null && existingValue.equals(entry.getValue())) {
                    toBeRemoved.add(entry.getKey());
                }
            }
        }
        toBeRemoved.forEach(target::remove);
        return target.isEmpty();
    }

}
