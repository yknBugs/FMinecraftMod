/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.List;

import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.RuleComponentFactory;

import net.minecraft.network.chat.Component;

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
     * Returns the metadata describing the parameters required by this condition.
     *
     * <p>The returned {@link RequiredParamMetadata} is used to validate rule JSON
     * and to generate command-line help for rule editing commands.
     *
     * @return a non-null {@link RequiredParamMetadata} describing this condition's parameters
     */
    public RequiredParamMetadata getParameters();

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
     * <p>The default implementation serializes {@link #getParameterValues()} against
     * {@link #getParameters()} via {@link RuleComponentFactory#toValueJson}. Override only when
     * the condition has state beyond its declared parameters.
     *
     * @return a non-null {@link JsonObject} containing the serialised parameters
     */
    default JsonObject getValueJson() {
        return RuleComponentFactory.toValueJson(getParameters(), getParameterValues());
    }

    /**
     * Returns this condition's current parameter values, in the same order as
     * {@link #getParameters()}'s {@link RequiredParamMetadata#getArgumentList()}.
     *
     * @return a non-null list of this condition's {@link RuleParameter} values
     */
    public List<RuleParameter<?>> getParameterValues();

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation renders {@code translate(renderKey, name, type,
     * param1.render(), param2.render(), ...)} via {@link RuleComponentFactory#render}, where
     * {@code renderKey} is {@link RequiredParamMetadata#getRenderI18nKey()}. Override only when
     * the condition needs to show something not derivable from its declared parameters.
     */
    @Override
    default Component render() {
        return RuleComponentFactory.render(getParameters(), getName(), getType(), getParameterValues());
    }

    @Override
    default JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("name", getName());
        result.addProperty("type", getType());
        result.add("value", getValueJson());
        return result;
    }

    /**
     * Logs an admin-debugging warning to {@code context}, prefixed with this condition
     * instance's user-defined {@link #getName() name} so that a rule with several conditions
     * still reads unambiguously (e.g. {@code [myCondition] ...}) instead of leaving the admin to
     * guess which condition a message came from.
     *
     * @param context the current rule execution context
     * @param message the warning body, appended after the identifying prefix
     */
    default public void addWarning(RuleContext context, Component message) {
        context.addWarning(Util.parseTranslatableText("fmod.rule.warning.prefix", getName(), message));
    }

    /**
     * Logs the standard "required parameter resolved to null" warning for {@code param}, using
     * its i18n display name rather than the raw {@link RequiredParamMetadata.Entry#constArgName}.
     *
     * <p>Meant to be called at the point where a required {@link RuleParameter} resolves to
     * {@code null} (neither its variable nor its constant produced a usable value), right before
     * falling back to a default result.
     *
     * @param context the current rule execution context
     * @param param   the metadata entry (from {@link #getParameters()}) for the null parameter
     */
    default public void warnNullInput(RuleContext context, RequiredParamMetadata.Entry param) {
        addWarning(context, Util.parseTranslatableText("fmod.rule.warning.nullinput", Util.parseTranslatableText(param.nameI18nKey)));
    }

}
