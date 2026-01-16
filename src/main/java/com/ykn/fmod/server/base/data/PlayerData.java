package com.ykn.fmod.server.base.data;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class PlayerData {

    public int afkTicks;
    public float lastPitch;
    public float lastYaw;
    public ResourceLocation lastBiomeId;
    public ResourceLocation lastDimensionId;

    /**
     * Recent positions for travel detection, oldest first.
     */
    public Deque<Vec3> recentPositions;

    /**
     * The last known "can sleep" status of the player.
     * null: Means we don't know yet. (i.e. the player is in nether or end dimension where sleeping is always impossible).
     * true: The player can sleep (i.e., it's night time in an overworld dimension).
     * false: The player cannot sleep (i.e., it's day time in an overworld dimension).
     */
    @Nullable
    public Boolean lastCanSleep;

    public int lastBossFightTick;
    public int lastMonsterSurroundTick;

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
