/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.EntityDeath;
import com.ykn.fmod.server.base.util.Util;

@Mixin(LivingEntity.class)
public class EntityDeathMixin {
	@Inject(
		method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", 
		at = @At(
			value = "INVOKE", 
			target = "Lnet/minecraft/entity/LivingEntity;getPrimeAdversary()Lnet/minecraft/entity/LivingEntity;", 
			shift = At.Shift.AFTER
		)
	)
	private void onDeath(final DamageSource damageSource, CallbackInfo info) {
		try {
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity.isRemoved() == false && entity.isPlayer() == false) {
				EntityDeath entityDeath = new EntityDeath(entity, damageSource);
				entityDeath.onEntityDeath();
			}
		} catch (Exception e) {
			Util.LOGGER.error("FMinecraftMod: Caught exception from EntityDeathEvent.", e);
		}
	}
}