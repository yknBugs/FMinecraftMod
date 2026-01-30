/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class PlayerHurtMessage extends ScheduledTask {

    private final ServerPlayer player;
    private final double lastHealth;

    public PlayerHurtMessage(ServerPlayer player, double lastHealth) {
        super(1, 0);
        this.player = player;
        this.lastHealth = lastHealth;
    }

    @Override
    public void onTrigger() {
        double health = player.getHealth();
        if (lastHealth - health >= Util.serverConfig.getPlayerHurtThreshold() * player.getMaxHealth()) {
            double damage = lastHealth - health;
            Component playerName = player.getDisplayName();
            MutableComponent text = Util.parseTranslatableText("fmod.message.playerhurt", playerName, String.format("%.1f", damage), String.format("%.1f", lastHealth), String.format("%.1f", health));
            Util.postMessage(player, Util.serverConfig.getPlayerSeriousHurtReceiver(), Util.serverConfig.getPlayerSeriousHurtLocation(), text);
        }
    }

    @Override
    public boolean shouldCancel() {
        if (player == null || player.hasDisconnected() || player.isSpectator() || player.isCreative() || player.getHealth() <= 0) {
            return true;
        }
        return false;
    }
}
