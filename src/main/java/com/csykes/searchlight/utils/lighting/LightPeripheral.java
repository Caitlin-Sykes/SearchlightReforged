package com.csykes.searchlight.utils.lighting;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightPeripheral implements IPeripheral {
    private final BlockEntity tile;

    public LightPeripheral(BlockEntity tile) {
        this.tile = tile;
    }

    @NotNull
    @Override
    public String getType() {
        return "searchlight";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other || (other instanceof LightPeripheral o && o.tile == tile);
    }

    @LuaFunction(mainThread = true)
    public final void setBrightness(int level) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof AbstractLightBlock block) {
            BrightnessStage stage = BrightnessStage.fromId(Math.max(0, Math.min(4, level)));
            world.setBlock(pos, state.setValue(AbstractLightBlock.BRIGHTNESS, stage), 3);
            world.updateNeighborsAt(pos, block);
        }
    }

    @LuaFunction(mainThread = true)
    public final int getBrightness() {
        BlockState state = tile.getBlockState();
        if (state.getBlock() instanceof AbstractLightBlock) {
            return state.getValue(AbstractLightBlock.BRIGHTNESS).getId();
        }
        return 0;
    }

    @LuaFunction(mainThread = true)
    public final void setLit(boolean lit) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof AbstractLightBlock block) {
            world.setBlock(pos, state.setValue(AbstractLightBlock.LIT, lit), 3);
            world.updateNeighborsAt(pos, block);
        }
    }

    @LuaFunction(mainThread = true)
    public final boolean isLit() {
        BlockState state = tile.getBlockState();
        if (state.getBlock() instanceof AbstractLightBlock) {
            return state.getValue(AbstractLightBlock.LIT);
        }
        return false;
    }

    @LuaFunction(mainThread = true)
    public final boolean setColor(String colorName) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null || world.isClientSide) return false;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        String normalizedColor = colorName.toLowerCase();

        // 2. Handle Corner Lights
        if (block instanceof CornerLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.CORNER_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                Block newBlock = newBlockHolder.get();
                BlockState newState = newBlock.defaultBlockState();
                
                // Copy over all matching block properties
                if (state.hasProperty(CornerLightBlock.CORNER)) newState = newState.setValue(CornerLightBlock.CORNER, state.getValue(CornerLightBlock.CORNER));
                if (state.hasProperty(CornerLightBlock.CONNECTION)) newState = newState.setValue(CornerLightBlock.CONNECTION, state.getValue(CornerLightBlock.CONNECTION));
                if (state.hasProperty(CornerLightBlock.LIT)) newState = newState.setValue(CornerLightBlock.LIT, state.getValue(CornerLightBlock.LIT));
                if (state.hasProperty(CornerLightBlock.BRIGHTNESS)) newState = newState.setValue(CornerLightBlock.BRIGHTNESS, state.getValue(CornerLightBlock.BRIGHTNESS));

                world.setBlock(pos, newState, 3);
                world.updateNeighborsAt(pos, newBlock);
                return true;
            }
        }

        return false;
    }

    @LuaFunction(mainThread = true)
    public final String getColor() {
        BlockState state = tile.getBlockState();
        Block block = state.getBlock();

        if (block instanceof CornerLightBlock cornerBlock) {
            return cornerBlock.getBlockColor().getName();
        }

        for (java.util.Map.Entry<String, DeferredBlock<Block>> entry : Searchlight.WALL_LIGHTS.entrySet()) {
            if (entry.getValue().get() == block) {
                return entry.getKey();
            }
        }

        return "unknown";
    }
}