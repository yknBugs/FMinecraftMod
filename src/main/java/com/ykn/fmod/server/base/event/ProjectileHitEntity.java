/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ProjectileMessage;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The projectile hit message.
 *
 * <p>This class is the existing feature only; the rule and flow events of the same moment are
 * dispatched by {@link GameEventHandler}, which calls this method first and reuses its result, so
 * the message and the event always agree about the shooter and the distance.
 */
public final class ProjectileHitEntity {

    private ProjectileHitEntity() {
    }

    /**
     * Reports one projectile hit.
     *
     * @param projectile the projectile
     * @param hit        the resolved hit
     * @return what was reported, or {@code null} when the handler was skipped (no server, no victim,
     *         no shooter) - then there is nothing to report and the event is not dispatched either
     * @see com.ykn.fmod.server.mixin.GameEventMixins.ProjectileHit
     */
    public static Hit onProjectileHit(Projectile projectile, EntityHitResult hit) {
        if (projectile.getServer() == null) {
            return null;
        }
        Entity victim = hit.getEntity();
        if (victim == null) {
            return null;
        }
        Entity shooter = projectile.getEffectSource();
        if (shooter == null) {
            return null;
        }

        double distance = GameMath.getEuclideanDistance(shooter, victim);
        ServerData data = Util.getServerData(projectile.getServer());
        data.submitScheduledTask(new ProjectileMessage(shooter, victim, distance));
        return new Hit(shooter, victim, distance);
    }

    /**
     * What the hit report was based on.
     *
     * @param shooter  the entity that launched the projectile
     * @param victim   the entity that was hit
     * @param distance the distance between them
     */
    public record Hit(Entity shooter, Entity victim, double distance) {
    }
}
