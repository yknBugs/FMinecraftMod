/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.EntityDeath;
import com.ykn.fmod.server.base.util.Util;

@Mixin(LivingEntity.class)
public class EntityDeathMixin {
	@Inject(
		method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", 
		at = @At(
			value = "INVOKE", 
			target = "Lnet/minecraft/world/entity/LivingEntity;getKillCredit()Lnet/minecraft/world/entity/LivingEntity;", 
			shift = At.Shift.AFTER
		)
	)
	private void onDeath(final DamageSource damageSource, CallbackInfo info) {
		try {
			LivingEntity entity = (LivingEntity) (Object) this;
			if (entity.isRemoved() == false && !(entity instanceof Player)) {
				EntityDeath entityDeath = new EntityDeath(entity, damageSource);
				entityDeath.onEntityDeath();
			}
		} catch (Exception e) {
			Util.LOGGER.error("FMinecraftMod: An error occurred when handling entity death event.", e);
		}
	}
}
