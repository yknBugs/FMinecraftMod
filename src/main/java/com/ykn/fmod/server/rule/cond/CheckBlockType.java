/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

/**
 * A {@link SourceCondition} that tests whether the block at a given world position
 * matches a specific block identifier.
 *
 * <p>When the target chunk is not loaded, or when the position is outside the build
 * limit, the condition evaluates to the value of the {@code defaultValue} parameter.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "dimension":    {"constant": "minecraft:overworld"},
 *   "position":     {"variable": "pos"},
 *   "block":        {"constant": "minecraft:stone"},
 *   "defaultValue": {"constant": false}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class CheckBlockType implements SourceCondition {

    private final String name;

    private final RuleParameter<Identifier> dimension;

    private final RuleParameter<Vec3d> position;

    private final RuleParameter<Identifier> block;

    private final RuleParameter<Boolean> defaultValue;

    public CheckBlockType(String name, RuleParameter<Identifier> dimension, RuleParameter<Vec3d> position, RuleParameter<Identifier> block, RuleParameter<Boolean> defaultValue) {
        this.name = name;
        this.dimension = dimension;
        this.position = position;
        this.block = block;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Identifier dimension = this.dimension.resolve(context, Identifier.class);
        Vec3d position = this.position.resolve(context, Vec3d.class);
        Identifier block = this.block.resolve(context, Identifier.class);
        Boolean defaultValue = this.defaultValue.resolve(context, Boolean.class);

        if (dimension == null || position == null || block == null) {
            return defaultValue != null && defaultValue;
        }

        ServerWorld world = context.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, dimension));
        if (world == null) {
            return defaultValue != null && defaultValue;
        }

        BlockPos blockPos = BlockPos.ofFloored(position);
        if (!world.isInBuildLimit(blockPos) || !world.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()), ChunkSectionPos.getSectionCoord(blockPos.getZ()))) {
            return defaultValue != null && defaultValue;
        }

        Identifier actualBlockId = Registries.BLOCK.getId(world.getBlockState(blockPos).getBlock());
        return block.equals(actualBlockId);
    }

    @Override
    public String getType() {
        return "CheckBlockType";
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new CheckBlockType(name, this.dimension, this.position, this.block, this.defaultValue);
    }

    @Override
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.condition.blocktype", this.getName(), this.getType(),
            this.dimension.render(), this.position.render(), this.block.render(), this.defaultValue.render());
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
        json.add("block", RuleParameter.toJson(block, e -> new JsonPrimitive(e.toString())));
        json.add("defaultValue", RuleParameter.toJson(defaultValue, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(CheckBlockType condition) {
        return condition.toJson();
    }

    public static CheckBlockType fromJson(JsonObject json) {
        RuleParameter<Identifier> dimension = RuleParameter.fromJson(json, "dimension", e -> new Identifier(e.getAsString()));
        RuleParameter<Vec3d> position = RuleParameter.fromJson(json, "position", e -> {
            JsonObject obj = e.getAsJsonObject();
            double x = obj.get("x").getAsDouble();
            double y = obj.get("y").getAsDouble();
            double z = obj.get("z").getAsDouble();
            return new Vec3d(x, y, z);
        });
        RuleParameter<Identifier> block = RuleParameter.fromJson(json, "block", e -> new Identifier(e.getAsString()));
        RuleParameter<Boolean> defaultValue = RuleParameter.fromJson(json, "defaultValue", JsonElement::getAsBoolean);
        return new CheckBlockType(json.get("name").getAsString(), dimension, position, block, defaultValue);
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
                    RuleParameter<Identifier> blockParameter = RuleParameter.fromCommandContext("block", "var.block", () -> {
                        return IdentifierArgumentType.getIdentifier(ctx, "block");
                    }, arguments, ctx);
                    RuleParameter<Boolean> defaultValueParameter = RuleParameter.fromCommandContext("default", "var.default", () -> {
                        return BoolArgumentType.getBool(ctx, "default");
                    }, arguments, ctx);
                    CheckBlockType condition = new CheckBlockType(name, dimensionParameter, positionParameter, blockParameter, defaultValueParameter);
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
            .add("block", "var.block", () -> CommandManager.argument("block", IdentifierArgumentType.identifier()))
            .add("default", "var.default", () -> CommandManager.argument("default", BoolArgumentType.bool()))
            .build(CommandManager.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
