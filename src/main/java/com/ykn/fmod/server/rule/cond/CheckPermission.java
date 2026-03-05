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
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
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
    public boolean evaluate(RuleContext context) {
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
        return "CheckPermission";
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
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<UUID> playerParameter = RuleParameter.fromCommandContext("player", "var.player", () -> {
                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                        return player.getUUID();
                    }, arguments, ctx);
                    RuleParameter<Integer> minParameter = RuleParameter.fromCommandContext("min", "var.min", () -> {
                        return IntegerArgumentType.getInteger(ctx, "min");
                    }, arguments, ctx);
                    RuleParameter<Integer> maxParameter = RuleParameter.fromCommandContext("max", "var.max", () -> {
                        return IntegerArgumentType.getInteger(ctx, "max");
                    }, arguments, ctx);
                    CheckPermission condition = new CheckPermission(name, playerParameter, minParameter, maxParameter);
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
            .add("player", "var.player", () -> Commands.argument("player", EntityArgument.player()))
            .add("min", "var.min", () -> Commands.argument("min", IntegerArgumentType.integer()))
            .add("max", "var.max", () -> Commands.argument("max", IntegerArgumentType.integer()))
            .build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }

}
