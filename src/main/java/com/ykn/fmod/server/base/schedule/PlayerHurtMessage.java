/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class PlayerHurtMessage extends ScheduledTask {

    private final ServerPlayerEntity player;
    private final double lastHealth;

    public PlayerHurtMessage(ServerPlayerEntity player, double lastHealth) {
        super(1, 0);
        this.player = player;
        this.lastHealth = lastHealth;
    }

    @Override
    public void onTrigger() {
        double health = player.getHealth();
        if (lastHealth - health >= Util.serverConfig.getPlayerHurtThreshold() * player.getMaxHealth()) {
            double damage = lastHealth - health;
            Text playerName = player.getDisplayName();
            MutableText text = Util.parseTranslatableText("fmod.message.playerhurt", playerName, String.format("%.1f", damage), String.format("%.1f", lastHealth), String.format("%.1f", health));
            Util.postMessage(player, Util.serverConfig.getPlayerSeriousHurtReceiver(), Util.serverConfig.getPlayerSeriousHurtLocation(), text);
        }
    }

    @Override
    public boolean shouldCancel() {
        if (player == null || player.isDisconnected() || player.isSpectator() || player.isCreative() || player.getHealth() <= 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "PlayerHurtMessage{player='" + player.getEntityName() + "', lastHealth=" + lastHealth + "}";
    }
}
