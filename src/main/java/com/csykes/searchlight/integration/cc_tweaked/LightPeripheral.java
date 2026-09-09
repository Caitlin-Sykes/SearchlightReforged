package com.csykes.searchlight.integration.cc_tweaked;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.centre_light.CentreLightBlock;
import com.csykes.searchlight.features.corner_light.CornerLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightBlock;
import com.csykes.searchlight.features.edge_light.EdgeLightChainHelper;
import com.csykes.searchlight.features.edge_light.EdgeLightChainHelper.PixelTarget;
import com.csykes.searchlight.features.edge_light.EdgeLightData;
import com.csykes.searchlight.features.rod_light.RodLightChainHelper;
import com.csykes.searchlight.features.rod_light.RodLightChainHelper.RodPixelTarget;
import com.csykes.searchlight.features.rod_light.RodLightData;
import com.csykes.searchlight.features.searchlight.SearchlightBlock;
import com.csykes.searchlight.features.searchlight.SearchlightBlockEntity;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.*;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

        if (block instanceof AbstractLightBlock alb) {
            for (BlockPos connectedPos : alb.getConnectedLights(world, pos, state)) {
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

        if (block instanceof AbstractLightBlock abstractLightBlock) {
            for (BlockPos connectedPos : abstractLightBlock.getConnectedLights(world, pos, state)) {
                BlockEntity be = world.getBlockEntity(connectedPos);
                if (be instanceof AddressableLight light) {
                    light.setLightRequest(lit);
                    be.setChanged();
                    BlockState s = world.getBlockState(connectedPos);
                    world.sendBlockUpdated(connectedPos, s, s, 3);
                    abstractLightBlock.updateLitState(world, connectedPos, s);
                }
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
        if (block instanceof AbstractLightBlock lightBlock) {
            newBlock = lightBlock.getBlockForColor(normalizedColor);
        }

        if (newBlock != null && newBlock != block) {
            AbstractLightBlock alb = (AbstractLightBlock) block;
            BlockEntity oldAnchorBe = world.getBlockEntity(pos);
            LightMode fixtureMode = (oldAnchorBe instanceof AddressableLight al) ? al.getLightMode() : LightMode.FIXTURE;

            List<BlockPos> targets = (fixtureMode == LightMode.SEPARATE) ? List.of(pos) : alb.getConnectedLights(world, pos, state);
            for (BlockPos targetPos : targets) {
                String address = "";
                BrightnessStage oldBrightness = BrightnessStage.MEDIUM;
                LightRequest oldLightRequest = LightRequest.RELEASE;
                LightMode oldLightMode = fixtureMode;
                BlockEntity oldBe = world.getBlockEntity(targetPos);
                if (oldBe instanceof AddressableLight addressable) {
                    address = addressable.getAddress();
                    oldBrightness = addressable.getBrightness();
                    oldLightRequest = addressable.getLightRequest();
                    oldLightMode = addressable.getLightMode();
                }

                BlockState s = world.getBlockState(targetPos);
                BlockState ns = copyMatchingProperties(s, newBlock.defaultBlockState());
                world.setBlockAndUpdate(targetPos, ns);
                world.updateNeighborsAt(targetPos, newBlock);

                BlockEntity newBe = world.getBlockEntity(targetPos);
                if (newBe instanceof AddressableLight addressable) {
                    addressable.setAddress(address);
                    addressable.setBrightness(oldBrightness);
                    addressable.setLightRequest(oldLightRequest);
                    addressable.setLightMode(oldLightMode);
                    newBe.setChanged();
                    world.sendBlockUpdated(targetPos, ns, ns, 3);
                    world.getLightEngine().checkBlock(targetPos);
                }
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

    private LightMode getLightMode() {
        return (tile instanceof AddressableLight al) ? al.getLightMode() : LightMode.FIXTURE;
    }

    private boolean applyPixelsToFixture(BlockPos anchorPos, Map<?, ?> pixelsTable) {
        Level world = tile.getLevel();
        if (world == null) return false;

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof EdgeLightBlock)) {
            return false;
        }

        if (getLightMode() != LightMode.PIXEL) {
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

    private boolean applyPixelsToRodFixture(BlockPos anchorPos, Map<?, ?> pixelsTable) {
        Level world = tile.getLevel();
        if (world == null) return false;

        BlockState anchorState = world.getBlockState(anchorPos);
        if (!(anchorState.getBlock() instanceof CornerLightBlock || anchorState.getBlock() instanceof CentreLightBlock)) {
            return false;
        }

        if (getLightMode() != LightMode.PIXEL) {
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

    @LuaFunction(mainThread = true)
    public final int getPixelCount() {
        Level world = tile.getLevel();
        if (world == null) return 0;
        BlockPos pos = tile.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (getLightMode() == LightMode.PIXEL && state.getBlock() instanceof EdgeLightBlock) {
            return EdgeLightChainHelper.getChain(world, pos).size();
        } else if (getLightMode() == LightMode.PIXEL && (state.getBlock() instanceof CornerLightBlock || state.getBlock() instanceof CentreLightBlock)) {
            return RodLightChainHelper.getChain(world, pos).size();
        }
        return 1;
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> getPixels() {
        Level world = tile.getLevel();
        List<Map<String, Object>> result = new ArrayList<>();
        if (world == null) return result;
        BlockPos pos = tile.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (getLightMode() == LightMode.PIXEL && state.getBlock() instanceof EdgeLightBlock) {
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
                BlockEntity be = world.getBlockEntity(target.pos());
                if (be instanceof WallLightBlockEntity wbe) {
                    map.put("color", wbe.getEdgeLightData().getSubPixelColor(target.edge(), target.subPixelIndex()));
                    map.put("lit", wbe.getEdgeLightData().isSubPixelLit(target.edge(), target.subPixelIndex()));
                } else {
                    map.put("color", "white");
                    map.put("lit", true);
                }
                result.add(map);
            }
        } else if (getLightMode() == LightMode.PIXEL && (state.getBlock() instanceof CornerLightBlock || state.getBlock() instanceof CentreLightBlock)) {
            List<RodPixelTarget> chain = RodLightChainHelper.getChain(world, pos);
            for (int i = 0; i < chain.size(); i++) {
                RodPixelTarget target = chain.get(i);
                Map<String, Object> map = new HashMap<>();
                map.put("index", i + 1);
                map.put("x", target.pos().getX());
                map.put("y", target.pos().getY());
                map.put("z", target.pos().getZ());
                map.put("sub_pixel", target.subPixelIndex() + 1);
                BlockEntity be = world.getBlockEntity(target.pos());
                if (be instanceof WallLightBlockEntity wbe) {
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
    public final boolean setPixel(int pixelIndex, String color, Optional<Boolean> litOpt) {
        BlockPos pos = tile.getBlockPos();
        Map<Integer, Object> map = new HashMap<>();
        map.put(pixelIndex, Map.of("color", color, "lit", litOpt.orElse(true)));
        return applyPixelsToFixture(pos, map) || applyPixelsToRodFixture(pos, map);
    }

    @LuaFunction(mainThread = true)
    public final boolean setPixels(Map<?, ?> pixelsTable) {
        BlockPos pos = tile.getBlockPos();
        return applyPixelsToFixture(pos, pixelsTable) || applyPixelsToRodFixture(pos, pixelsTable);
    }

    @LuaFunction(mainThread = true)
    public final boolean setEdge(String edgeName, String color, Optional<Boolean> litOpt) {
        Level world = tile.getLevel();
        if (world == null) return false;
        BlockPos pos = tile.getBlockPos();
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof WallLightBlockEntity wbe)) return false;

        Direction dir = Direction.byName(edgeName.toLowerCase(Locale.ROOT));
        if (dir == null || dir.getAxis().isVertical()) return false;

        wbe.getEdgeLightData().setEdge(dir, color, litOpt.orElse(true));
        wbe.setChanged();
        updateEdgeBlockAverageVariant(world, pos);
        return true;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getEdge(String edgeName) {
        Level world = tile.getLevel();
        Map<String, Object> result = new HashMap<>();
        if (world == null) return result;
        BlockPos pos = tile.getBlockPos();
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof WallLightBlockEntity wbe)) return result;

        Direction dir = Direction.byName(edgeName.toLowerCase(Locale.ROOT));
        if (dir == null || dir.getAxis().isVertical()) return result;

        result.put("color", wbe.getEdgeLightData().getEdgeColor(dir));
        result.put("lit", wbe.getEdgeLightData().isEdgeLit(dir));
        return result;
    }
}