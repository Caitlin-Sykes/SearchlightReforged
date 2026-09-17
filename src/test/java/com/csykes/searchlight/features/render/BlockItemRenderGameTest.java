package com.csykes.searchlight.features.render;

import com.mat.api.TestContext;
import com.csykes.searchlight.utils.BlockItemRendererUtil;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.gametest.GameTestHolder;
import com.csykes.searchlight.Searchlight;

import java.io.File;

@GameTestHolder(Searchlight.MODID)
public class BlockItemRenderGameTest {

    /**
     * Verifies off-screen rendering of a block item to PNG using Minecraft's ItemRenderer pipeline.
     * Guards against execution on dedicated server environment where client classes are stripped.
     */
    @GameTest
    public static void testRenderSingleBlockItem(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        context.queueAction(h -> {
            if (FMLEnvironment.dist.isDedicatedServer()) {
                // GameTest is running on dedicated server (gameTestServer).
                // Client-side rendering is only executable when running with a client (e.g. ciClient/client).
                return;
            }

            ClientRenderHelper.renderSingleBlock();
        });

        context.execute();
    }

    private static class ClientRenderHelper {
        public static void renderSingleBlock() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) {
                return;
            }

            mc.execute(() -> {
                try {
                    ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("searchlight", "colour_lamp_white");
                    File outFile = new File("build/test_renders/colour_lamp_white.png");

                    BlockItemRendererUtil.renderBlockItemToPng(blockId, outFile, 64, 64);

                    if (!outFile.exists() || outFile.length() == 0) {
                        throw new GameTestAssertException("Failed to render item: output file missing or empty at " + outFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error rendering block item", e);
                }
            });
        }
    }
}
