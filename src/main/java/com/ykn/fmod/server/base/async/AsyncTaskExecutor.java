/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.async;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ykn.fmod.server.base.util.Util;

/**
 * A small reusable helper for running work on a background thread and then
 * performing a single follow-up action on the main thread once the background
 * work has reached a safe completion point.
 *
 * <p>Intended usage:
 * - Subclass and override {@link #executeAsyncTask()} to perform background-safe
 *   work off the main thread; access mutable Minecraft state in the follow-up instead.
 * - From {@link #executeAsyncTask()} call {@link #markAsyncFinished()} once the
 *   task has reached the point where the main-thread follow-up is safe to run.
 *   markAsyncFinished() may be called even if additional cleanup is still pending.
 *
 * <p>Subclass responsibilities:
 * - Override {@link #executeAsyncTask()} to perform background work. Call
 *   {@link #markAsyncFinished()} when appropriate.
 * - Override {@link #taskAfterCompletion()} to perform the main-thread-only follow-up.
 * - Optionally override {@link #handleAsyncException(Exception)} and
 *   {@link #handleAfterCompletionException(Exception)} to change error handling.
 */
public class AsyncTaskExecutor implements Runnable {

    private final AtomicBoolean asyncFinished = new AtomicBoolean(false);
    private final AtomicBoolean afterCompletionExecuted = new AtomicBoolean(false);

    /**
     * Implement this method with the actual async logic in the subclass.
     * Call {@link #markAsyncFinished()} when have enough information for {@link #runAfterCompletion()} to execute, even if clean up remains.
     */
    protected void executeAsyncTask() {

    }

    /**
     * Implement this method with the logic to be executed after async task is finished.
     * This method will be called in the main thread, which is designed for non-thread-safe operations.
     * The server task pool calls this on the server thread at most once.
     * {@link #runAfterCompletion()} enforces the once-only guard, not thread affinity.
     */
    protected void taskAfterCompletion() {

    }

    /**
     * Call this from {@link #executeAsyncTask()} when async task reaches the point
     * where {@link #taskAfterCompletion()} can be safely called in the main thread.
     */
    protected final void markAsyncFinished() {
        this.asyncFinished.set(true);
    }

    /**
     * Handle exceptions thrown during async task execution.
     * @param e The exception thrown.
     */
    protected void handleAsyncException(Exception e) {
        // Default implementation: log the exception
        Util.LOGGER.error("FMinecraftMod: Exception in async task execution", e);
    }

    /**
     * Handle exceptions thrown during after-completion task execution.
     * @param e The exception thrown.
     */
    protected void handleAfterCompletionException(Exception e) {
        // Default implementation: log the exception
        Util.LOGGER.error("FMinecraftMod: Exception in after-completion task execution", e);
    }

    @Override
    public final void run() {
        try {
            executeAsyncTask();
        } catch (Exception e) {
            handleAsyncException(e);
        } finally {
            markAsyncFinished();
        }
    }

    /**
     * Checks whether the main-thread follow-up is ready.
     * @return True after completion has been signaled, even if background cleanup is still running
     */
    public final boolean isAsyncFinished() {
        return asyncFinished.get();
    }

    /**
     * Check if the after-completion task has been executed.
     * @return True if the after-completion task has been executed, false otherwise.
     */
    public final boolean isAfterCompletionExecuted() {
        return afterCompletionExecuted.get();
    }

    /**
     * Run the after-completion task if it hasn't been executed yet.
     * This method must be called from the server thread after {@link #isAsyncFinished()}
     * returns true; it does not check readiness or dispatch to that thread itself.
     */
    public final void runAfterCompletion() {
        if (afterCompletionExecuted.compareAndSet(false, true)) {
            try {
                taskAfterCompletion();
            } catch (Exception e) {
                handleAfterCompletionException(e);
            }
        }
    }
}
