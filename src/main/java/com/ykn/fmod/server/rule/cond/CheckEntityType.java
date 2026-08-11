/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

    public CheckEntityType(String name, RuleParameter<UUID> entity, RuleParameter<ResourceLocation> entityType) {
        this.name = name;
        this.entity = entity;
        this.entityType = entityType;
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
        return new CheckEntityType(name, this.entity, this.entityType);
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.entity, this.entityType);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.checkentitytype", this.getName(), this.getType(),
            this.entity.render(), this.entityType.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("entity", RuleParameter.toJson(entity, e -> new JsonPrimitive(e.toString())));
        json.add("type", RuleParameter.toJson(entityType, e -> new JsonPrimitive(e.toString())));
        return json;
    }

    public static JsonObject toJson(CheckEntityType condition) {
        return condition.toJson();
    }

    public static CheckEntityType fromJson(JsonObject json) {
        RuleParameter<UUID> entity = RuleParameter.fromJson(json, "entity", e -> UUID.fromString(e.getAsString()));
        RuleParameter<ResourceLocation> entityType = RuleParameter.fromJson(json, "type", e -> {
            String s = e.getAsString();
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                Util.LOGGER.warn("Invalid ResourceLocation in CheckEntityType entityType: " + s + ". Defaulting to minecraft:player");
                rl = new ResourceLocation("minecraft", "player");
            }
            return rl;
        });
        return new CheckEntityType(json.get("name").getAsString(), entity, entityType);
    }

    @SuppressWarnings("unchecked")
    public static SourceCondition withParameters(String name, List<RuleParameter<?>> values) {
        return new CheckEntityType(name, (RuleParameter<UUID>) values.get(0), (RuleParameter<ResourceLocation>) values.get(1));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<UUID> entityParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<ResourceLocation> entityTypeParameter = builder.resolveParameter(1, arguments, ctx);
                CheckEntityType condition = new CheckEntityType(name, entityParameter, entityTypeParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
