package com.ykn.fmod.server.base.data;

import net.minecraft.util.Identifier;

public class PlayerData {

    public int afkTicks;
    public float lastPitch;
    public float lastYaw;
    public Identifier lastBiomeId;

    public PlayerData() {
        this.afkTicks = 0;
        this.lastPitch = 0;
        this.lastYaw = 0;
        this.lastBiomeId = null;
    }
    
}
