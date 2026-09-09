package com.csykes.searchlight.utils;

import com.csykes.searchlight.Searchlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SearchlightUtil {
    public static BrightnessStage getBrightness(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("brightness")) {
                try {
                    return BrightnessStage.valueOf(tag.getString("brightness"));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return BrightnessStage.MEDIUM;
    }

    public static void setBrightness(ItemStack stack, BrightnessStage stage) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof AbstractLightBlock lightBlock) {
            BlockEntityType<?> beType = lightBlock.getBlockEntityType();
            CustomData.update(DataComponents.BLOCK_ENTITY_DATA, stack, tag -> {
                tag.putString("brightness", stage.name());
                if (beType != null) {
                    ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(beType);
                    if (id != null) {
                        tag.putString("id", id.toString());
                    }
                }
            });
        }
    }

    public static ItemStack createCreativeTabStack(Item item, BrightnessStage stage) {
        ItemStack stack = new ItemStack(item);
        setBrightness(stack, stage);
        return stack;
    }

    public static <T extends BlockEntity> boolean castBlockEntity(@Nullable BlockEntity blockEntity, @NotNull BlockPos blockPos, @NotNull Class<T> expectedClass, @NotNull Consumer<T> result) {
        if (blockEntity == null) {
            return false;
        }
        if (!blockEntity.hasLevel()) {
            return false;
        }
        if (expectedClass.isInstance(blockEntity)) {
            result.accept(expectedClass.cast(blockEntity));
            return true;
        } else {
            Searchlight.LOGGER.error("Attempted to cast '{}' ({}) at {} to {} but failed", blockEntity, blockEntity.getClass(), blockPos, expectedClass);
            return false;
        }
    }

    public static <T extends BlockEntity> boolean castBlockEntity(@Nullable BlockEntity blockEntity, @NotNull BlockPos blockPos, @NotNull Consumer<T> result) {
        if (blockEntity == null) {
            return false;
        }
        if (!blockEntity.hasLevel()) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            T casted = (T) blockEntity;
            result.accept(casted);
            return true;
        } catch (ClassCastException ex) {
            Searchlight.LOGGER.error("Attempted to cast '{}' ({}) at {} but failed", blockEntity, blockEntity.getClass(), blockPos, ex);
            return false;
        }
    }

    public static @NotNull BlockState getBlockStateForceLoad(@NotNull Level world, @NotNull BlockPos blockPos) {
        return world.getBlockState(blockPos);
    }

    public static @NotNull BlockState getBlockStateIfLoaded(Level world, BlockPos blockPos) {
        if (!world.isInWorldBounds(blockPos))
            return Blocks.VOID_AIR.defaultBlockState();
        if (!world.isLoaded(blockPos))
            return Blocks.VOID_AIR.defaultBlockState();
        return world.getBlockState(blockPos);
    }

    public static Direction getDirection(BlockState state) {
        AttachFace face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        if (face == AttachFace.CEILING)
            return Direction.DOWN;
        else if (face == AttachFace.FLOOR)
            return Direction.UP;
        return state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
    }

    public static @NotNull Vec3 directionToBeamVector(@NotNull Direction direction) {
        return Vec3.atLowerCornerOf(direction.getNormal());
    }

    public static BlockPos moveAwayFromSurfaces(Level world, BlockPos blockPos) {
        if (blockPos == null)
            return null;
        BlockPos resultPos = blockPos.immutable();

        if (!world.getBlockState(resultPos.relative(Direction.WEST)).isAir() && world.getBlockState(resultPos.relative(Direction.EAST)).isAir())
            resultPos = resultPos.relative(Direction.EAST);
        else if (!world.getBlockState(resultPos.relative(Direction.EAST)).isAir() && world.getBlockState(resultPos.relative(Direction.WEST)).isAir())
            resultPos = resultPos.relative(Direction.WEST);

        if (!world.getBlockState(resultPos.relative(Direction.DOWN)).isAir() && world.getBlockState(resultPos.relative(Direction.UP)).isAir())
            resultPos = resultPos.relative(Direction.UP);
        else if (!world.getBlockState(resultPos.relative(Direction.UP)).isAir() && world.getBlockState(resultPos.relative(Direction.DOWN)).isAir())
            resultPos = resultPos.relative(Direction.DOWN);

        if (!world.getBlockState(resultPos.relative(Direction.NORTH)).isAir() && world.getBlockState(resultPos.relative(Direction.SOUTH)).isAir())
            resultPos = resultPos.relative(Direction.SOUTH);
        else if (!world.getBlockState(resultPos.relative(Direction.SOUTH)).isAir() && world.getBlockState(resultPos.relative(Direction.NORTH)).isAir())
            resultPos = resultPos.relative(Direction.NORTH);

        return resultPos;
    }

    public static List<BlockPos> getConnectedCornerLights(Level level, BlockPos startPos, BlockState startState) {
        if (startState.getBlock() instanceof AbstractLightBlock alb) {
            return alb.getConnectedLights(level, startPos, startState);
        }
        List<BlockPos> list = new ArrayList<>();
        list.add(startPos);
        return list;
    }
}