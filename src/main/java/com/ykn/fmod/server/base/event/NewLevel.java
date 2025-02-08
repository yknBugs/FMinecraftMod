package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class NewLevel {

    private MinecraftServer server;
    private ServerWorld world;

    public NewLevel(MinecraftServer server, ServerWorld world) {
        this.server = server;
        this.world = world;
    }

    /**
     * This method is called when a world is loaded.
     */
    public void onNewLevel() {
        // Just make sure the world data is initialized, so we ignore the return value
        if (server != null && world != null) {
            Util.getServerData(server);
        }
    }

}
