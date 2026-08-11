/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
        .add(ParamKind.GREEDY_STRING, "fmod.rule.action.sendactionbar.param.message.name", "fmod.rule.action.sendactionbar.param.message.desc", "message", "var.message");

    /**
     * Creates a {@code SendActionbar} action.
     *
     * @param name    the unique name of this action instance within the rule
     * @param players the list of target player UUIDs
     * @param message the message parameter (variable reference and/or constant string)
     */
    public SendActionbar(String name, RuleParameter<List<UUID>> players, RuleParameter<String> message) {
        this.name = name;
        this.players = players;
        this.message = message;
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
        if (playerIds == null || message == null) {
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
        return new SendActionbar(name, this.players, this.message);
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

    @Override
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.action.sendactionbar", this.getName(), this.getType(),
            this.players.render(), this.message.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("players", RuleParameter.toJson(players, list -> {
            JsonArray array = new JsonArray();
            for (UUID uuid : list) {
                array.add(uuid.toString());
            }
            return array;
        }));
        json.add("message", RuleParameter.toJson(message, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(SendActionbar action) {
        return action.toJson();
    }

    public static SendActionbar fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        RuleParameter<List<UUID>> players = RuleParameter.fromJson(json, "players", e -> {
            JsonArray array = e.getAsJsonArray();
            List<UUID> uuids = new ArrayList<>();
            for (JsonElement elem : array) {
                uuids.add(UUID.fromString(elem.getAsString()));
            }
            return uuids;
        });
        RuleParameter<String> message = RuleParameter.fromJson(json, "message", JsonElement::getAsString);
        return new SendActionbar(name, players, message);
    }

    /**
     * Creates a new instance directly from a name and parameter values for use by the GUI editor.
     *
     * @param name   the unique name of this action instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order
     * @return a new instance with the given name and values
     */
    @SuppressWarnings("unchecked")
    public static RuleAction withParameters(String name, List<RuleParameter<?>> values) {
        return new SendActionbar(name, (RuleParameter<List<UUID>>) values.get(0), (RuleParameter<String>) values.get(1));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleAction> actionConsumer) {
        SayCommandSuggestion suggestion = SayCommandSuggestion.suggestDefault().add("${", "var:");
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<List<UUID>> playersParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<String> messageParameter = builder.resolveParameter(1, arguments, ctx);
                SendActionbar action = new SendActionbar(name, playersParameter, messageParameter);
                actionConsumer.accept(ctx, action);
            })
            .add(PARAM_METADATA.get(0))
            .add(PARAM_METADATA.get(1), suggestion);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }
}
