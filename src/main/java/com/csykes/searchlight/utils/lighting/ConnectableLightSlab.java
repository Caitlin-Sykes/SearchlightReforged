package com.csykes.searchlight.utils.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Standard interface for light slabs providing de-duplicated placement,
 * replacement, connection, and double-slab swap logic.
 */
public interface ConnectableLightSlab {

    /**
     * @return The {@link BooleanProperty} representing whether this slab is in the top half.
     */
    BooleanProperty getTopHalfProperty();

    /**
     * @return The corresponding full {@link Block} to swap to when forming a double slab, or null if unsupported.
     */
    @Nullable
    Block getFullBlock();

    /**
     * Standard voxel shape for a bottom slab (0, 0, 0 to 16, 8, 16).
     */
    VoxelShape DEFAULT_BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    /**
     * Standard voxel shape for a top slab (0, 8, 0 to 16, 16, 16).
     */
    VoxelShape DEFAULT_TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

    /**
     * Determines whether the placement click targets the top half of the block space.
     */
    static boolean isTopHalfPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        return clickedFace == Direction.DOWN || (clickedFace != Direction.UP && (context.getClickLocation().y - (double) context.getClickedPos().getY() > 0.5));
    }

    /**
     * Standard canBeReplaced implementation for half slabs.
     * Allows placing a matching slab into the remaining empty half of the block.
     */
    static boolean canSlabBeReplaced(BlockState state, BlockPlaceContext context, BooleanProperty topHalfProperty, Item slabItem) {
        ItemStack itemstack = context.getItemInHand();
        if (!itemstack.is(slabItem)) {
            return false;
        }
        boolean isTop = state.getValue(topHalfProperty);
        if (context.replacingClickedOnBlock()) {
            boolean isUpperHalf = context.getClickLocation().y - (double) context.getClickedPos().getY() > 0.5;
            Direction direction = context.getClickedFace();
            if (!isTop) {
                return direction == Direction.UP || (isUpperHalf && direction.getAxis().isHorizontal());
            } else {
                return direction == Direction.DOWN || (!isUpperHalf && direction.getAxis().isHorizontal());
            }
        } else {
            return true;
        }
    }

    /**
     * Calculates the placement state for a slab block, handling double slab swapping,
     * lit state from redstone, top/bottom half state, and directional connections.
     */
    static BlockState getSlabStateForPlacement(
            Block slabBlock,
            BlockPlaceContext context,
            BooleanProperty topHalfProperty,
            @Nullable Block fullBlock,
            @Nullable BiFunction<Direction, BlockState, BlockState> directionMapper
    ) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState existingState = level.getBlockState(pos);

        // Check if placing into an existing slab of this type -> swap to full block
        if (existingState.getBlock() == slabBlock && fullBlock != null) {
            BlockState fullState = fullBlock.getStateForPlacement(context);
            return fullState != null ? fullState : fullBlock.defaultBlockState();
        }

        BlockState state = slabBlock.defaultBlockState();
        state = state.setValue(topHalfProperty, isTopHalfPlacement(context));

        if (state.hasProperty(AbstractLightBlock.LIT)) {
            state = state.setValue(AbstractLightBlock.LIT, !level.hasNeighborSignal(pos));
        }

        if (directionMapper != null) {
            for (Direction dir : Direction.values()) {
                state = directionMapper.apply(dir, state);
            }
        }

        return state;
    }

    /**
     * Checks whether a neighbor slab connects to this slab (same block type and same top/bottom half).
     */
    static boolean isSlabConnected(BlockState state, BlockState neighborState, BooleanProperty topHalfProperty) {
        return neighborState.is(state.getBlock())
                && neighborState.hasProperty(topHalfProperty)
                && Objects.equals(neighborState.getValue(topHalfProperty), state.getValue(topHalfProperty));
    }

    /**
     * Returns the appropriate half-slab voxel shape based on the top_half property.
     */
    static @NotNull VoxelShape getSlabShape(BlockState state, BooleanProperty topHalfProperty) {
        return state.getValue(topHalfProperty) ? DEFAULT_TOP_SHAPE : DEFAULT_BOTTOM_SHAPE;
    }
}
