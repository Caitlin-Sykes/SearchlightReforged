package com.csykes.searchlight.features.brightness;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

@GameTestHolder(Searchlight.MODID)
public class LightBrightnessGameTest {

    /**
     * Tests that right-clicking a light block with Glowstone Dust increases its brightness stage,
     * and right-clicking with Redstone decreases its brightness stage.
     */
    @GameTest
    public static void testLightBrightnessAdjustment(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle lamp = context.placeBlock("searchlight:colour_lamp_white");

        lamp.assertProperty(AbstractLightBlock.BRIGHTNESS, BrightnessStage.MEDIUM)
                // Increase to HIGH with glowstone dust
                .rightClickWithItem("minecraft:glowstone_dust")
                .waitTicks(1)
                .assertProperty(AbstractLightBlock.BRIGHTNESS, BrightnessStage.HIGH)
                // Increase to ULTRA with glowstone dust
                .rightClickWithItem("minecraft:glowstone_dust")
                .waitTicks(1)
                .assertProperty(AbstractLightBlock.BRIGHTNESS, BrightnessStage.ULTRA)
                // Decrease back to HIGH with redstone
                .rightClickWithItem("minecraft:redstone")
                .waitTicks(1)
                .assertProperty(AbstractLightBlock.BRIGHTNESS, BrightnessStage.HIGH)
                // Decrease to MEDIUM with redstone
                .rightClickWithItem("minecraft:redstone")
                .waitTicks(1)
                .assertProperty(AbstractLightBlock.BRIGHTNESS, BrightnessStage.MEDIUM)
                // Decrease to LOW with redstone
                .rightClickWithItem("minecraft:redstone")
                .waitTicks(1)
                .assertProperty(AbstractLightBlock.BRIGHTNESS, BrightnessStage.LOW);

        context.execute();
    }
}
