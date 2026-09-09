package com.mat.api;

import com.mat.assertion.BlockAssertions;
import com.mat.engine.SimulatedPlayerEngine;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.BlockCapability;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Handle targeting a specific relative {@link BlockPos} in the MAT test environment.
 * Acts as a fluent facade for block interactions, state queries, block entities, capabilities, and assertions.
 */
@RequiredArgsConstructor
public class BlockHandle {

    @NonNull
    private final TestContext context;

    @Getter
    @NonNull
    private final BlockPos relativePos;

    /**
     * @return The absolute {@link BlockPos} in the world level.
     */
    public BlockPos getAbsolutePos() {
        return this.context.getHelper().absolutePos(this.relativePos);
    }

    /**
     * Retrieves the current raw {@link BlockState} at this handle's relative position.
     */
    public BlockState getBlockState() {
        return this.context.getHelper().getBlockState(this.relativePos);
    }

    /**
     * Retrieves the current {@link Block} at this handle's relative position.
     */
    public Block getBlock() {
        return getBlockState().getBlock();
    }

    /**
     * Retrieves the raw {@link BlockEntity} at this handle's position if present.
     */
    public BlockEntity getBlockEntity() {
        return this.context.getHelper().getBlockEntity(this.relativePos);
    }

    /**
     * Retrieves the {@link BlockEntity} cast to the specified class type if present.
     *
     * @param entityClass Target class of the block entity.
     * @param <T>         BlockEntity sub-type.
     * @return Typed block entity or null if absent / incompatible.
     */
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> T getBlockEntity(Class<T> entityClass) {
        BlockEntity be = getBlockEntity();
        if (entityClass.isInstance(be)) {
            return (T) be;
        }
        return null;
    }

    /**
     * Queries a NeoForge {@link BlockCapability} at this handle's position for a specific directional context.
     *
     * @param capability NeoForge BlockCapability to query.
     * @param context    Directional context (or null).
     * @param <T>        Capability interface type.
     * @param <C>        Context type (e.g. {@link Direction}).
     * @return Capability instance, or null if unsupported.
     */
    public <T, C> T getCapability(BlockCapability<T, C> capability, C context) {
        return this.context.getHelper().getLevel().getCapability(capability, getAbsolutePos(), context);
    }

    /**
     * Queries a NeoForge {@link BlockCapability} with no directional context.
     *
     * @param capability NeoForge BlockCapability to query.
     * @param <T>        Capability interface type.
     * @return Capability instance, or null if unsupported.
     */
    public <T> T getCapability(BlockCapability<T, Void> capability) {
        return getCapability(capability, null);
    }

    // ==========================================
    // Action Methods (Chainable)
    // ==========================================

    /**
     * Simulates an empty-hand right click on the top face of the block.
     */
    public BlockHandle rightClick() {
        return rightClick(Direction.UP);
    }

    /**
     * Simulates an empty-hand right click on a specific face of the block.
     *
     * @param face The target block face.
     */
    public BlockHandle rightClick(Direction face) {
        this.context.queueAction(helper -> {
            SimulatedPlayerEngine.interactBlockWithItem(
                    helper,
                    this.relativePos,
                    null,
                    InteractionHand.MAIN_HAND,
                    face
            );
        });
        return this;
    }

    /**
     * Simulates right-clicking the block using a specific item on the top face.
     *
     * @param itemId Namespaced item ID string (e.g. "minecraft:red_dye").
     */
    public BlockHandle rightClickWithItem(String itemId) {
        return rightClickWithItem(itemId, Direction.UP);
    }

    /**
     * Simulates right-clicking the block using a specific item on a target face.
     *
     * @param itemId Namespaced item ID string (e.g. "minecraft:red_dye").
     * @param face   The target block face.
     */
    public BlockHandle rightClickWithItem(String itemId, Direction face) {
        this.context.queueAction(helper -> {
            SimulatedPlayerEngine.interactBlockWithItem(
                    helper,
                    this.relativePos,
                    itemId,
                    InteractionHand.MAIN_HAND,
                    face
            );
        });
        return this;
    }

    /**
     * Delays execution on this handle for a given number of game ticks.
     *
     * @param ticks Number of ticks to wait.
     */
    public BlockHandle waitTicks(int ticks) {
        this.context.waitTicks(ticks);
        return this;
    }

