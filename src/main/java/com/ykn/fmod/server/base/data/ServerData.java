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

import com.ykn.fmod.server.base.async.AsyncTaskExecutor;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * WARNING: This class is not thread-safe.
 * WARNING: The asyncTaskPool must be gracefully shutdown when replacing or resetting ServerData.
 */
public class ServerData {

    public MinecraftServer server;
    public HashMap<UUID, PlayerData> playerData;

    public HashMap<String, FlowManager> logicFlows;
    public List<ExecutionContext> executeHistory;
    public List<ScheduledTask> scheduledTasks;
    public Collection<UUID> killerEntities;
    public ConcurrentHashMap<String, GptData> gptRequestStatus;
    private final Queue<AsyncTaskExecutor> asyncTasks;
    private final ExecutorService asyncTaskPool;

    private int serverTick;

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
     * If no PlayerData is found, a new PlayerData instance is created, 
     * associated with the player, and returned.
     *
     * @param player the ServerPlayerEntity for which to retrieve the PlayerData
     * @return the PlayerData associated with the given player, never null
     */
    @Nonnull
    public PlayerData getPlayerData(@Nonnull ServerPlayer player) {
        PlayerData data = playerData.get(player.getUUID());
        if (data == null) {
            data = new PlayerData();
            playerData.put(player.getUUID(), data);
        }
        return data;
    }

    @Nonnull
    public List<ScheduledTask> getScheduledTasks() {
        return scheduledTasks;
    }

    public void submitScheduledTask(@Nonnull ScheduledTask task) {
        if (scheduledTasks.contains(task)) {
            return;
        }
        scheduledTasks.add(task);
    }

    public void submitAsyncTask(@NotNull AsyncTaskExecutor task) {
        asyncTasks.add(task);
        asyncTaskPool.submit(task);
    }

    public void addKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return;
        }
        killerEntities.add(entity.getUUID());
    }

    public boolean isKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.contains(entity.getUUID());
    }

    public boolean removeKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.remove(entity.getUUID());
    }

    /**
     * Retrieves the GptData associated with the given source.
     * If no GptData is found for the provided source, a new GptData instance
     * is created, stored, and then returned.
     *
     * @param source the name of the source for which to retrieve the GptData
     * @return the GptData associated with the given source
     */
    @Nonnull
    public GptData getGptData(@Nonnull String source) {
        return gptRequestStatus.computeIfAbsent(source, k -> new GptData());
    }

    /**
     * Returns a list of ExecutionContext instances from the execution history whose associated flow name exactly matches the provided name.
     * If no matching contexts are found, an empty list is returned.
     * @param name the flow name to match against each ExecutionContext's flow.name
     * @return a list of matching ExecutionContext objects (never null)
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

    public void shutdownAsyncTaskPool() {
        asyncTaskPool.shutdown();
    }

    public int getServerTick() {
        return serverTick;
    }

    public int getTickPassed(int tick) {
        return serverTick - tick;
    }
}
