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
public class TenantDefinition {
    private String title;
    private String contactEmail;
    private String copyright;
    private String namespace;

    public static final TenantDefinition defaultDefinition = new TenantDefinition(
            "MarmotGraph",
            "support@marmotgraph.org",
            "MarmotGraph",
            "https://marmotgraph.org/"
    );

}
