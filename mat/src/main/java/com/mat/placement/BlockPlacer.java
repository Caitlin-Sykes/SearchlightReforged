package com.mat.placement;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles block placement, coordinate allocation, and world mutation for GameTest environments.
 */
@Getter
@RequiredArgsConstructor
public class BlockPlacer {

    @NonNull
    private final GameTestHelper helper;
    private final Set<BlockPos> occupiedPositions = new HashSet<>();

    private int nextAutoX = 1;
    private int nextAutoZ = 1;
    private static final int AUTO_Y = 1;
    private static final int MAX_GRID_SIZE = 10;

    /**
     * Automatically calculates a non-overlapping relative position and sets the block.
     *
     * @param blockId Namespaced block ID.
     * @return The allocated {@link BlockPos}.
     */
    public BlockPos placeBlock(String blockId) {
        BlockPos pos = computeNextAvailablePos();
        placeBlock(pos, blockId);
        return pos;
    }

    /**
     * Places a block at an explicit relative position.
     *
     * @param relativePos Relative coordinate within test structure bounds.
     * @param blockId     Namespaced block ID.
     */
    public void placeBlock(BlockPos relativePos, String blockId) {
        ResourceLocation id = ResourceLocation.parse(blockId);
        Block block = BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown block ID: " + blockId));

        placeBlock(relativePos, block.defaultBlockState());
    }

    /**
     * Places an explicit {@link BlockState} at a relative position.
     *
     * @param relativePos Relative coordinate.
     * @param state       The {@link BlockState} to set.
     */
    public void placeBlock(BlockPos relativePos, BlockState state) {
        this.helper.setBlock(relativePos, state);
        this.occupiedPositions.add(relativePos);
    }

    /**
     * Calculates the next available relative position on the ground plane.
     */
    public BlockPos computeNextAvailablePos() {
        while (true) {
            BlockPos candidate = new BlockPos(this.nextAutoX, AUTO_Y, this.nextAutoZ);
            this.nextAutoX++;
            if (this.nextAutoX > MAX_GRID_SIZE) {
                this.nextAutoX = 1;
                this.nextAutoZ++;
            }
            if (!this.occupiedPositions.contains(candidate)) {
                return candidate;
            }
        }
    }
}
