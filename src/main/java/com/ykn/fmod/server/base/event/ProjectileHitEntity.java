package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
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

        // Should be best executed 0.05 seconds after the event, since the entity's health may not have been updated yet.
        Text victimName = victim.getDisplayName();
        double victimHealth = Util.getHealth(victim);
        double distance = GameMath.getEuclideanDistance(shooter, victim);
        Text shooterName = shooter.getDisplayName();
        double shooterHealth = Util.getHealth(shooter);
        MutableText text = Util.parseTranslateableText("fmod.message.projectile.onhit", shooterName, String.format("%.1f", shooterHealth), String.format("%.1f", distance), victimName, String.format("%.1f", victimHealth));
        if (victim.isPlayer() && victim instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerVictim = (ServerPlayerEntity) victim;
            Util.postMessage(playerVictim, Util.serverConfig.getProjectileBeingHitMethod(), MessageType.ACTIONBAR, text);
        }
        if (shooter.isPlayer() && shooter instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerShooter = (ServerPlayerEntity) shooter;
            Util.postMessage(playerShooter, Util.serverConfig.getProjectileHitOthersMethod(), MessageType.ACTIONBAR, text);
        }
    }

}
