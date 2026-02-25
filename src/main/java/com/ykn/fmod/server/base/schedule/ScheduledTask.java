/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

/**
 * Represents a scheduled task with a delay and duration. The task can be triggered,
 * ticked, finished, canceled, or rescheduled. Subclasses can override specific
 * methods to define custom behavior for task events.
 */
public class ScheduledTask {

    /**
     * The delay in ticks before the task is triggered.
     */
    private int delay;
    
    /**
     * The duration in ticks for the task's execution.
     */
    private int duration;

    public ScheduledTask(int delay, int duration) {
        this.delay = delay;
        this.duration = duration;
    }

    /**
     * Handles the scheduled task's lifecycle during each tick.
     * 
     * - If the task is not finished and should be canceled, it cancels the task.
     * - If there is a delay, it decrements the delay counter and triggers the task
     *   when the delay reaches zero.
     * - If the task is in progress (duration > 0), it performs the task's tick
     *   logic, decrements the duration counter, and finishes the task when the
     *   duration reaches zero.
     */
    public void tick() {
        if (!isFinished() && shouldCancel()) {
            cancel();
        } else if (delay > 0) {
            delay--;
            if (delay == 0) {
                onTrigger();
            }
        } else if (duration > 0) {
            onTick();
            duration--;
            if (duration == 0) {
                onFinish();
            }
        }
    }

    /**
     * Checks if the scheduled task has finished execution.
     *
     * @return {@code true} if both the delay and duration of the task are less than or equal to zero,
     *         indicating that the task has completed; {@code false} otherwise.
     */
    public boolean isFinished() {
        return delay <= 0 && duration <= 0;
    }

    /**
     * Cancels the scheduled task by resetting its delay and duration to zero.
     * This method also triggers the {@code onCancel()} callback to handle any
     * additional cleanup or logic required when the task is canceled.
     */
    public void cancel() {
        delay = 0;
        duration = 0;
        onCancel();
    }

    /**
     * Called on each tick to perform scheduled tasks.
     * This method is intended to be overridden by subclasses
     * to define specific behavior that should occur on each tick.
     */
    public void onTick() {
        // Override by the subclass
    }

    /**
     * Called when the task is triggered after the delay has elapsed.
     * This method is intended to be overridden by subclasses
     * to define specific behavior that should occur when the task is triggered.
     * It is called only once when the delay reaches zero.
     */
    public void onTrigger() {
        // Override by the subclass
    }

    /**
     * Called when the task finishes after the duration has elapsed.
     * This method is intended to be overridden by subclasses
     * to define specific behavior that should occur when the task finishes.
     * It is called only once when the duration reaches zero.
     */
    public void onFinish() {
        // Override by the subclass
    }

    /**
     * Called when the task is canceled. This method is intended to be overridden by subclasses
     * to define specific behavior that should occur when the task is canceled.
     */
    public void onCancel() {
        // Override by the subclass
    }

    /**
     * Determines whether the task should be canceled based on specific conditions.
     * This method is intended to be overridden by subclasses to define custom cancellation logic.
     *
     * @return {@code true} if the task should be canceled; {@code false} otherwise.
     */
    public boolean shouldCancel() {
        // Override by the subclass
        return false;
    }

    /**
     * Reschedules the task with a new delay and duration.
     *
     * @param delay    The new delay before the task is triggered.
     * @param duration The new duration for the task's execution.
     */
    public void reschedule(int delay, int duration) {
        if (delay < 0 || duration < 0) {
            Util.LOGGER.warn("FMincraftMod: Attempted to reschedule a task with delay " + delay + " and duration " + duration + ". onTrigger and onFinish may not be called properly.");
        }
        this.delay = delay;
        this.duration = duration;
    }

    /**
     * Gets the current delay of the task.
     *
     * @return The current delay before the task is triggered.
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Gets the current duration of the task.
     *
     * @return The current duration for the task's execution.
     */
    public int getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "ScheduledTask{}";
    }
 }
