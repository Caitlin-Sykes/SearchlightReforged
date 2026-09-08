package com.csykes.searchlight.integration.dyenamics;

import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;

public class DyenamicHelper {
    public static int getDyenamicColor(String name) {
        for (DyenamicDyeColor color : DyenamicDyeColor.dyenamicValues()) {
            if (color.getSerializedName().equals(name)) {
                return 0xFF000000 | color.getFireworkColor();
            }
        }
        return -1;
    }
}
