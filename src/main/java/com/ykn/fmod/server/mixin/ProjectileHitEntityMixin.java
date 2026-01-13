package com.ykn.fmod.server.mixin;

import org.slf4j.LoggerFactory;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.ProjectileHitEntity;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;

// @Mixin(Projectile.class)
public class ProjectileHitEntityMixin {

    // @Inject(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"))
    @Deprecated
    public void onEntityHit(final EntityHitResult entityHitResult, CallbackInfo info) {
        try {
            Projectile projectile = (Projectile) (Object) this;
            ProjectileHitEntity projectileHitEntity = new ProjectileHitEntity(projectile, entityHitResult);
            projectileHitEntity.onProjectileHitEntity();
        } catch (Exception e) {
            LoggerFactory.getLogger("FMinecraftMod").error("FMinecraftMod: Caught exception from ProjectileHitEntityEvent.", e);
        }
    }
}
