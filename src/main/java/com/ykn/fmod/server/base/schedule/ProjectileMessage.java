/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

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
        Component victimName = victim.getDisplayName();
        double victimHealth = Util.getHealth(victim);
        Component shooterName = shooter.getDisplayName();
        double shooterHealth = Util.getHealth(shooter);
        MutableComponent text = Util.parseTranslatableText("fmod.message.projectile.onhit", shooterName, String.format("%.1f", shooterHealth), String.format("%.1f", distance), victimName, String.format("%.1f", victimHealth));
        if (victim.isAlwaysTicking() && victim instanceof ServerPlayer) {
            ServerPlayer playerVictim = (ServerPlayer) victim;
            if (playerVictim.isRemoved() || playerVictim.hasDisconnected() || playerVictim.getHealth() <= 0) {
                return;
            }
            Util.postMessage(playerVictim, Util.serverConfig.getProjectileBeingHitReceiver(), Util.serverConfig.getProjectileBeingHitLocation(), text);
        }
        if (shooter.isAlwaysTicking() && shooter instanceof ServerPlayer) {
            ServerPlayer playerShooter = (ServerPlayer) shooter;
            if (playerShooter.isRemoved() || playerShooter.hasDisconnected() || playerShooter.getHealth() <= 0) {
                return;
            }
            Util.postMessage(playerShooter, Util.serverConfig.getProjectileHitOthersReceiver(), Util.serverConfig.getProjectileHitOthersLocation(), text);
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
