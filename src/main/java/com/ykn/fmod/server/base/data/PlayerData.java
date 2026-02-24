/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.data;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import javax.annotation.Nullable;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Stores runtime data for a player on the server.
 * This data is not persisted and will be lost when the server restarts.
 * Each player has their own PlayerData instance managed by ServerData.
 */
public class PlayerData {

    /**
     * Epsilon threshold for detecting changes in the player's facing direction.
     * Differences smaller than this value are considered negligible.
     */
    private static final double FACING_DIRECTION_EPSILON = 0.01;

    /** 
     * The UUID of the player, used for identification and persistence if needed.
     * This is derived from the player entity and should not be modified directly.
     */
    private final UUID playerUuid;

    /**
     * The server data instance that owns and manages this player data.
     */
    private final ServerData serverData;

    /**
     * Number of ticks the player has been AFK (away from keyboard).
     * Increments when the player is idle.
     */
    private int afkTicks;
    
    /**
     * The player's view pitch from the last tick.
     * Used to detect player movement and activity.
     */
    private float lastPitch;
    
    /**
     * The player's view yaw from the last tick.
     * Used to detect player movement and activity.
     */
    private float lastYaw;
    
    /**
     * The biome ID the player was in during the last tick.
     * Used for biome change detection and related logic.
     */
    @Nullable
    private ResourceLocation lastBiomeId;
    
    /**
     * The dimension ID the player was in during the last tick.
     * Used for dimension change detection and related logic.
     */
    private ResourceLocation lastDimensionId;

    /**
     * Recent positions for travel detection, ordered from oldest to newest.
     * Used to calculate player movement distance and detect travel patterns.
     */
    private final Deque<Vec3> recentPositions;

    /**
     * The last known "can sleep" status of the player.
     * <ul>
     * <li>{@code null}: Unknown status (e.g., player is in nether or end where sleeping is impossible)</li>
     * <li>{@code true}: Player can sleep (e.g., night time in overworld dimension)</li>
     * <li>{@code false}: Player cannot sleep (e.g., day time in overworld dimension)</li>
     * </ul>
     */
    @Nullable
    private Boolean lastCanSleep;

    /**
     * The last tick when the player attacked an entity whose max health exceeds the configured threshold.
     * Used to track boss fight engagement.
     */
    private int lastBossFightTick;
    
    /**
     * The last tick when the player was being attacked while surrounded by monsters
     * exceeding the configured threshold count.
     * Used to detect dangerous combat situations.
     */
    private int lastMonsterSurroundTick;

    /**
     * The last tick when the player triggered a long-distance travel message.
     * Used to enforce the message interval and prevent spamming.
     */
    private int lastTravelMessageTick;

    /**
     * Constructs a new PlayerData instance with default values.
     * All counters are initialized to 0, all objects to null, and collections to empty.
     */
    public PlayerData(ServerPlayer player, ServerData serverData) {
        this.playerUuid = player.getUUID();
        this.serverData = serverData;
        this.afkTicks = 0;
        this.lastPitch = 0;
        this.lastYaw = 0;
        this.lastBiomeId = null;
        this.lastDimensionId = null;
        this.lastCanSleep = null;
        this.lastBossFightTick = 0;
        this.lastMonsterSurroundTick = 0;
        this.lastTravelMessageTick = 0;
        this.recentPositions = new ArrayDeque<>();
    }

