package com.ykn.fmod.server.base.data;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

public class PlayerData {

    public int afkTicks;
    public float lastPitch;
    public float lastYaw;
    public ResourceLocation lastBiomeId;

    /**
     * The last known "can sleep" status of the player.
     * null: Means we don't know yet. (i.e. the player is in nether or end dimension where sleeping is always not possible).
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
        this.lastCanSleep = null;
        this.lastBossFightTick = 0;
        this.lastMonsterSurroundTick = 0;
    }
}
