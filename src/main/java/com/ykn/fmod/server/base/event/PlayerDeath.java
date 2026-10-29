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

/**
 * The player death message.
 *
 * <p>This class is the existing feature only; the rule and flow events of the same moment are
 * dispatched by {@link GameEventHandler}, which calls this method first. Players are excluded from
 * the entity death hook, which is why their death reaches the shared death event through this path.
 */
public final class PlayerDeath {

    private PlayerDeath() {
    }

    /**
     * Broadcasts the death message of one player.
     *
     * @param player       the player that died
     * @param damageSource the killing damage source
     * @return {@code false} when the handler was skipped (no server), in which case there is nothing
     *         to report and the event is not dispatched either
     * @see com.ykn.fmod.server.mixin.GameEventMixins.PlayerDeath
     */
    public static boolean onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
        if (player.getServer() == null) {
            return false;
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
        return true;
    }
}
