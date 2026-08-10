/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.function.BiConsumer;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.command.SayCommandSuggestion;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.TextPlaceholderFactory;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A {@link RuleAction} that broadcasts a text message to all online players.
 *
 * <p>The message is resolved from the {@code message} {@link RuleParameter}: if a variable
 * binding is set and the context variable resolves to a non-null {@link String}, that value
 * is used; otherwise the constant fallback string is used, but you can still use placeholders
 * ${var:variableName} to refer to context variables in the message. If both resolve to {@code null},
 * no message is sent and the action returns {@code true} (execution continues).
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "message": {"constant": "Player ${var:player} has entered restricted area!"}
 * }
 * }</pre>
 *
 * @see com.ykn.fmod.server.base.util.ServerMessageType
 */
public class BroadcastMessage implements RuleAction {

    private final String name;

    private final RuleParameter<String> message;

    private static final String TYPE = "BroadcastMessage";

    private static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.bcmessage.summary")
        .add(ParamKind.GREEDY_STRING, "fmod.rule.action.bcmessage.param.message.name", "fmod.rule.action.bcmessage.param.message.desc", "message", "var.message", "");

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
            TextPlaceholderFactory<ServerPlayer> factory = TextPlaceholderFactory.ofDefault();
            for (String variable : context.getVariables().keySet()) {
                Object value = context.getVariable(variable);
                Component toShow = value == null ? Component.literal("${var:" + variable + "}") : Component.literal(TypeAdaptor.parse(value).asString());
                factory = factory.add("${var:" + variable + "}", player -> toShow);
            }
            MessageType.broadcastTextMessage(context.getServer(), factory.parse(message, null));
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
        return TYPE;
    }

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.action.bcmessage", this.getName(), this.getType(), this.message.render());
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
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

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleAction> actionConsumer) {
        SayCommandSuggestion suggestion = SayCommandSuggestion.suggest().add("${", "var:");
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<String> messageParameter = builder.resolveParameter(0, arguments, ctx);
                BroadcastMessage action = new BroadcastMessage(name, messageParameter);
                actionConsumer.accept(ctx, action);
            })
            .add(PARAM_METADATA.get(0), suggestion);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }
}
