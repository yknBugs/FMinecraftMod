/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ProjectileMessage;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.event.ProjectileHitEntityEvent;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;

public class ProjectileHitEntity {

    private final Projectile projectile;
    private final EntityHitResult entityHitResult;

    public ProjectileHitEntity(Projectile projectile, EntityHitResult entityHitResult) {
        this.projectile = projectile;
        this.entityHitResult = entityHitResult;
    }

    public Projectile getProjectile() {
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
        Entity shooter = projectile.getEffectSource();
        if (shooter == null) {
            return;
        }

        // Show projectile hit entity message
        double distance = GameMath.getEuclideanDistance(shooter, victim);
        ServerData data = Util.getServerData(projectile.getServer());
        data.submitScheduledTask(new ProjectileMessage(shooter, victim, distance));

        runCustomRule(data, shooter, victim, distance);
        runLogicFlow(data, shooter, victim, distance);
    }

    private void runCustomRule(ServerData data, Entity shooter, Entity victim, double distance) {
        List<RuleManager> hitEventRules = data.gatherRuleByEventType(ProjectileHitEntityEvent.class, true);
        for (RuleManager rule : hitEventRules) {
            Map<String, Object> eventVariables = new HashMap<>();
            eventVariables.put("entity", victim.getUUID());
            eventVariables.put("shooter", shooter.getUUID());
            eventVariables.put("projectile", projectile.getUUID());
            eventVariables.put("x", victim.getX());
            eventVariables.put("y", victim.getY());
            eventVariables.put("z", victim.getZ());
            eventVariables.put("position", victim.position());
            eventVariables.put("dimension", victim.level().dimension().location());
            eventVariables.put("biome", victim.level().getBiome(victim.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null));
            eventVariables.put("pitch", Double.valueOf(victim.getXRot()));
            eventVariables.put("yaw", Double.valueOf(victim.getYRot()));
            eventVariables.put("rotation", victim.getRotationVector());
            eventVariables.put("sx", shooter.getX());
            eventVariables.put("sy", shooter.getY());
            eventVariables.put("sz", shooter.getZ());
            eventVariables.put("sposition", shooter.position());
            eventVariables.put("sdimension", shooter.level().dimension().location());
            eventVariables.put("sbiome", shooter.level().getBiome(shooter.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null));
            eventVariables.put("spitch", Double.valueOf(shooter.getXRot()));
            eventVariables.put("syaw", Double.valueOf(shooter.getYRot()));
            eventVariables.put("srotation", shooter.getRotationVector());
            eventVariables.put("distance", distance);
            eventVariables.put("health", Util.getHealth(victim));
            eventVariables.put("name", victim.getDisplayName().getString());
            eventVariables.put("sname", shooter.getDisplayName().getString());
            eventVariables.put("__entity__", victim);
            eventVariables.put("__shooter__", shooter);
            eventVariables.put("__projectile__", projectile);
            eventVariables.put("__world__", victim.level());
            eventVariables.put("__sworld__", shooter.level());
            eventVariables.put("__name__", victim.getDisplayName());
            eventVariables.put("__sname__", shooter.getDisplayName());
            rule.trigger(data, eventVariables);
        }
    }

    private void runLogicFlow(ServerData data, Entity shooter, Entity victim, double distance) {
        List<FlowManager> hitEventFlow = data.gatherFlowByFirstNodeType("ProjectileHitEntityEventNode", true);
        for (FlowManager flow : hitEventFlow) {
            List<Object> eventOutput = new ArrayList<>();
            eventOutput.add(this.projectile);
            eventOutput.add(shooter);
            eventOutput.add(victim);
            eventOutput.add(entityHitResult.getLocation());
            eventOutput.add(distance);
            flow.execute(data, eventOutput, null);
        }
    }

}
