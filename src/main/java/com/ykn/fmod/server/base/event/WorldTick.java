package com.ykn.fmod.server.base.event;

import net.minecraft.server.MinecraftServer;

public class WorldTick {

    private MinecraftServer server;

    public WorldTick(MinecraftServer server) {
        this.server = server;
    }

    /**
     * This method is called every tick.
     */
    public void onWorldTick() {
        
    }

}
