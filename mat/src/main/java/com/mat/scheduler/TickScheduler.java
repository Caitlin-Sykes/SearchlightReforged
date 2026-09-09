package com.mat.scheduler;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the timeline of scheduled actions and delays for a test context.
 */
@RequiredArgsConstructor
public class TickScheduler {

    @NonNull
    private final GameTestHelper helper;
    private final List<ScheduledStep> steps = new ArrayList<>();
    @Getter
    private int currentTickOffset = 1;

    /**
     * Queues an action at the current tick offset and advances by 1 tick for the next sequential step.
     */
    public void scheduleAction(Consumer<GameTestHelper> action) {
        this.steps.add(new ScheduledStep(this.currentTickOffset, action));
        this.currentTickOffset++;
    }

    /**
     * Adds an explicit tick delay to the timeline.
     *
     * @param ticks Positive number of ticks to delay.
     */
    public void delay(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Ticks delay cannot be negative, got: " + ticks);
        }
        this.currentTickOffset += ticks;
    }

    /**
     * @return An unmodifiable view of the scheduled steps.
     */
    public List<ScheduledStep> getSteps() {
        return Collections.unmodifiableList(this.steps);
    }

    /**
     * Dispatches all queued steps to the underlying {@link GameTestHelper#runAtTickTime(long, Runnable)}
     * and schedules test success completion on the final tick.
     */
    public void executeAll() {
        for (ScheduledStep step : this.steps) {
            this.helper.runAtTickTime(step.tick(), () -> step.action().accept(this.helper));
        }

        long completionTick = this.currentTickOffset + 1L;
        this.helper.runAtTickTime(completionTick, this.helper::succeed);
    }
}
