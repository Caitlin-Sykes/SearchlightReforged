package com.csykes.searchlight.integration.dyenamics;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.colour_lamp.ColourLampBlock;
import com.csykes.searchlight.features.colour_lamp_slab.ColourLampSlabBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlock;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;

import java.util.Arrays;

public class DyenamicsIntegration {
    public static void init() {
        Arrays.stream(DyenamicDyeColor.dyenamicValues()).forEach(color -> {
            String name = color.getSerializedName();
            registerDyenamicWallLight(name);
            registerDyenamicCornerLight(name);
            registerDyenamicCentreLight(name);
            registerDyenamicEdgeLight(name);
            registerDyenamicColourLampLight(name);
            registerDyenamicColourLampSlabLight(name);
            registerDyenamicSearchlight(name);
        });
    }

    private static void registerDyenamicWallLight(String postfix) {
        Searchlight.registerBlockAndItem("wall_light_" + postfix, WallLightBlock::new, Searchlight.wallLightProperties(), Searchlight.WALL_LIGHTS, Searchlight.WALL_LIGHT_ITEMS, postfix);
    }

    private static void registerDyenamicSearchlight(String postfix) {
        Searchlight.registerBlockAndItem("searchlight_" + postfix, props -> new SearchlightBlock(props, postfix), Searchlight.searchlightProperties(), Searchlight.SEARCHLIGHTS, Searchlight.SEARCHLIGHT_ITEMS, postfix);
    }

    private static void registerDyenamicCornerLight(String postfix) {
        Searchlight.registerBlockAndItem("corner_light_" + postfix, props -> new CornerLightBlock(props, postfix), Searchlight.glassLightProperties(), Searchlight.CORNER_LIGHTS, Searchlight.CORNER_LIGHTS_ITEMS, postfix);
    }

    private static void registerDyenamicEdgeLight(String postfix) {
        Searchlight.registerBlockAndItem("edge_light_" + postfix, props -> new EdgeLightBlock(props, postfix), Searchlight.glassLightProperties(), Searchlight.EDGE_LIGHTS, Searchlight.EDGE_LIGHTS_ITEMS, postfix);
    }

    private static void registerDyenamicCentreLight(String postfix) {
        Searchlight.registerBlockAndItem("centre_light_" + postfix, props -> new CentreLightBlock(props, postfix), Searchlight.glassLightProperties(), Searchlight.CENTRE_LIGHTS, Searchlight.CENTRE_LIGHTS_ITEMS, postfix);
    }

    private static void registerDyenamicColourLampLight(String postfix) {
        Searchlight.registerBlockAndItem("colour_lamp_" + postfix, props -> new ColourLampBlock(props, postfix), Searchlight.glassLightProperties(), Searchlight.COLOUR_LAMPS, Searchlight.COLOUR_LAMP_ITEMS, postfix);
    }

    private static void registerDyenamicColourLampSlabLight(String postfix) {
        Searchlight.registerBlockAndItem("colour_lamp_slab_" + postfix, props -> new ColourLampSlabBlock(props, postfix), Searchlight.glassLightProperties(), Searchlight.COLOUR_SLAB_LAMPS, Searchlight.COLOUR_SLAB_ITEMS, postfix);
    }
}
