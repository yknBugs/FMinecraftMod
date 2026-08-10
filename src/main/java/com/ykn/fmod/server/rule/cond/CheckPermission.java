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
import net.minecraft.server.level.ServerPlayer;

/**
 * A {@link SourceCondition} that tests whether a player's permission level falls within
 * the range [{@code min}, {@code max}] (both inclusive).
 *
 * <p>All three parameters ({@code player}, {@code min}, {@code max}) are represented as
 * {@link RuleParameter}s, meaning each can be resolved from a context variable or fall back
 * to a constant.
 *
 * <p>The condition evaluates to {@code false} when any parameter resolves to {@code null},
 * or when no online player with the given UUID is found.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "player": {"variable": "playerId"},
 *   "min":    {"constant": 0},
 *   "max":    {"constant": 4}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class CheckPermission implements SourceCondition {

    private final String name;

    private final RuleParameter<UUID> player;

    private final RuleParameter<Integer> min;

    private final RuleParameter<Integer> max;

    private static final String TYPE = "CheckPermission";

    private static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.checkpermission.summary")
        .add(ParamKind.PLAYER, "fmod.rule.condition.checkpermission.param.player.name", "fmod.rule.condition.checkpermission.param.player.desc", "player", "var.player")
        .add(ParamKind.INT, "fmod.rule.condition.checkpermission.param.min.name", "fmod.rule.condition.checkpermission.param.min.desc", "min", "var.min")
        .add(ParamKind.INT, "fmod.rule.condition.checkpermission.param.max.name", "fmod.rule.condition.checkpermission.param.max.desc", "max", "var.max");

    /**
     * Creates a new {@code CheckPermission} condition.
     *
     * @param name   the unique name of this condition instance within the rule
     * @param player the UUID of the player to check
     * @param min    the minimum permission level (inclusive); values below 0 or above 4 are allowed
     * @param max    the maximum permission level (inclusive); values below 0 or above 4 are allowed
     */
    public CheckPermission(String name, RuleParameter<UUID> player, RuleParameter<Integer> min, RuleParameter<Integer> max) {
        this.name = name;
        this.player = player;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        UUID playerId = this.player.resolve(context, UUID.class);
        Integer min = this.min.resolve(context, Integer.class);
        Integer max = this.max.resolve(context, Integer.class);

        if (playerId == null || min == null || max == null) {
            return false;
        }

        ServerPlayer player = context.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return false;
        }

        int level = context.getServer().getProfilePermissions(player.getGameProfile());
        return level >= min && level <= max;
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
        return new CheckPermission(name, this.player, this.min, this.max);
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.condition.checkpermission", this.getName(), this.getType(),
            this.player.render(), this.min.render(), this.max.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("player", RuleParameter.toJson(player, e -> new JsonPrimitive(e.toString())));
        json.add("min", RuleParameter.toJson(min, JsonPrimitive::new));
        json.add("max", RuleParameter.toJson(max, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(CheckPermission condition) {
        return condition.toJson();
    }

    public static CheckPermission fromJson(JsonObject json) {
        RuleParameter<UUID> player = RuleParameter.fromJson(json, "player", e -> UUID.fromString(e.getAsString()));
        RuleParameter<Integer> min = RuleParameter.fromJson(json, "min", JsonElement::getAsInt);
        RuleParameter<Integer> max = RuleParameter.fromJson(json, "max", JsonElement::getAsInt);
        return new CheckPermission(json.get("name").getAsString(), player, min, max);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer) {
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<UUID> playerParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<Integer> minParameter = builder.resolveParameter(1, arguments, ctx);
                RuleParameter<Integer> maxParameter = builder.resolveParameter(2, arguments, ctx);
                CheckPermission condition = new CheckPermission(name, playerParameter, minParameter, maxParameter);
                conditionConsumer.accept(ctx, condition);
            })
            .addAll(PARAM_METADATA);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }

}
