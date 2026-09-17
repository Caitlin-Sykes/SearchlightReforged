package com.csykes.searchlight.features.integration.dyenamics;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.integration.dyenamics.DyenamicHelper;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import cy.jdkdigital.dyenamics.core.util.DyenamicDyeColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.io.InputStream;
import java.util.Arrays;

@GameTestHolder(Searchlight.MODID)
public class DyenamicsIntegrationTest {

    /**
     * Tests that all Dyenamics colour variants for all light types are registered in BuiltInRegistries.
     */
    @GameTest
    public static void testDyenamicsBlocksRegistration(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        String[] allDyenamicColours = Arrays.stream(DyenamicDyeColor.dyenamicValues())
                .map(DyenamicDyeColor::getSerializedName)
                .toArray(String[]::new);

        context.assertThat(() -> {
            for (String colour : allDyenamicColours) {
                ResourceLocation wallLightId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "wall_light_" + colour);
                ResourceLocation searchlightId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "searchlight_" + colour);
                ResourceLocation cornerLightId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "corner_light_" + colour);
                ResourceLocation edgeLightId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "edge_light_" + colour);
                ResourceLocation centreLightId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "centre_light_" + colour);
                ResourceLocation colourLampId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "colour_lamp_" + colour);
                ResourceLocation colourSlabId = ResourceLocation.fromNamespaceAndPath(Searchlight.MODID, "colour_lamp_slab_" + colour);

                if (!BuiltInRegistries.BLOCK.containsKey(wallLightId)) return false;
                if (!BuiltInRegistries.BLOCK.containsKey(searchlightId)) return false;
                if (!BuiltInRegistries.BLOCK.containsKey(cornerLightId)) return false;
                if (!BuiltInRegistries.BLOCK.containsKey(edgeLightId)) return false;
                if (!BuiltInRegistries.BLOCK.containsKey(centreLightId)) return false;
                if (!BuiltInRegistries.BLOCK.containsKey(colourLampId)) return false;
                if (!BuiltInRegistries.BLOCK.containsKey(colourSlabId)) return false;
            }
            return true;
        }, "Expected all dyenamic colour variants for all light types to be registered in BuiltInRegistries");

        context.execute();
    }

    /**
     * Tests placing Dyenamics-coloured blocks in world, verifying BlockHandle placement and colour helper methods.
     */
    @GameTest
    public static void testDyenamicsBlockPlacementAndHelper(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle peachWallLight = context.placeBlock("searchlight:wall_light_peach");
        BlockHandle aquaCentreLight = context.placeBlock("searchlight:centre_light_aquamarine");
        BlockHandle mintColourLamp = context.placeBlock("searchlight:colour_lamp_mint");
        BlockHandle maroonSearchlight = context.placeBlock("searchlight:searchlight_maroon");

        peachWallLight.assertBlockId("searchlight:wall_light_peach");
        aquaCentreLight.assertBlockId("searchlight:centre_light_aquamarine");
        mintColourLamp.assertBlockId("searchlight:colour_lamp_mint");
        maroonSearchlight.assertBlockId("searchlight:searchlight_maroon");

        // Verify DyenamicHelper colour conversion
        context.assertThat(() -> DyenamicHelper.getDyenamicColor("peach") != -1, "Expected peach to resolve RGB in DyenamicHelper");
        context.assertThat(() -> DyenamicHelper.getDyenamicColor("aquamarine") != -1, "Expected aquamarine to resolve RGB in DyenamicHelper");
        context.assertThat(() -> DyenamicHelper.getDyenamicColor("non_existent_colour") == -1, "Expected non-existent colour to return -1");

        // Verify blockstate and model assets exist on classpath for Dyenamics blocks
        context.assertThat(() -> {
            try (InputStream is = DyenamicsIntegrationTest.class.getResourceAsStream("/assets/searchlight/blockstates/wall_light_peach.json")) {
                return is != null;
            } catch (Exception e) {
                return false;
            }
        }, "Expected blockstate /assets/searchlight/blockstates/wall_light_peach.json to exist");

        context.assertThat(() -> {
            try (InputStream is = DyenamicsIntegrationTest.class.getResourceAsStream("/assets/searchlight/models/block/wall_light_floor_peach.json")) {
                return is != null;
            } catch (Exception e) {
                return false;
            }
        }, "Expected block model /assets/searchlight/models/block/wall_light_floor_peach.json to exist");

        context.execute();
    }
}
