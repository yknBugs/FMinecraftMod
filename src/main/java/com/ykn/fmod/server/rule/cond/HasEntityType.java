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

import net.minecraft.command.CommandException;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.command.suggestion.SuggestionProviders;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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

    private final RuleParameter<Identifier> dimension;

    private final RuleParameter<Vec3d> position;

    private final RuleParameter<Double> radius;

    private final RuleParameter<Identifier> entityType;

    public HasEntityType(String name, RuleParameter<Identifier> dimension, RuleParameter<Vec3d> position, RuleParameter<Double> radius, RuleParameter<Identifier> entityType) {
        this.name = name;
        this.dimension = dimension;
        this.position = position;
        this.radius = radius;
        this.entityType = entityType;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Identifier dimension = this.dimension.resolve(context, Identifier.class);
        Vec3d position = this.position.resolve(context, Vec3d.class);
        Double radius = this.radius.resolve(context, Double.class);
        Identifier entityType = this.entityType.resolve(context, Identifier.class);

        if (dimension == null || position == null || radius == null || entityType == null) {
            return false;
        }

        ServerWorld world = context.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimension));
        if (world == null) {
            return false;
        }

        Box box = new Box(
            position.x - radius, position.y - radius, position.z - radius,
            position.x + radius, position.y + radius, position.z + radius
        );
        List<Entity> nearbyEntities = world.getEntitiesByClass(Entity.class, box,
            entity -> EntityType.getId(entity.getType()).equals(entityType));
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
    public Text render() {
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
        RuleParameter<Identifier> dimension = RuleParameter.fromJson(json, "dimension", e -> new Identifier(e.getAsString()));
        RuleParameter<Vec3d> position = RuleParameter.fromJson(json, "position", e -> {
            JsonObject obj = e.getAsJsonObject();
            double x = obj.get("x").getAsDouble();
            double y = obj.get("y").getAsDouble();
            double z = obj.get("z").getAsDouble();
            return new Vec3d(x, y, z);
        });
        RuleParameter<Double> radius = RuleParameter.fromJson(json, "radius", JsonElement::getAsDouble);
        RuleParameter<Identifier> entityType = RuleParameter.fromJson(json, "type", e -> new Identifier(e.getAsString()));
        return new HasEntityType(json.get("name").getAsString(), dimension, position, radius, entityType);
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(LiteralArgumentBuilder<ServerCommandSource> commandNode, BiConsumer<CommandContext<ServerCommandSource>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<ServerCommandSource, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<Identifier> dimensionParameter = RuleParameter.fromCommandContext("dimension", "var.dimension", () -> {
                        ServerWorld serverWorld = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
                        return serverWorld.getRegistryKey().getValue();
                    }, arguments, ctx);
                    RuleParameter<Vec3d> positionParameter = RuleParameter.fromCommandContext("position", "var.position", () -> {
                        return Vec3ArgumentType.getVec3(ctx, "position");
                    }, arguments, ctx);
                    RuleParameter<Double> radiusParameter = RuleParameter.fromCommandContext("radius", "var.radius", () -> {
                        return DoubleArgumentType.getDouble(ctx, "radius");
                    }, arguments, ctx);
                    RuleParameter<Identifier> entityTypeParameter = RuleParameter.fromCommandContext("type", "var.type", () -> {
                        return IdentifierArgumentType.getIdentifier(ctx, "type");
                    }, arguments, ctx);
                    HasEntityType condition = new HasEntityType(name, dimensionParameter, positionParameter, radiusParameter, entityTypeParameter);
                    conditionConsumer.accept(ctx, condition);
                } catch (CommandException e) {
                    throw e;
                } catch (CommandSyntaxException e) {
                    throw new CommandException(Texts.toText(e.getRawMessage()));
                } catch (Exception e) {
                    Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                    throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
                }
                return Command.SINGLE_SUCCESS;
            })
            .add("dimension", "var.dimension", () -> CommandManager.argument("dimension", DimensionArgumentType.dimension()))
            .add("position", "var.position", () -> CommandManager.argument("position", Vec3ArgumentType.vec3()))
            .add("radius", "var.radius", () -> CommandManager.argument("radius", DoubleArgumentType.doubleArg(0)))
            .add("type", "var.type", () -> CommandManager.argument("type", IdentifierArgumentType.identifier()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES))
            .build(CommandManager.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
