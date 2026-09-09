package com.csykes.searchlight.features.lighting_director;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity.LinkedLightEntry;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.LightMode;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

@GameTestHolder(Searchlight.MODID)
public class LightingDirectorGameTest {

    /**
     * Tests that the LightingDirector accurately tracks linked lights, their types,
     * addresses, modes, and connected block counts via getLinkedLightEntries.
     */
    @GameTest
    public static void testLightingDirectorEntries(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle directorHandle = context.placeBlock("searchlight:lighting_director");

        BlockPos light1Pos = new BlockPos(1, 1, 1);
        BlockPos light2Pos = new BlockPos(1, 2, 1);
        BlockHandle light1 = context.placeBlock(light1Pos, "searchlight:centre_light_white");
        BlockHandle light2 = context.placeBlock(light2Pos, "searchlight:centre_light_white");

        directorHandle.verifyBlockEntity(LightingDirectorBlockEntity.class, director -> {
            int slot = director.toggleLinkedLight(light1.getAbsolutePos(), helper.getLevel());
            if (slot != 1) {
                throw new GameTestAssertException("Expected light to be linked to slot 1, but got slot " + slot);
            }

            List<LinkedLightEntry> entries = director.getLinkedLightEntries();
            if (entries.size() != 1) {
                throw new GameTestAssertException("Expected 1 entry, but got " + entries.size());
            }

            LinkedLightEntry entry = entries.get(0);
            if (entry.slot() != 1) {
                throw new GameTestAssertException("Expected entry slot 1, but got " + entry.slot());
            }
            if (!"001".equals(entry.address())) {
                throw new GameTestAssertException("Expected 3-digit default address '001', but got " + entry.address());
            }
            if (!"Fixture".equals(entry.mode())) {
                throw new GameTestAssertException("Expected Fixture mode, but got " + entry.mode());
            }
            if (entry.connectedCount() != 2) {
                throw new GameTestAssertException("Expected connected count 2, but got " + entry.connectedCount());
            }

            // Unlink the light
            boolean unlinked = director.removeLinkedLight(0);
            if (!unlinked) {
                throw new GameTestAssertException("Failed to unlink light at slot 0");
            }

            List<LinkedLightEntry> remainingEntries = director.getLinkedLightEntries();
            if (!remainingEntries.isEmpty()) {
                throw new GameTestAssertException("Expected 0 entries after unlinking, but got " + remainingEntries.size());
            }
        });

        context.execute();
    }

    /**
     * Tests that SEPARATE mode restricts dye propagation so only the clicked block is modified.
     */
    @GameTest
    public static void testSeparateModeDyeRestriction(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos bottomPos = new BlockPos(1, 1, 1);
        BlockPos topPos = new BlockPos(1, 2, 1);

        BlockHandle bottomLight = context.placeBlock(bottomPos, "searchlight:centre_light_white");
        BlockHandle topLight = context.placeBlock(topPos, "searchlight:centre_light_white");

        bottomLight.verifyBlockEntity(WallLightBlockEntity.class, be -> {
            be.setLightMode(LightMode.SEPARATE);
            be.setAddress("001");
        });
        topLight.verifyBlockEntity(WallLightBlockEntity.class, be -> {
            be.setLightMode(LightMode.SEPARATE);
            be.setAddress("002");
        });

        // Dye the bottom light orange
        bottomLight.rightClickWithItem("minecraft:orange_dye")
                .waitTicks(1);

        // Bottom light should be orange, but top light should remain white because mode is SEPARATE
        bottomLight.assertBlockId("searchlight:centre_light_orange");
        topLight.assertBlockId("searchlight:centre_light_white");

        context.execute();
    }
}
