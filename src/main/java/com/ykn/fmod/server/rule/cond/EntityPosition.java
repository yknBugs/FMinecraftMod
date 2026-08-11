/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;
import java.util.UUID;
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
 *   "entityId":  {"variable": "playerId"},
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
     * @param name      the unique name of this condition instance within the rule
     * @param entityId  the UUID of the entity to check
     * @param dimension the dimension identifier where the entity and reference position reside
     * @param position  the centre of the search sphere
     * @param radius    the maximum allowed Euclidean distance from {@code position}
     */
    public EntityPosition(String name, RuleParameter<UUID> entityId, RuleParameter<ResourceLocation> dimension, RuleParameter<Vec3> position, RuleParameter<Double> radius) {
        this.name = name;
        this.entityId = entityId;
        this.dimension = dimension;
        this.position = position;
        this.radius = radius;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        UUID entityId = this.entityId.resolve(context, UUID.class);
        ResourceLocation dimension = this.dimension.resolve(context, ResourceLocation.class);
        Vec3 position = this.position.resolve(context, Vec3.class);
        Double radius = this.radius.resolve(context, Double.class);

        if (entityId == null || dimension == null || position == null || radius == null) {
            return false;
        }

        Entity entity = null;
        ServerLevel world = context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (world != null) {
            entity = world.getEntity(entityId);
        }
        if (entity == null) {
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
        return new EntityPosition(name, this.entityId, this.dimension, this.position, this.radius);
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.entityId, this.dimension, this.position, this.radius);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.entityposition", this.getName(), this.getType(), 
            this.entityId.render(), this.dimension.render(), this.position.render(), this.radius.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("entityId", RuleParameter.toJson(entityId, e -> new JsonPrimitive(e.toString())));
        json.add("dimension", RuleParameter.toJson(dimension, e -> new JsonPrimitive(e.toString())));
        json.add("position", RuleParameter.toJson(position, e -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", e.x);
            obj.addProperty("y", e.y);
            obj.addProperty("z", e.z);
            return obj;
        }));
        json.add("radius", RuleParameter.toJson(radius, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(EntityPosition condition) {
        return condition.toJson();
    }

    public static EntityPosition fromJson(JsonObject json) {
        RuleParameter<UUID> entityId = RuleParameter.fromJson(json, "entityId", e -> UUID.fromString(e.getAsString()));
        RuleParameter<ResourceLocation> dimension = RuleParameter.fromJson(json, "dimension", e -> {
            String s = e.getAsString();
            ResourceLocation rl = ResourceLocation.tryParse(s);
            if (rl == null) {
                Util.LOGGER.warn("Invalid ResourceLocation in EntityPosition dimension: " + s + ". Defaulting to minecraft:overworld");
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
        return new EntityPosition(json.get("name").getAsString(), entityId, dimension, position, radius);
    }

    @SuppressWarnings("unchecked")
    public static SourceCondition withParameters(String name, List<RuleParameter<?>> values) {
        return new EntityPosition(name, (RuleParameter<UUID>) values.get(0), (RuleParameter<ResourceLocation>) values.get(1),
            (RuleParameter<Vec3>) values.get(2), (RuleParameter<Double>) values.get(3));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<UUID> entityIdParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<ResourceLocation> dimensionParameter = builder.resolveParameter(1, arguments, ctx);
                RuleParameter<Vec3> positionParameter = builder.resolveParameter(2, arguments, ctx);
                RuleParameter<Double> radiusParameter = builder.resolveParameter(3, arguments, ctx);
                EntityPosition condition = new EntityPosition(name, entityIdParameter, dimensionParameter, positionParameter, radiusParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
