/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

/**
 * A {@link SourceCondition} that tests whether {@code left.equals(right)}.
 *
 * <p>Parameters are resolved as {@link Object} and compared with
 * {@link Object#equals(Object)}.
 *
 * <p>Because constants are stored as strings in JSON, both the {@code left} and
 * {@code right} parameters are serialised as plain string values. Variable
 * bindings may carry any runtime type.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "left":  {"variable": "someVar"},
 *   "right": {"constant": "expectedValue"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class EqualsTo implements SourceCondition {

    private final String name;

    private final RuleParameter<Object> left;

    private final RuleParameter<Object> right;

    public EqualsTo(String name, RuleParameter<Object> left, RuleParameter<Object> right) {
        this.name = name;
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Object left = this.left.resolve(context, Object.class);
        Object right = this.right.resolve(context, Object.class);

        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.equals(right);
    }

    @Override
    public String getType() {
        return "EqualsTo";
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new EqualsTo(name, this.left, this.right);
    }

    @Override
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.condition.equalsto", this.getName(), this.getType(),
            this.left.render(), this.right.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("left", RuleParameter.toJson(left, e -> new JsonPrimitive(TypeAdaptor.parse(e).asString())));
        json.add("right", RuleParameter.toJson(right, e -> new JsonPrimitive(TypeAdaptor.parse(e).asString())));
        return json;
    }

    public static JsonObject toJson(EqualsTo condition) {
        return condition.toJson();
    }

    public static EqualsTo fromJson(JsonObject json) {
        RuleParameter<Object> left = RuleParameter.fromJson(json, "left", e -> TypeAdaptor.parse(e).autoCast());
        RuleParameter<Object> right = RuleParameter.fromJson(json, "right", e -> TypeAdaptor.parse(e).autoCast());
        return new EqualsTo(json.get("name").getAsString(), left, right);
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(LiteralArgumentBuilder<ServerCommandSource> commandNode, BiConsumer<CommandContext<ServerCommandSource>, RuleCondition> conditionConsumer) {
        RequiredArgumentBuilder<ServerCommandSource, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<Object> leftParameter = RuleParameter.fromCommandContext("left", "var.left", () -> {
                        return TypeAdaptor.parse(StringArgumentType.getString(ctx, "left")).autoCast();
                    }, arguments, ctx);
                    RuleParameter<Object> rightParameter = RuleParameter.fromCommandContext("right", "var.right", () -> {
                        return TypeAdaptor.parse(StringArgumentType.getString(ctx, "right")).autoCast();
                    }, arguments, ctx);
                    EqualsTo condition = new EqualsTo(name, leftParameter, rightParameter);
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
            .add("left", "var.left", () -> CommandManager.argument("left", StringArgumentType.string()))
            .add("right", "var.right", () -> CommandManager.argument("right", StringArgumentType.string()))
            .build(CommandManager.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
