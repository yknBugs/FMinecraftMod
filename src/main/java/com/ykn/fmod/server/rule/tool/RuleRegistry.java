/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.rule.action.*;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleEvent;
import com.ykn.fmod.server.rule.core.UnaryConditionExpression;
import com.ykn.fmod.server.rule.event.*;

import net.minecraft.commands.CommandSourceStack;

import com.ykn.fmod.server.rule.core.BinaryConditionExpression;
import com.ykn.fmod.server.rule.core.ConditionReference;
import com.ykn.fmod.server.rule.core.ConstCondition;
import com.ykn.fmod.server.rule.core.DummyEvent;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.cond.*;

/**
 * Central factory registry for all rule component types.
 *
 * <p>Each component type (events, conditions, actions) is identified by a string key equal
 * to its {@link com.ykn.fmod.server.rule.core.RuleComponent#getType() type} and mapped to a
 * factory that can:
 * <ul>
 *   <li><b>events</b>     – create or return a singleton {@link RuleEvent} instance</li>
 *   <li><b>conditions</b> – deserialize a {@link RuleCondition} from a {@link JsonObject}</li>
 *   <li><b>actions</b>    – deserialize a {@link RuleAction} from a {@link JsonObject}</li>
 *   <li><b>command factories</b> – add Brigadier sub-commands for interactive condition/action creation</li>
 * </ul>
 *
 * <p>All built-in types are registered by calling {@link #registerDefault()} during mod initialization.
 * Third-party code can add custom types by calling the appropriate
 * {@link #register(String, RuleEventFactory)}, {@link #register(String, ConditionFactory)},
 * or {@link #register(String, RuleActionFactory)} overloads.
 *
 * <p>Usage in {@link RuleSerializer}:
 * <pre>{@code
 * RuleCondition cond = RuleRegistry.createCondition(jsonObject);
 * RuleAction    act  = RuleRegistry.createRuleAction(jsonObject);
 * RuleEvent     evt  = RuleRegistry.createRuleEvent("TickEvent");
 * }</pre>
 *
 * @see RuleSerializer
 * @see com.ykn.fmod.server.rule.core.RuleComponent#getType()
 */
public class RuleRegistry {

    private static final Map<String, RuleEventFactory> events = new HashMap<>();

    private static final Map<String, ConditionFactory> conditions = new HashMap<>();

    private static final Map<String, RuleActionFactory> actions = new HashMap<>();

    private static final Map<String, ConditionCommandFactory> conditionCommands = new HashMap<>();

    private static final Map<String, RuleActionCommandFactory> actionCommands = new HashMap<>();

    private static final Map<String, ConditionParamsFactory> conditionParams = new HashMap<>();

    private static final Map<String, ActionParamsFactory> actionParams = new HashMap<>();

    private static final Map<String, RequiredParamMetadata> requiredParamMetadata = new HashMap<>();

    /** 
     * Factory that creates (or returns the singleton of) a {@link RuleEvent}. 
     */
    @FunctionalInterface
    public interface RuleEventFactory {
        RuleEvent create();
    }

    /** 
     * Factory that deserializes a {@link RuleCondition} from its JSON object. 
     */
    @FunctionalInterface
    public interface ConditionFactory {
        RuleCondition create(JsonObject json);
    }

