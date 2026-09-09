package com.csykes.searchlight.integration.dyenamics;

import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;

import java.util.Map;

public class DyenamicHelper {
    public static int getDyenamicColor(String name) {
        for (DyenamicDyeColor color : DyenamicDyeColor.dyenamicValues()) {
            if (color.getSerializedName().equals(name)) {
                return 0xFF000000 | color.getFireworkColor();
            }
        }
        return -1;
    }

    public static Map<String, Integer> getAllDyenamicColors() {
        Map<String, Integer> map = new java.util.HashMap<>();
        for (DyenamicDyeColor color : DyenamicDyeColor.dyenamicValues()) {
            map.put(color.getSerializedName(), 0xFF000000 | color.getFireworkColor());
        }
        return map;
    }
}
