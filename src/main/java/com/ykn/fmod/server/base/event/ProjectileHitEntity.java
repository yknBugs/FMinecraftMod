/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ProjectileMessage;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;

public class ProjectileHitEntity {

    private final ProjectileEntity projectile;
    private final EntityHitResult entityHitResult;

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

        // Show projectile hit entity message
        double distance = GameMath.getEuclideanDistance(shooter, victim);
        ServerData data = Util.getServerData(projectile.getServer());
        data.submitScheduledTask(new ProjectileMessage(shooter, victim, distance));

        // Trigger the event for LogicFlow
        List<FlowManager> hitEventFlow = data.gatherFlowByFirstNodeType("ProjectileHitEntityEventNode", true);
        for (FlowManager flow : hitEventFlow) {
            List<Object> eventOutput = new ArrayList<>();
            eventOutput.add(this.projectile);
            eventOutput.add(shooter);
            eventOutput.add(victim);
            eventOutput.add(entityHitResult.getPos());
            eventOutput.add(distance);
            flow.execute(data, eventOutput, null);
        }
    }

}
