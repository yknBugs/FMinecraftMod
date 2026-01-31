/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;

public class EntityDeath {

    private final LivingEntity livingEntity;
    private final DamageSource damageSource;

    public EntityDeath(LivingEntity livingEntity, DamageSource damageSource) {
        this.livingEntity = livingEntity;
        this.damageSource = damageSource;
    }

    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    /**
     * This method is called when an entity dies.
     * @see com.ykn.fmod.mixin.EntityDeathMixin
     */
    public void onEntityDeath() {
        if (livingEntity.getServer() == null) {
            return;
        }

        ServerData data = Util.getServerData(livingEntity.getServer());

        // Broadcast death message, each type of message can be broadcasted only once
        HashMap<MessageLocation, Boolean> isAlreadyBroadcasted = new HashMap<>();
        for (MessageLocation type : MessageLocation.values()) {
            isAlreadyBroadcasted.put(type, false);
        }

        if (this.livingEntity.hasCustomName()) {
            MessageLocation type = Util.serverConfig.getNamedEntityDeathMessage();
            Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
            isAlreadyBroadcasted.put(type, true);
        }
        if (this.livingEntity.getMaxHealth() > Util.serverConfig.getBossMaxHpThreshold()) {
            MessageLocation type = Util.serverConfig.getBossDeathMessage();
            if (!isAlreadyBroadcasted.get(type)) {
                Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
                isAlreadyBroadcasted.put(type, true);
            }
        }
        if (Util.getServerData(livingEntity.getServer()).isKillerEntity(livingEntity)) {
            MessageLocation type = Util.serverConfig.getKillerEntityDeathMessage();
            if (!isAlreadyBroadcasted.get(type)) {
                Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
                isAlreadyBroadcasted.put(type, true);
            }
            Util.getServerData(livingEntity.getServer()).removeKillerEntity(livingEntity);
        }

        // Normal entity death message
        MessageLocation type = Util.serverConfig.getEntityDeathMessage();
        if (livingEntity instanceof PassiveEntity) {
            type = Util.serverConfig.getPassiveDeathMessage();
        } else if (livingEntity instanceof HostileEntity) {
            type = Util.serverConfig.getHostileDeathMessage();
        }
        // broadcast only if not already broadcasted
        if (!isAlreadyBroadcasted.get(type)) {
            Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
            isAlreadyBroadcasted.put(type, true);
        }

        // Trigger the event for LogicFlow
        List<FlowManager> deathEventFlow = data.gatherFlowByFirstNodeType("EntityDeathEventNode", true);
        for (FlowManager flow : deathEventFlow) {
            List<Object> eventOutput = new ArrayList<>();
            eventOutput.add(this.livingEntity);
            eventOutput.add(this.damageSource.getType());
            eventOutput.add(this.damageSource.getAttacker());
            eventOutput.add(this.damageSource.getSource());
            eventOutput.add(this.damageSource.getPosition());
            eventOutput.add(this.livingEntity.getDamageTracker().getDeathMessage());
            flow.execute(data, eventOutput, null);
        }
    }
}
