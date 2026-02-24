/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ykn.fmod.server.base.async.AsyncTaskExecutor;
import com.ykn.fmod.server.base.async.EntityDensityCalculator;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Manages runtime data for the entire server.
 * This class stores per-player data, logic flows, scheduled tasks, and async task execution.
 * <p>
 * <br><b>WARNING:</b> The asyncTaskPool must be gracefully shutdown when replacing or resetting ServerData.
 * <br><b>NOTE:</b> All data stored here is not persisted and will be lost when the server restarts.
 * </p>
 */
public class ServerData {

    /**
     * The Minecraft server instance associated with this data.
     */
    private final MinecraftServer server;
    
    /**
     * Maps player UUIDs to their corresponding PlayerData instances.
     * Stores runtime information for each player.
     */
    private final HashMap<UUID, PlayerData> playerData;

    /**
     * Maps flow names to their corresponding FlowManager instances.
     * Manages all loaded logic flows in the system.
     */
    private final HashMap<String, FlowManager> logicFlows;
    
    /**
     * History of executed logic flows.
     * Maintains a chronological record of all flow execution contexts.
     */
    private final List<ExecutionContext> executeHistory;
    
    /**
     * List of scheduled tasks to be executed.
     * Tasks are ticked each server tick and removed when finished.
     */
    private final List<ScheduledTask> scheduledTasks;

    /**
     * List of scheduled tasks that are submitted during the ticking of scheduled tasks.
     * This is used to avoid {@link ConcurrentModificationException} when a scheduled task
     * submits another task while the main task list is being iterated.
     * Pending tasks are flushed into {@code scheduledTasks} at the end of each tick.
     */
    private final List<ScheduledTask> pendingScheduledTasks;

    /**
     * Flag to indicate whether the server is currently ticking scheduled tasks.
     */
    private boolean isTickingScheduledTasks;
    
    /**
     * Collection of UUIDs for entities that have killed a player before.
     * Used to track hostile entities in various game logic.
     */
    private final Collection<UUID> killerEntities;
    
    /**
     * Maps source names to their corresponding GPT request data.
     * Thread-safe for concurrent access from async tasks.
     */
    private final ConcurrentHashMap<String, GptData> gptRequestStatus;
    
    /**
     * Queue of async tasks pending execution or completion processing.
     * Thread-safe for concurrent task submission and completion.
     */
    private final Queue<AsyncTaskExecutor> asyncTasks;

    /**
     * Lock object used to synchronize access to {@code asyncTasks} during task submission.
     * Ensures thread-safe addition of tasks from off-server threads.
     */
    private final Object asyncTaskLock;
    
    /**
     * Thread pool for executing async tasks.
     * Must be shut down gracefully before server shutdown.
     */
    private final ExecutorService asyncTaskPool;

    /**
     * Current server tick count.
     * Increments by 1 each server tick.
     */
    private int serverTick;

    /**
     * Currently active EntityDensityCalculator, if any.
     * Used for calculating entity density in the world.
     */
    @Nullable
    private EntityDensityCalculator activeDensityCalculator;

    /**
     * The server tick when the number of loaded entities was last checked.
     * Used to avoid checking entity count every tick.
     */
    private int lastCheckEntityTick;

    /**
     * The server tick when entity density was last checked.
     * Used to avoid checking entity density every tick.
     */
    private int lastCheckDensityTick;

    /**
     * Constructs a new ServerData instance for the given server.
     * Initializes all collections, the async task pool, and sets the server tick to 0.
     * 
     * @param server the Minecraft server instance
     */
    public ServerData(MinecraftServer server) {
        this.server = server;
        playerData = new HashMap<>();
        logicFlows = new HashMap<>();
        executeHistory = new ArrayList<>();
        scheduledTasks = new ArrayList<>();
        pendingScheduledTasks = new ArrayList<>();
        isTickingScheduledTasks = false;
        killerEntities = new HashSet<>();
        gptRequestStatus = new ConcurrentHashMap<>();
        asyncTasks = new ConcurrentLinkedQueue<>();
        asyncTaskLock = new Object();
        asyncTaskPool = Executors.newFixedThreadPool(2);
        serverTick = 0;
        activeDensityCalculator = null;
        lastCheckEntityTick = 0;
        lastCheckDensityTick = 0;
    }

