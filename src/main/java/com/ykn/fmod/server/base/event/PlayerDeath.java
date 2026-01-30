/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerDeath {

    private final ServerPlayerEntity player;
    private final DamageSource damageSource;

    public PlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
        this.player = player;
        this.damageSource = damageSource;
    }

    public ServerPlayerEntity getPlayer() {
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

        LivingEntity killer = player.getPrimeAdversary();
        if (killer != null && killer.isPlayer() == false) {
            Util.getServerData(killer.getServer()).addKillerEntity(killer);
        }

        Text playerName = player.getDisplayName();
        Text deathCoord = Util.parseCoordText(player);

        MutableText text = Util.parseTranslatableText("fmod.message.playerdeathcoord", playerName, deathCoord).formatted(Formatting.RED);
        Util.postMessage(player, Util.serverConfig.getPlayerDeathCoordReceiver(), Util.serverConfig.getPlayerDeathCoordLocation(), text);
    }

}
