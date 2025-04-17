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

package org.marmotgraph.commons.model.tenant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ColorScheme {

    private BrandColors brandColors;
    private Greys greys;
    private Blacks blacks;
    private StateColors stateColors;
    private LinkColors linkColors;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class BrandColors {
        private String primary;
        private String secondary;
        private String tertiary;

        public static final BrandColors DEFAULT = new BrandColors(
                "#414042",
                "#E67634",
                "#B2B1B2"
        );
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Greys {
        private String grey200;
        private String grey300;
        private String grey400;
        private String grey500;
        private String grey600;
        private String grey700;
        private String grey800;
        private String grey900;

        public static final Greys DEFAULT = new Greys(
                "#414042",
                "#414042",
                "#414042",
                "#414042",
                "#414042",
                "#414042",
                "#414042",
                "#414042"
        );

    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Blacks {
        private String black05;
        private String black09;
        public static final Blacks DEFAULT = new Blacks(
                "#80414042",
                "#E6414042"
        );

    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class StateColors {
        private String warning;
        private String info;
        private String success;
        private String error;
        public static final StateColors DEFAULT = new StateColors(
                "#414042",
                "#414042",
                "#414042",
                "#414042"
        );

    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class LinkColors {
        private String defaultColor;
        private String activeColor;
        private String visitedColor;
        private String disabledColor;
        private String hoverColor;

        public static final LinkColors DEFAULT = new LinkColors(
                "#E67634",
                "#E67634",
                "#E67634",
                "#B2B1B2",
                "#E67634"
        );
    }

    public static final ColorScheme DEFAULT = new ColorScheme(BrandColors.DEFAULT, Greys.DEFAULT, Blacks.DEFAULT, StateColors.DEFAULT, LinkColors.DEFAULT);

    public String toCSS(){
        return String.format("""
                :root {
                  --brand-primary: "%s";
                  --brand-seconday: "%s";
                  --brand-tertiary: "%s";
                
                  --grey-200: "%s";
                  --grey-300: "%s";
                  --grey-400: "%s";
                  --grey-500: "%s";
                  --grey-600: "%s";
                  --grey-700: "%s";
                  --grey-800: "%s";
                  --grey-900: "%s";
                
                  --black-05: "%s";
                  --black-09: "%s";
                
                  --state-warning: "%s";
                  --state-info: "%s";
                  --state-success: "%s";
                  --state-error: "%s";
                
                  --link-default: "%s";
                  --link-active: "%s";
                  --link-visited: "%s";
                  --link-disabled: "%s";
                  --link-hover: "%s";
                }
                """,
                brandColors.primary, brandColors.secondary, brandColors.tertiary,
                greys.grey200, greys.grey300, greys.grey400, greys.grey500, greys.grey600, greys.grey700, greys.grey800, greys.grey900,
                blacks.black05, blacks.black09,
                stateColors.warning, stateColors.info, stateColors.success, stateColors.error,
                linkColors.defaultColor, linkColors.activeColor, linkColors.visitedColor, linkColors.disabledColor, linkColors.hoverColor
        );
    }
}
