/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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

    private static final String TYPE = "HasEntityType";

    private static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.hasentitytype.summary")
        .add(ParamKind.DIMENSION, "fmod.rule.condition.hasentitytype.param.dimension.name", "fmod.rule.condition.hasentitytype.param.dimension.desc", "dimension", "var.dimension")
        .add(ParamKind.POSITION, "fmod.rule.condition.hasentitytype.param.position.name", "fmod.rule.condition.hasentitytype.param.position.desc", "position", "var.position")
        .add(ParamKind.doubleAtLeast(0), "fmod.rule.condition.hasentitytype.param.radius.name", "fmod.rule.condition.hasentitytype.param.radius.desc", "radius", "var.radius")
        .add(ParamKind.ENTITY_TYPE_ID, "fmod.rule.condition.hasentitytype.param.type.name", "fmod.rule.condition.hasentitytype.param.type.desc", "type", "var.type");

    public HasEntityType(String name, RuleParameter<ResourceLocation> dimension, RuleParameter<Vec3> position, RuleParameter<Double> radius, RuleParameter<ResourceLocation> entityType) {
        this.name = name;
        this.dimension = dimension;
        this.position = position;
        this.radius = radius;
        this.entityType = entityType;
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
        return new HasEntityType(name, this.dimension, this.position, this.radius, this.entityType);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.hasentitytype", this.getName(), this.getType(),
            this.dimension.render(), this.position.render(), this.radius.render(), this.entityType.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("dimension", RuleParameter.toJson(dimension, e -> new JsonPrimitive(e.toString())));
        json.add("position", RuleParameter.toJson(position, e -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", e.x);
            obj.addProperty("y", e.y);
            obj.addProperty("z", e.z);
            return obj;
        }));
        json.add("radius", RuleParameter.toJson(radius, JsonPrimitive::new));
        json.add("type", RuleParameter.toJson(entityType, e -> new JsonPrimitive(e.toString())));
        return json;
    }

    public static JsonObject toJson(HasEntityType condition) {
        return condition.toJson();
    }

    public static HasEntityType fromJson(JsonObject json) {
        RuleParameter<ResourceLocation> dimension = RuleParameter.fromJson(json, "dimension", e -> {
            String s = e.getAsString();
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                Util.LOGGER.warn("Invalid ResourceLocation in HasEntityType dimension: " + s + ". Defaulting to minecraft:overworld");
                rl = new ResourceLocation("minecraft", "overworld");
            }
            return rl;
        });
        RuleParameter<Vec3> position = RuleParameter.fromJson(json, "position", e -> {
            JsonObject obj = e.getAsJsonObject();
            double x = obj.get("x").getAsDouble();
            double y = obj.get("y").getAsDouble();
            double z = obj.get("z").getAsDouble();
            return new Vec3(x, y, z);
        });
        RuleParameter<Double> radius = RuleParameter.fromJson(json, "radius", JsonElement::getAsDouble);
        RuleParameter<ResourceLocation> entityType = RuleParameter.fromJson(json, "type", e -> {
            String s = e.getAsString();
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                Util.LOGGER.warn("Invalid ResourceLocation in HasEntityType entityType: " + s + ". Defaulting to minecraft:player");
                rl = new ResourceLocation("minecraft", "player");
            }
            return rl;
        });
        return new HasEntityType(json.get("name").getAsString(), dimension, position, radius, entityType);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<ResourceLocation> dimensionParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<Vec3> positionParameter = builder.resolveParameter(1, arguments, ctx);
                RuleParameter<Double> radiusParameter = builder.resolveParameter(2, arguments, ctx);
                RuleParameter<ResourceLocation> entityTypeParameter = builder.resolveParameter(3, arguments, ctx);
                HasEntityType condition = new HasEntityType(name, dimensionParameter, positionParameter, radiusParameter, entityTypeParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