    /**
     * Schedules a verification callback on this handle in the execution timeline.
     *
     * @param assertion Consumer taking this handle.
     */
    public BlockHandle verify(Consumer<BlockHandle> assertion) {
        this.context.queueAction(helper -> assertion.accept(this));
        return this;
    }

    /**
     * Schedules a verification callback on a typed {@link BlockEntity} at this handle.
     *
     * @param entityClass Target class of the block entity.
     * @param assertion   Consumer taking the typed block entity.
     * @param <T>         BlockEntity sub-type.
     */
    public <T extends BlockEntity> BlockHandle verifyBlockEntity(Class<T> entityClass, Consumer<T> assertion) {
        this.context.queueAction(helper -> {
            T be = getBlockEntity(entityClass);
            if (be == null) {
                throw new IllegalStateException("Expected block entity of type " + entityClass.getSimpleName() + " at " + relativePos + " but found null or different type");
            }
            assertion.accept(be);
        });
        return this;
    }

    /**
     * Schedules a verification callback on a NeoForge {@link BlockCapability} at this handle.
     *
     * @param capability NeoForge capability.
     * @param side       Direction context.
     * @param assertion  Consumer receiving capability.
     * @param <T>        Capability interface type.
     */
    public <T> BlockHandle verifyCapability(BlockCapability<T, Direction> capability, Direction side, Consumer<T> assertion) {
        this.context.queueAction(helper -> {
            T cap = getCapability(capability, side);
            if (cap == null) {
                throw new IllegalStateException("Expected capability " + capability.name() + " on side " + side + " at " + relativePos + " but was null");
            }
            assertion.accept(cap);
        });
        return this;
    }

    // ==========================================
    // Assertion Methods
    // ==========================================

    /**
     * Queues an assertion evaluating a condition on the server tick.
     *
     * @param assertion      Condition evaluating whether the assertion passes.
     * @param failureMessage Message to fail the GameTest with if false.
     */
    public BlockHandle assertThat(Supplier<Boolean> assertion, String failureMessage) {
        this.context.assertThat(assertion, failureMessage);
        return this;
    }

    /**
     * Queues an assertion verifying that the block at this handle matches the expected block ID string.
     *
     * @param expectedBlockId Namespaced block ID string (e.g. "searchlight:colour_lamp_red").
     */
    public BlockHandle assertBlockId(String expectedBlockId) {
        this.context.queueAction(helper -> BlockAssertions.assertBlockId(this.relativePos, getBlockState(), expectedBlockId));
        return this;
    }

    /**
     * Queues an assertion verifying that the block at this handle matches the expected colour name.
     *
     * @param expectedColor Expected colour name (e.g. "red", "blue").
     */
    public BlockHandle assertColor(String expectedColor) {
        this.context.queueAction(helper -> BlockAssertions.assertColour(this.relativePos, getBlockState(), expectedColor));
        return this;
    }

    /**
     * Queues an assertion verifying that a specific property equals the expected value.
     */
    public <T extends Comparable<T>> BlockHandle assertProperty(Property<T> property, T expectedValue) {
        this.context.queueAction(helper -> BlockAssertions.assertProperty(this.relativePos, getBlockState(), property, expectedValue));
        return this;
    }

    // ==========================================
    // Property Inspection Methods
    // ==========================================

    /**
     * Checks if the block matches an ID string (e.g. "minecraft:stone").
     *
     * @param expectedBlockId Namespaced block ID.
     * @return true if matches, false otherwise.
     */
    public boolean hasBlockId(String expectedBlockId) {
        return BlockAssertions.matchesBlockId(getBlockState(), expectedBlockId);
    }

    /**
     * Inspects BlockState properties for colour (e.g., EnumProperty).
     *
     * @param expectedColor Expected colour name.
     * @return true if matching colour property is found, false otherwise.
     */
    public boolean hasColour(String expectedColor) {
        return BlockAssertions.matchesColour(getBlockState(), expectedColor);
    }

    /**
     * Retrieves the value of a specific {@link Property} if present on this BlockState.
     */
    public <T extends Comparable<T>> Optional<T> getPropertyValue(Property<T> property) {
        return BlockAssertions.getPropertyValue(getBlockState(), property);
    }
}
