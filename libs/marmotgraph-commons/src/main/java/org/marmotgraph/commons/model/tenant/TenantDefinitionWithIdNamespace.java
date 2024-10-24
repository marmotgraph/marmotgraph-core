package org.marmotgraph.commons.model.tenant;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantDefinitionWithIdNamespace extends TenantDefinition {
    private String idNamespace;

    public static TenantDefinitionWithIdNamespace fromTenantDefinition(TenantDefinition tenantDefinition, String idNamespace) {
        TenantDefinitionWithIdNamespace definition = new TenantDefinitionWithIdNamespace();
        if (tenantDefinition != null) {
            definition.setCopyright(tenantDefinition.getCopyright());
            definition.setTitle(tenantDefinition.getTitle());
            definition.setContactEmail(tenantDefinition.getContactEmail());
        }
        definition.setIdNamespace(idNamespace);
        return definition;
    }

}
