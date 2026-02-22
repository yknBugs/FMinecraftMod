/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        HashMap<MessageType.Location, Boolean> isMainBroadcasted = new HashMap<>();
        HashMap<MessageType.Location, Boolean> isOtherBroadcasted = new HashMap<>();
        for (MessageType.Location type : MessageType.Location.values()) {
            isMainBroadcasted.put(type, false);
            isOtherBroadcasted.put(type, false);
        }

        Text mainTextCoord = Util.parseCoordText(livingEntity).formatted(Formatting.GRAY);
        Text mainTextDeath = livingEntity.getDamageTracker().getDeathMessage();
        Text mainText = Text.empty().append(mainTextCoord).append(" ").append(mainTextDeath);
        Text otherText = livingEntity.getDamageTracker().getDeathMessage();

        if (this.livingEntity.hasCustomName()) {
            ServerMessageType type = Util.serverConfig.getNamedEntityDeathMessage();
            type.postMessage(livingEntity.getServer(), mainText, otherText);
            isMainBroadcasted.put(type.mainPlayerLocation, true);
            isOtherBroadcasted.put(type.otherPlayerLocation, true);
        }
        if (this.livingEntity.getMaxHealth() > Util.serverConfig.getBossMaxHealthThreshold()) {
            ServerMessageType type = Util.serverConfig.getBossDeathMessage();
            if (!isMainBroadcasted.get(type.mainPlayerLocation)) {
                type.updateOther(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isMainBroadcasted.put(type.mainPlayerLocation, true);
            }
            if (!isOtherBroadcasted.get(type.otherPlayerLocation)) {
                type.updateMain(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isOtherBroadcasted.put(type.otherPlayerLocation, true);
            }
        }
        if (Util.getServerData(livingEntity.getServer()).isKillerEntity(livingEntity)) {
            ServerMessageType type = Util.serverConfig.getKillerDeathMessage();
            if (!isMainBroadcasted.get(type.mainPlayerLocation)) {
                type.updateOther(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isMainBroadcasted.put(type.mainPlayerLocation, true);
            }
            if (!isOtherBroadcasted.get(type.otherPlayerLocation)) {
                type.updateMain(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isOtherBroadcasted.put(type.otherPlayerLocation, true);
            }
            Util.getServerData(livingEntity.getServer()).removeKillerEntity(livingEntity);
        }

        // Normal entity death message
        ServerMessageType type = Util.serverConfig.getEntityDeathMessage();
        if (livingEntity instanceof PassiveEntity) {
            type = Util.serverConfig.getPassiveDeathMessage();
        } else if (livingEntity instanceof HostileEntity) {
            type = Util.serverConfig.getHostileDeathMessage();
        }
        // broadcast only if not already broadcasted
        if (!isMainBroadcasted.get(type.mainPlayerLocation)) {
            type.updateOther(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
            isMainBroadcasted.put(type.mainPlayerLocation, true);
        }
        if (!isOtherBroadcasted.get(type.otherPlayerLocation)) {
            type.updateMain(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
            isOtherBroadcasted.put(type.otherPlayerLocation, true);
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
