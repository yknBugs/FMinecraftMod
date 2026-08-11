/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.util.TypeAdaptor;
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

    public static final String TYPE = "EqualsTo";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.equalsto.summary")
        .add(ParamKind.AUTO, "fmod.rule.condition.equalsto.param.left.name", "fmod.rule.condition.equalsto.param.left.desc", "left", "var.left")
        .add(ParamKind.AUTO, "fmod.rule.condition.equalsto.param.right.name", "fmod.rule.condition.equalsto.param.right.desc", "right", "var.right");

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
        return new EqualsTo(name, this.left, this.right);
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.left, this.right);
    }

    @Override
    public Component render() {
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

    @SuppressWarnings("unchecked")
    public static SourceCondition withParameters(String name, List<RuleParameter<?>> values) {
        return new EqualsTo(name, (RuleParameter<Object>) values.get(0), (RuleParameter<Object>) values.get(1));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<Object> leftParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<Object> rightParameter = builder.resolveParameter(1, arguments, ctx);
                EqualsTo condition = new EqualsTo(name, leftParameter, rightParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
