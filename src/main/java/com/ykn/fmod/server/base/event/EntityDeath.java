/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.EnumSet;
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
        EnumSet<MessageType.Location> isMainBroadcasted = EnumSet.noneOf(MessageType.Location.class);
        EnumSet<MessageType.Location> isOtherBroadcasted = EnumSet.noneOf(MessageType.Location.class);

        Text mainTextCoord = Util.parseCoordText(livingEntity).formatted(Formatting.GRAY);
        Text mainTextDeath = livingEntity.getDamageTracker().getDeathMessage();
        Text mainText = Text.empty().append(mainTextCoord).append(" ").append(mainTextDeath);
        Text otherText = livingEntity.getDamageTracker().getDeathMessage();

        if (this.livingEntity.hasCustomName()) {
            ServerMessageType type = Util.getServerConfig().getNamedEntityDeathMessage();
            type.postMessage(livingEntity.getServer(), mainText, otherText);
            isMainBroadcasted.add(type.mainPlayerLocation);
            isOtherBroadcasted.add(type.otherPlayerLocation);
        }
        if (this.livingEntity.getMaxHealth() > Util.getServerConfig().getBossMaxHealthThreshold()) {
            ServerMessageType type = Util.getServerConfig().getBossDeathMessage();
            if (!isMainBroadcasted.contains(type.mainPlayerLocation)) {
                type.updateOther(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isMainBroadcasted.add(type.mainPlayerLocation);
            }
            if (!isOtherBroadcasted.contains(type.otherPlayerLocation)) {
                type.updateMain(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isOtherBroadcasted.add(type.otherPlayerLocation);
            }
        }
        if (data.isKillerEntity(livingEntity)) {
            ServerMessageType type = Util.getServerConfig().getKillerDeathMessage();
            if (!isMainBroadcasted.contains(type.mainPlayerLocation)) {
                type.updateOther(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isMainBroadcasted.add(type.mainPlayerLocation);
            }
            if (!isOtherBroadcasted.contains(type.otherPlayerLocation)) {
                type.updateMain(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
                isOtherBroadcasted.add(type.otherPlayerLocation);
            }
            data.removeKillerEntity(livingEntity);
        }

        // Normal entity death message
        ServerMessageType type = Util.getServerConfig().getEntityDeathMessage();
        if (livingEntity instanceof PassiveEntity) {
            type = Util.getServerConfig().getPassiveDeathMessage();
        } else if (livingEntity instanceof HostileEntity) {
            type = Util.getServerConfig().getHostileDeathMessage();
        }
        // broadcast only if not already broadcasted
        if (!isMainBroadcasted.contains(type.mainPlayerLocation)) {
            type.updateOther(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
            isMainBroadcasted.add(type.mainPlayerLocation);
        }
        if (!isOtherBroadcasted.contains(type.otherPlayerLocation)) {
            type.updateMain(ServerMessageType.Location.NONE).postMessage(livingEntity.getServer(), mainText, otherText);
            isOtherBroadcasted.add(type.otherPlayerLocation);
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
