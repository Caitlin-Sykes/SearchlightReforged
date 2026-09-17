package com.csykes.searchlight.features.integration.jade;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.integration.jade.LightDataComponent;
import com.mat.api.BlockHandle;
import com.mat.api.TestContext;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import snownee.jade.api.BlockAccessor;

import java.lang.reflect.Proxy;

@GameTestHolder(Searchlight.MODID)
public class JadeIntegrationTest {

    /**
     * Tests that LightDataComponent appends correct server NBT data (colour, brightness, mode)
     * from light block states into Jade BlockAccessor data tags.
     */
    @GameTest
    public static void testJadeServerDataAppending(GameTestHelper helper) {
        TestContext context = new TestContext(helper);

        BlockHandle handle = context.placeBlock("searchlight:searchlight_white");

        handle.verifyBlockEntity(BlockEntity.class, be -> {
            BlockAccessor accessor = (BlockAccessor) Proxy.newProxyInstance(
                    BlockAccessor.class.getClassLoader(),
                    new Class<?>[]{BlockAccessor.class},
                    (proxy, method, args) -> {
                        if ("getBlockEntity".equals(method.getName())) {
                            return be;
                        }
                        if ("getBlockState".equals(method.getName())) {
                            return be.getBlockState();
                        }
                        if ("getPosition".equals(method.getName())) {
                            return be.getBlockPos();
                        }
                        if ("getLevel".equals(method.getName())) {
                            return be.getLevel();
                        }
                        return null;
                    }
            );

            CompoundTag data = new CompoundTag();
            LightDataComponent.INSTANCE.appendServerData(data, accessor);

            context.assertThat(
                    () -> data.contains(LightDataComponent.SERVERDATA_COLOR),
                    "Expected Jade server data to contain 'colour'"
            );
            context.assertThat(
                    () -> "white".equalsIgnoreCase(data.getString(LightDataComponent.SERVERDATA_COLOR)),
                    "Expected Jade server data colour to be 'white'"
            );
            context.assertThat(
                    () -> data.contains(LightDataComponent.SERVERDATA_BRIGHTNESS),
                    "Expected Jade server data to contain 'brightness'"
            );
            context.assertThat(
                    () -> "medium".equalsIgnoreCase(data.getString(LightDataComponent.SERVERDATA_BRIGHTNESS)),
                    "Expected Jade server data brightness to be 'medium'"
            );
            context.assertThat(
                    () -> data.contains(LightDataComponent.SERVERDATA_MODE),
                    "Expected Jade server data to contain 'mode'"
            );
        });

        context.execute();
    }
}
