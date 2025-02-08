package com.ykn.fmod.server.base.event;

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

        if (Util.serverConfig.isNamedMobDeathMsg() && this.livingEntity.hasCustomName()) {
            Util.broadcastTextMessage(livingEntity.getServer(), livingEntity.getDamageTracker().getDeathMessage());
        } else if (Util.serverConfig.isBcBossDeathMsg() && this.livingEntity.getMaxHealth() > Util.serverConfig.getBossMaxHpThreshold()) {
            Util.broadcastTextMessage(livingEntity.getServer(), livingEntity.getDamageTracker().getDeathMessage());
        } else if (Util.serverConfig.isKillerEntityDeathMsg() && Util.getServerData(livingEntity.getServer()).isKillerEntity(livingEntity)) {
            Util.broadcastTextMessage(livingEntity.getServer(), livingEntity.getDamageTracker().getDeathMessage());    
        } else if (Util.serverConfig.isEnableEntityDeathMsg()) {
            Util.broadcastActionBarMessage(livingEntity.getServer(), livingEntity.getDamageTracker().getDeathMessage());
        }

        Util.getServerData(livingEntity.getServer()).removeKillerEntity(livingEntity);
    }
}
