/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.data;

import java.util.ArrayList;
import java.util.Collection;
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
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.async.AsyncTaskExecutor;
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
    public final MinecraftServer server;
    
    /**
     * Maps player UUIDs to their corresponding PlayerData instances.
     * Stores runtime information for each player.
     */
    private final HashMap<UUID, PlayerData> playerData;

    /**
     * Maps flow names to their corresponding FlowManager instances.
     * Manages all loaded logic flows in the system.
     */
    public final HashMap<String, FlowManager> logicFlows;
    
    /**
     * History of executed logic flows.
     * Maintains a chronological record of all flow execution contexts.
     */
    public final List<ExecutionContext> executeHistory;
    
    /**
     * List of scheduled tasks to be executed.
     * Tasks are ticked each server tick and removed when finished.
     */
    private final List<ScheduledTask> scheduledTasks;
    
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
        killerEntities = new HashSet<>();
        gptRequestStatus = new ConcurrentHashMap<>();
        asyncTasks = new ConcurrentLinkedQueue<>();
        asyncTaskPool = Executors.newCachedThreadPool();
        serverTick = 0;
    }

    /**
     * Called every server tick to update scheduled tasks, async tasks, and server tick counter.
     * Ticks all scheduled tasks, removes finished tasks, processes completed async tasks,
     * and increments the server tick counter.
     */
    public void tick() {
        scheduledTasks.forEach(ScheduledTask::tick);
        scheduledTasks.removeIf(ScheduledTask::isFinished);

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
            LoggerFactory.getLogger(Util.LOGGERNAME).info("FMinecraftMod: Creating new PlayerData for player " + player.getName().getString());
            data = new PlayerData();
            playerData.put(player.getUUID(), data);
        }
        return data;
    }

    /**
     * Returns the list of all currently scheduled tasks.
     * 
     * @return a list of scheduled tasks, never null
     */
    @Nonnull
    public List<ScheduledTask> getScheduledTasks() {
        return scheduledTasks;
    }

    /**
     * Submits a scheduled task for execution.
     * Duplicate tasks and already finished tasks are rejected with a warning.
     * 
     * @param task the task to schedule
     */
    public void submitScheduledTask(@Nonnull ScheduledTask task) {
        if (scheduledTasks.contains(task) || task.isFinished()) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Attempted to submit a duplicate or finished scheduled task.");
            return;
        }
        scheduledTasks.add(task);
    }

    /**
     * Submits an async task for execution in the thread pool.
     * Duplicate tasks and already finished tasks are rejected with a warning.
     * 
     * @param task the async task to execute
     */
    public void submitAsyncTask(@Nonnull AsyncTaskExecutor task) {
        if (asyncTasks.contains(task) || task.isAsyncFinished()) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Attempted to submit a duplicate async task.");
            return;
        }
        asyncTasks.add(task);
        asyncTaskPool.submit(task);
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
            if (context.getFlow().name.equals(name)) {
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
            FlowNode firstNode = manager.flow.getFirstNode();
            if (firstNode != null && firstNode.getType().equals(type)) {
                if (enabledOnly == false || manager.isEnabled) {
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