    /**
     * Advances the server state by one tick.
     * <p>
     * On each call this method:
     * <ol>
     *   <li>Ticks all active {@link ScheduledTask}s, removes finished ones, and flushes pending tasks.</li>
     *   <li>Polls completed {@link AsyncTaskExecutor}s and invokes their post-completion callbacks on the server thread.</li>
     *   <li>Increments the internal {@code serverTick} counter.</li>
     * </ol>
     * </p>
     */
    public void tick() {
        try {
            isTickingScheduledTasks = true;
            scheduledTasks.forEach(ScheduledTask::tick);
            scheduledTasks.removeIf(ScheduledTask::isFinished);
            scheduledTasks.addAll(pendingScheduledTasks);
            pendingScheduledTasks.clear();
            isTickingScheduledTasks = false;
        } catch (ConcurrentModificationException e) {
            Util.LOGGER.error("FMinecraftMod: Concurrent modification detected while ticking scheduled tasks. This can be caused if a scheduled task submits another task or modifies the scheduledTasks list during its execution. To avoid this, ensure that tasks do not submit new tasks or modify the scheduledTasks list while being ticked.", e);
            List<String> tasksInfo = new ArrayList<>();
            for (ScheduledTask task : scheduledTasks) {
                tasksInfo.add(task.toString());
            }
            Util.LOGGER.error("FMinecraftMod: Current scheduled tasks at the time of exception: " + String.join(", ", tasksInfo));
            scheduledTasks.clear();
            pendingScheduledTasks.clear();
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Exception occurred while ticking scheduled tasks. This should not happen and may indicate a bug in a scheduled task. To debug this, check the stack trace for the source of the exception and ensure that all scheduled tasks are implemented correctly.", e);
            scheduledTasks.clear();
            pendingScheduledTasks.clear();
        }

        Iterator<AsyncTaskExecutor> iterator = asyncTasks.iterator();
        while (iterator.hasNext()) {
            AsyncTaskExecutor task = iterator.next();
            if (task.isAsyncFinished()) {
                task.runAfterCompletion();
                iterator.remove();
            }
        }

        serverTick++;
    }

    /**
     * Retrieves the PlayerData associated with the given ServerPlayerEntity.
     * If no PlayerData exists for the player, a new PlayerData instance is created,
     * associated with the player's UUID, and returned.
     *
     * @param player the ServerPlayerEntity for which to retrieve the PlayerData
     * @return the PlayerData associated with the given player, never null
     */
    @Nonnull
    public PlayerData getPlayerData(@Nonnull ServerPlayer player) {
        PlayerData data = playerData.get(player.getUUID());
        if (data == null) {
            Util.LOGGER.info("FMinecraftMod: Creating new PlayerData for player " + player.getName().getString());
            data = new PlayerData(player, this);
            playerData.put(player.getUUID(), data);
        }
        return data;
    }

