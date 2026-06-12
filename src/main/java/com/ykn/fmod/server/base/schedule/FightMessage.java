/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class FightMessage extends ScheduledTask {

    private final ServerPlayer player;
    private final LivingEntity entity;

    public FightMessage(ServerPlayer player, LivingEntity entity) {
        super(1, 0);
        this.player = player;
        this.entity = entity;
    }

    @Override
    public void onTrigger() {
        Component playerName = player.getDisplayName();
        Component entityName = entity.getDisplayName();
        double entityHealth = entity.getHealth();
        Component mainText = Util.parseTranslatableText("fmod.message.bossfight.main", playerName, entityName, String.format("%.1f", entityHealth));
        Component otherText = Util.parseTranslatableText("fmod.message.bossfight.other", playerName, entityName);
        Util.getServerConfig().getBossFightMessage().postMessage(player, mainText, otherText);
    }

    @Override
    public void onCancel() {
        Util.getPlayerData(player).setLastBossFightTick();
    }

    @Override
    public boolean shouldCancel() {
        if (entity == null || player == null || entity.isRemoved() || player.hasDisconnected() || player.getHealth() <= 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FightMessage{player='" + player.getDisplayName().getString() + "', entity='" + entity.getDisplayName().getString() + "'}";
    }
}
