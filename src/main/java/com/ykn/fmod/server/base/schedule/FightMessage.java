/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public class FightMessage extends ScheduledTask {

    private ServerPlayer player;
    private LivingEntity entity;

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
        MutableComponent text = Util.parseTranslatableText("fmod.message.bossfight", playerName, entityName, String.format("%.1f", entityHealth));
        Util.postMessage(player, Util.serverConfig.getBossFightMessageReceiver(), Util.serverConfig.getBossFightMessageLocation(), text);
    }

    @Override
    public void onCancel() {
        Util.getPlayerData(player).lastBossFightTick = 0;
    }

    @Override
    public boolean shouldCancel() {
        if (entity.isRemoved() || player.hasDisconnected()) {
            return true;
        }
        if (entity.getHealth() <= 0 || player.getHealth() <= 0) {
            return true;
        }
        return false;
    }
}
