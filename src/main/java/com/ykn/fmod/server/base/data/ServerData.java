package com.ykn.fmod.server.base.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.schedule.ScheduledTask;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * WARNING: This class is not thread-safe.
 */
public class ServerData {

    public HashMap<ServerPlayerEntity, PlayerData> playerData;

    public ArrayList<ScheduledTask> scheduledTasks;
    public Collection<LivingEntity> killerEntities;
    public HashMap<String, GptData> gptRequestStatus;

    public ServerData() {
        playerData = new HashMap<>();
        scheduledTasks = new ArrayList<>();
        killerEntities = new HashSet<>();
        gptRequestStatus = new HashMap<>();
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
        PlayerData data = playerData.get(player);
        if (data == null) {
            data = new PlayerData();
            playerData.put(player, data);
        }
        return data;
    }

    @NotNull
    public ArrayList<ScheduledTask> getScheduledTasks() {
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
        killerEntities.add(entity);
    }

    public boolean isKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.contains(entity);
    }

    public boolean removeKillerEntity(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return killerEntities.remove(entity);
    }

    /**
     * Retrieves the GptData associated with the given ServerCommandSource.
     * If no GptData is found for the provided source, a new GptData instance
     * is created, stored, and then returned.
     *
     * @param source the name of the CommandSource for which to retrieve the GptData
     * @return the GptData associated with the given ServerCommandSource
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
}
