package com.ykn.fmod.server.mixin;

import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ykn.fmod.server.base.event.ProjectileHitEntity;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;

@Mixin(ProjectileEntity.class)
public class ProjectileHitEntityMixin {

    @Inject(method = "onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V", at = @At("HEAD"))
    private void onEntityHit(final EntityHitResult entityHitResult, CallbackInfo info)
    {
        try {
            ProjectileEntity projectile = (ProjectileEntity) (Object) this;
            ProjectileHitEntity projectileHitEntity = new ProjectileHitEntity(projectile, entityHitResult);
            projectileHitEntity.onProjectileHitEntity();
        } catch (Exception e) {
            LoggerFactory.getLogger("FMinecraftMod").error("FMinecraftMod: Caught exception from ProjectileHitEntityEvent.", e);
        }
    }
}
