package org.marmotgraph.commons.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ImageDefinition {
    private String fileName;
    private String mimeType;
    private String base64;
}
