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

package org.marmotgraph.tenants.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.commons.model.tenant.TenantDefinition;

import java.util.Optional;

@Entity
@Getter
@Setter
public class Tenant {

    @Id
    private String name;

    private String title;

    private String contactEmail;
    private String copyright;

    @OneToOne(cascade = CascadeType.ALL)
    private Image favIcon;

    @OneToOne(cascade = CascadeType.ALL)
    private Image darkLogo;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Image brightLogo;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Image darkBgImage;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Image brightBgImage;

    @Column(columnDefinition = "TEXT")
    private String colorScheme;

    @Column(columnDefinition = "TEXT")
    private String customCSS;

    @Column(columnDefinition = "TEXT")
    private String font;

    public static Tenant fromTenantDefinition(String tenantName, TenantDefinition tenantDefinition, Optional<Tenant> existingTenant) {
        if(tenantDefinition == null) {
            return null;
        }
        Tenant tenant = null;
        if(existingTenant.isPresent()) {
            tenant = existingTenant.get();
        }
        else{
            tenant = new Tenant();
            tenant.setName(tenantName);
        }
        tenant.setContactEmail(tenantDefinition.getContactEmail());
        tenant.setCopyright(tenantDefinition.getCopyright());
        tenant.setTitle(tenantDefinition.getTitle());
        return tenant;

    }

    public TenantDefinition toTenantDefinition() {
        return new TenantDefinition(getTitle(), getContactEmail(), getCopyright());

    }

}
