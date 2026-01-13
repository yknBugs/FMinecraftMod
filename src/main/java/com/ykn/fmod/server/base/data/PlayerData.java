package com.ykn.fmod.server.base.data;

import net.minecraft.resources.ResourceLocation;

public class PlayerData {

    public int afkTicks;
    public float lastPitch;
    public float lastYaw;
    public ResourceLocation lastBiomeId;

    public int lastBossFightTick;
    public int lastMonsterSurroundTick;

    public PlayerData() {
        this.afkTicks = 0;
        this.lastPitch = 0;
        this.lastYaw = 0;
        this.lastBiomeId = null;

        this.lastBossFightTick = 0;
        this.lastMonsterSurroundTick = 0;
    }
}
