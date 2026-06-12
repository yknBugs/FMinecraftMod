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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
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
        return "HasEntityType";
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
                rl = ResourceLocation.withDefaultNamespace("overworld");
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
                rl = ResourceLocation.withDefaultNamespace("player");
            }
            return rl;
        });
        return new HasEntityType(json.get("name").getAsString(), dimension, position, radius, entityType);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<ResourceLocation> dimensionParameter = RuleParameter.fromCommandContext("dimension", "var.dimension", () -> {
                        ServerLevel serverWorld = DimensionArgument.getDimension(ctx, "dimension");
                        return serverWorld.dimension().location();
                    }, arguments, ctx);
                    RuleParameter<Vec3> positionParameter = RuleParameter.fromCommandContext("position", "var.position", () -> {
                        return Vec3Argument.getVec3(ctx, "position");
                    }, arguments, ctx);
                    RuleParameter<Double> radiusParameter = RuleParameter.fromCommandContext("radius", "var.radius", () -> {
                        return DoubleArgumentType.getDouble(ctx, "radius");
                    }, arguments, ctx);
                    RuleParameter<ResourceLocation> entityTypeParameter = RuleParameter.fromCommandContext("type", "var.type", () -> {
                        return ResourceLocationArgument.getId(ctx, "type");
                    }, arguments, ctx);
                    HasEntityType condition = new HasEntityType(name, dimensionParameter, positionParameter, radiusParameter, entityTypeParameter);
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
            .add("dimension", "var.dimension", () -> Commands.argument("dimension", DimensionArgument.dimension()))
            .add("position", "var.position", () -> Commands.argument("position", Vec3Argument.vec3()))
            .add("radius", "var.radius", () -> Commands.argument("radius", DoubleArgumentType.doubleArg(0)))
            .add("type", "var.type", () -> Commands.argument("type", ResourceLocationArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES))
            .build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
