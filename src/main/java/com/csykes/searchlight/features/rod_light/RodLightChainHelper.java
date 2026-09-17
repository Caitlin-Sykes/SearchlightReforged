package com.csykes.searchlight.features.rod_light;

import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers and builds an ordered sequence of addressable pixel targets
 * along a contiguous chain of single rod lights (CornerLight and CentreLight).
 */
public class RodLightChainHelper {

    public record RodPixelTarget(@NotNull BlockPos pos, int subPixelIndex) {
    }

    /**
     * Discovers all connected blocks along the rod axis, sorts them from negative to positive
     * (e.g. bottom-to-top for Y axis, west-to-east for X axis, north-to-south for Z axis),
     * and expands each block into 8 sub-pixel targets in sequential order.
     */
    public static List<RodPixelTarget> getChain(@NotNull Level level, @NotNull BlockPos startPos) {
        BlockState startState = level.getBlockState(startPos);
        if (!(startState.getBlock() instanceof AbstractLightBlock alb) || !alb.isConnectingLight(startState)) {
            return Collections.emptyList();
        }

        List<BlockPos> connected = alb.getConnectedLights(level, startPos, startState);
        if (connected.isEmpty()) {
            connected = List.of(startPos);
        }

        Direction.Axis axis = startState.hasProperty(BlockStateProperties.AXIS)
                ? startState.getValue(BlockStateProperties.AXIS)
                : Direction.Axis.Y;

        List<BlockPos> sorted = new ArrayList<>(connected);
        sorted.sort((a, b) -> switch (axis) {
            case X -> Integer.compare(a.getX(), b.getX());
            case Y -> Integer.compare(a.getY(), b.getY());
            case Z -> Integer.compare(a.getZ(), b.getZ());
        });

        List<RodPixelTarget> targets = new ArrayList<>(sorted.size() * RodLightData.SUB_PIXELS);
        for (BlockPos pos : sorted) {
            for (int i = 0; i < RodLightData.SUB_PIXELS; i++) {
                targets.add(new RodPixelTarget(pos, i));
            }
        }

        return targets;
    }
}
