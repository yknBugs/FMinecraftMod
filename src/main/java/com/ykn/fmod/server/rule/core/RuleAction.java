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
     * <p>The default implementation serializes {@link #getParameterValues()} against
     * {@link #getParameters()} via {@link RuleComponentFactory#toValueJson}. Override only when
     * the action has state beyond its declared parameters (e.g. a privileged field never exposed
     * to command/GUI editing) - call {@code RuleAction.super.getValueJson()} first to get the
     * declared-parameter portion, then add the extra state.
     *
     * @return a non-null {@link JsonObject} containing parameters for this action
     */
    default public JsonObject getValueJson() {
        return RuleComponentFactory.toValueJson(getParameters(), getParameterValues());
    }

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
     * Returns this action's current parameter values, in the same order as
     * {@link #getParameters()}'s {@link RequiredParamMetadata#getArgumentList()}.
     *
     * @return a non-null list of this action's {@link RuleParameter} values
     */
    public List<RuleParameter<?>> getParameterValues();

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation renders {@code translate(renderKey, name, type,
     * param1.render(), param2.render(), ...)} via {@link RuleComponentFactory#render}, where
     * {@code renderKey} is {@link RequiredParamMetadata#getRenderI18nKey()}. Override only when
     * the action needs to show something not derivable from its declared parameters.
     */
    @Override
    default public Component render() {
        return RuleComponentFactory.render(getParameters(), getName(), getType(), getParameterValues());
    }

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

    /**
     * Logs an admin-debugging warning to {@code context}, prefixed with this action instance's
     * user-defined {@link #getName() name} so that a rule with several actions still reads
     * unambiguously (e.g. {@code [myAction] ...}) instead of leaving the admin to guess which
     * action a message came from.
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
