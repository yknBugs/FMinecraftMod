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

    public static final String TYPE = "BroadcastActionbar";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.bcactionbar.summary")
        .add(ParamKind.GREEDY_STRING, "fmod.rule.action.bcactionbar.param.message.name", "fmod.rule.action.bcactionbar.param.message.desc", "message", "var.message", "",
            SayCommandSuggestion.suggest().add("${", "var:"));

    /**
     * Creates a {@code BroadcastActionbar} action.
     *
     * @param name   the unique name of this action instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code message}
     */
    @SuppressWarnings("unchecked")
    public BroadcastActionbar(String name, List<RuleParameter<?>> values) {
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
        return new BroadcastActionbar(name, getParameterValues());
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
