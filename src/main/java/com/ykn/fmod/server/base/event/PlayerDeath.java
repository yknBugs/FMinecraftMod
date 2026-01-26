/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class PlayerDeath {

    private ServerPlayer player;
    private DamageSource damageSource;

    public PlayerDeath(ServerPlayer player, DamageSource damageSource) {
        this.player = player;
        this.damageSource = damageSource;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    /**
     * This method is called when a player dies.
     * @see com.ykn.fmod.server.mixin.PlayerDeathMixin
     */
    public void onPlayerDeath() {
        if (player.getServer() == null) {
            return;
        }

        LivingEntity killer = player.getKillCredit();
        if (killer != null && killer.isAlwaysTicking() == false) {
            Util.getServerData(killer.getServer()).addKillerEntity(killer);
        }

        Component playerName = player.getDisplayName();
        Component deathCoord = Util.parseCoordText(player);

        MutableComponent text = Util.parseTranslatableText("fmod.message.playerdeathcoord", playerName, deathCoord).withStyle(ChatFormatting.RED);
        Util.postMessage(player, Util.serverConfig.getPlayerDeathCoord(), MessageLocation.CHAT, text);
    }

}
