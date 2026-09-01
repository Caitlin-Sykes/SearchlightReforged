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
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.gametest.GameTestHolder;

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
}
