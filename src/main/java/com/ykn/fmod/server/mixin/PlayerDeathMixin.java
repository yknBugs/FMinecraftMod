/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.mixin;

import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.PlayerDeath;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayerEntity.class)
public class PlayerDeathMixin {

    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"))
    private void onDeath(final DamageSource damageSource, CallbackInfo info) {
        try {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            if (player.isRemoved() == false) {
                PlayerDeath playerDeath = new PlayerDeath(player, damageSource);
                playerDeath.onPlayerDeath();
            }
        } catch (Exception e) {
			LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught exception from PlayerDeathEvent.", e);
		}
    }   

}
