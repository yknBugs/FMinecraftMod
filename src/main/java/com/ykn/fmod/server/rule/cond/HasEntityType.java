/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;

import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SourceCondition} that tests whether at least one entity of a specific type
 * is present within a radius of a given position in a dimension.
 *
 * <p>The search area is an axis-aligned box whose half-extents equal {@code radius} on
 * every axis. The condition evaluates to {@code false} when any parameter resolves to
 * {@code null}, or when the specified world does not exist.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "dimension":  {"constant": "minecraft:overworld"},
 *   "position":   {"variable": "pos"},
 *   "radius":     {"constant": 16.0},
 *   "type": {"constant": "minecraft:creeper"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class HasEntityType implements SourceCondition {

    private final String name;

    private final RuleParameter<ResourceLocation> dimension;

    private final RuleParameter<Vec3> position;

    private final RuleParameter<Double> radius;

    private final RuleParameter<ResourceLocation> entityType;

    public static final String TYPE = "HasEntityType";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.hasentitytype.summary")
        .add(ParamKind.DIMENSION, "fmod.rule.condition.hasentitytype.param.dimension.name", "fmod.rule.condition.hasentitytype.param.dimension.desc", "dimension", "var.dimension")
        .add(ParamKind.POSITION, "fmod.rule.condition.hasentitytype.param.position.name", "fmod.rule.condition.hasentitytype.param.position.desc", "position", "var.position")
        .add(ParamKind.doubleAtLeast(0), "fmod.rule.condition.hasentitytype.param.radius.name", "fmod.rule.condition.hasentitytype.param.radius.desc", "radius", "var.radius")
        .add(ParamKind.ENTITY_TYPE_ID, "fmod.rule.condition.hasentitytype.param.type.name", "fmod.rule.condition.hasentitytype.param.type.desc", "type", "var.type");

    @SuppressWarnings("unchecked")
    public HasEntityType(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.dimension = (RuleParameter<ResourceLocation>) values.get(0);
        this.position = (RuleParameter<Vec3>) values.get(1);
        this.radius = (RuleParameter<Double>) values.get(2);
        this.entityType = (RuleParameter<ResourceLocation>) values.get(3);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        ResourceLocation dimension = this.dimension.resolve(context, ResourceLocation.class);
        Vec3 position = this.position.resolve(context, Vec3.class);
        Double radius = this.radius.resolve(context, Double.class);
        ResourceLocation entityType = this.entityType.resolve(context, ResourceLocation.class);

        if (dimension == null || position == null || radius == null || entityType == null) {
            return false;
        }

        ServerLevel world = context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (world == null) {
            return false;
        }

        AABB box = new AABB(
            position.x - radius, position.y - radius, position.z - radius,
            position.x + radius, position.y + radius, position.z + radius
        );
        List<Entity> nearbyEntities = world.getEntitiesOfClass(Entity.class, box,
            entity -> EntityType.getKey(entity.getType()).equals(entityType));
        return !nearbyEntities.isEmpty();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new HasEntityType(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.dimension, this.position, this.radius, this.entityType);
    }

}
