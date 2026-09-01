package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.colour_lamp.ColourLampBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LightingDirectorPeripheral implements IPeripheral {
    private final LightingDirectorBlockEntity tile;

    public LightingDirectorPeripheral(BlockEntity tile) {
        this.tile = (LightingDirectorBlockEntity) tile;
    }

    @NotNull
    @Override
    public String getType() {
        return "lighting_director";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other || (other instanceof LightingDirectorPeripheral o && o.tile == tile);
    }

    private String getLightColorName(BlockState state) {
        Block block = state.getBlock();
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

    private LightRequest parseLightRequest(Object value) {
        if (value instanceof Boolean boolVal) {
            return boolVal ? LightRequest.ON : LightRequest.OFF;
        }
        if (value instanceof String strVal) {
            try {
                return LightRequest.valueOf(strVal.toUpperCase());
            } catch (IllegalArgumentException e) {
                // fall back to case insensitive matching
                for (LightRequest req : LightRequest.values()) {
                    if (req.name().equalsIgnoreCase(strVal)) {
                        return req;
                    }
                }
            }
        }
        return LightRequest.RELEASE;
    }

    private BrightnessStage parseBrightness(Object value) {
        if (value instanceof Number numVal) {
            return BrightnessStage.fromId(Math.clamp(numVal.intValue(), 0, 4));
        }
        if (value instanceof String strVal) {
            try {
                return BrightnessStage.valueOf(strVal.toUpperCase());
            } catch (IllegalArgumentException e) {
                for (BrightnessStage stage : BrightnessStage.values()) {
                    if (stage.getName().equalsIgnoreCase(strVal)) {
                        return stage;
                    }
                }
            }
        }
        return null;
    }

    private BlockState updateColorProperty(Level world, BlockPos pos, BlockState state, String colorName) {
        Block block = state.getBlock();
        String normalizedColor = colorName.toLowerCase();

        Block newBlock = null;
        if (block instanceof WallLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.WALL_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof CornerLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.CORNER_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof EdgeLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.EDGE_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof CentreLightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.CENTRE_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof ColourLampBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.COLOUR_LAMPS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                newBlock = newBlockHolder.get();
            }
        } else if (block instanceof SearchlightBlock) {
            DeferredBlock<Block> newBlockHolder = Searchlight.SEARCHLIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                newBlock = newBlockHolder.get();
            }
        }

        if (newBlock != null) {
            BlockState newState = copyMatchingProperties(state, newBlock.defaultBlockState());

            String oldAddress = "";
            BlockEntity oldBe = world.getBlockEntity(pos);
            if (oldBe instanceof AddressableLight addressable) {
                oldAddress = addressable.getAddress();
            }

            world.setBlockAndUpdate(pos, newState);
            world.updateNeighborsAt(pos, newBlock);

            BlockEntity newBe = world.getBlockEntity(pos);
            if (newBe instanceof AddressableLight addressable) {
                addressable.setAddress(oldAddress);
                newBe.setChanged();
                world.sendBlockUpdated(pos, newState, newState, 3);
            }

            return newState;
        }

        return state;
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

    private void applyLightUpdates(Level world, BlockPos pos, Map<?, ?> options) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof AbstractLightBlock)) return;

        BlockState updatedState = state;

        if (options.containsKey("color") && options.get("color") instanceof String colorName) {
            updatedState = updateColorProperty(world, pos, updatedState, colorName);
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof AddressableLight light) {
            if (options.containsKey("brightness")) {
                BrightnessStage stage = parseBrightness(options.get("brightness"));
                if (stage != null) {
                    light.setBrightness(stage);
                    be.setChanged();
                }
            }

            boolean hasLitRequest = false;
            if (options.containsKey("lit")) {
                LightRequest request = parseLightRequest(options.get("lit"));
                light.setLightRequest(request);
                hasLitRequest = true;
                be.setChanged();
            }

            world.sendBlockUpdated(pos, updatedState, updatedState, 3);
            world.getLightEngine().checkBlock(pos);
            if (be instanceof SearchlightBlockEntity searchlight && searchlight.getLightSourcePos() != null) {
                world.getLightEngine().checkBlock(searchlight.getLightSourcePos());
            }

            if (hasLitRequest && updatedState.getBlock() instanceof AbstractLightBlock abstractLightBlock) {
                abstractLightBlock.updateLitState(world, pos, updatedState);
            }
        }
    }

    private void processLightUpdate(Level world, BlockPos pos, Map<?, ?> options) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof AbstractLightBlock)) return;

        List<BlockPos> targets = new ArrayList<>();
        if (block instanceof CornerLightBlock) {
            targets.addAll(SearchlightUtil.getConnectedCornerLights(world, pos, state));
        } else {
            targets.add(pos);
        }

        for (BlockPos targetPos : targets) {
            applyLightUpdates(world, targetPos, options);
        }
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Map<String, Object>> getLinkedLights() {
        Level world = tile.getLevel();
        Map<String, Map<String, Object>> result = new HashMap<>();
        if (world == null) return result;

        List<BlockPos> positions = tile.getLinkedLights();
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            if (pos == null) continue;

            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            Map<String, Object> lightInfo = new HashMap<>();
            lightInfo.put("index", i + 1); // 1-based Lua index
            lightInfo.put("x", pos.getX());
            lightInfo.put("y", pos.getY());
            lightInfo.put("z", pos.getZ());

            if (block instanceof AbstractLightBlock) {
                lightInfo.put("active", true);
                lightInfo.put("type", block.getClass().getSimpleName());
                lightInfo.put("lit", state.getValue(AbstractLightBlock.LIT));

                AddressableLight addressable = (world.getBlockEntity(pos) instanceof AddressableLight a) ? a : null;
                String lightRequestName = addressable != null ? addressable.getLightRequest().name().toLowerCase() : "release";
                String brightnessName = addressable != null ? addressable.getBrightness().name().toLowerCase() : "medium";
                String address = addressable != null ? addressable.getAddress() : "";

                lightInfo.put("light_request", lightRequestName);
                lightInfo.put("brightness", brightnessName);
                lightInfo.put("color", getLightColorName(state));

                if (address.isEmpty()) {
                    address = "light_" + (i + 1);
                }
                lightInfo.put("address", address);
                result.put(address, lightInfo);
            } else {
                lightInfo.put("active", false);
                lightInfo.put("type", "broken");
                result.put("broken_" + (i + 1), lightInfo);
            }
        }
        return result;
    }

    @LuaFunction(mainThread = true)
    public final boolean setLight(Object key, Map<?, ?> options) {
        Level world = tile.getLevel();
        if (world == null) return false;

        BlockPos targetPos = null;

        if (key instanceof Number numVal) {
            int index = numVal.intValue() - 1;
            List<BlockPos> positions = tile.getLinkedLights();
            if (index >= 0 && index < positions.size()) {
                targetPos = positions.get(index);
            }
        } else if (key instanceof String addressVal) {
            List<BlockPos> positions = tile.getLinkedLights();
            for (BlockPos pos : positions) {
                if (pos != null) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof AddressableLight addressable && addressable.getAddress().equalsIgnoreCase(addressVal)) {
                        targetPos = pos;
                        break;
                    }
                }
            }
        }

        if (targetPos == null) return false;
        processLightUpdate(world, targetPos, options);
        return true;
    }

    @LuaFunction(mainThread = true)
    public final boolean setLights(Map<?, ?> bulkOptions) {
        Level world = tile.getLevel();
        if (world == null) return false;

        List<BlockPos> positions = tile.getLinkedLights();

        for (Map.Entry<?, ?> entry : bulkOptions.entrySet()) {
            Object key = entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> options)) {
                continue;
            }

            BlockPos targetPos = null;

            if (key instanceof Number numVal) {
                int index = numVal.intValue() - 1;
                if (index >= 0 && index < positions.size()) {
                    targetPos = positions.get(index);
                }
            } else if (key instanceof String addressVal) {
                for (BlockPos pos : positions) {
                    if (pos != null) {
                        BlockEntity be = world.getBlockEntity(pos);
                        if (be instanceof AddressableLight addressable && addressable.getAddress().equalsIgnoreCase(addressVal)) {
                            targetPos = pos;
                            break;
                        }
                    }
                }
            }

            if (targetPos != null) {
                processLightUpdate(world, targetPos, options);
            }
        }
        return true;
    }

    @LuaFunction(mainThread = true)
    public final boolean removeLight(Object key) {
        if (key instanceof Number numVal) {
            return tile.removeLinkedLight(numVal.intValue() - 1);
        } else if (key instanceof String addressVal) {
            Level world = tile.getLevel();
            if (world != null) {
                List<BlockPos> positions = tile.getLinkedLights();
                for (int i = 0; i < positions.size(); i++) {
                    BlockPos pos = positions.get(i);
                    if (pos != null) {
                        BlockEntity be = world.getBlockEntity(pos);
                        if (be instanceof AddressableLight addressable && addressable.getAddress().equalsIgnoreCase(addressVal)) {
                            return tile.removeLinkedLight(i);
                        }
                    }
                }
            }
        }
        return false;
    }

    @LuaFunction(mainThread = true)
    public final void clearLights() {
        tile.clearLinkedLights();
    }
}
