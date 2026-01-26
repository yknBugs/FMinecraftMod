/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.mixin;

import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ykn.fmod.server.base.event.LivingEntityDamage;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"))
    public void onDamage(final DamageSource damageSource, final float amount, CallbackInfoReturnable<Boolean> info) {
        try {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity.isRemoved() == false) {
                LivingEntityDamage livingEntityDamage = new LivingEntityDamage(entity, damageSource, amount);
                livingEntityDamage.onDamage();
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught exception from LivingEntityDamageEvent.", e);
        }
    }

}
