/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.ykn.fmod.server.base.command.SayCommandSuggestion;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.TextPlaceholderFactory;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A {@link RuleAction} that sends an action-bar message to a specific set of players.
 *
 * <p>The {@code players} parameter holds a list of player UUIDs (serialized as a
 * {@link com.google.gson.JsonArray} of UUID strings). The {@code message} parameter
 * supports the same {@code ${var:variableName}} placeholder syntax as {@link BroadcastMessage}.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "players": {"constant": ["550e8400-e29b-41d4-a716-446655440000"]},
 *   "message": {"constant": "You are in a restricted zone!"}
 * }
 * }</pre>
 *
 * @see BroadcastActionbar
 * @see MessageType#sendActionBarMessage
 */
public class SendActionbar implements RuleAction {

    private final String name;

    private final RuleParameter<List<UUID>> players;

    private final RuleParameter<String> message;

    public static final String TYPE = "SendActionbar";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.sendactionbar.summary")
        .add(ParamKind.PLAYERS, "fmod.rule.action.sendactionbar.param.players.name", "fmod.rule.action.sendactionbar.param.players.desc", "players", "var.players")
        .add(ParamKind.GREEDY_STRING, "fmod.rule.action.sendactionbar.param.message.name", "fmod.rule.action.sendactionbar.param.message.desc", "message", "var.message",
            SayCommandSuggestion.suggestDefault().add("${", "var:"));

    /**
     * Creates a {@code SendActionbar} action.
     *
     * @param name   the unique name of this action instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code players},
     *               {@code message}
     */
    @SuppressWarnings("unchecked")
    public SendActionbar(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.players = (RuleParameter<List<UUID>>) values.get(0);
        this.message = (RuleParameter<String>) values.get(1);
    }

    /**
     * Resolves the {@code players} parameter to a list of UUIDs.
     *
     * <p>If the parameter has a variable binding, the context variable is inspected:
     * <ul>
     *   <li>A single {@link UUID} is wrapped in a one-element list.</li>
     *   <li>A {@link java.util.List} is iterated; only {@link UUID} elements are kept.</li>
     * </ul>
     * If the variable lookup yields no UUIDs, the constant fallback is returned.
     *
     * @param context the rule execution context
     * @return a list of UUIDs, or {@code null} if neither source provides a value
     */
    private List<UUID> resolvePlayers(RuleContext context) {
        String varName = this.players.getVariableName();
        if (varName != null) {
            Object value = context.getVariable(varName);
            if (value instanceof UUID) {
                List<UUID> result = new ArrayList<>();
                result.add((UUID) value);
                return result;
            }
            if (value instanceof List<?>) {
                List<UUID> result = new ArrayList<>();
                List<?> valueList = (List<?>) value;
                for (Object elem : valueList) {
                    if (elem instanceof UUID) {
                        result.add((UUID) elem);
                    }
                }
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return this.players.getConstantValue();
    }

    @Override
    public boolean execute(RuleContext context) {
        List<UUID> playerIds = resolvePlayers(context);
        String message = this.message.resolve(context, String.class);
        if (playerIds == null) {
            warnNullInput(context, PARAM_METADATA.get(0));
            return true;
        }
        if (message == null) {
            warnNullInput(context, PARAM_METADATA.get(1));
            return true;
        }
        TextPlaceholderFactory<ServerPlayer> factory = TextPlaceholderFactory.ofDefault();
        for (String variable : context.getVariables().keySet()) {
            Object value = context.getVariable(variable);
            Component toShow = value == null ? Component.literal("${var:" + variable + "}") : Component.literal(TypeAdaptor.parse(value).asString());
            factory = factory.add("${var:" + variable + "}", player -> toShow);
        }
        for (UUID playerId : playerIds) {
            ServerPlayer player = context.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                MessageType.sendActionBarMessage(player, factory.parse(message, player));
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleAction setName(String name) {
        return new SendActionbar(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.players, this.message);
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
