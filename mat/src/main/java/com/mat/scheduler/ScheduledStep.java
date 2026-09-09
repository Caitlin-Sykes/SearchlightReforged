package com.mat.scheduler;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.Consumer;

/**
 * Represents an individual action scheduled to execute at a specific tick time.
 *
 * @param tick   Target tick timestamp relative to test start.
 * @param action The action callback to execute on the server thread.
 */
public record ScheduledStep(long tick, Consumer<GameTestHelper> action) {
}
