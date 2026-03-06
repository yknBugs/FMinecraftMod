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

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

/**
 * A {@link SourceCondition} that tests whether {@code left} is strictly less than {@code right}.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "left":  {"constant": 1.0},
 *   "right": {"variable": "someDouble"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class SmallerThan implements SourceCondition {

    private final String name;

    private final RuleParameter<Double> left;

    private final RuleParameter<Double> right;

    public SmallerThan(String name, RuleParameter<Double> left, RuleParameter<Double> right) {
        this.name = name;
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Double left = this.left.resolve(context, Double.class);
        Double right = this.right.resolve(context, Double.class);

        if (left == null || right == null) {
            return false;
        }

        return left < right;
    }

    @Override
    public String getType() {
        return "SmallerThan";
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new SmallerThan(name, this.left, this.right);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.smallerthan", this.getName(), this.getType(),
            this.left.render(), this.right.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("left", RuleParameter.toJson(left, JsonPrimitive::new));
        json.add("right", RuleParameter.toJson(right, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(SmallerThan condition) {
        return condition.toJson();
    }

    public static SmallerThan fromJson(JsonObject json) {
        RuleParameter<Double> left = RuleParameter.fromJson(json, "left", JsonElement::getAsDouble);
        RuleParameter<Double> right = RuleParameter.fromJson(json, "right", JsonElement::getAsDouble);
        return new SmallerThan(json.get("name").getAsString(), left, right);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<Double> leftParameter = RuleParameter.fromCommandContext("left", "var.left", () -> {
                        return DoubleArgumentType.getDouble(ctx, "left");
                    }, arguments, ctx);
                    RuleParameter<Double> rightParameter = RuleParameter.fromCommandContext("right", "var.right", () -> {
                        return DoubleArgumentType.getDouble(ctx, "right");
                    }, arguments, ctx);
                    SmallerThan condition = new SmallerThan(name, leftParameter, rightParameter);
                    conditionConsumer.accept(ctx, condition);
                } catch (CommandRuntimeException e) {
                    throw e;
                } catch (CommandSyntaxException e) {
                    throw new CommandRuntimeException(ComponentUtils.fromMessage(e.getRawMessage()));
                } catch (Exception e) {
                    Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                    throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
                }
                return Command.SINGLE_SUCCESS;
            })
            .add("left", "var.left", () -> Commands.argument("left", DoubleArgumentType.doubleArg()))
            .add("right", "var.right", () -> Commands.argument("right", DoubleArgumentType.doubleArg()))
            .build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
