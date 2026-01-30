/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.FightMessage;
import com.ykn.fmod.server.base.schedule.PlayerHurtMessage;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

public class LivingEntityDamage {

    private final LivingEntity entity;
    private final DamageSource damageSource;
    private final float amount;

    public LivingEntityDamage(LivingEntity entity, DamageSource damageSource, float amount) {
        this.entity = entity;
        this.damageSource = damageSource;
        this.amount = amount;
    }

    /**
     * This method is called when a living entity is damaged.
     * @see com.ykn.fmod.mixin.LivingEntityDamageMixin
     */
    public void onDamage() {
        if (entity.getServer() == null) {
            return;
        }
        if (damageSource == null) {
            return;
        }

        MinecraftServer server = entity.getServer();
        Entity attacker = damageSource.getDirectEntity();
        ServerData serverData = Util.getServerData(server);

        // Send boss fight message
        if (attacker != null && attacker.isAlwaysTicking() && attacker instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) attacker;
            PlayerData data = serverData.getPlayerData(player);
            if (entity.getMaxHealth() > Util.serverConfig.getBossMaxHpThreshold() && serverData.getTickPassed(data.lastBossFightTick) > Util.serverConfig.getBossFightInterval() && amount > 0) {
                data.lastBossFightTick = serverData.getServerTick();
                serverData.submitScheduledTask(new FightMessage(player, entity));
            }
        }

        // Send monster surround message
        if (entity.isAlwaysTicking() && entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            PlayerData data = serverData.getPlayerData(player);
            if (attacker != null && attacker instanceof Monster && serverData.getTickPassed(data.lastMonsterSurroundTick) > Util.serverConfig.getMonsterSurroundInterval() && amount > 0) {
                double distance = Util.serverConfig.getMonsterDistanceThreshold();
                AABB box = new AABB(player.getX() - distance, player.getY() - distance, player.getZ() - distance, player.getX() + distance, player.getY() + distance, player.getZ() + distance);
                List<Entity> nearestEntities = player.level().getEntitiesOfClass(Entity.class, box, monster -> (monster != null && (monster instanceof Monster)));
                if (nearestEntities.size() >= Util.serverConfig.getMonsterNumberThreshold() && player.getHealth() > 0) {
                    Component playerName = player.getDisplayName();
                    Component entityName = attacker.getDisplayName();
                    MutableComponent text = Util.parseTranslatableText("fmod.message.monsterattack", playerName, entityName, Integer.toString(nearestEntities.size()));
                    Util.postMessage(player, Util.serverConfig.getMonsterSurroundMessageReceiver(), Util.serverConfig.getMonsterSurroundMessageLocation(), text);
                    data.lastMonsterSurroundTick = serverData.getServerTick();
                }
            }
            serverData.submitScheduledTask(new PlayerHurtMessage(player, player.getHealth()));
        }

        // Trigger flow events
        List<FlowManager> damageEventFlow = serverData.gatherFlowByFirstNodeType("EntityDamageEventNode", true);
        for (FlowManager flow : damageEventFlow) {
            List<Object> eventOutput = new ArrayList<>();
            eventOutput.add(this.entity);
            eventOutput.add((double)this.amount);
            eventOutput.add(this.damageSource.type());
            eventOutput.add(this.damageSource.getEntity());
            eventOutput.add(this.damageSource.getDirectEntity());
            eventOutput.add(this.damageSource.getSourcePosition());
            flow.execute(serverData, eventOutput, null);
        }
    }

}
