/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.FightMessage;
import com.ykn.fmod.server.base.schedule.PlayerHurtMessage;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

/**
 * The damage messages: boss fight prompts and monster-surround warnings.
 *
 * <p>This class is the existing feature only; the rule and flow events of the same moment are
 * dispatched by {@link GameEventHandler}, which calls this method first.
 */
public final class LivingEntityDamage {

    private LivingEntityDamage() {
    }

    /**
     * Sends the damage-related messages for one hit.
     *
     * @param entity       the entity that took damage
     * @param damageSource the damage source
     * @param amount       the damage amount
     * @return {@code false} when the handler was skipped (no server or no damage source), in which
     *         case there is nothing to report and the event is not dispatched either
     * @see com.ykn.fmod.server.mixin.GameEventMixins.LivingEntityDamage
     */
    public static boolean onDamage(LivingEntity entity, DamageSource damageSource, float amount) {
        if (entity.getServer() == null) {
            return false;
        }
        if (damageSource == null) {
            return false;
        }

        MinecraftServer server = entity.getServer();
        Entity attacker = damageSource.getDirectEntity();
        ServerData serverData = Util.getServerData(server);

        // Send boss fight message
        if (attacker != null && attacker.isAlwaysTicking() && attacker instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) attacker;
            PlayerData data = serverData.getPlayerData(player);
            if (entity.getMaxHealth() > Util.getServerConfig().getBossMaxHealthThreshold() && data.getBossFightTickPassed() > Util.getServerConfig().getBossFightInterval() && amount > 0) {
                data.setLastBossFightTick();
                serverData.submitScheduledTask(new FightMessage(player, entity));
            }
        }

        // Send monster surround message
        if (entity.isAlwaysTicking() && entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            PlayerData data = serverData.getPlayerData(player);
            if (attacker != null && attacker instanceof Monster && data.getMonsterSurroundTickPassed() > Util.getServerConfig().getMonsterSurroundInterval() && amount > 0) {
                double distance = Util.getServerConfig().getMonsterDistanceThreshold();
                AABB box = new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance);
                List<Entity> nearestEntities = player.level().getEntitiesOfClass(Entity.class, box, monster -> (monster != null && (monster instanceof Monster)));
                if (nearestEntities.size() >= Util.getServerConfig().getMonsterNumberThreshold() && player.getHealth() > 0) {
                    Component playerName = player.getDisplayName();
                    Component entityName = attacker.getDisplayName();
                    String entityNumber = Integer.toString(nearestEntities.size());
                    Map.Entry<Entity, Integer> dominantMonster = Util.getDominantEntities(nearestEntities);
                    if (dominantMonster == null) {
                        // Unlikely to happen
                        Util.LOGGER.warn("FMinecraftMod: Got unexpected null value when searching for dominant monster.");
                    } else {
                        Component dominantMonsterName = dominantMonster.getKey().getDisplayName();
                        String dominantMonsterNumber = Integer.toString(dominantMonster.getValue());
                        Component mainText = Util.parseTranslatableText("fmod.message.monsterattack.main", playerName, entityName, entityNumber, dominantMonsterNumber, dominantMonsterName);
                        Component otherText = Util.parseTranslatableText("fmod.message.monsterattack.other", playerName, entityNumber);
                        Util.getServerConfig().getMonsterSurroundMessage().postMessage(player, mainText, otherText);
                    }
                    data.setLastMonsterSurroundTick();
                }
            }
            serverData.submitScheduledTask(new PlayerHurtMessage(player, player.getHealth()));
        }

        return true;
    }
}
