/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.List;

import com.ykn.fmod.server.base.command.SayCommandSuggestion;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.TextPlaceholderFactory;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;

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

    public static final String TYPE = "BroadcastMessage";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.bcmessage.summary")
        .add(ParamKind.GREEDY_STRING, "fmod.rule.action.bcmessage.param.message.name", "fmod.rule.action.bcmessage.param.message.desc", "message", "var.message", "",
            SayCommandSuggestion.suggest().add("${", "var:"));

    /**
     * Creates a {@code BroadcastMessage} action.
     *
     * @param name   the unique name of this action instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code message}
     */
    @SuppressWarnings("unchecked")
    public BroadcastMessage(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.message = (RuleParameter<String>) values.get(0);
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
        return new BroadcastMessage(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.message);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
    }

}
