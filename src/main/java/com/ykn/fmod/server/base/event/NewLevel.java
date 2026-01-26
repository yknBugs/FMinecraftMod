/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class NewLevel {

    private MinecraftServer server;
    private ServerLevel world;

    public NewLevel(MinecraftServer server, ServerLevel world) {
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
