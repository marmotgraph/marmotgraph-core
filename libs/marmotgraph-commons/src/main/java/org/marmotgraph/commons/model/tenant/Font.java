package org.marmotgraph.commons.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Font {
    private String url;
    private String fontFamily;

    public static final Font DEFAULT = new Font("http://fonts.googleapis.com/css?family=Roboto:400,100,100italic,300,300italic,400italic,500,500italic,700,700italic,900italic,900", "'Roboto', sans-serif");

    public String toCSS(){
        StringBuilder sb = new StringBuilder();
        if(url != null && !url.isBlank()){
            sb.append(String.format("@import url(%s);\n\n", url));
        }
        if(fontFamily != null && !fontFamily.isBlank()){
            sb.append(String.format("""
                     html, body, html * {
                        font-family: %s;
                     }
                    """, fontFamily));
        }
        return sb.toString();
    }
}