    /**
     * Submits a {@link ScheduledTask} for execution.
     * <p>
     * If the server is currently ticking scheduled tasks, the task is queued in {@code pendingScheduledTasks}
     * and will be added to the main list at the end of the current tick to avoid
     * {@link ConcurrentModificationException}. Duplicate or already-finished tasks are silently rejected
     * with a warning log.
     * </p>
     * <b>Must be called on the server thread.</b>
     *
     * @param task the task to submit; must not be null
     */
    public void submitScheduledTask(@Nonnull ScheduledTask task) {
        if (scheduledTasks.contains(task) || pendingScheduledTasks.contains(task) || task.isFinished()) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to submit a duplicate or finished scheduled task: " + task.toString());
            return;
        }
        if (isTickingScheduledTasks) {
            pendingScheduledTasks.add(task);
        } else {
            scheduledTasks.add(task);
        }
    }

    /**
     * Submits an {@link AsyncTaskExecutor} for asynchronous execution on the internal thread pool.
     * The executor's {@code executeAsyncTask()} runs off the server thread; once it completes,
     * {@code runAfterCompletion()} is invoked on the server thread during the next {@link #tick()}.
     * <p>
     * <b>This is the only thread-safe method in {@link ServerData}.</b>
     * Do NOT call any other method of this class from off the server thread, including
     * {@link #submitScheduledTask(ScheduledTask)}.
     * </p>
     * <p>
     * If you need to submit a scheduled task from an off-thread context, wrap your logic inside
     * an {@link AsyncTaskExecutor} with an empty {@code executeAsyncTask()} and place the
     * {@link #submitScheduledTask(ScheduledTask)} call inside {@code taskAfterCompletion()},
     * which is guaranteed to run on the server thread.
     * </p>
     * Duplicate or already-finished tasks are silently rejected with a warning log.
     *
     * @param task the async task to submit; must not be null
     */
    public void submitAsyncTask(@Nonnull AsyncTaskExecutor task) {
        if (task.isAsyncFinished()) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to submit an already finished async task.");
            return;
        }

        synchronized (asyncTaskLock) {
            if (asyncTasks.contains(task)) {
                Util.LOGGER.warn("FMinecraftMod: Attempted to submit a duplicate async task.");
                return;
            }
            asyncTasks.add(task);
            asyncTaskPool.submit(task);
        }
    }

    /**
     * Marks an entity as a "killer" by adding its UUID to the killer entities collection.
     * Does nothing if the entity is null.
     * 
     * @param entity the entity to mark as a killer, may be null
     */
    public void addKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return;
        }
        killerEntities.add(entity.getUUID());
    }

    /**
     * Checks if an entity is marked as a "killer".
     * Returns false if the entity is null.
     * 
     * @param entity the entity to check, may be null
     * @return true if the entity is a killer, false otherwise
     */
    public boolean isKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.contains(entity.getUUID());
    }

    /**
     * Removes an entity from the killer entities collection.
     * Returns false if the entity is null.
     * 
     * @param entity the entity to remove, may be null
     * @return true if the entity was removed, false otherwise
     */
    public boolean removeKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.remove(entity.getUUID());
    }

    /**
     * Retrieves the GptData associated with the given source.
     * If no GptData exists for the provided source, a new GptData instance
     * is created, stored, and returned. This operation is thread-safe.
     *
     * @param source the name of the source for which to retrieve the GptData
     * @return the GptData associated with the given source, never null
     */
    @Nonnull
    public GptData getGptData(@Nonnull String source) {
        return gptRequestStatus.computeIfAbsent(source, k -> new GptData());
    }

    /**
     * Returns a list of ExecutionContext instances from the execution history
     * whose associated flow name exactly matches the provided name.
     * If no matching contexts are found, an empty list is returned.
     * 
     * @param name the flow name to match against each ExecutionContext's flow.name
     * @return a list of matching ExecutionContext objects, never null
     */
    public List<ExecutionContext> gatherHistoryByName(String name) {
        List<ExecutionContext> result = new ArrayList<>();
        for (ExecutionContext context : executeHistory) {
            if (context.getFlow().getName().equals(name)) {
                result.add(context);
            }
        }
        return result;
    }

    /**
     * Gathers all FlowManager instances whose first node type matches the given type.
     * Optionally filters to include only enabled flows.
     * 
     * @param type the node type to match
     * @param enabledOnly if true, only include enabled flows; if false, include all flows
     * @return a list of matching FlowManager instances, never null
     */
    public List<FlowManager> gatherFlowByFirstNodeType(String type, boolean enabledOnly) {
        List<FlowManager> result = new ArrayList<>();
        for (FlowManager manager : logicFlows.values()) {
            FlowNode firstNode = manager.getFlow().getFirstNode();
            if (firstNode != null && firstNode.getType().equals(type)) {
                if (!enabledOnly || manager.isEnabled()) {
                    result.add(manager);
                }
            }
        }
        return result;
    }

    /**
     * Shuts down the async task pool gracefully.
     * Should be called before server shutdown to properly terminate all async threads.
     */
    public void shutdownAsyncTaskPool() {
        asyncTaskPool.shutdown();
    }

    /**
     * Returns the Minecraft server instance associated with this data.
     *
     * @return the {@link MinecraftServer}
     */
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Returns the mutable map of logic flows, keyed by flow name.
     *
     * @return the {@link HashMap} of flow name to {@link FlowManager}
     */
    public HashMap<String, FlowManager> getLogicFlows() {
        return logicFlows;
    }

    /**
     * Returns an unmodifiable view of the execution history.
     *
     * @return an unmodifiable {@link List} of {@link ExecutionContext} instances
     */
    public List<ExecutionContext> getExecuteHistory() {
        return Collections.unmodifiableList(executeHistory);
    }

    /**
     * Appends an {@link ExecutionContext} to the execution history and trims the history
     * to the specified maximum size by removing the oldest entries.
     *
     * @param context      the execution context to add; must not be null
     * @param maxHistorySize the maximum number of entries to retain
     */
    public void addExecuteHistory(ExecutionContext context, int maxHistorySize) {
        executeHistory.add(context);
        while (executeHistory.size() > maxHistorySize) {
            executeHistory.remove(0);
        }
    }

    /**
     * Returns an unmodifiable view of the currently active scheduled tasks.
     *
     * @return an unmodifiable {@link List} of {@link ScheduledTask} instances
     */
    public List<ScheduledTask> getScheduledTasks() {
        return Collections.unmodifiableList(this.scheduledTasks);
    }

    /**
     * Returns the currently active {@link EntityDensityCalculator}, or {@code null} if none is running.
     *
     * @return the active calculator, or {@code null}
     */
    @Nullable
    public EntityDensityCalculator getActiveDensityCalculator() {
        return activeDensityCalculator;
    }

    /**
     * Attempts to set the active {@link EntityDensityCalculator} and submit it for async execution.
     * Succeeds only when no calculator is currently running (i.e., none exists or the previous one
     * has completed its post-completion callback).
     *
     * @param calculator the calculator to activate; must not be null
     * @return {@code true} if the calculator was successfully set and submitted; {@code false} otherwise
     */
    public boolean trySetActiveDensityCalculator(@Nonnull EntityDensityCalculator calculator) {
        if (activeDensityCalculator == null) {
            activeDensityCalculator = calculator;
            submitAsyncTask(calculator);
            return true;
        } else if (activeDensityCalculator.isAfterCompletionExecuted()) {
            activeDensityCalculator = calculator;
            submitAsyncTask(calculator);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempts to clear the active {@link EntityDensityCalculator}.
     * Succeeds only when there is no active calculator or the existing one has already
     * completed its post-completion callback.
     *
     * @return {@code true} if the calculator was successfully cleared; {@code false} if it is still running
     */
    public boolean tryRemoveActiveDensityCalculator() {
        if (activeDensityCalculator == null) {
            return true;
        } else if (activeDensityCalculator.isAfterCompletionExecuted()) {
            activeDensityCalculator = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates {@code lastCheckEntityTick} to the current server tick and returns the previous value.
     *
     * @return the tick value before the update
     */
    public int setLastCheckEntityTick() {
        int lastTick = lastCheckEntityTick;
        lastCheckEntityTick = serverTick;
        return lastTick;
    }

    /**
     * Returns the server tick at which the number of loaded entities was last checked.
     *
     * @return the last entity-check tick
     */
    public int getLastCheckEntityTick() {
        return lastCheckEntityTick;
    }

    /**
     * Returns the number of ticks elapsed since the last entity count check.
     *
     * @return ticks passed since last entity check
     */
    public int getCheckEntityTickPassed() {
        return serverTick - lastCheckEntityTick;
    }

    /**
     * Updates {@code lastCheckDensityTick} to the current server tick and returns the previous value.
     *
     * @return the tick value before the update
     */
    public int setLastCheckDensityTick() {
        int lastTick = lastCheckDensityTick;
        lastCheckDensityTick = serverTick;
        return lastTick;
    }

    /**
     * Returns the server tick at which entity density was last checked.
     *
     * @return the last density-check tick
     */
    public int getLastCheckDensityTick() {
        return lastCheckDensityTick;
    }

    /**
     * Returns the number of ticks elapsed since the last entity density check.
     *
     * @return ticks passed since last density check
     */
    public int getCheckDensityTickPassed() {
        return serverTick - lastCheckDensityTick;
    }

    /**
     * Returns the current server tick count.
     * 
     * @return the current server tick
     */
    public int getServerTick() {
        return serverTick;
    }

    /**
     * Calculates how many ticks have passed since the given tick.
     * 
     * @param tick the previous tick to compare against
     * @return the number of ticks that have passed
     */
    public int getTickPassed(int tick) {
        return serverTick - tick;
    }
}
