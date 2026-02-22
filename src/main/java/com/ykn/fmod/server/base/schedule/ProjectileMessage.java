/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class ProjectileMessage extends ScheduledTask {

    private final Entity shooter;
    private final Entity victim;
    private final double distance;

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
        Text mainText = Util.parseTranslatableText("fmod.message.projectile.onhit.main", shooterName, String.format("%.1f", shooterHealth), String.format("%.1f", distance), victimName, String.format("%.1f", victimHealth));
        Text otherText = Util.parseTranslatableText("fmod.message.projectile.onhit.other", shooterName, victimName);
        if (victim.isPlayer() && victim instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerVictim = (ServerPlayerEntity) victim;
            if (playerVictim.isRemoved() || playerVictim.isDisconnected() || playerVictim.getHealth() <= 0) {
                return;
            }
            Util.serverConfig.getProjectileBeingHit().postMessage(playerVictim, mainText, otherText);
        }
        if (shooter.isPlayer() && shooter instanceof ServerPlayerEntity) {
            ServerPlayerEntity playerShooter = (ServerPlayerEntity) shooter;
            if (playerShooter.isRemoved() || playerShooter.isDisconnected() || playerShooter.getHealth() <= 0) {
                return;
            }
            Util.serverConfig.getProjectileHitOthers().postMessage(playerShooter, mainText, otherText);
        }
    }

    @Override
    public boolean shouldCancel() {
        if (shooter == null || victim == null) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ProjectileMessage{shooter='" + shooter.getDisplayName().getString() +
               "', victim='" + victim.getDisplayName().getString() +
               "', distance=" + distance +
               "}";
    }
}
