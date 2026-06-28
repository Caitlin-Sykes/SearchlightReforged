package com.csykes.searchlight;

import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

@Mod(value = Searchlight.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Searchlight.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SearchlightClient {

    public SearchlightClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Searchlight.SEARCHLIGHT_BE.get(), SearchlightBlockRenderer::new);
    }

    @SubscribeEvent
    static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        for (net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block> blockHolder : Searchlight.CORNER_LIGHTS.values()) {
            event.register((state, world, pos, tintIndex) -> {

                if (tintIndex == 0) {
                    // Read directly from the block instance cast
                    if (state.getBlock() instanceof CornerLightBlock cornerBlock) {
                        return cornerBlock.getBlockColor().getTextureDiffuseColor();
                    }
                    return -1;
                }

                if (tintIndex == 1 && world != null && pos != null) {
                    net.minecraft.core.Direction facing = state.getValue(BlockStateProperties.FACING);
                    BlockPos targetPos = pos.relative(facing.getOpposite());
                    BlockState targetState = world.getBlockState(targetPos);

                    int color = event.getBlockColors().getColor(targetState, world, targetPos, 0);
                    if (color == -1) {
                        return targetState.getMapColor(world, targetPos).col;
                    }
                    return color;
                }

                return -1;
            }, blockHolder.get());
        }
    }

    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        // 1. Handle Wall Light Item Colors
        for (java.util.Map.Entry<String, net.neoforged.neoforge.registries.DeferredItem<? extends net.minecraft.world.item.Item>> entry : Searchlight.WALL_LIGHT_ITEMS.entrySet()) {
            net.minecraft.world.item.DyeColor color = net.minecraft.world.item.DyeColor.byName(entry.getKey(), DyeColor.PURPLE);
            int hexColor = (color != null) ? color.getTextureDiffuseColor() : -1;

            // FIX: Passing entry.getValue().get() extracts the raw Item instance required by the event register method signature
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    return hexColor;
                }
                return -1;
            }, entry.getValue().get());
        }

        // 2. Handle Corner Light Item Colors
        for (java.util.Map.Entry<String, net.neoforged.neoforge.registries.DeferredItem<? extends net.minecraft.world.item.Item>> entry : Searchlight.CORNER_LIGHTS_ITEMS.entrySet()) {
            net.minecraft.world.item.DyeColor color = net.minecraft.world.item.DyeColor.byName(entry.getKey(), DyeColor.PURPLE);
            int hexColor = (color != null) ? color.getTextureDiffuseColor() : -1;

            // FIX: Unpacking the corner light item out of the lazy DeferredItem map holder
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    return hexColor;
                }
                return -1;
            }, entry.getValue().get());
        }
    }

    public static void openLightAddressScreen(net.minecraft.core.BlockPos pos) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.csykes.searchlight.features.lighting_director.LightAddressScreen(pos));
    }

    @EventBusSubscriber(modid = Searchlight.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameClientEvents {
        @SubscribeEvent
        public static void onRenderLevelStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
            if (event.getStage() == net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.world.entity.player.Player player = mc.player;
                if (player == null) return;

                net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof com.csykes.searchlight.features.lighting_director.LightingLinkerCardItem)) {
                    stack = player.getOffhandItem();
                }
                if (!(stack.getItem() instanceof com.csykes.searchlight.features.lighting_director.LightingLinkerCardItem)) {
                    return;
                }

                net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                    if (tag.contains("director_x") && tag.contains("director_y") && tag.contains("director_z")) {
                        int dx = tag.getInt("director_x");
                        int dy = tag.getInt("director_y");
                        int dz = tag.getInt("director_z");
                        BlockPos directorPos = new BlockPos(dx, dy, dz);

                        String currentDim = player.level().dimension().location().toString();
                        String storedDim = tag.getString("director_dim");
                        if (currentDim.equals(storedDim)) {
                            renderDirectorHighlight(event, directorPos);
                        }
                    }
                }
            }
        }

        private static void renderDirectorHighlight(net.neoforged.neoforge.client.event.RenderLevelStageEvent event, BlockPos pos) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            Vec3 camPos = event.getCamera().getPosition();
            double x = pos.getX() - camPos.x;
            double y = pos.getY() - camPos.y;
            double z = pos.getZ() - camPos.z;

            com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(x, y, z);

            VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

            LevelRenderer.renderLineBox(
                poseStack,
                buffer,
                0, 0, 0,
                1, 1, 1,
                1.0F, 1.0F, 0.0F, 1.0F
            );

            poseStack.popPose();
            mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        }
    }
}