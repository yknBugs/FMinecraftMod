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
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * A {@link RuleAction} that broadcasts a text message to all online players.
 *
 * <p>The message is resolved from the {@code message} {@link RuleParameter}: if a variable
 * binding is set and the context variable resolves to a non-null {@link String}, that value
 * is used; otherwise the constant fallback string is used. If both resolve to {@code null},
 * no message is sent and the action returns {@code true} (execution continues).
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "message": {"constant": "Player has entered restricted area!"}
 * }
 * }</pre>
 *
 * @see com.ykn.fmod.server.base.util.ServerMessageType
 */
public class BroadcastMessage implements RuleAction {

    private final String name;

    private final RuleParameter<String> message;

    /**
     * Creates a {@code BroadcastMessage} action.
     *
     * @param name    the unique name of this action instance within the rule
     * @param message the message parameter (variable reference and/or constant string)
     */
    public BroadcastMessage(String name, RuleParameter<String> message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public boolean execute(RuleContext context) {
        String message = this.message.resolve(context, String.class);
        if (message != null) {
            ServerMessageType.broadcastTextMessage(context.getServer(), Text.literal(message));
        }
        return true;   
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleAction setName(String name) {
        return new BroadcastMessage(name, this.message);
    }

    @Override
    public String getType() {
        return "BroadcastMessage";
    }

    @Override
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.action.bcmessage", this.getName(), this.getType(), this.message.render());
    }
    
    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("message", RuleParameter.toJson(message, e -> new JsonPrimitive(e)));
        return json;
    }

    public static JsonObject toJson(BroadcastMessage action) {
        return action.toJson();
    }

    public static BroadcastMessage fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        RuleParameter<String> message = RuleParameter.fromJson(json, "message", e -> e.getAsString());
        return new BroadcastMessage(name, message);
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand(LiteralArgumentBuilder<ServerCommandSource> commandNode, BiConsumer<CommandContext<ServerCommandSource>, RuleAction> actionConsumer) {
        RequiredArgumentBuilder<ServerCommandSource, ?> commandTree = RecursiveCommandBuilder.builder((arguments, ctx) -> {
                try {
                    String name = StringArgumentType.getString(ctx, "name");
                    RuleParameter<String> messageParameter = RuleParameter.fromCommandContext("message", "var.message", () -> {
                        return StringArgumentType.getString(ctx, "message");
                    }, arguments, ctx);
                    BroadcastMessage action = new BroadcastMessage(name, messageParameter);
                    actionConsumer.accept(ctx, action);
                } catch (CommandException e) {
                    throw e;
                } catch (Exception e) {
                    Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit", e);
                    throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
                }
                return Command.SINGLE_SUCCESS;
            })
            .add("message", "var.message", () -> CommandManager.argument("message", StringArgumentType.greedyString()))
            .build(CommandManager.argument("name", StringArgumentType.string()));
        return commandNode.then(commandTree);
    }
}
