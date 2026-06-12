/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
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
        return "EntityPosition";
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

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<UUID> entityIdParameter = RuleParameter.fromCommandContext("entity", "var.entity", () -> {
                        Entity entity = EntityArgument.getEntity(ctx, "entity");
                        return entity.getUUID();
                    }, arguments, ctx);
                    RuleParameter<ResourceLocation> dimensionParameter = RuleParameter.fromCommandContext("dimension", "var.dimension", () -> {
                        ServerLevel serverWorld = DimensionArgument.getDimension(ctx, "dimension");
                        ResourceLocation dimensionId = serverWorld.dimension().location();
                        return dimensionId;
                    }, arguments, ctx);
                    RuleParameter<Vec3> positionParameter = RuleParameter.fromCommandContext("position", "var.position", () -> {
                        return Vec3Argument.getVec3(ctx, "position");
                    }, arguments, ctx);
                    RuleParameter<Double> radiusParameter = RuleParameter.fromCommandContext("radius", "var.radius", () -> {
                        double radius = DoubleArgumentType.getDouble(ctx, "radius");
                        return radius;
                    }, arguments, ctx);
                    EntityPosition condition = new EntityPosition(name, entityIdParameter, dimensionParameter, positionParameter, radiusParameter);
                    conditionConsumer.accept(ctx, condition);
                } catch (CommandSyntaxException e) {
                    ctx.getSource().sendFailure(ComponentUtils.fromMessage(e.getRawMessage()));
                    return 0;
                } catch (Exception e) {
                    Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                    ctx.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
                    return 0;
                }
                return Command.SINGLE_SUCCESS;
            })
            .add("entity", "var.entity", () -> Commands.argument("entity", EntityArgument.entity()))
            .add("dimension", "var.dimension", () -> Commands.argument("dimension", DimensionArgument.dimension()))
            .add("position", "var.position", () -> Commands.argument("position", Vec3Argument.vec3()))
            .add("radius", "var.radius", () -> Commands.argument("radius", DoubleArgumentType.doubleArg(0)))
            .build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
