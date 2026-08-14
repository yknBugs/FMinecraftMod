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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * A {@link SourceCondition} that tests whether a specific entity (identified by UUID)
 * has a given entity type.
 *
 * <p>The condition searches across all loaded worlds for an entity with the provided UUID.
 * It evaluates to {@code false} when the entity is not found in any loaded world.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "entity":     {"variable": "targetId"},
 *   "type": {"constant": "minecraft:creeper"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class CheckEntityType implements SourceCondition {

    private final String name;

    private final RuleParameter<UUID> entity;

    private final RuleParameter<ResourceLocation> entityType;

    public static final String TYPE = "CheckEntityType";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.checkentitytype.summary")
        .add(ParamKind.ENTITY, "fmod.rule.condition.checkentitytype.param.entity.name", "fmod.rule.condition.checkentitytype.param.entity.desc", "entity", "var.entity")
        .add(ParamKind.ENTITY_TYPE_ID, "fmod.rule.condition.checkentitytype.param.type.name", "fmod.rule.condition.checkentitytype.param.type.desc", "type", "var.type");

    @SuppressWarnings("unchecked")
    public CheckEntityType(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.entity = (RuleParameter<UUID>) values.get(0);
        this.entityType = (RuleParameter<ResourceLocation>) values.get(1);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        UUID entityId = this.entity.resolve(context, UUID.class);
        ResourceLocation entityType = this.entityType.resolve(context, ResourceLocation.class);

        if (entityId == null && entityType == null) {
            return true;
        }

        if (entityId == null || entityType == null) {
            return false;
        }

        for (ServerLevel world : context.getServer().getAllLevels()) {
            Entity found = world.getEntity(entityId);
            if (found != null) {
                return entityType.equals(EntityType.getKey(found.getType()));
            }
        }

        return false;
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
        return new CheckEntityType(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.entity, this.entityType);
    }

}
