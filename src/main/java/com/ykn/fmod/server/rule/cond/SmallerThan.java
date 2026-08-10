/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

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
import net.minecraft.network.chat.Component;

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

    private static final String TYPE = "SmallerThan";

    private static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.smallerthan.summary")
        .add(ParamKind.DOUBLE, "fmod.rule.condition.smallerthan.param.left.name", "fmod.rule.condition.smallerthan.param.left.desc", "left", "var.left")
        .add(ParamKind.DOUBLE, "fmod.rule.condition.smallerthan.param.right.name", "fmod.rule.condition.smallerthan.param.right.desc", "right", "var.right");

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
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<Double> leftParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<Double> rightParameter = builder.resolveParameter(1, arguments, ctx);
                SmallerThan condition = new SmallerThan(name, leftParameter, rightParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
