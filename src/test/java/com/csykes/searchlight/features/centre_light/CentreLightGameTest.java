package com.csykes.searchlight.features.centre_light;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.lighting.LightRodConnection;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import net.minecraft.gametest.framework.GameTestAssertException;

@GameTestHolder(Searchlight.MODID)
public class CentreLightGameTest {

    /**
     * Tests placing a CentreLightBlock, verifying initial axis and connection properties,
     * and changing its colour via dye items.
     */
    @GameTest
    public static void testCentreLightDyeAndProperties(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle light = context.placeBlock("searchlight:centre_light_white");

        light.assertBlockId("searchlight:centre_light_white")
                .assertProperty(CentreLightBlock.AXIS, Direction.Axis.Y)
                .assertProperty(CentreLightBlock.CONNECTION, LightRodConnection.SINGLE)
                .rightClickWithItem("minecraft:orange_dye")
                .waitTicks(1)
                .assertBlockId("searchlight:centre_light_orange")
                .rightClickWithItem("minecraft:purple_dye")
                .waitTicks(1)
                .assertBlockId("searchlight:centre_light_purple");

        context.execute();
    }

    /**
     * Tests that placing multiple CentreLightBlocks along the Y axis updates their connection states
     * (TOP, BOTTOM, MIDDLE) correctly.
     */
    @GameTest
    public static void testCentreLightAxisConnections(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos bottomPos = new BlockPos(1, 1, 1);
        BlockPos midPos = new BlockPos(1, 2, 1);
        BlockPos topPos = new BlockPos(1, 3, 1);

        BlockHandle bottomLight = context.placeBlock(bottomPos, "searchlight:centre_light_white");
        BlockHandle midLight = context.placeBlock(midPos, "searchlight:centre_light_white");
        BlockHandle topLight = context.placeBlock(topPos, "searchlight:centre_light_white");

        bottomLight.assertProperty(CentreLightBlock.CONNECTION, LightRodConnection.BOTTOM);
        midLight.assertProperty(CentreLightBlock.CONNECTION, LightRodConnection.MIDDLE);
        topLight.assertProperty(CentreLightBlock.CONNECTION, LightRodConnection.TOP);

        context.execute();
    }

    /**
     * Tests that dyeing one CentreLightBlock propagates the colour change to all connected centre lights.
     */
    @GameTest
    public static void testCentreLightConnectedDyePropagation(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos bottomPos = new BlockPos(1, 1, 1);
        BlockPos midPos = new BlockPos(1, 2, 1);
        BlockPos topPos = new BlockPos(1, 3, 1);

        BlockHandle bottomLight = context.placeBlock(bottomPos, "searchlight:centre_light_white");
        BlockHandle midLight = context.placeBlock(midPos, "searchlight:centre_light_white");
        BlockHandle topLight = context.placeBlock(topPos, "searchlight:centre_light_white");

        // Dye the middle light blue; all three should become blue
        midLight.rightClickWithItem("minecraft:blue_dye")
                .waitTicks(1);

        bottomLight.assertBlockId("searchlight:centre_light_blue");
        midLight.assertBlockId("searchlight:centre_light_blue");
        topLight.assertBlockId("searchlight:centre_light_blue");

        context.execute();
    }

    /**
     * Tests that adjusting brightness on one CentreLightBlock propagates across all connected centre lights.
     */
    @GameTest
    public static void testCentreLightConnectedBrightnessPropagation(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos bottomPos = new BlockPos(1, 1, 1);
        BlockPos topPos = new BlockPos(1, 2, 1);

        BlockHandle bottomLight = context.placeBlock(bottomPos, "searchlight:centre_light_white");
        BlockHandle topLight = context.placeBlock(topPos, "searchlight:centre_light_white");

        // Increase brightness on bottom light with glowstone dust
        bottomLight.rightClickWithItem("minecraft:glowstone_dust")
                .waitTicks(1);

        bottomLight.verifyBlockEntity(WallLightBlockEntity.class, be -> {
            if (be.getBrightness() != BrightnessStage.HIGH) {
                throw new GameTestAssertException("Expected bottom light HIGH brightness, but was " + be.getBrightness());
            }
        });
        topLight.verifyBlockEntity(WallLightBlockEntity.class, be -> {
            if (be.getBrightness() != BrightnessStage.HIGH) {
                throw new GameTestAssertException("Expected top light HIGH brightness, but was " + be.getBrightness());
            }
        });

        context.execute();
    }
}
