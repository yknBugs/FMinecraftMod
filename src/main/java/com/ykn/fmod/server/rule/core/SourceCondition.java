/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import com.google.gson.JsonObject;

/**
 * Marker interface for <em>leaf</em> (source) conditions that directly inspect game state.
 *
 * <p>Unlike composite conditions ({@link IterableCondition}), a {@code SourceCondition}
 * has no child conditions. Its {@link #evaluate(RuleContext)} method reads the current
 * server state (entity positions, NBT, permissions, etc.) and returns a boolean result.
 *
 * <p>To implement a new source condition:
 * <ol>
 *   <li>Create a class implementing {@code SourceCondition}.</li>
 *   <li>Declare the required parameters as {@link RuleParameter} fields.</li>
 *   <li>Implement {@link #evaluate(RuleContext)} to resolve parameters and test the predicate.</li>
 *   <li>Implement {@link #getValueJson()} to serialise parameters.</li>
 *   <li>Provide a static {@code fromJson(JsonObject)} and optionally a {@code buildCommand(...)} for command support.</li>
 *   <li>Register with {@link com.ykn.fmod.server.rule.tool.RuleRegistry}.</li>
 * </ol>
 *
 * @see RuleParameter
 * @see RuleCondition
 */
public interface SourceCondition extends RuleCondition {

    /**
     * Serializes condition-specific parameters to a {@link JsonObject}.
     *
     * <p>The returned object is placed under the {@code "value"} key in the full condition
     * JSON produced by the default {@link #toJson()} implementation.
     *
     * <p>Each parameter should be serialised using
     * {@link RuleParameter#toJson(RuleParameter, java.util.function.Function)} so that
     * both the variable name and the constant fallback are preserved.
     *
     * @return a non-null {@link JsonObject} containing the serialised parameters
     */
    public JsonObject getValueJson();

    @Override
    default JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("name", getName());
        result.addProperty("type", getType());
        result.add("value", getValueJson());
        return result;
    }
    
}