    /**
     * Updates the cached last-tick state (pitch, yaw, dimension, biome) to the player's current values.
     * Should be called once per tick after all per-tick logic has been processed.
     */
    public void updateLastTickData(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(this.playerUuid)) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to update PlayerData with a player entity that does not match the stored UUID. Skipping.");
            return;
        }
        this.lastPitch = player.getXRot();
        this.lastYaw = player.getYRot();
        this.lastDimensionId = player.level().dimension().location();
        this.lastBiomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
    }

    /**
     * Appends the player's current position to the recent positions history and trims the history
     * to the specified maximum size by removing the oldest entries.
     *
     * @param maxHistorySize the maximum number of positions to retain
     */
    public void updatePositionHistory(ServerPlayer player, int maxHistorySize) {
        if (player == null || !player.getUUID().equals(this.playerUuid)) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to update PlayerData (recentPositions) with a player entity that does not match the stored UUID. Skipping.");
            return;
        }
        this.recentPositions.addLast(player.position());
        while (this.recentPositions.size() > maxHistorySize) {
            this.recentPositions.removeFirst();
        }
    }

    /**
     * Clears the recent positions history and seeds it with the player's current position.
     */
    public void clearPositionHistory(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(this.playerUuid)) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to clear PlayerData position history with a player entity that does not match the stored UUID. Skipping.");
            return;
        }
        this.recentPositions.clear();
        this.recentPositions.addLast(player.position());
    }

    /**
     * Returns whether the player's facing direction (pitch or yaw) has changed since the last tick
     * by more than {@link #FACING_DIRECTION_EPSILON}.
     *
     * @return {@code true} if the facing direction has changed; {@code false} otherwise
     */
    public boolean isFacingDirectionChanged(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(this.playerUuid)) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to check facing direction change with a player entity that does not match the stored UUID. Returning false.");
            return false;
        }
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        return Math.abs(pitch - this.lastPitch) > FACING_DIRECTION_EPSILON || Math.abs(yaw - this.lastYaw) > FACING_DIRECTION_EPSILON;
    }

    /**
     * Returns whether the player's current biome differs from the biome recorded during the last tick.
     *
     * @return {@code true} if the biome has changed; {@code false} otherwise
     */
    public boolean isBiomeChanged(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(this.playerUuid)) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to check biome change with a player entity that does not match the stored UUID. Returning false.");
            return false;
        }
        ResourceLocation currentBiomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        if (this.lastBiomeId == null && currentBiomeId == null) {
            return false; 
        } else if (this.lastBiomeId == null || currentBiomeId == null) {
            return true; 
        } else {
            return !currentBiomeId.equals(this.lastBiomeId);
        }
    }

    /**
     * Returns whether the player's current dimension differs from the dimension recorded during the last tick.
     *
     * @return {@code true} if the dimension has changed; {@code false} otherwise
     */
    public boolean isDimensionChanged(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(this.playerUuid)) {
            Util.LOGGER.warn("FMinecraftMod: Attempted to check dimension change with a player entity that does not match the stored UUID. Returning false.");
            return false;
        }
        ResourceLocation currentDimId = player.level().dimension().location();
        return !currentDimId.equals(this.lastDimensionId);
    }

    /**
     * Increments the AFK tick counter by one.
     * Should be called each tick the player is considered idle.
     */
    public void updateAfkTicks() {
        this.afkTicks++;
    }

    /**
     * Resets the AFK tick counter to zero and returns the previous value.
     *
     * @return the number of AFK ticks accumulated before the reset
     */
    public int resetAfkTicks() {
        int ticks = this.afkTicks;
        this.afkTicks = 0;
        return ticks;
    }

    /**
     * Returns the player entity associated with this data instance.
     *
     * @return the associated {@link ServerPlayer}
     */
    public ServerPlayer getPlayer() {
        return this.serverData.getServer().getPlayerList().getPlayer(playerUuid);
    }

    /**
     * Returns the server data instance that owns this player data.
     *
     * @return the owning {@link ServerData}
     */
    public ServerData getServerData() {
        return this.serverData;
    }

    /**
     * Returns the current AFK tick count.
     *
     * @return the number of ticks the player has been AFK
     */
    public int getAfkTicks() {
        return this.afkTicks;
    }

    /**
     * Returns the biome identifier recorded during the last tick, or {@code null} if unavailable.
     *
     * @return the last known biome {@link ResourceLocation}, or {@code null}
     */
    @Nullable
    public ResourceLocation getLastBiomeId() {
        return this.lastBiomeId;
    }

    /**
     * Returns the dimension identifier recorded during the last tick.
     *
     * @return the last known dimension {@link ResourceLocation}
     */
    public ResourceLocation getLastDimensionId() {
        return this.lastDimensionId;
    }

    /**
     * Returns a snapshot of the recent positions history as an array, ordered from oldest to newest.
     *
     * @return array of recent {@link Vec3} positions
     */
    public Vec3[] getRecentPositions() {
        return this.recentPositions.toArray(new Vec3[0]);
    }

    /**
     * Updates the last known "can sleep" status of the player.
     *
     * @param canSleep {@code true} if the player can sleep, {@code false} if not, or {@code null} if unknown
     */
    public void setLastCanSleep(@Nullable Boolean canSleep) {
        this.lastCanSleep = canSleep;
    }

    /**
     * Returns the last known "can sleep" status of the player.
     *
     * @return {@code true} if the player could sleep, {@code false} if not, or {@code null} if unknown
     */
    @Nullable
    public Boolean getLastCanSleep() {
        return this.lastCanSleep;
    }

    /**
     * Updates {@code lastBossFightTick} to the current server tick and returns the previous value.
     *
     * @return the tick value before the update
     */
    public int setLastBossFightTick() {
        int oldTick = this.lastBossFightTick;
        this.lastBossFightTick = this.serverData.getServerTick();
        return oldTick;
    }

    /**
     * Returns the server tick at which the player last engaged in a boss fight.
     *
     * @return the last boss fight tick
     */
    public int getLastBossFightTick() {
        return this.lastBossFightTick;
    }

    /**
     * Returns the number of ticks elapsed since the last boss fight engagement.
     *
     * @return ticks passed since last boss fight
     */
    public int getBossFightTickPassed() {
        return this.serverData.getTickPassed(this.lastBossFightTick);
    }

    /**
     * Updates {@code lastMonsterSurroundTick} to the current server tick and returns the previous value.
     *
     * @return the tick value before the update
     */
    public int setLastMonsterSurroundTick() {
        int oldTick = this.lastMonsterSurroundTick;
        this.lastMonsterSurroundTick = this.serverData.getServerTick();
        return oldTick;
    }

    /**
     * Returns the server tick at which the player was last detected as being surrounded by monsters.
     *
     * @return the last monster surround tick
     */
    public int getLastMonsterSurroundTick() {
        return this.lastMonsterSurroundTick;
    }

    /**
     * Returns the number of ticks elapsed since the player was last surrounded by monsters.
     *
     * @return ticks passed since last monster surround event
     */
    public int getMonsterSurroundTickPassed() {
        return this.serverData.getTickPassed(this.lastMonsterSurroundTick);
    }

    /**
     * Updates {@code lastTravelMessageTick} to the current server tick and returns the previous value.
     *
     * @return the tick value before the update
     */
    public int setLastTravelMessageTick() {
        int oldTick = this.lastTravelMessageTick;
        this.lastTravelMessageTick = this.serverData.getServerTick();
        return oldTick;
    }

    /**
     * Returns the server tick at which the last long-distance travel message was triggered.
     *
     * @return the last travel message tick
     */
    public int getLastTravelMessageTick() {
        return this.lastTravelMessageTick;
    }

    /**
     * Returns the number of ticks elapsed since the last long-distance travel message was sent.
     *
     * @return ticks passed since last travel message
     */
    public int getTravelMessageTickPassed() {
        return this.serverData.getTickPassed(this.lastTravelMessageTick);
    }
}
