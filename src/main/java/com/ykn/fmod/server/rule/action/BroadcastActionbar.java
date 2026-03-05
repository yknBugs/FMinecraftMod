/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.command.SayCommandSuggestion;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.TextPlaceholderFactory;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * A {@link RuleAction} that broadcasts a message to all online players via the action bar
 * (above the hotbar).
 *
 * <p>The message is resolved from the {@code message} {@link RuleParameter} and supports
 * the same {@code ${var:variableName}} placeholder syntax as {@link BroadcastMessage}.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "message": {"constant": "Server restarting in 60 seconds!"}
 * }
 * }</pre>
 *
 * @see MessageType#broadcastActionBarMessage
 * @see BroadcastMessage
 */
public class BroadcastActionbar implements RuleAction {

    private final String name;

    private final RuleParameter<String> message;

    /**
     * Creates a {@code BroadcastActionbar} action.
     *
     * @param name    the unique name of this action instance within the rule
     * @param message the message parameter (variable reference and/or constant string)
     */
    public BroadcastActionbar(String name, RuleParameter<String> message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public boolean execute(RuleContext context) {
        String message = this.message.resolve(context, String.class);
        if (message != null) {
            TextPlaceholderFactory<ServerPlayerEntity> factory = TextPlaceholderFactory.ofDefault();
            for (String variable : context.getVariables().keySet()) {
                Object value = context.getVariable(variable);
                Text toShow = value == null ? Text.literal("${var:" + variable + "}") : Text.literal(TypeAdaptor.parse(value).asString());
                factory = factory.add("${var:" + variable + "}", player -> toShow);
            }
            MessageType.broadcastActionBarMessage(context.getServer(), factory.parse(message, null));
        }
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleAction setName(String name) {
        return new BroadcastActionbar(name, this.message);
    }

    @Override
    public String getType() {
        return "BroadcastActionbar";
    }

    @Override
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.action.bcactionbar", this.getName(), this.getType(), this.message.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("message", RuleParameter.toJson(message, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(BroadcastActionbar action) {
        return action.toJson();
    }

    public static BroadcastActionbar fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        RuleParameter<String> message = RuleParameter.fromJson(json, "message", e -> e.getAsString());
        return new BroadcastActionbar(name, message);
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(LiteralArgumentBuilder<ServerCommandSource> commandNode, BiConsumer<CommandContext<ServerCommandSource>, RuleAction> actionConsumer) {
        SayCommandSuggestion suggestion = SayCommandSuggestion.suggest().add("${", "var:");
        RequiredArgumentBuilder<ServerCommandSource, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<String> messageParameter = RuleParameter.fromCommandContext("message", "var.message", () -> {
                        return StringArgumentType.getString(ctx, "message");
                    }, arguments, ctx);
                    BroadcastActionbar action = new BroadcastActionbar(name, messageParameter);
                    actionConsumer.accept(ctx, action);
                } catch (CommandException e) {
                    throw e;
                } catch (Exception e) {
                    Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                    throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
                }
                return Command.SINGLE_SUCCESS;
            })
            .add("message", "var.message", () -> CommandManager.argument("message", StringArgumentType.greedyString()).suggests(suggestion))
            .build(CommandManager.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }
}
