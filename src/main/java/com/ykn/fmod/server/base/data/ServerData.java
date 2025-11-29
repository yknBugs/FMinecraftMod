package com.ykn.fmod.server.base.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * WARNING: This class is not thread-safe.
 * WARNING: The globalRequestPool must be gracefully shutdown when replacing or resetting ServerData.
 */
public class ServerData {

    public HashMap<UUID, PlayerData> playerData;

    public HashMap<String, FlowManager> logicFlows;
    public List<ExecutionContext> executeHistory;
    public List<ScheduledTask> scheduledTasks;
    public Collection<UUID> killerEntities;
    public HashMap<String, GptData> gptRequestStatus;
    public ExecutorService globalRequestPool;

    private int serverTick;

    public ServerData() {
        playerData = new HashMap<>();
        logicFlows = new HashMap<>();
        executeHistory = new ArrayList<>();
        scheduledTasks = new ArrayList<>();
        killerEntities = new HashSet<>();
        gptRequestStatus = new HashMap<>();
        globalRequestPool = Executors.newFixedThreadPool(8);
        serverTick = 0;
    }

    public void tick() {
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
    @NotNull
    public PlayerData getPlayerData(@NotNull ServerPlayerEntity player) {
        PlayerData data = playerData.get(player.getUuid());
        if (data == null) {
            data = new PlayerData();
            playerData.put(player.getUuid(), data);
        }
        return data;
    }

    @NotNull
    public List<ScheduledTask> getScheduledTasks() {
        return scheduledTasks;
    }

    public void submitScheduledTask(@NotNull ScheduledTask task) {
        if (scheduledTasks.contains(task)) {
            return;
        }
        scheduledTasks.add(task);
    }

    public void addKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return;
        }
        killerEntities.add(entity.getUuid());
    }

    public boolean isKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.contains(entity.getUuid());
    }

    public boolean removeKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.remove(entity.getUuid());
    }

    /**
     * Retrieves the GptData associated with the given source.
     * If no GptData is found for the provided source, a new GptData instance
     * is created, stored, and then returned.
     *
     * @param source the name of the source for which to retrieve the GptData
     * @return the GptData associated with the given source
     */
    @NotNull
    public GptData getGptData(@NotNull String source) {
        GptData data = gptRequestStatus.get(source);
        if (data == null) {
            data = new GptData();
            gptRequestStatus.put(source, data);
        }
        return data;
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

    public int getServerTick() {
        return serverTick;
    }

    public int getTickPassed(int tick) {
        return serverTick - tick;
    }
}
