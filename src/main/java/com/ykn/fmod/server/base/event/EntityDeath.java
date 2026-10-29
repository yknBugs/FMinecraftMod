/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.EnumSet;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

/**
 * The non-player entity death messages.
 *
 * <p>This class is the existing feature only; the rule and flow events of the same moment are
 * dispatched by {@link GameEventHandler}, which calls this method first. Players are not handled
 * here - their death message lives in {@link PlayerDeath}.
 */
public final class EntityDeath {

    private EntityDeath() {
    }

    /**
     * Broadcasts the death message(s) of one dying entity.
     *
     * @param livingEntity the entity that died
     * @param damageSource the killing damage source
     * @return {@code false} when the handler was skipped (no server), in which case there is
     *         nothing to report and the event is not dispatched either
     * @see com.ykn.fmod.server.mixin.GameEventMixins.EntityDeath
     */
    public static boolean onEntityDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (livingEntity.getServer() == null) {
            return false;
        }

        ServerData data = Util.getServerData(livingEntity.getServer());

        // Broadcast death message, each type of message can be broadcasted only once
        EnumSet<MessageType.Location> isMainBroadcasted = EnumSet.noneOf(MessageType.Location.class);
        EnumSet<MessageType.Location> isOtherBroadcasted = EnumSet.noneOf(MessageType.Location.class);

        Component mainTextCoord = Util.parseCoordText(livingEntity).withStyle(ChatFormatting.GRAY);
        Component mainTextDeath = livingEntity.getCombatTracker().getDeathMessage();
        Component mainText = Component.empty().append(mainTextCoord).append(" ").append(mainTextDeath);
        Component otherText = livingEntity.getCombatTracker().getDeathMessage();

        if (livingEntity.hasCustomName()) {
            ServerMessageType type = Util.getServerConfig().getNamedEntityDeathMessage();
            type.postMessage(livingEntity.getServer(), mainText, otherText);
            isMainBroadcasted.add(type.mainPlayerLocation);
            isOtherBroadcasted.add(type.otherPlayerLocation);
        }
        if (livingEntity.getMaxHealth() > Util.getServerConfig().getBossMaxHealthThreshold()) {
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

        return true;
    }
}
