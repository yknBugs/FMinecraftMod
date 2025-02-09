package com.ykn.fmod.server.base.event;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;

public class ProjectileHitEntity {

    private ProjectileEntity projectile;
    private EntityHitResult entityHitResult;

    public ProjectileHitEntity(ProjectileEntity projectile, EntityHitResult entityHitResult) {
        this.projectile = projectile;
        this.entityHitResult = entityHitResult;
    }

    public ProjectileEntity getProjectile() {
        return projectile;
    }

    public EntityHitResult getEntityHitResult() {
        return entityHitResult;
    }

    /**
     * This method is called when a projectile hits an entity.
     * @see com.ykn.fmod.server.mixin.ProjectileHitEntityMixin
     */
    public void onProjectileHitEntity() {
    }

}
