/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.network.ServerPlayerEntity;
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
        if (lastHealth - health >= Util.getServerConfig().getPlayerHurtThreshold() * player.getMaxHealth()) {
            double damage = lastHealth - health;
            Text playerName = player.getDisplayName();
            Text mainText = Util.parseTranslatableText("fmod.message.playerhurt.main", playerName, String.format("%.1f", damage), String.format("%.1f", lastHealth), String.format("%.1f", health));
            Text otherText = Util.parseTranslatableText("fmod.message.playerhurt.other", playerName, String.format("%.1f", damage));
            Util.getServerConfig().getPlayerHurtMessage().postMessage(player, mainText, otherText);
        }
    }

    @Override
    public boolean shouldCancel() {
        if (player == null || player.isDisconnected() || player.isRemoved() || player.isSpectator() || player.isCreative() || player.getHealth() <= 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "PlayerHurtMessage{player='" + player.getDisplayName().getString() + "', lastHealth=" + lastHealth + "}";
    }
}
