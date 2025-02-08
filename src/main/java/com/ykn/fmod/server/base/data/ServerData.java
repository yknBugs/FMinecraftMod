package com.ykn.fmod.server.base.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerData {

    public HashMap<ServerPlayerEntity, PlayerData> playerData;

    public Collection<LivingEntity> killerEntities;

    public ServerData() {
        playerData = new HashMap<>();
        killerEntities = new HashSet<>();
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
}
