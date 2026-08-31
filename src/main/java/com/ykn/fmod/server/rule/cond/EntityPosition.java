/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;
import java.util.UUID;

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
import net.minecraft.world.phys.Vec3;

/**
 * A {@link SourceCondition} that tests whether an entity is within a given radius
 * of a specified world position.
 *
 * <p>All four parameters ({@code entityId}, {@code dimension}, {@code position}, {@code radius})
 * are represented as {@link RuleParameter}s, meaning each can be resolved from a
 * context variable or fall back to a constant.
 *
 * <p>The condition evaluates to {@code false} when any parameter resolves to {@code null},
 * or when no entity with the given UUID is found in the specified dimension.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "entity":    {"variable": "playerId"},
 *   "dimension": {"constant": "minecraft:overworld"},
 *   "position":  {"variable": "worldSpawn", "constant": {"x": 0, "y": 64, "z": 0}},
 *   "radius":    {"constant": 128.0}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class EntityPosition implements SourceCondition {

    private final String name;

    private final RuleParameter<UUID> entityId;

    private final RuleParameter<ResourceLocation> dimension;

    private final RuleParameter<Vec3> position;

    private final RuleParameter<Double> radius;

    public static final String TYPE = "EntityPosition";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.entityposition.summary")
        .add(ParamKind.ENTITY, "fmod.rule.condition.entityposition.param.entity.name", "fmod.rule.condition.entityposition.param.entity.desc", "entity", "var.entity")
        .add(ParamKind.DIMENSION, "fmod.rule.condition.entityposition.param.dimension.name", "fmod.rule.condition.entityposition.param.dimension.desc", "dimension", "var.dimension")
        .add(ParamKind.POSITION, "fmod.rule.condition.entityposition.param.position.name", "fmod.rule.condition.entityposition.param.position.desc", "position", "var.position")
        .add(ParamKind.doubleAtLeast(0), "fmod.rule.condition.entityposition.param.radius.name", "fmod.rule.condition.entityposition.param.radius.desc", "radius", "var.radius");

    /**
     * Creates a new {@code EntityPosition} condition.
     *
     * @param name   the unique name of this condition instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code entityId},
     *               {@code dimension}, {@code position}, {@code radius}
     */
    @SuppressWarnings("unchecked")
    public EntityPosition(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.entityId = (RuleParameter<UUID>) values.get(0);
        this.dimension = (RuleParameter<ResourceLocation>) values.get(1);
        this.position = (RuleParameter<Vec3>) values.get(2);
        this.radius = (RuleParameter<Double>) values.get(3);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        UUID entityId = this.entityId.resolve(context, UUID.class);
        ResourceLocation dimension = this.dimension.resolve(context, ResourceLocation.class);
        Vec3 position = this.position.resolve(context, Vec3.class);
        Double radius = this.radius.resolve(context, Double.class);

        if (entityId == null) {
            warnNullInput(context, PARAM_METADATA.get(0));
            return false;
        }
        if (dimension == null) {
            warnNullInput(context, PARAM_METADATA.get(1));
            return false;
        }
        if (position == null) {
            warnNullInput(context, PARAM_METADATA.get(2));
            return false;
        }
        if (radius == null) {
            warnNullInput(context, PARAM_METADATA.get(3));
            return false;
        }

        Entity entity = null;
        ServerLevel world = context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (world != null) {
            entity = world.getEntity(entityId);
        }
        if (entity == null) {
            warnNullInput(context, PARAM_METADATA.get(0));
            return false;
        }

        Vec3 entityPos = entity.position();
        double distance = position.distanceTo(entityPos);
        return distance <= radius;
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
        return new EntityPosition(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.entityId, this.dimension, this.position, this.radius);
    }

}
