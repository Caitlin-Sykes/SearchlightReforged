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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.Arrays;

import static com.csykes.searchlight.utils.lighting.AbstractLightBlock.LIT;

public class DyenamicsIntegration {
    public static void init() {
        Arrays.stream(DyenamicDyeColor.dyenamicValues()).forEach(color -> {
            registerDyenamicWallLight(color.getSerializedName());
            registerDyenamicCornerLight(color.getSerializedName());
            registerDyenamicCentreLight(color.getSerializedName());
            registerDyenamicEdgeLight(color.getSerializedName());
            registerDyenamicColourLampLight(color.getSerializedName());
            registerDyenamicColourLampSlabLight(color.getSerializedName());
            registerDyenamicSearchlight(color.getSerializedName());
        });
    }

    private static void registerDyenamicWallLight(String postfix) {
        String wl_name = "wall_light_" + postfix;
        DeferredBlock<Block> block = Searchlight.BLOCKS.register(wl_name, () -> new WallLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion()));

        Searchlight.WALL_LIGHTS.put(postfix, block);
        Searchlight.WALL_LIGHT_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(wl_name, block));
    }

    private static void registerDyenamicSearchlight(String postfix) {
        String wl_name = "searchlight_" + postfix;

        DeferredBlock<Block> block = Searchlight.BLOCKS.register(wl_name, () -> new SearchlightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .pushReaction(PushReaction.DESTROY)
                .sound(SoundType.METAL)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion(), postfix));

        Searchlight.SEARCHLIGHTS.put(postfix, block);
        Searchlight.SEARCHLIGHT_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(wl_name, block));
    }

    private static void registerDyenamicCornerLight(String postfix) {
        String cl_name = "corner_light_" + postfix;
        DeferredBlock<Block> block = Searchlight.BLOCKS.register(cl_name, () -> new CornerLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion(), postfix));
        Searchlight.CORNER_LIGHTS.put(postfix, block);
        Searchlight.CORNER_LIGHTS_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(cl_name, block));
    }

    private static void registerDyenamicEdgeLight(String postfix) {
        String el_name = "edge_light_" + postfix;
        DeferredBlock<Block> block = Searchlight.BLOCKS.register(el_name, () -> new EdgeLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion(), postfix));
        Searchlight.EDGE_LIGHTS.put(postfix, block);
        Searchlight.EDGE_LIGHTS_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(el_name, block));
    }

    private static void registerDyenamicCentreLight(String postfix) {
        String cl_name = "centre_light_" + postfix;
        DeferredBlock<Block> block = Searchlight.BLOCKS.register(cl_name, () -> new CentreLightBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
                .noOcclusion(), postfix));
        Searchlight.CENTRE_LIGHTS.put(postfix, block);
        Searchlight.CENTRE_LIGHTS_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(cl_name, block));
    }

    private static void registerDyenamicColourLampLight(String postfix) {
        String cl_name = "colour_lamp_" + postfix;
        DeferredBlock<Block> block = Searchlight.BLOCKS.register(cl_name, () -> new ColourLampBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(SoundType.GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), postfix));
        Searchlight.COLOUR_LAMPS.put(postfix, block);
        Searchlight.COLOUR_LAMP_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(cl_name, block));
    }

    private static void registerDyenamicColourLampSlabLight(String postfix) {
        String cl_name = "colour_lamp_slab_" + postfix;
        DeferredBlock<Block> block = Searchlight.BLOCKS.register(cl_name, () -> new ColourLampSlabBlock(BlockBehaviour.Properties.of()
                .lightLevel((state) -> state.hasProperty(LIT) && !state.getValue(LIT) ? 0 : 15)
                .sound(SoundType.GLASS)
                .strength(2.0f, 4.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion(), postfix));
        Searchlight.COLOUR_SLAB_LAMPS.put(postfix, block);
        Searchlight.COLOUR_SLAB_ITEMS.put(postfix, Searchlight.ITEMS.registerSimpleBlockItem(cl_name, block));
    }
}
