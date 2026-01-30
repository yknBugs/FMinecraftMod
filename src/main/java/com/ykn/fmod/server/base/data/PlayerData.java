/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.data;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Stores runtime data for a player on the server.
 * This data is not persisted and will be lost when the server restarts.
 * Each player has their own PlayerData instance managed by ServerData.
 */
public class PlayerData {

    /**
     * Number of ticks the player has been AFK (away from keyboard).
     * Increments when the player is idle.
     */
    public int afkTicks;
    
    /**
     * The player's view pitch from the last tick.
     * Used to detect player movement and activity.
     */
    public float lastPitch;
    
    /**
     * The player's view yaw from the last tick.
     * Used to detect player movement and activity.
     */
    public float lastYaw;
    
    /**
     * The biome ID the player was in during the last tick.
     * Used for biome change detection and related logic.
     */
    public ResourceLocation lastBiomeId;
    
    /**
     * The dimension ID the player was in during the last tick.
     * Used for dimension change detection and related logic.
     */
    public ResourceLocation lastDimensionId;

    /**
     * Recent positions for travel detection, ordered from oldest to newest.
     * Used to calculate player movement distance and detect travel patterns.
     */
    public Deque<Vec3> recentPositions;

    /**
     * The last known "can sleep" status of the player.
     * <ul>
     * <li>{@code null}: Unknown status (e.g., player is in nether or end where sleeping is impossible)</li>
     * <li>{@code true}: Player can sleep (e.g., night time in overworld dimension)</li>
     * <li>{@code false}: Player cannot sleep (e.g., day time in overworld dimension)</li>
     * </ul>
     */
    @Nullable
    public Boolean lastCanSleep;

    /**
     * The last tick when the player attacked an entity whose max health exceeds the configured threshold.
     * Used to track boss fight engagement.
     */
    public int lastBossFightTick;
    
    /**
     * The last tick when the player was being attacked while surrounded by monsters
     * exceeding the configured threshold count.
     * Used to detect dangerous combat situations.
     */
    public int lastMonsterSurroundTick;

    /**
     * Constructs a new PlayerData instance with default values.
     * All counters are initialized to 0, all objects to null, and collections to empty.
     */
    public PlayerData() {
        this.afkTicks = 0;
        this.lastPitch = 0;
        this.lastYaw = 0;
        this.lastBiomeId = null;
        this.lastDimensionId = null;
        this.lastCanSleep = null;
        this.lastBossFightTick = 0;
        this.lastMonsterSurroundTick = 0;
        this.recentPositions = new ArrayDeque<>();
    }
}
