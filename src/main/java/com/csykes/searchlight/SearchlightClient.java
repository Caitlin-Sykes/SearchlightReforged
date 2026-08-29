package com.csykes.searchlight;

import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.colour_lamp.ColourLampBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.lighting_director.LightAddressScreen;
import com.csykes.searchlight.features.lighting_director.LightingLinkerCardItem;
import com.csykes.searchlight.features.searchlight.SearchlightBlockRenderer;
import com.csykes.searchlight.integration.dyenamics.DyenamicHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map.Entry;

import static net.minecraft.world.item.DyeColor.byName;

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
        for (DeferredBlock<Block> blockHolder : Searchlight.WALL_LIGHTS.values()) {
            event.register((state, world, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    String name = Searchlight.WALL_LIGHTS.entrySet().stream()
                            .filter(e -> e.getValue().get() == state.getBlock())
                            .map(Entry::getKey)
                            .findFirst()
                            .orElse("white");

                    DyeColor standard = byName(name, null);
                    if (standard != null) return standard.getTextureDiffuseColor();

                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(name);
                    }
                }
                return -1;
            }, blockHolder.get());
        }

        for (DeferredBlock<Block> blockHolder : Searchlight.CORNER_LIGHTS.values()) {
            event.register((state, world, pos, tintIndex) -> {

                if (tintIndex == 0) {
                    // Read directly from the block instance cast
                    if (state.getBlock() instanceof CornerLightBlock cornerBlock) {
                        if (cornerBlock.getBlockColor() != null) {
                            return cornerBlock.getBlockColor().getTextureDiffuseColor();
                        }
                        if (ModList.get().isLoaded("dyenamics") && cornerBlock.getDyenamicColor() != null) {
                            return DyenamicHelper.getDyenamicColor(cornerBlock.getDyenamicColor());
                        }
                    }
                    return -1;
                }

                if (tintIndex == 1 && world != null && pos != null) {
                    Direction facing = state.getValue(BlockStateProperties.FACING);
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
        for (DeferredBlock<Block> centreBlockHolder : Searchlight.CENTRE_LIGHTS.values()) {
            event.register((state, world, pos, tintIndex) -> {

                if (tintIndex == 0) {
                    // Read directly from the block instance cast
                    if (state.getBlock() instanceof CentreLightBlock centreLightBlock) {
                        if (centreLightBlock.getBlockColor() != null) {
                            return centreLightBlock.getBlockColor().getTextureDiffuseColor();
                        }
                        if (ModList.get().isLoaded("dyenamics") && centreLightBlock.getDyenamicColor() != null) {
                            return DyenamicHelper.getDyenamicColor(centreLightBlock.getDyenamicColor());
                        }
                    }
                    return -1;
                }

                if (tintIndex == 1 && world != null && pos != null) {
                    Direction facing = state.getValue(BlockStateProperties.FACING);
                    BlockPos targetPos = pos.relative(facing.getOpposite());
                    BlockState targetState = world.getBlockState(targetPos);

                    int color = event.getBlockColors().getColor(targetState, world, targetPos, 0);
                    if (color == -1) {
                        return targetState.getMapColor(world, targetPos).col;
                    }
                    return color;
                }

                return -1;
            }, centreBlockHolder.get());
        }

        for (DeferredBlock<Block> colourLampHolder : Searchlight.COLOUR_LAMPS.values()) {
            event.register((state, world, pos, tintIndex) -> {
                if (tintIndex == 0) {
                    if (state.getBlock() instanceof ColourLampBlock colourLampBlock) {
                        if (colourLampBlock.getBlockColor() != null) {
                            return colourLampBlock.getBlockColor().getTextureDiffuseColor();
                        }
                        if (ModList.get().isLoaded("dyenamics") && colourLampBlock.getDyenamicColor() != null) {
                            return DyenamicHelper.getDyenamicColor(colourLampBlock.getDyenamicColor());
                        }
                    }
                }
                return -1;
            }, colourLampHolder.get());
        }
        for (DeferredBlock<Block> edgeBlockHolder : Searchlight.EDGE_LIGHTS.values()) {
            event.register((state, world, pos, tintIndex) -> {

                if (tintIndex == 0) {
                    // Read directly from the block instance cast
                    if (state.getBlock() instanceof EdgeLightBlock edgeLightBlock) {
                        if (edgeLightBlock.getBlockColor() != null) {
                            return edgeLightBlock.getBlockColor().getTextureDiffuseColor();
                        }
                        if (ModList.get().isLoaded("dyenamics") && edgeLightBlock.getDyenamicColor() != null) {
                            return DyenamicHelper.getDyenamicColor(edgeLightBlock.getDyenamicColor());
                        }
                    }
                    return -1;
                }

                if (tintIndex == 1 && world != null && pos != null) {
                    Direction facing = state.getValue(BlockStateProperties.FACING);
                    BlockPos targetPos = pos.relative(facing.getOpposite());
                    BlockState targetState = world.getBlockState(targetPos);

                    int color = event.getBlockColors().getColor(targetState, world, targetPos, 0);
                    if (color == -1) {
                        return targetState.getMapColor(world, targetPos).col;
                    }
                    return color;
                }

                return -1;
            }, edgeBlockHolder.get());
        }

        for (DeferredBlock<Block> blockHolder : Searchlight.SEARCHLIGHTS.values()) {
            event.register((state, world, pos, tintIndex) -> {

                if (tintIndex == 0) {
                    if (state.getBlock() instanceof com.csykes.searchlight.features.searchlight.SearchlightBlock searchlightBlock) {
                        if (searchlightBlock.getBlockColor() != null) {
                            return searchlightBlock.getBlockColor().getTextureDiffuseColor();
                        }
                        if (ModList.get().isLoaded("dyenamics") && searchlightBlock.getDyenamicColor() != null) {
                            return DyenamicHelper.getDyenamicColor(searchlightBlock.getDyenamicColor());
                        }
                    }
                    return -1;
                }

                if (tintIndex == 1 && world != null && pos != null) {
                    Direction facing = state.getValue(BlockStateProperties.FACING);
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
        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.WALL_LIGHT_ITEMS.entrySet()) {
            String colorName = entry.getKey();

            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }

        // 2. Handle Corner Light Item Colors
        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.CORNER_LIGHTS_ITEMS.entrySet()) {
            String colorName = entry.getKey();
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }


        // 2. Handle Colour Lamp Light Item Colors
        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.COLOUR_LAMP_ITEMS.entrySet()) {
            String colorName = entry.getKey();
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }


        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.CENTRE_LIGHTS_ITEMS.entrySet()) {
            String colorName = entry.getKey();
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }

        // 2. Handle Edge Light Item Colors
        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.EDGE_LIGHTS_ITEMS.entrySet()) {
            String colorName = entry.getKey();
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }

        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.SEARCHLIGHT_ITEMS.entrySet()) {
            String colorName = entry.getKey();
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }
        for (Entry<String, DeferredItem<? extends Item>> entry : Searchlight.COLOUR_SLAB_ITEMS.entrySet()) {
            String colorName = entry.getKey();
            event.register((stack, tintIndex) -> {
                if (tintIndex == 0) {
                    DyeColor color = byName(colorName, null);
                    if (color != null) return color.getTextureDiffuseColor();
                    if (ModList.get().isLoaded("dyenamics")) {
                        return DyenamicHelper.getDyenamicColor(colorName);
                    }
                }
                return -1;
            }, entry.getValue().get());
        }
    }

    public static void openLightAddressScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new LightAddressScreen(pos));
    }

    public static boolean displayBeams() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (player.isHolding(Searchlight.SEARCHLIGHT_ITEM.get())) return true;
        for (DeferredItem<? extends Item> item : Searchlight.SEARCHLIGHT_ITEMS.values()) {
            if (player.isHolding(item.get())) return true;
        }
        return false;
    }

    @EventBusSubscriber(modid = Searchlight.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
    public static class GameClientEvents {
        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                if (player == null) return;

                ItemStack stack = player.getMainHandItem();
                if (!(stack.getItem() instanceof LightingLinkerCardItem)) {
                    stack = player.getOffhandItem();
                }
                if (!(stack.getItem() instanceof LightingLinkerCardItem)) {
                    return;
                }

                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
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

        private static void renderDirectorHighlight(RenderLevelStageEvent event, BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            Vec3 camPos = event.getCamera().getPosition();
            double x = pos.getX() - camPos.x;
            double y = pos.getY() - camPos.y;
            double z = pos.getZ() - camPos.z;

            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            poseStack.translate(x, y, z);

            VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

            LevelRenderer.renderLineBox(poseStack, buffer, 0, 0, 0, 1, 1, 1, 1.0F, 1.0F, 0.0F, 1.0F);

            poseStack.popPose();
            mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        }
    }
}