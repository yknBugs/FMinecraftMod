/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class FightMessage extends ScheduledTask {

    private final ServerPlayerEntity player;
    private final LivingEntity entity;

    public FightMessage(ServerPlayerEntity player, LivingEntity entity) {
        super(1, 0);
        this.player = player;
        this.entity = entity;
    }

    @Override
    public void onTrigger() {
        Text playerName = player.getDisplayName();
        Text entityName = entity.getDisplayName();
        double entityHealth = entity.getHealth();
        MutableText text = Util.parseTranslatableText("fmod.message.bossfight", playerName, entityName, String.format("%.1f", entityHealth));
        Util.postMessage(player, Util.serverConfig.getBossFightMessageReceiver(), Util.serverConfig.getBossFightMessageLocation(), text);
    }

    @Override
    public void onCancel() {
        Util.getPlayerData(player).lastBossFightTick = 0;
    }

    @Override
    public boolean shouldCancel() {
        if (entity == null || player == null || entity.isRemoved() || player.isDisconnected()) {
            return true;
        }
        if (entity.getHealth() <= 0 || player.getHealth() <= 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FightMessage{player='" + player.getEntityName() + "', entity='" + entity.getEntityName() + "'}";
    }
}
