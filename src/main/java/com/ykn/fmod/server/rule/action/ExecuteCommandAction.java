/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.command.RuleComponentSuggestion;
import com.ykn.fmod.server.base.util.RedirectedCommandOutput;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * A {@link RuleAction} that executes a command as a specified entity with a given permission level.
 *
 * <p>The {@code entity} parameter identifies a single entity (by UUID) that acts as the
 * command source. The {@code permissionLevel} field is hidden from interactive command
 * creation - it is always captured from the {@link CommandSourceStack#getPermissionLevel()}
 * of the operator who created the action. The {@code command} parameter is the raw command
 * string to execute.
 *
 * <p>The {@code permissionLevel} is validated on every execution; values outside the
 * range {@code [0, 4]} cause the action to abort and record an error message on the
 * {@link RuleContext}.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "entity":          {"constant": "550e8400-e29b-41d4-a716-446655440000"},
 *   "permissionLevel": 2,
 *   "command":         {"constant": "say Hello, world!"}
 * }
 * }</pre>
 *
 * @see Util#runCommand
 * @see RedirectedCommandOutput
 */
public class ExecuteCommandAction implements RuleAction {

    private final String name;

    private final RuleParameter<UUID> entity;

    /**
     * The permission level used when executing the command.
     *
     * <p>This field is intentionally <em>not</em> exposed as a command argument to prevent
     * privilege escalation. When an action is created via the {@code /f rule edit} command,
     * this value is always set to the permission level of the issuing {@link CommandSourceStack}.
     *
     * <p>Valid range: {@code 0} – {@code 4} (inclusive). Any other value causes
     * {@link #execute(RuleContext)} to return {@code false} and set an error message.
     */
    private final int permissionLevel;

    private final RuleParameter<String> command;

    public static final String TYPE = "ExecuteCommand";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.runcmd.summary")
        .add(ParamKind.ENTITY, "fmod.rule.action.runcmd.param.entity.name", "fmod.rule.action.runcmd.param.entity.desc", "entity", "var.entity")
        .add(ParamKind.GREEDY_STRING, "fmod.rule.action.runcmd.param.command.name", "fmod.rule.action.runcmd.param.command.desc", "command", "var.command");

    /**
     * Creates an {@code ExecuteCommandAction}.
     *
     * @param name            the unique name of this action instance within the rule
     * @param entity          the UUID of the entity to use as the command source
     * @param permissionLevel the permission level to run the command with (must be 0–4)
     * @param command         the command string to execute
     */
    public ExecuteCommandAction(String name, RuleParameter<UUID> entity, int permissionLevel, RuleParameter<String> command) {
        this.name = name;
        this.entity = entity;
        this.permissionLevel = permissionLevel;
        this.command = command;
    }

    @Override
    public boolean execute(RuleContext context) {
        if (this.permissionLevel < 0 || this.permissionLevel > 4) {
            context.setErrorMessage(Util.parseTranslatableText("fmod.rule.action.runcmd.error.permission"));
            return false;
        }

        UUID entityId = this.entity.resolve(context, UUID.class);
        String command = this.command.resolve(context, String.class);

        if (entityId == null || command == null) {
            return true;
        }

        Entity sourceEntity = null;
        for (ServerLevel world : context.getServer().getAllLevels()) {
            sourceEntity = world.getEntity(entityId);
            if (sourceEntity != null) {
                break;
            }
        }
        if (sourceEntity == null) {
            return true;
        }

        TextPlaceholderFactory<Entity> factory = TextPlaceholderFactory.empty();
        for (String variable : context.getVariables().keySet()) {
            Object value = context.getVariable(variable);
            Component toShow = value == null ? Component.literal("${var:" + variable + "}") : Component.literal(TypeAdaptor.parse(value).asString());
            factory = factory.add("${var:" + variable + "}", entity -> toShow);
        }
        command = factory.parsePlaceholders(command, sourceEntity).getString();

        RedirectedCommandOutput output = RedirectedCommandOutput.create();
        Util.runCommand(output, sourceEntity, command, this.permissionLevel);
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleAction setName(String name) {
        return new ExecuteCommandAction(name, this.entity, this.permissionLevel, this.command);
    }

    /**
     * Returns only the {@code entity} and {@code command} parameters, matching
     * {@link #getParameters()}'s two-entry {@code PARAM_METADATA}. {@code permissionLevel}
     * is intentionally excluded, matching its exclusion from interactive command creation.
     */
    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.entity, this.command);
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
        return Util.parseTranslatableText("fmod.rule.action.runcmd", this.getName(), this.getType(),
            this.entity.render(), this.command.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("entity", RuleParameter.toJson(entity, e -> new JsonPrimitive(e.toString())));
        json.addProperty("permissionLevel", this.permissionLevel);
        json.add("command", RuleParameter.toJson(command, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(ExecuteCommandAction action) {
        return action.toJson();
    }

    public static ExecuteCommandAction fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        RuleParameter<UUID> entity = RuleParameter.fromJson(json, "entity", e -> UUID.fromString(e.getAsString()));
        int permissionLevel = 0;
        if (json.has("value") && json.get("value").isJsonObject() && json.getAsJsonObject("value").has("permissionLevel") && json.getAsJsonObject("value").get("permissionLevel").isJsonPrimitive()) {
            permissionLevel = json.getAsJsonObject("value").get("permissionLevel").getAsInt();
        } else {
            Util.LOGGER.warn("FMinecraftMod: ExecuteCommandAction JSON is missing 'permissionLevel' field or it is not an integer. Defaulting to 0.");
        }
        if (permissionLevel < 0 || permissionLevel > 4) {
            Util.LOGGER.warn("FMinecraftMod: ExecuteCommandAction has an invalid permission level " + permissionLevel);
        }
        RuleParameter<String> command = RuleParameter.fromJson(json, "command", JsonElement::getAsString);
        return new ExecuteCommandAction(name, entity, permissionLevel, command);
    }

    @SuppressWarnings("unchecked")
    public static RuleAction withParameters(String name, List<RuleParameter<?>> values) {
        return new ExecuteCommandAction(name, (RuleParameter<UUID>) values.get(0), 3, (RuleParameter<String>) values.get(1));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleAction> actionConsumer) {
        RuleComponentSuggestion suggestion = RuleComponentSuggestion.suggestPlaceholder(3);
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<UUID> entityParameter = builder.resolveParameter(0, arguments, ctx);
                int permissionLevel = ctx.getSource().hasPermission(3) ? 3 : 0;
                RuleParameter<String> commandParameter = builder.resolveParameter(1, arguments, ctx);
                ExecuteCommandAction action = new ExecuteCommandAction(name, entityParameter, permissionLevel, commandParameter);
                actionConsumer.accept(ctx, action);
            })
            .add(PARAM_METADATA.get(0))
            .add(PARAM_METADATA.get(1), suggestion);
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }
}
