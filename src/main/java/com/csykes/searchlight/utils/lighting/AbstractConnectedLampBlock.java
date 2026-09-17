package com.csykes.searchlight.utils.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public abstract class AbstractConnectedLampBlock extends AbstractColoredLightBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public AbstractConnectedLampBlock(@NotNull Properties properties, @Nullable DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public AbstractConnectedLampBlock(@NotNull Properties properties, @Nullable String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    public AbstractConnectedLampBlock(@NotNull Properties properties, @Nullable DyeColor blockColor, @Nullable String dyenamicColor) {
        super(properties, blockColor, dyenamicColor);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, true)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public boolean isConnectingLight(BlockState state) {
        return true;
    }

    @Override
    public List<BlockPos> getConnectedLights(Level level, BlockPos pos, BlockState state) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);
        Set<BlockPos> visited = new LinkedHashSet<>();
        visited.add(pos);

        while (!queue.isEmpty() && visited.size() < 256) {
            BlockPos currentPos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.getBlock() == this && !visited.contains(neighborPos)) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                }
            }
        }
        return new ArrayList<>(visited);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            boolean shouldBeLit = !level.hasNeighborSignal(pos);

            // If the signal state changed, propagate the new state through the whole chain
            if (state.getValue(LIT) != shouldBeLit) {
                updateLitState(level, pos, shouldBeLit);
            }
        }
    }

    public void updateLitState(Level level, BlockPos pos, boolean newState) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos);
        Set<BlockPos> visited = new HashSet<>();
        visited.add(pos);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockState currentState = level.getBlockState(currentPos);

            if (currentState.getBlock() == this && currentState.getValue(LIT) != newState) {
                // Using flag 10 prevents this update from triggering neighborChanged
                // 10 = 2 (notify client) + 8 (no neighbor notification)
                level.setBlock(currentPos, currentState.setValue(LIT, newState), 10);

                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = currentPos.relative(dir);
                    BlockState neighborState = level.getBlockState(neighborPos);

                    if (neighborState.getBlock() == this && !visited.contains(neighborPos)) {
                        visited.add(neighborPos);
                        queue.add(neighborPos);
                    }
                }
            }
        }
    }

    public static BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
        };
    }
}
