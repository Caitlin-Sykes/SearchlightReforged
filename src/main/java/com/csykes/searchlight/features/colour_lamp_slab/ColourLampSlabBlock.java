package com.csykes.searchlight.features.colour_lamp_slab;

import com.csykes.searchlight.Searchlight;
import com.csykes.searchlight.features.wall_light.WallLightBlockEntity;
import com.csykes.searchlight.utils.lighting.AbstractLightBlock;
import com.csykes.searchlight.utils.lighting.ConnectableLightSlab;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec;

@Getter
public class ColourLampSlabBlock extends AbstractLightBlock implements EntityBlock, ConnectableLightSlab {
    private final DyeColor blockColor;
    private final String dyenamicColor;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty TOP_HALF = BooleanProperty.create("top_half");

    @Override
    public BooleanProperty getTopHalfProperty() {
        return TOP_HALF;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new WallLightBlockEntity(pos, state);
    }

    public ColourLampSlabBlock(Properties properties, DyeColor blockColor) {
        this(properties, blockColor, null);
    }

    public ColourLampSlabBlock(Properties properties, String dyenamicColor) {
        this(properties, null, dyenamicColor);
    }

    private ColourLampSlabBlock(Properties properties, DyeColor blockColor, String dyenamicColor) {
        super(properties);
        this.blockColor = blockColor;
        this.dyenamicColor = dyenamicColor;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, true)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(TOP_HALF, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH);
        builder.add(SOUTH);
        builder.add(EAST);
        builder.add(WEST);
        builder.add(UP);
        builder.add(DOWN);
        builder.add(TOP_HALF);
    }

    @Override
    protected boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        return true;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return ConnectableLightSlab.getSlabShape(state, TOP_HALF);
    }

    @Override
    public @Nullable Block getFullBlock() {
        String colorKey = this.blockColor != null ? this.blockColor.getName() : this.dyenamicColor;
        if (colorKey != null) {
            DeferredBlock<Block> holder = Searchlight.COLOUR_LAMPS.get(colorKey);
            if (holder != null) {
                return holder.get();
            }
        }
        return null;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return ConnectableLightSlab.canSlabBeReplaced(state, context, TOP_HALF, this.asItem());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return ConnectableLightSlab.getSlabStateForPlacement(
                this,
                context,
                TOP_HALF,
                getFullBlock(),
                (dir, st) -> getDirection(dir, context.getLevel(), context.getClickedPos(), st)
        );
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

    private BlockState getDirection(Direction dir, Level level, BlockPos pos, BlockState state) {
        BlockState neighborState = level.getBlockState(pos.relative(dir));
        boolean isConnected = ConnectableLightSlab.isSlabConnected(state, neighborState, TOP_HALF);

        return switch (dir) {
            case UP -> state.setValue(UP, !isConnected);
            case DOWN -> state.setValue(DOWN, !isConnected);
            case NORTH -> state.setValue(NORTH, isConnected);
            case EAST -> state.setValue(EAST, isConnected);
            case WEST -> state.setValue(WEST, isConnected);
            case SOUTH -> state.setValue(SOUTH, isConnected);
        };
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean isConnected = ConnectableLightSlab.isSlabConnected(state, neighborState, TOP_HALF);

        if (direction == Direction.UP || direction == Direction.DOWN) {
            return state.setValue(getPropertyForDirection(direction), !isConnected);
        }
        return state.setValue(getPropertyForDirection(direction), isConnected);
    }

    private BooleanProperty getPropertyForDirection(Direction dir) {
        return switch (dir) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
        };
    }

    public static final MapCodec<ColourLampSlabBlock> CODEC = mapCodec(instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("color").forGetter(ColourLampSlabBlock::getBlockColor)).apply(instance, ColourLampSlabBlock::new));

    @Override
    protected @NotNull MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}