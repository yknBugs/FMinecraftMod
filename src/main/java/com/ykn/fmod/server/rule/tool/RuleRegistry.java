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

import javax.annotation.Nullable;

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
import com.ykn.fmod.server.rule.core.RuleAction;
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

    private static void registerCore() {
        RuleRegistry.register("Dummy", DummyEvent::getInstance);

        RuleRegistry.register("BinaryConditionExpression", BinaryConditionExpression::fromJson);
        RuleRegistry.register("ConditionReference", ConditionReference::fromJson);
        RuleRegistry.register("ConstCondition", ConstCondition::fromJson);
        RuleRegistry.register("UnaryConditionExpression", UnaryConditionExpression::fromJson);
    }

    /**
     * Registers all built-in event, condition, and action types.
     *
     * <p>Call this once during mod initialisation (e.g. from your
     * {@code ModInitializer.onInitialize()}) before any rules are loaded or commands are registered.
     */
    public static void registerDefault() {
        registerCore();

        RuleRegistry.register("ServerTickEvent", ServerTickEvent::getInstance);
        RuleRegistry.register("EntityDamageEvent", EntityDamageEvent::getInstance);
        RuleRegistry.register("EntityDeathEvent", EntityDeathEvent::getInstance);
        RuleRegistry.register("PlayerTickEvent", PlayerTickEvent::getInstance);
        RuleRegistry.register("ProjectileHitEntityEvent", ProjectileHitEntityEvent::getInstance);

        RuleRegistry.register("EntityPosition", EntityPosition::fromJson);
        RuleRegistry.register("CheckPermission", CheckPermission::fromJson);
        RuleRegistry.register("SmallerThan", SmallerThan::fromJson);
        RuleRegistry.register("EqualsTo", EqualsTo::fromJson);
        RuleRegistry.register("CheckBlockType", CheckBlockType::fromJson);
        RuleRegistry.register("CheckEntityType", CheckEntityType::fromJson);
        RuleRegistry.register("HasEntityType", HasEntityType::fromJson);

        RuleRegistry.register("BroadcastMessage", BroadcastMessage::fromJson);
        RuleRegistry.register("SendMessage", SendMessage::fromJson);
        RuleRegistry.register("BroadcastActionbar", BroadcastActionbar::fromJson);
        RuleRegistry.register("SendActionbar", SendActionbar::fromJson);
        RuleRegistry.register("ExecuteCommand", ExecuteCommandAction::fromJson);
        RuleRegistry.register("RunFlow", RunFlowAction::fromJson);

        RuleRegistry.register("EntityPosition", EntityPosition::buildCommand);
        RuleRegistry.register("CheckPermission", CheckPermission::buildCommand);
        RuleRegistry.register("SmallerThan", SmallerThan::buildCommand);
        RuleRegistry.register("EqualsTo", EqualsTo::buildCommand);
        RuleRegistry.register("BlockType", CheckBlockType::buildCommand);
        RuleRegistry.register("CheckEntityType", CheckEntityType::buildCommand);
        RuleRegistry.register("HasEntityType", HasEntityType::buildCommand);

        RuleRegistry.register("BroadcastMessage", BroadcastMessage::buildCommand);
        RuleRegistry.register("SendMessage", SendMessage::buildCommand);
        RuleRegistry.register("BroadcastActionbar", BroadcastActionbar::buildCommand);
        RuleRegistry.register("SendActionbar", SendActionbar::buildCommand);
        RuleRegistry.register("ExecuteCommand", ExecuteCommandAction::buildCommand);
        RuleRegistry.register("RunFlow", RunFlowAction::buildCommand);
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
}
