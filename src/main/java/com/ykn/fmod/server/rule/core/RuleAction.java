/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import com.google.gson.JsonObject;

/**
 * Represents a single executable action within a {@link CustomRule}.
 *
 * <p>When a rule's condition evaluates to {@code true}, all actions registered in
 * {@code actionIfSatisfied} are executed in order. When it evaluates to {@code false},
 * all actions in {@code actionIfViolated} are executed instead.
 *
 * <p>To define a new action type, implement this interface and register it with
 * {@link com.ykn.fmod.server.rule.tool.RuleRegistry#register(String, com.ykn.fmod.server.rule.tool.RuleRegistry.RuleActionFactory)}
 * using a unique type string. Provide a static {@code fromJson(JsonObject)} method
 * to enable deserialization.
 *
 * @see CustomRule
 * @see RuleContext
 */
public interface RuleAction extends RuleComponent {

    /**
     * Executes this action in the given rule execution context.
     *
     * @param context the current rule execution context, providing access to the
     *                {@link net.minecraft.server.MinecraftServer}, variables, and the rule itself
     * @return {@code true} if subsequent actions in the same list should continue executing;
     *         {@code false} to abort the remaining actions in the current phase
     */
    public boolean execute(RuleContext context);

    /**
     * Returns the unique name of this action instance within its rule.
     *
     * <p>Action names are used to identify actions in command operations
     * (e.g. {@code /f rule edit <rule> action rename <oldName> <newName>}).
     *
     * @return the non-null name of this action
     */
    public String getName();

    /**
     * Returns a copy of this action with the given name.
     *
     * <p>Actions are immutable by convention; this method returns a new instance
     * rather than mutating the current one.
     *
     * @param name the new name to assign
     * @return a new {@code RuleAction} that is otherwise identical to this one
     */
    public RuleAction setName(String name);

    /**
     * Serializes action-specific parameters to a {@link JsonObject}.
     *
     * <p>The returned object is placed under the {@code "value"} key in the full
     * action JSON produced by {@link #toJson()}.
     *
     * @return a non-null {@link JsonObject} containing parameters for this action
     */
    public JsonObject getValueJson();

    /**
     * Returns the metadata describing the parameters required by this action.
     *
     * <p>The returned {@link RequiredParamMetadata} is used to validate rule JSON
     * and to generate command-line help for rule editing commands.
     *
     * @return a non-null {@link RequiredParamMetadata} describing this action's parameters
     */
    public RequiredParamMetadata getParameters();

    /**
     * Serializes this action to a {@link JsonObject} that can be stored in a rule JSON file.
     *
     * <p>The produced JSON contains at minimum:
     * <ul>
     *   <li>{@code "name"} – the action instance name</li>
     *   <li>{@code "type"} – the registered type string (see {@link #getType()})</li>
     *   <li>{@code "value"} – parameters returned by {@link #getValueJson()}</li>
     * </ul>
     *
     * @return a non-null {@link JsonObject} suitable for persisting this action
     */
    default JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("name", getName());
        result.addProperty("type", getType());
        result.add("value", getValueJson());
        return result;
    }
}
