package org.marmotgraph.commons.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ImageResource {
    private String fileName;
    private String bright;
    private String dark;
}
