package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.lighting_director.LightingDirectorBlockEntity;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractColoredLightBlock;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.AddressableLight;
import com.csykes.searchlight.utils.lighting.BrightnessStage;
import com.csykes.searchlight.utils.lighting.LightRequest;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightChainHelper;
import com.csykes.searchlight.features.edge_light.EdgeLightChainHelper.PixelTarget;
import com.csykes.searchlight.features.edge_light.EdgeLightData;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.rod_light.RodLightChainHelper;
import com.csykes.searchlight.features.rod_light.RodLightChainHelper.RodPixelTarget;
import com.csykes.searchlight.features.rod_light.RodLightData;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.LightMode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.csykes.searchlight.utils.lighting.AbstractLightBlock.LIT;

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
        if (block instanceof AbstractColoredLightBlock coloredBlock) {
            if (coloredBlock.getBlockColor() != null) {
                return coloredBlock.getBlockColor().getName();
            }
            if (coloredBlock.getDyenamicColor() != null) {
                return coloredBlock.getDyenamicColor();
            }
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
        if (block instanceof AbstractLightBlock lightBlock) {
            newBlock = lightBlock.getBlockForColor(normalizedColor);
        }

        if (newBlock != null && newBlock != block) {
            BlockState newState = copyMatchingProperties(state, newBlock.defaultBlockState());

            String oldAddress = "";
            BrightnessStage oldBrightness = BrightnessStage.MEDIUM;
            LightRequest oldLightRequest = LightRequest.RELEASE;
            LightMode oldLightMode = LightMode.FIXTURE;
            BlockEntity oldBe = world.getBlockEntity(pos);
            if (oldBe instanceof AddressableLight addressable) {
                oldAddress = addressable.getAddress();
                oldBrightness = addressable.getBrightness();
                oldLightRequest = addressable.getLightRequest();
                oldLightMode = addressable.getLightMode();
            }

            world.setBlockAndUpdate(pos, newState);
            world.updateNeighborsAt(pos, newBlock);

            BlockEntity newBe = world.getBlockEntity(pos);
            if (newBe instanceof AddressableLight addressable) {
                addressable.setAddress(oldAddress);
                addressable.setBrightness(oldBrightness);
                addressable.setLightRequest(oldLightRequest);
                addressable.setLightMode(oldLightMode);
                newBe.setChanged();
                world.sendBlockUpdated(pos, newState, newState, 3);
                world.getLightEngine().checkBlock(pos);
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
        if (!(block instanceof AbstractLightBlock lightBlock)) return;

        BlockEntity be = world.getBlockEntity(pos);
        LightMode mode = (be instanceof AddressableLight al) ? al.getLightMode() : LightMode.FIXTURE;

        List<BlockPos> targets = (mode == LightMode.SEPARATE) ? List.of(pos) : lightBlock.getConnectedLights(world, pos, state);

        if (mode == LightMode.PIXEL && block instanceof EdgeLightBlock && options.containsKey("pixels") && options.get("pixels") instanceof Map<?, ?> pixelsTable) {
            applyPixelsToFixture(pos, pixelsTable);
        } else if (mode == LightMode.PIXEL && (block instanceof CornerLightBlock || block instanceof CentreLightBlock) && options.containsKey("pixels") && options.get("pixels") instanceof Map<?, ?> pixelsTable) {
            applyPixelsToRodFixture(pos, pixelsTable);
        }

        for (BlockPos targetPos : targets) {
            applyLightUpdates(world, targetPos, options);
        }
    }

    private List<BlockPos> resolvePositions(Object key) {
        Level world = tile.getLevel();
        List<BlockPos> targetPositions = new ArrayList<>();
        if (world == null) return targetPositions;

        if (key instanceof Number numVal) {
            int index = numVal.intValue() - 1;
            List<BlockPos> positions = tile.getLinkedLights();
            if (index >= 0 && index < positions.size()) {
                BlockPos pos = positions.get(index);
                if (pos != null) {
                    targetPositions.add(pos);
                }
            }
        } else if (key instanceof String addressVal) {
            List<BlockPos> positions = tile.getLinkedLights();
            Set<BlockPos> checked = new HashSet<>();
            for (BlockPos pos : positions) {
                if (pos != null) {
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be instanceof AddressableLight addressable) {
                        if (addressable.getAddress().equalsIgnoreCase(addressVal)) {
                            targetPositions.add(pos);
                        }
                        // If fixture is in SEPARATE mode, search its connected blocks for individual addresses
                        if (addressable.getLightMode() == LightMode.SEPARATE) {
                            BlockState state = world.getBlockState(pos);
                            if (state.getBlock() instanceof AbstractLightBlock alb) {
                                for (BlockPos connPos : alb.getConnectedLights(world, pos, state)) {
                                    if (checked.add(connPos) && !connPos.equals(pos)) {
                                        BlockEntity connBe = world.getBlockEntity(connPos);
                                        if (connBe instanceof AddressableLight connAddr && connAddr.getAddress().equalsIgnoreCase(addressVal)) {
                                            targetPositions.add(connPos);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return targetPositions;
    }

    private static int parseIndex(Object key) {
        if (key instanceof Number n) {
            return n.intValue() - 1;
        }
        if (key != null) {
            try {
                return Integer.parseInt(key.toString().trim()) - 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }

    private boolean applyPixelsToRodFixture(BlockPos anchorPos, Map<?, ?> pixelsTable) {
        Level world = tile.getLevel();
        if (world == null) return false;

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof CornerLightBlock || anchorState.getBlock() instanceof CentreLightBlock)) {
            return false;
        }

        BlockEntity anchorBe = world.getBlockEntity(anchorPos);
        if (anchorBe instanceof AddressableLight al && al.getLightMode() != LightMode.PIXEL) {
            return false;
        }

        List<RodPixelTarget> chain = RodLightChainHelper.getChain(world, anchorPos);
        if (chain.isEmpty()) return false;

        Set<BlockPos> affectedPositions = new HashSet<>();

        for (Map.Entry<?, ?> entry : pixelsTable.entrySet()) {
            int targetIndex = parseIndex(entry.getKey());
            if (targetIndex >= 0 && targetIndex < chain.size()) {
                RodPixelTarget target = chain.get(targetIndex);
                BlockEntity be = world.getBlockEntity(target.pos());
                if (be instanceof WallLightBlockEntity wbe) {
                    Object val = entry.getValue();
                    String color = null;
                    boolean lit = true;

                    if (val instanceof String s) {
                        color = s;
                    } else if (val instanceof Map<?, ?> map) {
                        if (map.containsKey("color")) {
                            color = map.get("color").toString();
                        }
                        if (map.containsKey("lit") && map.get("lit") instanceof Boolean b) {
                            lit = b;
                        }
                    } else if (val instanceof Boolean b) {
                        lit = b;
                    }

                    if (color != null) {
                        wbe.getRodLightData().setSubPixel(target.subPixelIndex(), color, lit);
                    } else {
                        String curColor = wbe.getRodLightData().getSubPixelColor(target.subPixelIndex());
                        wbe.getRodLightData().setSubPixel(target.subPixelIndex(), curColor, lit);
                    }
                    wbe.setChanged();
                    affectedPositions.add(target.pos());
                }
            }
        }

        for (BlockPos affectedPos : affectedPositions) {
            updateRodBlockAverageVariant(world, affectedPos);
        }

        return true;
    }

    private void updateRodBlockAverageVariant(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractLightBlock currentBlock)) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof WallLightBlockEntity wbe)) return;

        RodLightData data = wbe.getRodLightData();
        boolean hasLit = data.hasAnyLitSubPixel();
        String avgColor = data.getAverageColor(state);
        if (avgColor == null) {
            avgColor = "white";
        }

        Block newBlock = currentBlock.getBlockForColor(avgColor);
        if (newBlock != null && newBlock != currentBlock) {
            BlockState newState = copyMatchingProperties(state, newBlock.defaultBlockState());
            if (newState.hasProperty(AbstractLightBlock.LIT)) {
                newState = newState.setValue(AbstractLightBlock.LIT, hasLit);
            }

            String oldAddress = wbe.getAddress();
            BrightnessStage oldBrightness = wbe.getBrightness();
            LightRequest oldLightRequest = wbe.getLightRequest();
            LightMode oldLightMode = wbe.getLightMode();
            RodLightData oldData = new RodLightData(data);

            world.setBlockAndUpdate(pos, newState);
            world.updateNeighborsAt(pos, newBlock);

            BlockEntity newBe = world.getBlockEntity(pos);
            if (newBe instanceof WallLightBlockEntity newWbe) {
                newWbe.setAddress(oldAddress);
                newWbe.setBrightness(oldBrightness);
                newWbe.setLightRequest(oldLightRequest);
                newWbe.setLightMode(oldLightMode);
                newWbe.setRodLightData(oldData);
                newBe.setChanged();
                world.sendBlockUpdated(pos, newState, newState, 3);
                world.getLightEngine().checkBlock(pos);
            }
        } else {
            if (state.hasProperty(AbstractLightBlock.LIT) && state.getValue(AbstractLightBlock.LIT) != hasLit) {
                BlockState newState = state.setValue(AbstractLightBlock.LIT, hasLit);
                world.setBlockAndUpdate(pos, newState);
            }
            wbe.setChanged();
            world.sendBlockUpdated(pos, state, state, 3);
            world.getLightEngine().checkBlock(pos);
        }
    }

    private boolean applyPixelsToFixture(BlockPos anchorPos, Map<?, ?> pixelsTable) {
        Level world = tile.getLevel();
        if (world == null) return false;

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof EdgeLightBlock)) {
            return false;
        }

        BlockEntity anchorBe = world.getBlockEntity(anchorPos);
        if (anchorBe instanceof AddressableLight al && al.getLightMode() != LightMode.PIXEL) {
            return false;
        }

        List<PixelTarget> chain = EdgeLightChainHelper.getChain(world, anchorPos);
        if (chain.isEmpty()) return false;

        Set<BlockPos> affectedPositions = new HashSet<>();

        for (Map.Entry<?, ?> entry : pixelsTable.entrySet()) {
            int targetIndex = parseIndex(entry.getKey());
            if (targetIndex >= 0 && targetIndex < chain.size()) {
                PixelTarget target = chain.get(targetIndex);
                BlockEntity be = world.getBlockEntity(target.pos());
                if (be instanceof WallLightBlockEntity wbe) {
                    Object val = entry.getValue();
                    String color = null;
                    boolean lit = true;

                    if (val instanceof String s) {
                        color = s;
                    } else if (val instanceof Map<?, ?> map) {
                        if (map.containsKey("color")) {
                            color = map.get("color").toString();
                        }
                        if (map.containsKey("lit") && map.get("lit") instanceof Boolean b) {
                            lit = b;
                        }
                    } else if (val instanceof Boolean b) {
                        lit = b;
                    }

                    if (color != null) {
                        wbe.getEdgeLightData().setSubPixel(target.edge(), target.subPixelIndex(), color, lit);
                    } else {
                        String curColor = wbe.getEdgeLightData().getSubPixelColor(target.edge(), target.subPixelIndex());
                        wbe.getEdgeLightData().setSubPixel(target.edge(), target.subPixelIndex(), curColor, lit);
                    }
                    wbe.setChanged();
                    affectedPositions.add(target.pos());
                }
            }
        }

        for (BlockPos affectedPos : affectedPositions) {
            updateEdgeBlockAverageVariant(world, affectedPos);
        }

        return true;
    }

    private void updateEdgeBlockAverageVariant(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof EdgeLightBlock currentBlock)) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof WallLightBlockEntity wbe)) return;

        EdgeLightData data = wbe.getEdgeLightData();
        boolean hasLit = data.hasAnyLitEdge(state);
        String avgColor = data.getAverageColor(state);
        if (avgColor == null) {
            avgColor = "white";
        }

        Block newBlock = currentBlock.getBlockForColor(avgColor);
        if (newBlock != null && newBlock != currentBlock) {
            BlockState newState = copyMatchingProperties(state, newBlock.defaultBlockState());
            if (newState.hasProperty(AbstractLightBlock.LIT)) {
                newState = newState.setValue(AbstractLightBlock.LIT, hasLit);
            }

            String oldAddress = wbe.getAddress();
            BrightnessStage oldBrightness = wbe.getBrightness();
            LightRequest oldLightRequest = wbe.getLightRequest();
            LightMode oldLightMode = wbe.getLightMode();
            EdgeLightData oldData = new EdgeLightData(data);

            world.setBlockAndUpdate(pos, newState);
            world.updateNeighborsAt(pos, newBlock);

            BlockEntity newBe = world.getBlockEntity(pos);
            if (newBe instanceof WallLightBlockEntity newWbe) {
                newWbe.setAddress(oldAddress);
                newWbe.setBrightness(oldBrightness);
                newWbe.setLightRequest(oldLightRequest);
                newWbe.setLightMode(oldLightMode);
                newWbe.setEdgeLightData(oldData);
                newBe.setChanged();
                world.sendBlockUpdated(pos, newState, newState, 3);
                world.getLightEngine().checkBlock(pos);
            }
        } else {
            if (state.hasProperty(AbstractLightBlock.LIT) && state.getValue(AbstractLightBlock.LIT) != hasLit) {
                BlockState newState = state.setValue(AbstractLightBlock.LIT, hasLit);
                world.setBlockAndUpdate(pos, newState);
            }
            wbe.setChanged();
            world.sendBlockUpdated(pos, state, state, 3);
            world.getLightEngine().checkBlock(pos);
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

            if (block instanceof AbstractLightBlock alb) {
                lightInfo.put("active", true);
                lightInfo.put("type", block.getClass().getSimpleName());
                lightInfo.put("lit", state.getValue(LIT));

                AddressableLight addressable = (world.getBlockEntity(pos) instanceof AddressableLight a) ? a : null;
                String lightRequestName = addressable != null ? addressable.getLightRequest().name().toLowerCase() : "release";
                String brightnessName = addressable != null ? addressable.getBrightness().name().toLowerCase() : "medium";
                String address = addressable != null ? addressable.getAddress() : "";
                String modeName = addressable != null ? addressable.getLightMode().name().toLowerCase() : "fixture";

                lightInfo.put("light_request", lightRequestName);
                lightInfo.put("brightness", brightnessName);
                lightInfo.put("color", getLightColorName(state));
                lightInfo.put("mode", modeName);
                lightInfo.put("connected_blocks", alb.getConnectedLights(world, pos, state).size());
                if (addressable != null && addressable.getLightMode() == LightMode.PIXEL && block instanceof EdgeLightBlock) {
                    lightInfo.put("pixel_count", EdgeLightChainHelper.getChain(world, pos).size());
                } else if (addressable != null && addressable.getLightMode() == LightMode.PIXEL && (block instanceof CornerLightBlock || block instanceof CentreLightBlock)) {
                    lightInfo.put("pixel_count", RodLightChainHelper.getChain(world, pos).size());
                } else if (addressable != null && addressable.getLightMode() == LightMode.SEPARATE) {
                    lightInfo.put("pixel_count", alb.getConnectedLights(world, pos, state).size());
                } else {
                    lightInfo.put("pixel_count", 1);
                }

                if (address.isEmpty()) {
                    address = "light_" + (i + 1);
                }
                lightInfo.put("address", address);
                
                String key = address;
                int suffix = 2;
                while (result.containsKey(key)) {
                    key = address + "_" + suffix;
                    suffix++;
                }
                result.put(key, lightInfo);
            } else {
                lightInfo.put("active", false);
                lightInfo.put("type", "broken");
                result.put("broken_" + (i + 1), lightInfo);
            }
        }
        return result;
    }

    @LuaFunction(mainThread = true)
    public final int getPixelCount(Object key) {
        Level world = tile.getLevel();
        if (world == null) return 0;
        List<BlockPos> positions = resolvePositions(key);
        if (positions.isEmpty()) return 0;
        BlockPos pos = positions.get(0);
        BlockState state = world.getBlockState(pos);
        BlockEntity be = world.getBlockEntity(pos);
        LightMode mode = (be instanceof AddressableLight al) ? al.getLightMode() : LightMode.FIXTURE;

        if (mode == LightMode.PIXEL && state.getBlock() instanceof EdgeLightBlock) {
            return EdgeLightChainHelper.getChain(world, pos).size();
        } else if (mode == LightMode.PIXEL && (state.getBlock() instanceof CornerLightBlock || state.getBlock() instanceof CentreLightBlock)) {
            return RodLightChainHelper.getChain(world, pos).size();
        } else if (mode == LightMode.SEPARATE && state.getBlock() instanceof AbstractLightBlock alb) {
            return alb.getConnectedLights(world, pos, state).size();
        }
        return 1;
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getPixels(Object key) {
        Level world = tile.getLevel();
        List<Map<String, Object>> result = new ArrayList<>();
        if (world == null) return result;
        List<BlockPos> positions = resolvePositions(key);
        if (positions.isEmpty()) return result;
        BlockPos pos = positions.get(0);
        BlockState state = world.getBlockState(pos);
        BlockEntity be = world.getBlockEntity(pos);
        LightMode mode = (be instanceof AddressableLight al) ? al.getLightMode() : LightMode.FIXTURE;

        if (mode == LightMode.PIXEL && state.getBlock() instanceof EdgeLightBlock) {
            List<PixelTarget> chain = EdgeLightChainHelper.getChain(world, pos);
            for (int i = 0; i < chain.size(); i++) {
                PixelTarget target = chain.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("index", i + 1);
                map.put("x", target.pos().getX());
                map.put("y", target.pos().getY());
                map.put("z", target.pos().getZ());
                map.put("edge", target.edge().getName());
                map.put("sub_pixel", target.subPixelIndex() + 1);
                BlockEntity targetBe = world.getBlockEntity(target.pos());
                if (targetBe instanceof WallLightBlockEntity wbe) {
                    map.put("color", wbe.getEdgeLightData().getSubPixelColor(target.edge(), target.subPixelIndex()));
                    map.put("lit", wbe.getEdgeLightData().isSubPixelLit(target.edge(), target.subPixelIndex()));
                } else {
                    map.put("color", "white");
                    map.put("lit", true);
                }
                result.add(map);
            }
        } else if (mode == LightMode.PIXEL && (state.getBlock() instanceof CornerLightBlock || state.getBlock() instanceof CentreLightBlock)) {
            List<RodPixelTarget> chain = RodLightChainHelper.getChain(world, pos);
            for (int i = 0; i < chain.size(); i++) {
                RodPixelTarget target = chain.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("index", i + 1);
                map.put("x", target.pos().getX());
                map.put("y", target.pos().getY());
                map.put("z", target.pos().getZ());
                map.put("sub_pixel", target.subPixelIndex() + 1);
                BlockEntity targetBe = world.getBlockEntity(target.pos());
                if (targetBe instanceof WallLightBlockEntity wbe) {
                    map.put("color", wbe.getRodLightData().getSubPixelColor(target.subPixelIndex()));
                    map.put("lit", wbe.getRodLightData().isSubPixelLit(target.subPixelIndex()));
                } else {
                    map.put("color", "white");
                    map.put("lit", true);
                }
                result.add(map);
            }
        }
        return result;
    }

    @LuaFunction(mainThread = true)
    public final boolean setPixel(Object key, int pixelIndex, String color, Optional<Boolean> litOpt) {
        List<BlockPos> positions = resolvePositions(key);
        if (positions.isEmpty()) return false;
        Map<Integer, Object> map = new HashMap<>();
        map.put(pixelIndex, Map.of("color", color, "lit", litOpt.orElse(true)));
        boolean anyUpdated = false;
        for (BlockPos pos : positions) {
            if (applyPixelsToFixture(pos, map) || applyPixelsToRodFixture(pos, map)) {
                anyUpdated = true;
            }
        }
        return anyUpdated;
    }

    @LuaFunction(mainThread = true)
    public final boolean setPixels(Object key, Map<?, ?> pixelsTable) {
        List<BlockPos> positions = resolvePositions(key);
        if (positions.isEmpty()) return false;
        boolean anyUpdated = false;
        for (BlockPos pos : positions) {
            if (applyPixelsToFixture(pos, pixelsTable) || applyPixelsToRodFixture(pos, pixelsTable)) {
                anyUpdated = true;
            }
        }
        return anyUpdated;
    }

    @LuaFunction(mainThread = true)
    public final boolean setPixelBatch(Map<?, ?> batchTable) {
        boolean anyUpdated = false;
        for (Map.Entry<?, ?> entry : batchTable.entrySet()) {
            Object fixtureKey = entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> pixelsTable) {
                List<BlockPos> positions = resolvePositions(fixtureKey);
                for (BlockPos pos : positions) {
                    if (applyPixelsToFixture(pos, pixelsTable) || applyPixelsToRodFixture(pos, pixelsTable)) {
                        anyUpdated = true;
                    }
                }
            }
        }
        return anyUpdated;
    }

    @LuaFunction(mainThread = true)
    public final boolean setLight(Object key, Map<?, ?> options) {
        Level world = tile.getLevel();
        if (world == null) return false;

        List<BlockPos> targetPositions = resolvePositions(key);
        if (targetPositions.isEmpty()) return false;
        
        for (BlockPos targetPos : targetPositions) {
            processLightUpdate(world, targetPos, options);
        }
        return true;
    }

    @LuaFunction(mainThread = true)
    public final boolean setLights(Map<?, ?> bulkOptions) {
        Level world = tile.getLevel();
        if (world == null) return false;

        for (Map.Entry<?, ?> entry : bulkOptions.entrySet()) {
            Object key = entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> options)) {
                continue;
            }

            List<BlockPos> targetPositions = resolvePositions(key);
            for (BlockPos targetPos : targetPositions) {
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
                boolean removedAny = false;
                List<BlockPos> positions = tile.getLinkedLights();
                for (int i = positions.size() - 1; i >= 0; i--) {
                    BlockPos pos = positions.get(i);
                    if (pos != null) {
                        BlockEntity be = world.getBlockEntity(pos);
                        if (be instanceof AddressableLight addressable && addressable.getAddress().equalsIgnoreCase(addressVal)) {
                            if (tile.removeLinkedLight(i)) {
                                removedAny = true;
                            }
                        }
                    }
                }
                return removedAny;
            }
        }
        return false;
    }

    @LuaFunction(mainThread = true)
    public final void clearLights() {
        tile.clearLinkedLights();
    }
}
