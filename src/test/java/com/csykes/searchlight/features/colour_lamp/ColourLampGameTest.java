package com.csykes.searchlight.features.colour_lamp;

import com.csykes.searchlight.Searchlight;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(Searchlight.MODID)
public class ColourLampGameTest {

    /**
     * Tests that right-clicking a ColourLampBlock with different dyes updates its colour/variant correctly.
     */
    @GameTest
    public static void testColourLampDyeInteractions(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle lamp = context.placeBlock("searchlight:colour_lamp_white");

        lamp.assertBlockId("searchlight:colour_lamp_white")
                .rightClickWithItem("minecraft:red_dye", Direction.UP)
                .waitTicks(1)
                .assertBlockId("searchlight:colour_lamp_red")
                .rightClickWithItem("minecraft:blue_dye", Direction.DOWN)
                .waitTicks(1)
                .assertBlockId("searchlight:colour_lamp_blue")
                .rightClickWithItem("minecraft:lime_dye", Direction.WEST)
                .waitTicks(1)
                .assertBlockId("searchlight:colour_lamp_lime");

        context.execute();
    }

    /**
     * Tests placing adjacent ColourLampBlocks and verifying connection properties update accordingly.
     */
    @GameTest
    public static void testColourLampAdjacencyConnections(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos pos1 = new BlockPos(1, 1, 1);
        BlockPos pos2 = new BlockPos(1, 1, 2);

        BlockHandle lamp1 = context.placeBlock(pos1, "searchlight:colour_lamp_white");
        BlockHandle lamp2 = context.placeBlock(pos2, "searchlight:colour_lamp_red");

        context.assertThat(
                () -> lamp1.getBlockState().getValue(ColourLampBlock.SOUTH),
                "Expected first lamp to be connected SOUTH to adjacent lamp"
        );

        context.assertThat(
                () -> lamp2.getBlockState().getValue(ColourLampBlock.NORTH),
                "Expected second lamp to be connected NORTH to adjacent lamp"
        );

        context.execute();
    }
}
