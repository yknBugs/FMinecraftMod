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

import net.minecraft.command.CommandException;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

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

    private final RuleParameter<Identifier> dimension;

    private final RuleParameter<Vec3d> position;

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
    public EntityPosition(String name, RuleParameter<UUID> entityId, RuleParameter<Identifier> dimension, RuleParameter<Vec3d> position, RuleParameter<Double> radius) {
        this.name = name;
        this.entityId = entityId;
        this.dimension = dimension;
        this.position = position;
        this.radius = radius;
    }

    @Override
    public boolean evaluate(RuleContext context) {
        UUID entityId = this.entityId.resolve(context, UUID.class);
        Identifier dimension = this.dimension.resolve(context, Identifier.class);
        Vec3d position = this.position.resolve(context, Vec3d.class);
        Double radius = this.radius.resolve(context, Double.class);

        if (entityId == null || dimension == null || position == null || radius == null) {
            return false;
        }

        Entity entity = null;
        Iterable<ServerWorld> world = context.getServer().getWorlds();
        for (ServerWorld w : world) {
            if (w.getRegistryKey().getValue().equals(dimension)) {
                entity = w.getEntity(entityId);
                if (entity != null) {
                    break;
                }
            }
        }
        if (entity == null) {
            return false;
        }

        Vec3d entityPos = entity.getPos();
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
    public Text render() {
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
        RuleParameter<Identifier> dimension = RuleParameter.fromJson(json, "dimension", e -> new Identifier(e.getAsString()));
        RuleParameter<Vec3d> position = RuleParameter.fromJson(json, "position", e -> {
            JsonObject obj = e.getAsJsonObject();
            double x = obj.get("x").getAsDouble();
            double y = obj.get("y").getAsDouble();
            double z = obj.get("z").getAsDouble();
            return new Vec3d(x, y, z);
        });
        RuleParameter<Double> radius = RuleParameter.fromJson(json, "radius", JsonElement::getAsDouble);
        return new EntityPosition(json.get("name").getAsString(), entityId, dimension, position, radius);
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(LiteralArgumentBuilder<ServerCommandSource> commandNode, BiConsumer<CommandContext<ServerCommandSource>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<ServerCommandSource, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<UUID> entityIdParameter = RuleParameter.fromCommandContext("entity", "var.entity", () -> {
                        Entity entity = EntityArgumentType.getEntity(ctx, "entity");
                        return entity.getUuid();
                    }, arguments, ctx);
                    RuleParameter<Identifier> dimensionParameter = RuleParameter.fromCommandContext("dimension", "var.dimension", () -> {
                        ServerWorld serverWorld = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
                        Identifier dimensionId = serverWorld.getRegistryKey().getValue();
                        return dimensionId;
                    }, arguments, ctx);
                    RuleParameter<Vec3d> positionParameter = RuleParameter.fromCommandContext("position", "var.position", () -> {
                        return Vec3ArgumentType.getVec3(ctx, "position");
                    }, arguments, ctx);
                    RuleParameter<Double> radiusParameter = RuleParameter.fromCommandContext("radius", "var.radius", () -> {
                        double radius = DoubleArgumentType.getDouble(ctx, "radius");
                        return radius;
                    }, arguments, ctx);
                    EntityPosition condition = new EntityPosition(name, entityIdParameter, dimensionParameter, positionParameter, radiusParameter);
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
            .add("entity", "var.entity", () -> CommandManager.argument("entity", EntityArgumentType.entity()))
            .add("dimension", "var.dimension", () -> CommandManager.argument("dimension", DimensionArgumentType.dimension()))
            .add("position", "var.position", () -> CommandManager.argument("position", Vec3ArgumentType.vec3()))
            .add("radius", "var.radius", () -> CommandManager.argument("radius", DoubleArgumentType.doubleArg(0)))
            .build(CommandManager.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
