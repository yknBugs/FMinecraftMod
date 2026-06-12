/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.event.EntityDeathEvent;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

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

        Component mainTextCoord = Util.parseCoordText(livingEntity).withStyle(ChatFormatting.GRAY);
        Component mainTextDeath = livingEntity.getCombatTracker().getDeathMessage();
        Component mainText = Component.empty().append(mainTextCoord).append(" ").append(mainTextDeath);
        Component otherText = livingEntity.getCombatTracker().getDeathMessage();

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
        if (livingEntity instanceof AgeableMob) {
            type = Util.getServerConfig().getPassiveDeathMessage();
        } else if (livingEntity instanceof Monster) {
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

        runCustomRule(data);
        runLogicFlow(data);
    }

    private void runCustomRule(ServerData data) {
        List<RuleManager> deathEventRules = data.gatherRuleByEventType(EntityDeathEvent.class, true);
        for (RuleManager rule : deathEventRules) {
            Map<String, Object> eventVariables = new HashMap<>();
            eventVariables.put("entity", this.livingEntity.getUUID());
            eventVariables.put("cause", this.damageSource.getEntity() == null ? null : this.damageSource.getEntity().getUUID());
            eventVariables.put("source", this.damageSource.getDirectEntity() == null ? null : this.damageSource.getDirectEntity().getUUID());
            eventVariables.put("x", this.livingEntity.getX());
            eventVariables.put("y", this.livingEntity.getY());
            eventVariables.put("z", this.livingEntity.getZ());
            eventVariables.put("position", this.livingEntity.position());
            eventVariables.put("dimension", this.livingEntity.level().dimension().location());
            eventVariables.put("biome", this.livingEntity.level().getBiome(this.livingEntity.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null));
            eventVariables.put("message", this.livingEntity.getCombatTracker().getDeathMessage().getString());
            eventVariables.put("name", this.livingEntity.getDisplayName().getString());
            eventVariables.put("__entity__", this.livingEntity);
            eventVariables.put("__cause__", this.damageSource.getEntity());
            eventVariables.put("__source__", this.damageSource.getDirectEntity());
            eventVariables.put("__world__", this.livingEntity.level());
            eventVariables.put("__message__", this.livingEntity.getCombatTracker().getDeathMessage());
            eventVariables.put("__name__", this.livingEntity.getDisplayName());
            rule.trigger(data, eventVariables);
        }
    }

    private void runLogicFlow(ServerData data) {
        List<FlowManager> deathEventFlow = data.gatherFlowByFirstNodeType("EntityDeathEventNode", true);
        for (FlowManager flow : deathEventFlow) {
            List<Object> eventOutput = new ArrayList<>();
            eventOutput.add(this.livingEntity);
            eventOutput.add(this.damageSource.type());
            eventOutput.add(this.damageSource.getEntity());
            eventOutput.add(this.damageSource.getDirectEntity());
            eventOutput.add(this.damageSource.getSourcePosition());
            eventOutput.add(this.livingEntity.getCombatTracker().getDeathMessage());
            flow.execute(data, eventOutput, null);
        }
    }
}
