/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class PlayerDeath {

    private final ServerPlayer player;
    private final DamageSource damageSource;

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
        if (killer != null && !killer.isAlwaysTicking() && killer.getServer() != null) {
            Util.getServerData(killer.getServer()).addKillerEntity(killer);
        }

        Component playerName = player.getDisplayName();
        Component deathCoord = Util.parseCoordText(player);
        Component deathBiome = Util.getBiomeText(player);

        MutableComponent mainText = Util.parseTranslatableText("fmod.message.playerdeath.main", playerName, deathCoord).withStyle(ChatFormatting.RED);
        MutableComponent otherText = Util.parseTranslatableText("fmod.message.playerdeath.other", playerName, deathBiome).withStyle(ChatFormatting.RED);

        Util.getServerConfig().getPlayerDeathCoord().postMessage(player, mainText, otherText);
    }

}
