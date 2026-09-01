package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.colour_lamp.ColourLampBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.utils.SearchlightUtil;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.LightRequest;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class LightPeripheral implements IPeripheral {
    private final BlockEntity tile;
    private final String type;

    public LightPeripheral(BlockEntity tile, String type) {
        this.tile = tile;
        this.type = type;
    }

    @NotNull
    @Override
    public String getType() {
        return this.type;
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
        Block block = state.getBlock();
        BrightnessStage stage = BrightnessStage.fromId(Math.clamp(level, 0, 4));

        if (block instanceof CornerLightBlock) {
            for (BlockPos connectedPos : SearchlightUtil.getConnectedCornerLights(world, pos, state)) {
                BlockEntity be = world.getBlockEntity(connectedPos);
                if (be instanceof AddressableLight light) {
                    light.setBrightness(stage);
                    be.setChanged();
                    BlockState s = world.getBlockState(connectedPos);
                    world.sendBlockUpdated(connectedPos, s, s, 3);
                    world.getLightEngine().checkBlock(connectedPos);
                }
            }
        } else {
            if (tile instanceof AddressableLight light) {
                light.setBrightness(stage);
                tile.setChanged();
                world.sendBlockUpdated(pos, state, state, 3);
                world.getLightEngine().checkBlock(pos);
                if (tile instanceof SearchlightBlockEntity searchlight && searchlight.getLightSourcePos() != null) {
                    world.getLightEngine().checkBlock(searchlight.getLightSourcePos());
                }
            }
        }
    }

    @LuaFunction(mainThread = true)
    public final int getBrightness() {
        if (tile instanceof AddressableLight light) {
            return light.getBrightness().getId();
        }
        return 0;
    }

    @LuaFunction(mainThread = true)
    public final void setLit(LightRequest lit) {
        Level world = tile.getLevel();
        BlockPos pos = tile.getBlockPos();
        if (world == null) return;

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof CornerLightBlock cornerBlock) {
            for (BlockPos connectedPos : SearchlightUtil.getConnectedCornerLights(world, pos, state)) {
                BlockEntity be = world.getBlockEntity(connectedPos);
                if (be instanceof AddressableLight light) {
                    light.setLightRequest(lit);
                    be.setChanged();
                    BlockState s = world.getBlockState(connectedPos);
                    world.sendBlockUpdated(connectedPos, s, s, 3);
                    cornerBlock.updateLitState(world, connectedPos, s);
                }
            }
        } else if (block instanceof AbstractLightBlock abstractLightBlock) {
            if (tile instanceof AddressableLight light) {
                light.setLightRequest(lit);
                tile.setChanged();
                world.sendBlockUpdated(pos, state, state, 3);
                abstractLightBlock.updateLitState(world, pos, state);
            }
        }
    }

    @LuaFunction(mainThread = true)
    public final boolean isLit() {
        BlockState state = tile.getBlockState();
        if (state.hasProperty(AbstractLightBlock.LIT)) {
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

        Block newBlock = null;

        if (block instanceof WallLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.WALL_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof CornerLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.CORNER_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof EdgeLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.EDGE_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof CentreLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.CENTRE_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof ColourLampBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.COLOUR_LAMPS.get(normalizedColor);
            if (newBlockHolder != null) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof SearchlightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.SEARCHLIGHTS.get(normalizedColor);
            if (newBlockHolder != null) {
                newBlock = newBlockHolder.get();
            }
        }

        if (newBlock != null) {
            if (block instanceof CornerLightBlock) {
                List<BlockPos> connected = SearchlightUtil.getConnectedCornerLights(world, pos, state);
                for (BlockPos connectedPos : connected) {
                    BlockState s = world.getBlockState(connectedPos);
                    BlockState ns = copyMatchingProperties(s, newBlock.defaultBlockState());
                    world.setBlockAndUpdate(connectedPos, ns);
                    world.updateNeighborsAt(connectedPos, newBlock);
                }
            } else {
                BlockState ns = copyMatchingProperties(state, newBlock.defaultBlockState());
                world.setBlockAndUpdate(pos, ns);
                world.updateNeighborsAt(pos, newBlock);
            }
            return true;
        }

        return false;
    }

    @LuaFunction(mainThread = true)
    public final String getColor() {
        BlockState state = tile.getBlockState();
        Block block = state.getBlock();

        if (block instanceof SearchlightBlock) {
            if (tile instanceof SearchlightBlockEntity searchlightBe) {
                return searchlightBe.getColor().getName();
            }
        }

        if (block instanceof CornerLightBlock cornerBlock) {
            return cornerBlock.getBlockColor().getName();
        }
        if (block instanceof EdgeLightBlock edgeBlock) {
            return edgeBlock.getBlockColor().getName();
        }
        if (block instanceof CentreLightBlock centreBlock) {
            return centreBlock.getBlockColor().getName();
        }
        if (block instanceof ColourLampBlock colourLampBlock) {
            return colourLampBlock.getBlockColor().getName();
        }

        for (Map.Entry<String, DeferredBlock<Block>> entry : Searchlight.WALL_LIGHTS.entrySet()) {
            if (entry.getValue().get() == block) {
                return entry.getKey();
            }
        }

        return "unknown";
    }

    @SuppressWarnings("unchecked")
    private static BlockState copyMatchingProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property<?> property : from.getProperties()) {
            if (result.hasProperty(property)) {
                result = copyProperty(from, result, (Property) property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}