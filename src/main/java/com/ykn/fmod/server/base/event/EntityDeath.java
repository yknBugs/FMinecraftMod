package com.ykn.fmod.server.base.event;

import java.util.HashMap;

import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

public class EntityDeath {

    private LivingEntity livingEntity;
    private DamageSource damageSource;

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

        // Broadcast death message, each type of message can be broadcasted only once
        HashMap<MessageType, Boolean> isAlreadyBroadcasted = new HashMap<>();
        for (MessageType type : MessageType.values()) {
            isAlreadyBroadcasted.put(type, false);
        }

        if (this.livingEntity.hasCustomName()) {
            MessageType type = Util.serverConfig.getNamedEntityDeathMessageType();
            Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
            isAlreadyBroadcasted.put(type, true);
        }
        if (this.livingEntity.getMaxHealth() > Util.serverConfig.getBossMaxHpThreshold()) {
            MessageType type = Util.serverConfig.getBossDeathMessageType();
            if (!isAlreadyBroadcasted.get(type)) {
                Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
                isAlreadyBroadcasted.put(type, true);
            }
        }
        if (Util.getServerData(livingEntity.getServer()).isKillerEntity(livingEntity)) {
            MessageType type = Util.serverConfig.getKillerEntityDeathMessageType();
            if (!isAlreadyBroadcasted.get(type)) {
                Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
                isAlreadyBroadcasted.put(type, true);
            }
            Util.getServerData(livingEntity.getServer()).removeKillerEntity(livingEntity);
        }
        MessageType type = Util.serverConfig.getEntityDeathMessageType();
        if (!isAlreadyBroadcasted.get(type)) {
            Util.broadcastMessage(livingEntity.getServer(), type, livingEntity.getDamageTracker().getDeathMessage());
            isAlreadyBroadcasted.put(type, true);
        }
    }
}
