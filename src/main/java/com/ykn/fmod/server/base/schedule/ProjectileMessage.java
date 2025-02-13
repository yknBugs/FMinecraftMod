package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ProjectileMessage extends ScheduledTask {

    private Entity shooter;
    private Entity victim;
    private double distance;

    public ProjectileMessage(Entity shooter, Entity victim, double distance) {
        super(1, 0);
        this.shooter = shooter;
        this.victim = victim;
        this.distance = distance;
    }

    @Override
    public void onTrigger() {
        Text victimName = victim.getDisplayName();
        double victimHealth = Util.getHealth(victim);
        Text shooterName = shooter.getDisplayName();
        double shooterHealth = Util.getHealth(shooter);
        MutableText text = Util.parseTranslateableText("fmod.message.projectile.onhit", shooterName, String.format("%.1f", shooterHealth), String.format("%.1f", distance), victimName, String.format("%.1f", victimHealth));
        if (victim.isPlayer() && victim instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerVictim = (ServerPlayerEntity) victim;
            Util.postMessage(playerVictim, Util.serverConfig.getProjectileBeingHit(), MessageLocation.ACTIONBAR, text);
        }
        if (shooter.isPlayer() && shooter instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerShooter = (ServerPlayerEntity) shooter;
            Util.postMessage(playerShooter, Util.serverConfig.getProjectileHitOthers(), MessageLocation.ACTIONBAR, text);
        }
    }

    @Override
    public boolean shouldCancel() {
        // Avoid the shoot message override the entity death message
        if (shooter instanceof LivingEntity && Util.getHealth(shooter) <= 0) {
            return true;
        }
        if (victim instanceof LivingEntity && Util.getHealth(victim) <= 0) {
            return true;
        }
        return false;
    }
}
