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
}