    /**
     * Factory that adds Brigadier command sub-nodes for interactive condition creation.
     *
     * <p>When invoked, the factory must attach its sub-tree to {@code commandNode} and
     * invoke {@code conditionConsumer} with the fully constructed condition when the
     * command is executed by a player.
     */
    @FunctionalInterface
    public interface ConditionCommandFactory {
        LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> conditionConsumer);
    }

    /** 
     * Factory that deserializes a {@link RuleAction} from its JSON object. 
     */
    @FunctionalInterface
    public interface RuleActionFactory {
        RuleAction create(JsonObject json);
    }

    /**
     * Factory that adds Brigadier command sub-nodes for interactive action creation.
     *
     * <p>When invoked, the factory must attach its sub-tree to {@code commandNode} and
     * invoke {@code actionConsumer} with the fully constructed action when the
     * command is executed by a player.
     */
    @FunctionalInterface
    public interface RuleActionCommandFactory {
        LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleAction> actionConsumer);
    }

    @FunctionalInterface
    public interface ConditionParamsFactory {
        SourceCondition create(String name, List<RuleParameter<?>> values);
    }

    @FunctionalInterface
    public interface ActionParamsFactory {
        RuleAction create(String name, List<RuleParameter<?>> values);
    }

    /**
     * Registers a new event factory under {@code name}.
     *
     * @param name    the type string used in rule JSON ({@code "event"} field)
     * @param factory the factory that returns the event instance
     */
    public static void register(String name, RuleEventFactory factory) {
        events.put(name, factory);
    }

    /**
     * Registers a new condition factory under {@code name}.
     *
     * @param name    the type string used in condition JSON ({@code "type"} field)
     * @param factory the factory that deserializes a condition from JSON
     */
    public static void register(String name, ConditionFactory factory) {
        conditions.put(name, factory);
    }

    /**
     * Registers a new action factory under {@code name}.
     *
     * @param name    the type string used in action JSON ({@code "type"} field)
     * @param factory the factory that deserializes an action from JSON
     */
    public static void register(String name, RuleActionFactory factory) {
        actions.put(name, factory);
    }

    /**
     * Registers a condition command factory under {@code name}.
     *
     * <p>This enables the {@code /f rule edit ... cond add <type>} command to
     * interactively create conditions of this type.
     *
     * @param name    the type string matching the condition's type
     * @param factory the factory that builds the Brigadier command sub-tree
     */
    public static void register(String name, ConditionCommandFactory factory) {
        conditionCommands.put(name, factory);
    }

    /**
     * Registers an action command factory under {@code name}.
     *
     * <p>This enables the {@code /f rule edit ... action add <type>} command to
     * interactively create actions of this type.
     *
     * @param name    the type string matching the action's type
     * @param factory the factory that builds the Brigadier command sub-tree
     */
    public static void register(String name, RuleActionCommandFactory factory) {
        actionCommands.put(name, factory);
    }

    /**
     * Registers the required parameter metadata for a given type.
     * 
     * <p>This gives the ability to get the required parameters without need to
     * construct the condition or action instance.
     *
     * @param name     the type string matching the condition or action's type
     * @param metadata the required parameter metadata
     */
    public static void register(String name, RequiredParamMetadata metadata) {
        requiredParamMetadata.put(name, metadata);
    }

    /**
     * Registers a factory that builds a {@link SourceCondition} directly from a name and a list of
     * parameter values, bypassing JSON deserialization. Used by the GUI editor's
     * {@code ParamEditorScreen} to construct a condition instance from user-supplied values.
     *
     * @param name    the type string matching the condition's type
     * @param factory the factory that creates a condition from a name and parameter values
     */
    public static void register(String name, ConditionParamsFactory factory) {
        conditionParams.put(name, factory);
    }

    /**
     * Registers a factory that builds a {@link RuleAction} directly from a name and a list of
     * parameter values, bypassing JSON deserialization. Used by the GUI editor's
     * {@code ParamEditorScreen} to construct an action instance from user-supplied values.
     *
     * @param name    the type string matching the action's type
     * @param factory the factory that creates an action from a name and parameter values
     */
    public static void register(String name, ActionParamsFactory factory) {
        actionParams.put(name, factory);
    }

    /**
     * Creates the {@link RuleEvent} instance for the given type string.
     *
     * @param name the event type string (e.g. {@code "TickEvent"})
     * @return the event instance, or {@code null} if the type is not registered
     */
    @Nullable
    public static RuleEvent createRuleEvent(String name) {
        RuleEventFactory factory = events.get(name);
        if (factory == null) {
            return null;
        }
        return factory.create();
    }

    /**
     * Deserializes the {@link RuleCondition} whose type matches {@code name} from {@code json}.
     *
     * @param name the type string
     * @param json the condition JSON object
     * @return a new condition, or {@code null} if the type is not registered
     */
    @Nullable
    public static RuleCondition createCondition(String name, JsonObject json) {
        ConditionFactory factory = conditions.get(name);
        if (factory == null) {
            return null;
        }
        return factory.create(json);
    }

    /**
     * Deserializes a {@link RuleCondition} by reading the {@code "type"} field from {@code json}
     * and delegating to the matching factory.
     *
     * @param json the condition JSON object (must contain a {@code "type"} field)
     * @return a new condition, or {@code null} if the type is not registered
     */
    @Nullable
    public static RuleCondition createCondition(JsonObject json) {
        if (!json.has("type") || !json.get("type").isJsonPrimitive()) {
            return null;
        }
        String type = json.get("type").getAsString();
        return createCondition(type, json);
    }

    /**
     * Builds the {@link SourceCondition} whose type matches {@code type} directly from
     * {@code name} and {@code values}, via the {@link ConditionParamsFactory} registered for
     * {@code type} - no placeholder/blank instance is ever constructed.
     *
     * @param type   the type string (the registry lookup key)
     * @param name   the new instance's own name
     * @param values the new instance's parameter values, in {@code getParameters()} order
     * @return a new condition, or {@code null} if {@code type} is not registered
     */
    @Nullable
    public static SourceCondition createCondition(String type, String name, List<RuleParameter<?>> values) {
        ConditionParamsFactory factory = conditionParams.get(type);
        if (factory == null) {
            return null;
        }
        return factory.create(name, values);
    }

    /**
     * Deserializes the {@link RuleAction} whose type matches {@code name} from {@code json}.
     *
     * @param name the type string
     * @param json the action JSON object
     * @return a new action, or {@code null} if the type is not registered
     */
    @Nullable
    public static RuleAction createRuleAction(String name, JsonObject json) {
        RuleActionFactory factory = actions.get(name);
        if (factory == null) {
            return null;
        }
        return factory.create(json);
    }

    /**
     * Deserializes a {@link RuleAction} by reading the {@code "type"} field from {@code json}
     * and delegating to the matching factory.
     *
     * @param json the action JSON object (must contain a {@code "type"} field)
     * @return a new action, or {@code null} if the type is not registered
     */
    @Nullable
    public static RuleAction createRuleAction(JsonObject json) {
        if (!json.has("type") || !json.get("type").isJsonPrimitive()) {
            return null;
        }
        String type = json.get("type").getAsString();
        return createRuleAction(type, json);
    }

    /**
     * Builds the {@link RuleAction} whose type matches {@code type} directly from {@code name}
     * and {@code values}, via the {@link ActionParamsFactory} registered for {@code type} - no
     * placeholder/blank instance is ever constructed.
     *
     * @param type   the type string (the registry lookup key)
     * @param name   the new instance's own name
     * @param values the new instance's parameter values, in {@code getParameters()} order
     * @return a new action, or {@code null} if {@code type} is not registered
     */
    @Nullable
    public static RuleAction createRuleAction(String type, String name, List<RuleParameter<?>> values) {
        ActionParamsFactory factory = actionParams.get(type);
        if (factory == null) {
            return null;
        }
        return factory.create(name, values);
    }

    private static void registerCore() {
        RuleRegistry.register(DummyEvent.TYPE, DummyEvent::getInstance);

        RuleRegistry.register(BinaryConditionExpression.TYPE, BinaryConditionExpression::fromJson);
        RuleRegistry.register(ConditionReference.TYPE, ConditionReference::fromJson);
        RuleRegistry.register(ConstCondition.TYPE, ConstCondition::fromJson);
        RuleRegistry.register(UnaryConditionExpression.TYPE, UnaryConditionExpression::fromJson);
    }

    /**
     * Registers all built-in event, condition, and action types.
     *
     * <p>Call this once during mod initialisation (e.g. from your
     * {@code ModInitializer.onInitialize()}) before any rules are loaded or commands are registered.
     */
    public static void registerDefault() {
        registerCore();

        RuleRegistry.register(ServerTickEvent.TYPE, ServerTickEvent::getInstance);
        RuleRegistry.register(EntityDamageEvent.TYPE, EntityDamageEvent::getInstance);
        RuleRegistry.register(EntityDeathEvent.TYPE, EntityDeathEvent::getInstance);
        RuleRegistry.register(PlayerJoinEvent.TYPE, PlayerJoinEvent::getInstance);
        RuleRegistry.register(PlayerTickEvent.TYPE, PlayerTickEvent::getInstance);
        RuleRegistry.register(ProjectileHitEntityEvent.TYPE, ProjectileHitEntityEvent::getInstance);

        RuleRegistry.register(EntityPosition.TYPE, EntityPosition::fromJson);
        RuleRegistry.register(CheckPermission.TYPE, CheckPermission::fromJson);
        RuleRegistry.register(SmallerThan.TYPE, SmallerThan::fromJson);
        RuleRegistry.register(EqualsTo.TYPE, EqualsTo::fromJson);
        RuleRegistry.register(CheckBlockType.TYPE, CheckBlockType::fromJson);
        RuleRegistry.register(CheckEntityType.TYPE, CheckEntityType::fromJson);
        RuleRegistry.register(HasEntityType.TYPE, HasEntityType::fromJson);

        RuleRegistry.register(BroadcastMessage.TYPE, BroadcastMessage::fromJson);
        RuleRegistry.register(SendMessage.TYPE, SendMessage::fromJson);
        RuleRegistry.register(BroadcastActionbar.TYPE, BroadcastActionbar::fromJson);
        RuleRegistry.register(SendActionbar.TYPE, SendActionbar::fromJson);
        RuleRegistry.register(ExecuteCommandAction.TYPE, ExecuteCommandAction::fromJson);
        RuleRegistry.register(RunFlowAction.TYPE, RunFlowAction::fromJson);

        RuleRegistry.register(EntityPosition.TYPE, EntityPosition::buildCommand);
        RuleRegistry.register(CheckPermission.TYPE, CheckPermission::buildCommand);
        RuleRegistry.register(SmallerThan.TYPE, SmallerThan::buildCommand);
        RuleRegistry.register(EqualsTo.TYPE, EqualsTo::buildCommand);
        RuleRegistry.register(CheckBlockType.TYPE, CheckBlockType::buildCommand);
        RuleRegistry.register(CheckEntityType.TYPE, CheckEntityType::buildCommand);
        RuleRegistry.register(HasEntityType.TYPE, HasEntityType::buildCommand);

        RuleRegistry.register(BroadcastMessage.TYPE, BroadcastMessage::buildCommand);
        RuleRegistry.register(SendMessage.TYPE, SendMessage::buildCommand);
        RuleRegistry.register(BroadcastActionbar.TYPE, BroadcastActionbar::buildCommand);
        RuleRegistry.register(SendActionbar.TYPE, SendActionbar::buildCommand);
        RuleRegistry.register(ExecuteCommandAction.TYPE, ExecuteCommandAction::buildCommand);
        RuleRegistry.register(RunFlowAction.TYPE, RunFlowAction::buildCommand);
        
        RuleRegistry.register(EntityPosition.TYPE, EntityPosition.PARAM_METADATA);
        RuleRegistry.register(CheckPermission.TYPE, CheckPermission.PARAM_METADATA);
        RuleRegistry.register(SmallerThan.TYPE, SmallerThan.PARAM_METADATA);
        RuleRegistry.register(EqualsTo.TYPE, EqualsTo.PARAM_METADATA);
        RuleRegistry.register(CheckBlockType.TYPE, CheckBlockType.PARAM_METADATA);
        RuleRegistry.register(CheckEntityType.TYPE, CheckEntityType.PARAM_METADATA);
        RuleRegistry.register(HasEntityType.TYPE, HasEntityType.PARAM_METADATA);

        RuleRegistry.register(BroadcastMessage.TYPE, BroadcastMessage.PARAM_METADATA);
        RuleRegistry.register(SendMessage.TYPE, SendMessage.PARAM_METADATA);
        RuleRegistry.register(BroadcastActionbar.TYPE, BroadcastActionbar.PARAM_METADATA);
        RuleRegistry.register(SendActionbar.TYPE, SendActionbar.PARAM_METADATA);
        RuleRegistry.register(ExecuteCommandAction.TYPE, ExecuteCommandAction.PARAM_METADATA);
        RuleRegistry.register(RunFlowAction.TYPE, RunFlowAction.PARAM_METADATA);

        RuleRegistry.register(EntityPosition.TYPE, EntityPosition::withParameters);
        RuleRegistry.register(CheckPermission.TYPE, CheckPermission::withParameters);
        RuleRegistry.register(SmallerThan.TYPE, SmallerThan::withParameters);
        RuleRegistry.register(EqualsTo.TYPE, EqualsTo::withParameters);
        RuleRegistry.register(CheckBlockType.TYPE, CheckBlockType::withParameters);
        RuleRegistry.register(CheckEntityType.TYPE, CheckEntityType::withParameters);
        RuleRegistry.register(HasEntityType.TYPE, HasEntityType::withParameters);

        RuleRegistry.register(BroadcastMessage.TYPE, BroadcastMessage::withParameters);
        RuleRegistry.register(SendMessage.TYPE, SendMessage::withParameters);
        RuleRegistry.register(BroadcastActionbar.TYPE, BroadcastActionbar::withParameters);
        RuleRegistry.register(SendActionbar.TYPE, SendActionbar::withParameters);
        // permissionLevel is intentionally hidden from PARAM_METADATA. Default it to 3, matching the command path's own op-level cap.
        RuleRegistry.register(ExecuteCommandAction.TYPE, ExecuteCommandAction::withParameters);
        RuleRegistry.register(RunFlowAction.TYPE, RunFlowAction::withParameters);
    }

    /**
     * Returns a snapshot of all registered event type names.
     *
     * @return an unmodifiable collection of event type strings
     */
    public static Collection<String> getRegisteredEventNames() {
        return List.copyOf(events.keySet());
    }

    /**
     * Returns a snapshot of all registered condition type names.
     *
     * @return an unmodifiable collection of condition type strings
     */
    public static Collection<String> getRegisteredConditionNames() {
        return List.copyOf(conditions.keySet());
    }

    /**
     * Returns a snapshot of all registered action type names.
     *
     * @return an unmodifiable collection of action type strings
     */
    public static Collection<String> getRegisteredActionNames() {
        return List.copyOf(actions.keySet());
    }

    /**
     * Returns a snapshot of all type names for which a condition command factory is registered.
     *
     * @return an unmodifiable collection of condition command type strings
     */
    public static Collection<String> getRegisteredConditionCommandNames() {
        return List.copyOf(conditionCommands.keySet());
    }

    /**
     * Returns the {@link ConditionCommandFactory} registered under {@code name}, or {@code null}.
     *
     * @param name the condition type string
     * @return the factory, or {@code null} if not registered
     */
    public static ConditionCommandFactory getConditionCommandFactory(String name) {
        return conditionCommands.get(name);
    }

    /**
     * Returns a snapshot of all type names for which an action command factory is registered.
     *
     * @return an unmodifiable collection of action command type strings
     */
    public static Collection<String> getRegisteredActionCommandNames() {
        return List.copyOf(actionCommands.keySet());
    }

    /**
     * Returns the {@link RuleActionCommandFactory} registered under {@code name}, or {@code null}.
     *
     * @param name the action type string
     * @return the factory, or {@code null} if not registered
     */
    public static RuleActionCommandFactory getActionCommandFactory(String name) {
        return actionCommands.get(name);
    }

    /**
     * Returns the {@link RequiredParamMetadata} registered under {@code name}, or {@code null}.
     *
     * @param name the condition or action type string
     * @return the metadata, or {@code null} if not registered
     */
    public static RequiredParamMetadata getRequiredParamMetadata(String name) {
        return requiredParamMetadata.get(name);
    }
}
