/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import org.jetbrains.annotations.NotNull;

import com.ykn.fmod.server.base.util.MessageType;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Delivers a message to the first player that joins the server.
 * <p>
 * Used for feedback that is generated before any player is online, such as the auto-loaded
 * rule/flow counts reported by {@link com.ykn.fmod.server.base.event.NewLevel}. On a singleplayer
 * integrated server the world finishes loading before the local player actually joins it, so the
 * message can't be sent immediately; this task polls each server tick until a player is present
 * and delivers it then, finishing itself afterward.
 */
public class StartupMessageTask extends ScheduledTask {

    private final MinecraftServer server;
    private final Component message;

    public StartupMessageTask(@NotNull MinecraftServer server, @NotNull Component message) {
        super(0, Integer.MAX_VALUE);
        this.server = server;
        this.message = message;
    }

    @Override
    public void onTick() {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        MessageType.sendTextMessage(player, message);
        cancel();
    }

    @Override
    public boolean shouldCancel() {
        return server == null || server.isStopped();
    }

    @Override
    public String toString() {
        return "StartupMessageTask{message='" + message.getString() + "'}";
    }
}
