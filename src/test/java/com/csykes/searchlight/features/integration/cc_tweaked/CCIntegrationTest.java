package com.csykes.searchlight.features.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.integration.cc_tweaked.LightPeripheral;
import com.csykes.searchlight.integration.cc_tweaked.LightingDirectorPeripheral;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.LightRequest;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;
import java.util.Map;

@GameTestHolder(Searchlight.MODID)
public class CCIntegrationTest {

    private static final BlockCapability<IPeripheral, Direction> PERIPHERAL_CAPABILITY =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("computercraft", "peripheral"), IPeripheral.class);

    /**
     * Tests that Searchlight / Light block entities correctly expose the CC: Tweaked peripheral capability
     * and that peripheral methods (getBrightness, setBrightness, getColour, setColour, setLit, isLit) function properly.
     */
    @GameTest
    public static void testLightPeripheralOperations(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle handle = context.placeBlock("searchlight:searchlight_white");

        // Verify capability is attached on sides
        handle.verifyCapability(PERIPHERAL_CAPABILITY, Direction.NORTH, peripheral -> {
            context.assertThat(() -> peripheral != null, "Expected CC peripheral capability on light block");
            context.assertThat(() -> "search_light".equals(peripheral.getType()), "Expected peripheral type 'search_light'");
        });

        // Test LightPeripheral methods directly on the block entity
        handle.verifyBlockEntity(SearchlightBlockEntity.class, be -> {
            LightPeripheral peripheral = new LightPeripheral(be, "search_light");

            // Initial state checks
            boolean initialLit = peripheral.isLit();
            context.assertThat(() -> initialLit, "Expected light to be lit initially");

            int initialBrightness = peripheral.getBrightness();
            context.assertThat(() -> initialBrightness == 2, "Expected initial brightness stage to be 2 (MEDIUM)");

            // Change brightness to ULTRA (4)
            peripheral.setBrightness(4);
            context.assertThat(() -> peripheral.getBrightness() == 4, "Expected brightness stage to be 4 after setBrightness");

            // Change colour to BLUE
            peripheral.setColor("blue");
            context.assertThat(() -> "blue".equalsIgnoreCase(peripheral.getColor()), "Expected colour to be 'blue' after setColour");

            // Turn off light
            peripheral.setLit(LightRequest.OFF);
            context.assertThat(() -> !peripheral.isLit(), "Expected light to be unlit after setLit(OFF)");

            // Toggle light back on
            peripheral.setLit(LightRequest.ON);
            context.assertThat(peripheral::isLit, "Expected light to be lit after setLit(ON)");
        });

        context.execute();
    }

    /**
     * Tests that the LightingDirectorBlockEntity exposes the LightingDirector peripheral capability
     * and properly manages light addresses and queries.
     */
    @GameTest
    public static void testLightingDirectorPeripheralOperations(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle directorHandle = context.placeBlock("searchlight:lighting_director");
        BlockHandle lightHandle = context.placeBlock("searchlight:wall_light_iron");

        directorHandle.verifyCapability(PERIPHERAL_CAPABILITY, Direction.UP, peripheral -> {
            context.assertThat(() -> peripheral instanceof LightingDirectorPeripheral, "Expected LightingDirectorPeripheral capability");
            context.assertThat(() -> "lighting_director".equals(peripheral.getType()), "Expected type 'lighting_director'");
        });

        directorHandle.verifyBlockEntity(LightingDirectorBlockEntity.class, director -> {
            WallLightBlockEntity lightBe = lightHandle.getBlockEntity(WallLightBlockEntity.class);
            context.assertThat(() -> lightBe != null, "Expected WallLightBlockEntity at light position");

            if (lightBe instanceof AddressableLight addressable) {
                addressable.setAddress("test_light_1");
            }
            director.toggleLinkedLight(lightHandle.getAbsolutePos(), helper.getLevel());

            LightingDirectorPeripheral peripheral = new LightingDirectorPeripheral(director);
            // Verify getLinkedLights contains the address
            Map<String, Map<String, Object>> linkedLights = peripheral.getLinkedLights();
            context.assertThat(() -> linkedLights.containsKey("test_light_1"), "Expected registered light address 'test_light_1' in director peripheral");

            // Control light via Director peripheral
            peripheral.setLight("test_light_1", Map.of("brightness", 3, "colour", "red", "lit", false));

            WallLightBlockEntity updatedLightBe = lightHandle.getBlockEntity(WallLightBlockEntity.class);
            context.assertThat(() -> updatedLightBe != null && updatedLightBe.getBrightness() == BrightnessStage.HIGH,
                    "Expected light brightness to be updated to HIGH via director peripheral");

            context.assertThat(() -> !lightHandle.getBlockState().getValue(AbstractLightBlock.LIT),
                    "Expected light to be unlit via director peripheral");
        });

        context.execute();
    }

    /**
     * Tests that the LightingDirectorBlockEntity handles multiple lights with the same address correctly.
     */
    @GameTest
    public static void testLightingDirectorDuplicateAddresses(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle directorHandle = context.placeBlock("searchlight:lighting_director");
        BlockHandle light1Handle = context.placeBlock("searchlight:wall_light_iron");
        BlockHandle light2Handle = context.placeBlock("searchlight:wall_light_copper");

        directorHandle.verifyBlockEntity(LightingDirectorBlockEntity.class, director -> {
            WallLightBlockEntity light1Be = light1Handle.getBlockEntity(WallLightBlockEntity.class);
            WallLightBlockEntity light2Be = light2Handle.getBlockEntity(WallLightBlockEntity.class);
            context.assertThat(() -> light1Be != null && light2Be != null, "Expected WallLightBlockEntities at light positions");

            if (light1Be instanceof AddressableLight addressable1) {
                addressable1.setAddress("dup_light");
            }
            if (light2Be instanceof AddressableLight addressable2) {
                addressable2.setAddress("dup_light");
            }
            director.toggleLinkedLight(light1Handle.getAbsolutePos(), helper.getLevel());
            director.toggleLinkedLight(light2Handle.getAbsolutePos(), helper.getLevel());

            LightingDirectorPeripheral peripheral = new LightingDirectorPeripheral(director);
            // Verify getLinkedLights contains both addresses with suffixing
            Map<String, Map<String, Object>> linkedLights = peripheral.getLinkedLights();
            context.assertThat(() -> linkedLights.containsKey("dup_light") && linkedLights.containsKey("dup_light_2"), 
                    "Expected registered duplicate light addresses to be suffixed correctly (dup_light, dup_light_2)");

            // Control lights via Director peripheral using the shared address
            peripheral.setLight("dup_light", Map.of("brightness", 4, "lit", false));

            WallLightBlockEntity updatedLight1Be = light1Handle.getBlockEntity(WallLightBlockEntity.class);
            WallLightBlockEntity updatedLight2Be = light2Handle.getBlockEntity(WallLightBlockEntity.class);
            
            context.assertThat(() -> updatedLight1Be != null && updatedLight1Be.getBrightness() == BrightnessStage.ULTRA,
                    "Expected first light brightness to be updated to ULTRA");
            context.assertThat(() -> updatedLight2Be != null && updatedLight2Be.getBrightness() == BrightnessStage.ULTRA,
                    "Expected second light brightness to be updated to ULTRA");

            context.assertThat(() -> !light1Handle.getBlockState().getValue(AbstractLightBlock.LIT),
                    "Expected first light to be unlit");
            context.assertThat(() -> !light2Handle.getBlockState().getValue(AbstractLightBlock.LIT),
                    "Expected second light to be unlit");
        });

        context.execute();
    }

    /**
     * Tests that the LightingDirectorPeripheral can pixel-map connected Edge Lights,
     * batch updates across pixels, and automatically swap the physical block to the
     * average color variant for shaders.
     */
    @GameTest
    public static void testDirectorEdgeLightPixelMapping(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockPos directorPos = new BlockPos(3, 1, 1);
        BlockPos edgeLightPos = new BlockPos(1, 1, 1);

        BlockHandle directorHandle = context.placeBlock(directorPos, "searchlight:lighting_director");
        BlockHandle edgeLightHandle = context.placeBlock(edgeLightPos, "searchlight:edge_light_white");

        directorHandle.verifyBlockEntity(LightingDirectorBlockEntity.class, director -> {
            int slot = director.toggleLinkedLight(edgeLightHandle.getAbsolutePos(), helper.getLevel());
            context.assertThat(() -> slot == 1, "Expected edge light to be linked to slot 1");

            LightingDirectorPeripheral peripheral = new LightingDirectorPeripheral(director);

            // Query pixel count (should have 28 2x2 voxel pixels for a standalone square edge light: 8 + 7 + 7 + 6 = 28)
            int pixelCount = peripheral.getPixelCount(1);
            context.assertThat(() -> pixelCount == 28, "Expected standalone edge light to have 28 addressable 2x2 sub-pixels, got " + pixelCount);

            // Verify getPixels returns 28 pixels with sub_pixel index
            List<Map<String, Object>> pixels = peripheral.getPixels(1);
            context.assertThat(() -> pixels.size() == 28, "Expected getPixels to return 28 pixels");
            context.assertThat(() -> pixels.get(0).containsKey("sub_pixel"), "Expected pixels to contain sub_pixel index");

            // Set first 14 sub-pixels to red and remaining 14 to yellow (average color = orange)
            java.util.Map<Integer, String> halfAndHalf = new java.util.HashMap<>();
            for (int p = 1; p <= 14; p++) halfAndHalf.put(p, "red");
            for (int p = 15; p <= 28; p++) halfAndHalf.put(p, "yellow");
            peripheral.setPixels(1, halfAndHalf);

            // Verify the physical block was automatically replaced with edge_light_orange for shaders!
            BlockState updatedState = helper.getLevel().getBlockState(edgeLightHandle.getAbsolutePos());
            String updatedBlockId = BuiltInRegistries.BLOCK.getKey(updatedState.getBlock()).toString();
            context.assertThat(() -> "searchlight:edge_light_orange".equals(updatedBlockId),
                    "Expected block to be replaced with average color variant 'searchlight:edge_light_orange', but was: " + updatedBlockId);

            // Verify block entity retained its data
            WallLightBlockEntity updatedBe = (WallLightBlockEntity) helper.getLevel().getBlockEntity(edgeLightHandle.getAbsolutePos());
            context.assertThat(() -> updatedBe != null && updatedBe.hasEdgeLightData(), "Expected WallLightBlockEntity with EdgeLightData");

            // Test batching: set all 28 sub-pixels to blue via setPixelBatch
            java.util.Map<Integer, String> allBlue = new java.util.HashMap<>();
            for (int p = 1; p <= 28; p++) allBlue.put(p, "blue");
            peripheral.setPixelBatch(Map.of(1, allBlue));

            BlockState batchUpdatedState = helper.getLevel().getBlockState(edgeLightHandle.getAbsolutePos());
            String batchBlockId = BuiltInRegistries.BLOCK.getKey(batchUpdatedState.getBlock()).toString();
            context.assertThat(() -> "searchlight:edge_light_blue".equals(batchBlockId),
                    "Expected block to be replaced with 'searchlight:edge_light_blue', but was: " + batchBlockId);
        });

        context.execute();
    }
}
