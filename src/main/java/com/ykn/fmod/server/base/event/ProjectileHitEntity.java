package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.schedule.ProjectileMessage;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.Entity;
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
        if (projectile.getServer() == null) {
            return;
        }
        Entity victim = entityHitResult.getEntity();
        if (victim == null) {
            return;
        }
        Entity shooter = projectile.getEffectCause();
        if (shooter == null) {
            return;
        }

        double distance = GameMath.getEuclideanDistance(shooter, victim);
        Util.getServerData(projectile.getServer()).submitScheduledTask(new ProjectileMessage(shooter, victim, distance));
    }

}
