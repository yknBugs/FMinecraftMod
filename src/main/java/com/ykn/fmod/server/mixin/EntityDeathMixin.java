/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.mixin;

// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.EntityDeath;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

// @Mixin(LivingEntity.class)
public class EntityDeathMixin {
	// @Inject(
	// 	method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", 
	// 	at = @At(
	// 		value = "INVOKE", 
	// 		target = "Lnet/minecraft/world/entity/LivingEntity;getKillCredit()Lnet/minecraft/world/entity/LivingEntity;", 
	// 		shift = At.Shift.AFTER
	// 	)
	// )
	@Deprecated
	public void onDeath(final DamageSource damageSource, CallbackInfo info) {
		try {
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity.isRemoved() == false && entity.isAlwaysTicking() == false) {
				EntityDeath entityDeath = new EntityDeath(entity, damageSource);
				entityDeath.onEntityDeath();
			}
		} catch (Exception e) {
			Util.LOGGER.error("FMinecraftMod: Caught exception from EntityDeathEvent.", e);
		}
	}
}