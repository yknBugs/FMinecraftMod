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
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ComponentCall;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.Params;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Actions that show text to players.
 *
 * <p>All four share one operational shape: build the {@code ${var:...}} placeholder table from the
 * current rule variables, parse the message, hand it to a target (broadcast or one player) through
 * one {@link MessageType} method. Only the target and the chat channel differ, so the shared part
 * lives in {@link #parse}.
 *
 * <p>A failed delivery is not fatal: the player may have disconnected between resolution and send.
 * It records {@code fmod.rule.action.message.deliveryfailed} and the remaining actions still run.
 * A variable whose {@code toString()} throws is a bug rather than a delivery problem, so it is
 * logged as an error and rethrown, aborting the current evaluation.
 */
public final class MessageActions {

    private MessageActions() {
    }

    /**
     * Registers every message action type.
     */
    public static void register() {
        Components.action("BroadcastMessage", Params.of("fmod.rule.action.bcmessage")
                .defaulted("message", ParamKind.GREEDY_STRING, "", SayCommandSuggestion.suggest().add("${", "var:")),
                (call) -> {
                    Component message = parse(call, call.text(0), null);
                    try {
                        MessageType.broadcastTextMessage(call.context().getServer(), message);
                    } catch (RuntimeException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.action.message.deliveryfailed"));
                    }
                    return true;
                });

        Components.action("SendMessage", Params.of("fmod.rule.action.sendmessage")
                .param("players", ParamKind.PLAYERS)
                .suggested("message", ParamKind.GREEDY_STRING, SayCommandSuggestion.suggestDefault().add("${", "var:"))
                .allowNulls(),
                (call) -> {
                    List<UUID> players = players(call);
                    if (players == null) {
                        call.warnNull(0);
                        return true;
                    }
                    if (call.isNull(1)) {
                        call.warnNull(1);
                        return true;
                    }
                    String raw = call.text(1);
                    for (UUID id : players) {
                        ServerPlayer player = call.context().getServer().getPlayerList().getPlayer(id);
                        if (player == null) {
                            continue;
                        }
                        Component message = parse(call, raw, player);
                        try {
                            MessageType.sendTextMessage(player, message);
                        } catch (RuntimeException exception) {
                            call.warn(Util.parseTranslatableText("fmod.rule.action.message.deliveryfailed"));
                        }
                    }
                    return true;
                });

        Components.action("BroadcastActionbar", Params.of("fmod.rule.action.bcactionbar")
                .defaulted("message", ParamKind.GREEDY_STRING, "", SayCommandSuggestion.suggest().add("${", "var:")),
                (call) -> {
                    Component message = parse(call, call.text(0), null);
                    try {
                        MessageType.broadcastActionBarMessage(call.context().getServer(), message);
                    } catch (RuntimeException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.action.message.deliveryfailed"));
                    }
                    return true;
                });

        Components.action("SendActionbar", Params.of("fmod.rule.action.sendactionbar")
                .param("players", ParamKind.PLAYERS)
                .suggested("message", ParamKind.GREEDY_STRING, SayCommandSuggestion.suggestDefault().add("${", "var:"))
                .allowNulls(),
                (call) -> {
                    List<UUID> players = players(call);
                    if (players == null) {
                        call.warnNull(0);
                        return true;
                    }
                    if (call.isNull(1)) {
                        call.warnNull(1);
                        return true;
                    }
                    String raw = call.text(1);
                    for (UUID id : players) {
                        ServerPlayer player = call.context().getServer().getPlayerList().getPlayer(id);
                        if (player == null) {
                            continue;
                        }
                        Component message = parse(call, raw, player);
                        try {
                            MessageType.sendActionBarMessage(player, message);
                        } catch (RuntimeException exception) {
                            call.warn(Util.parseTranslatableText("fmod.rule.action.message.deliveryfailed"));
                        }
                    }
                    return true;
                });
    }

    /**
     * Resolves the {@code players} parameter, which may hold either one UUID or a list of them.
     *
     * <p>This is the one place where the shared accessors are not enough: {@link ParamKind#PLAYERS}
     * declares a list, but a rule may bind the parameter to a variable that holds a single UUID, so
     * the raw declaration is inspected instead of the pre-resolved value.
     *
     * @param call the current evaluation
     * @return the player ids, or {@code null} when the parameter had neither a usable variable nor a constant
     */
    private static List<UUID> players(ComponentCall call) {
        RuleParameter<?> parameter = call.parameter(0);
        String variable = parameter.getVariableName();
        if (variable != null) {
            Object value = call.context().getVariable(variable);
            if (value instanceof UUID uuid) {
                List<UUID> single = new ArrayList<>();
                single.add(uuid);
                return single;
            }
            if (value instanceof List<?> list) {
                List<UUID> many = new ArrayList<>();
                for (Object element : list) {
                    if (element instanceof UUID uuid) {
                        many.add(uuid);
                    }
                }
                if (!many.isEmpty()) {
                    return many;
                }
            }
        }
        Object constant = parameter.getConstantValue();
        if (constant instanceof List<?> list) {
            List<UUID> many = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof UUID uuid) {
                    many.add(uuid);
                }
            }
            return many.isEmpty() ? null : many;
        }
        return null;
    }

    /**
     * Builds the placeholder table for the current rule variables and parses the message with it.
     *
     * @param call    the current evaluation, supplying the rule variables
     * @param message the raw message text
     * @param viewer  the player the message is rendered for, or {@code null} when broadcasting
     * @return the parsed message component
     */
    private static Component parse(ComponentCall call, String message, ServerPlayer viewer) {
        TextPlaceholderFactory<ServerPlayer> factory = TextPlaceholderFactory.ofDefault();
        for (String variable : call.context().getVariables().keySet()) {
            Object value = call.context().getVariable(variable);
            Component shown = value == null
                    ? Component.literal("${var:" + variable + "}")
                    : Component.literal(render(call, variable, value));
            factory = factory.add("${var:" + variable + "}", (player) -> shown);
        }
        return factory.parse(message, viewer);
    }

    /**
     * Renders one rule variable as text for {@code ${var:...}}.
     *
     * <p>A throwing {@code toString()} means the variable holds something the text layer cannot
     * describe, which is a bug rather than a user error: log it with a stack trace and let it abort
     * the evaluation instead of silently rendering a placeholder.
     *
     * @param call     the current evaluation
     * @param variable the variable name, for the log message
     * @param value    the non-null variable value
     * @return the text representation
     */
    private static String render(ComponentCall call, String variable, Object value) {
        try {
            return TypeAdaptor.parse(value).asString();
        } catch (RuntimeException exception) {
            Util.LOGGER.error("FMinecraftMod: Could not render rule variable '" + variable + "' as text", exception);
            throw exception;
        }
    }
}
