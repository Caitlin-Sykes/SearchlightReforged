package com.csykes.searchlight.features.wall_light;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(Searchlight.MODID)
public class WallLightGameTest {

    /**
     * Tests placing a WallLightBlock, checking its initial state properties,
     * and adjusting its brightness stage using glowstone dust.
     */
    @GameTest
    public static void testWallLightPropertiesAndBrightness(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle wallLight = context.placeBlock("searchlight:wall_light_iron");

        wallLight.assertBlockId("searchlight:wall_light_iron")
                .assertProperty(WallLightBlock.FACING, Direction.NORTH)
                .assertProperty(WallLightBlock.FACE, AttachFace.WALL)
                .verifyBlockEntity(WallLightBlockEntity.class, be -> {
                    if (be.getBrightness() != BrightnessStage.MEDIUM) {
                        throw new GameTestAssertException("Expected initial MEDIUM brightness, but was " + be.getBrightness());
                    }
                })
                .rightClickWithItem("minecraft:glowstone_dust")
                .waitTicks(1)
                .verifyBlockEntity(WallLightBlockEntity.class, be -> {
                    if (be.getBrightness() != BrightnessStage.HIGH) {
                        throw new GameTestAssertException("Expected HIGH brightness, but was " + be.getBrightness());
                    }
                });

        context.execute();
    }
}
