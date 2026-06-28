package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.wall_light.WallLightBlock;
import com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.CornerLightStage;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.LightRequest;
import com.csykes.searchlight.utils.lighting.LightRodConnection;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

    private List<BlockPos> getConnectedCornerLights(Level world, BlockPos startPos, BlockState startState) {
        List<BlockPos> positions = new ArrayList<>();
        if (!(startState.getBlock() instanceof CornerLightBlock)) {
            positions.add(startPos);
            return positions;
        }

        CornerLightStage targetCorner = startState.getValue(CornerLightBlock.CORNER);
        positions.add(startPos);

        // Traverse UP
        BlockPos current = startPos.above();
        while (true) {
            BlockState state = world.getBlockState(current);
            if (state.getBlock() instanceof CornerLightBlock && state.getValue(CornerLightBlock.CORNER) == targetCorner) {
                positions.add(current);
                current = current.above();
            } else {
                break;
            }
        }

        // Traverse DOWN
        current = startPos.below();
        while (true) {
            BlockState state = world.getBlockState(current);
            if (state.getBlock() instanceof CornerLightBlock && state.getValue(CornerLightBlock.CORNER) == targetCorner) {
                positions.add(current);
                current = current.below();
            } else {
                break;
            }
        }

        return positions;
    }

    private String getLightColorName(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CornerLightBlock cornerBlock) {
            return cornerBlock.getBlockColor().getName();
        }
        for (Map.Entry<String, net.neoforged.neoforge.registries.DeferredBlock<Block>> entry : Searchlight.WALL_LIGHTS.entrySet()) {
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

        if (block instanceof WallLightBlock) {
            net.neoforged.neoforge.registries.DeferredBlock<Block> newBlockHolder = Searchlight.WALL_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                Block newBlock = newBlockHolder.get();
                BlockState newState = newBlock.defaultBlockState();

                String oldAddress = "";
                BlockEntity oldBe = world.getBlockEntity(pos);
                if (oldBe instanceof AddressableLight addressable) {
                    oldAddress = addressable.getAddress();
                }

                if (state.hasProperty(WallLightBlock.FACING))
                    newState = newState.setValue(WallLightBlock.FACING, state.getValue(WallLightBlock.FACING));
                if (state.hasProperty(WallLightBlock.FACE))
                    newState = newState.setValue(WallLightBlock.FACE, state.getValue(WallLightBlock.FACE));
                if (state.hasProperty(WallLightBlock.LIT))
                    newState = newState.setValue(WallLightBlock.LIT, state.getValue(WallLightBlock.LIT));
                if (state.hasProperty(WallLightBlock.BRIGHTNESS))
                    newState = newState.setValue(WallLightBlock.BRIGHTNESS, state.getValue(WallLightBlock.BRIGHTNESS));
                if (state.hasProperty(WallLightBlock.LIGHT_REQUEST))
                    newState = newState.setValue(WallLightBlock.LIGHT_REQUEST, state.getValue(WallLightBlock.LIGHT_REQUEST));

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
        }

        if (block instanceof CornerLightBlock) {
            net.neoforged.neoforge.registries.DeferredBlock<Block> newBlockHolder = Searchlight.CORNER_LIGHTS.get(normalizedColor);
            if (newBlockHolder != null && newBlockHolder.get() != block) {
                Block newBlock = newBlockHolder.get();
                BlockState newState = newBlock.defaultBlockState();

                String oldAddress = "";
                BlockEntity oldBe = world.getBlockEntity(pos);
                if (oldBe instanceof AddressableLight addressable) {
                    oldAddress = addressable.getAddress();
                }

                if (state.hasProperty(CornerLightBlock.CORNER))
                    newState = newState.setValue(CornerLightBlock.CORNER, state.getValue(CornerLightBlock.CORNER));
                if (state.hasProperty(CornerLightBlock.CONNECTION))
                    newState = newState.setValue(CornerLightBlock.CONNECTION, state.getValue(CornerLightBlock.CONNECTION));
                if (state.hasProperty(CornerLightBlock.LIT))
                    newState = newState.setValue(CornerLightBlock.LIT, state.getValue(CornerLightBlock.LIT));
                if (state.hasProperty(CornerLightBlock.BRIGHTNESS))
                    newState = newState.setValue(CornerLightBlock.BRIGHTNESS, state.getValue(CornerLightBlock.BRIGHTNESS));
                if (state.hasProperty(CornerLightBlock.LIGHT_REQUEST))
                    newState = newState.setValue(CornerLightBlock.LIGHT_REQUEST, state.getValue(CornerLightBlock.LIGHT_REQUEST));

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
        }
        return state;
    }

    private void applyLightUpdates(Level world, BlockPos pos, Map<?, ?> options) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof AbstractLightBlock)) return;

        BlockState updatedState = state;

        if (options.containsKey("color") && options.get("color") instanceof String colorName) {
            updatedState = updateColorProperty(world, pos, updatedState, colorName);
        }

        if (options.containsKey("brightness")) {
            BrightnessStage stage = parseBrightness(options.get("brightness"));
            if (stage != null && updatedState.hasProperty(AbstractLightBlock.BRIGHTNESS)) {
                updatedState = updatedState.setValue(AbstractLightBlock.BRIGHTNESS, stage);
            }
        }

        boolean hasLitRequest = false;
        if (options.containsKey("lit")) {
            LightRequest request = parseLightRequest(options.get("lit"));
            if (updatedState.hasProperty(AbstractLightBlock.LIGHT_REQUEST)) {
                updatedState = updatedState.setValue(AbstractLightBlock.LIGHT_REQUEST, request);
                hasLitRequest = true;
            }
        }

        if (updatedState != state) {
            world.setBlockAndUpdate(pos, updatedState);
            world.updateNeighborsAt(pos, updatedState.getBlock());

            BlockEntity be = world.getBlockEntity(pos);
            if (be != null) {
                be.setChanged();
                world.sendBlockUpdated(pos, state, updatedState, 3);
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
            targets.addAll(getConnectedCornerLights(world, pos, state));
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
                lightInfo.put("light_request", state.getValue(AbstractLightBlock.LIGHT_REQUEST).name().toLowerCase());
                lightInfo.put("brightness", state.getValue(AbstractLightBlock.BRIGHTNESS).name().toLowerCase());
                lightInfo.put("color", getLightColorName(state));

                String address = "";
                if (world.getBlockEntity(pos) instanceof AddressableLight addressable) {
                    address = addressable.getAddress();
                }
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
