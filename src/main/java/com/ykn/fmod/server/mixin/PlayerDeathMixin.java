package com.ykn.fmod.server.mixin;

import org.slf4j.LoggerFactory;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.PlayerDeath;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

// @Mixin(ServerPlayer.class)
public class PlayerDeathMixin {

    // @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    @Deprecated
    public void onDeath(final DamageSource damageSource, CallbackInfo info) {
        try {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (player.isRemoved() == false) {
                PlayerDeath playerDeath = new PlayerDeath(player, damageSource);
                playerDeath.onPlayerDeath();
            }
        } catch (Exception e) {
			LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught exception from PlayerDeathEvent.", e);
		}
    }   

}
