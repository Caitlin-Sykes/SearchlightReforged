package com.mat.api;

import com.mat.placement.BlockPlacer;
import com.mat.scheduler.TickScheduler;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The execution engine and builder for MAT (Minecraft Automated Testing) tests.
 * Acts as a facade orchestrating {@link BlockPlacer}, {@link TickScheduler}, and fluent assertions.
 */
@Getter
public class TestContext {

    private final GameTestHelper helper;
    private final TickScheduler scheduler;
    private final BlockPlacer placer;

    public TestContext(GameTestHelper helper) {
        this.helper = Objects.requireNonNull(helper, "GameTestHelper must not be null");
        this.scheduler = new TickScheduler(helper);
        this.placer = new BlockPlacer(helper);
    }

    // ==========================================
    // Block Placement & Locator API
    // ==========================================

    /**
     * Automatically calculates a non-overlapping relative position, sets the block in the world,
     * and returns a new {@link BlockHandle}.
     *
     * @param blockId Namespaced block ID (e.g. "minecraft:target").
     * @return A {@link BlockHandle} pointing to the newly placed block.
     */
    public BlockHandle placeBlock(String blockId) {
        BlockPos pos = this.placer.placeBlock(blockId);
        return new BlockHandle(this, pos);
    }

    /**
     * Places a block at an explicit relative position and returns a {@link BlockHandle}.
     *
     * @param relativePos Relative position within the test structure bounds.
     * @param blockId     Namespaced block ID (e.g. "minecraft:target").
     * @return A {@link BlockHandle} pointing to the placed block.
     */
    public BlockHandle placeBlock(BlockPos relativePos, String blockId) {
        this.placer.placeBlock(relativePos, blockId);
        return new BlockHandle(this, relativePos);
    }

    /**
     * Places an explicit {@link BlockState} at a relative position and returns a {@link BlockHandle}.
     */
    public BlockHandle placeBlock(BlockPos relativePos, BlockState state) {
        this.placer.placeBlock(relativePos, state);
        return new BlockHandle(this, relativePos);
    }

    /**
     * Obtains a {@link BlockHandle} targeting an existing relative position without modifying the world.
     */
    public BlockHandle locateBlock(BlockPos relativePos) {
        return new BlockHandle(this, relativePos);
    }

    // ==========================================
    // Action Queuing & Scheduling
    // ==========================================

    /**
     * Queues an action to be executed on the current tick step.
     *
     * @param action the action to queue
     */
    public TestContext queueAction(Consumer<GameTestHelper> action) {
        this.scheduler.scheduleAction(action);
        return this;
    }

    /**
     * Adds an explicit tick delay to the execution timeline.
     *
     * @param ticks Number of ticks to wait.
     */
    public TestContext waitTicks(int ticks) {
        this.scheduler.delay(ticks);
        return this;
    }

    /**
     * Evaluates a condition on the server tick. Fails the GameTest if false.
     *
     * @param assertion      Supplier evaluating whether the condition is met.
     * @param failureMessage Message to fail the GameTest with if false.
     */
    public TestContext assertThat(Supplier<Boolean> assertion, String failureMessage) {
        return queueAction(helper -> {
            if (!Boolean.TRUE.equals(assertion.get())) {
                throw new GameTestAssertException(failureMessage);
            }
        });
    }

    /**
     * Compiles and dispatches all queued actions across sequential game ticks,
     * marking test success upon completion.
     */
    public void execute() {
        this.scheduler.executeAll();
    }
}
