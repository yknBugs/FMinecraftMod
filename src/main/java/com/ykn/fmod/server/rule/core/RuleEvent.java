/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.Map;
import java.util.Set;

import com.ykn.fmod.server.base.util.Util;

/**
 * Represents a game event that can trigger a {@link CustomRule}.
 *
 * <p>An event declaration serves two purposes:
 * <ol>
 *   <li><b>Documentation contract</b> — it advertises which variables (and their types) the
 *       event dispatcher will place into the {@link RuleContext} variables map before invoking
 *       the rule.  Rule authors refer to these variable names in their condition parameter
 *       bindings (e.g. {@code {"variable": "playerId"}}).</li>
 *   <li><b>Validation</b> — {@link #validateVariables(RuleContext)} is called automatically
 *       when a rule fires, allowing early detection of mis-configured event dispatchers.</li>
 * </ol>
 *
 * <p>To register a new event type, implement this interface (usually as a singleton) and call
 * {@link com.ykn.fmod.server.rule.tool.RuleRegistry#register(String, com.ykn.fmod.server.rule.tool.RuleRegistry.RuleEventFactory)}
 * with a unique type string.  Then, in the relevant Fabric event callback, build a variable map
 * and call {@link com.ykn.fmod.server.rule.tool.RuleManager#trigger(com.ykn.fmod.server.base.data.ServerData, java.util.Map)}.
 *
 * @see CustomRule
 * @see RuleContext
 * @see com.ykn.fmod.server.rule.tool.RuleManager
 */
public interface RuleEvent extends RuleComponent {

    /**
     * Returns the set of variable names that the event dispatcher <em>must</em> supply as
     * non-null values before this rule is evaluated.
     *
     * <p>This is the <b>non-null subset</b> of the variables declared in {@link #variablesType()}.
     * During {@link #validateVariables(RuleContext)}, any variable in this set that resolves
     * to {@code null} in the context is treated as a validation failure and a warning is logged.
     *
     * <p>Variables that may legitimately be {@code null} at runtime should be declared only in
     * {@link #variablesType()} and omitted from this set.
     *
     * @return an unmodifiable set of variable names that must be non-null; never {@code null}
     */
    public Set<String> variablesList();

    /**
     * Returns a map from <em>every</em> declared variable name to its expected boxed {@link Class} type.
     *
     * <p>This map covers all variables the event dispatcher may place into the context —
     * both those required to be non-null (see {@link #variablesList()}) and those that are
     * optionally present.
     *
     * <p><b>Important:</b> always use boxed types (e.g. {@link Integer}, {@link Double}) rather
     * than primitives ({@code int}, {@code double}). Due to auto-boxing, an
     * {@code instanceof} check against a primitive type will always fail. If a primitive type
     * is declared, {@link #validateVariables(RuleContext)} will log a warning accordingly.
     *
     * <p>This map is iterated by {@link #validateVariables(RuleContext)} to verify that every
     * present variable has the correct runtime type.
     *
     * @return an unmodifiable map of variable name → expected boxed type; never {@code null}
     */
    public Map<String, Class<? extends Object>> variablesType();

    /**
     * Validates that the variables supplied in {@code context} satisfy this event's contract.
     *
     * <p>Iterates over every variable declared in {@link #variablesType()} and performs three checks:
     * <ol>
     *   <li><b>Null check</b> - if the variable is in {@link #variablesList()} (i.e. required
     *       non-null) but resolves to {@code null} in the context, validation fails and a warning
     *       is logged.  Variables absent from {@link #variablesList()} are allowed to be
     *       {@code null} and are skipped for the remaining checks.</li>
     *   <li><b>Type declaration check</b> - if no expected type is declared in
     *       {@link #variablesType()} for a given variable, validation fails and a warning is
     *       logged.</li>
     *   <li><b>Primitive type check</b> - if the declared type is a primitive, a warning is logged
     *       because auto-boxing makes {@code instanceof} checks against primitive types always
     *       fail at runtime. Use boxed types (e.g. {@link Integer}) instead.</li>
     *   <li><b>Type match check</b> - if the variable's runtime type is not assignable to the
     *       declared expected type, validation fails and a warning is logged.</li>
     * </ol>
     *
     * <p>A warning is produced by {@link CustomRule#test(RuleContext)} when this returns
     * {@code false}, but rule execution is not aborted — condition evaluation proceeds
     * and individual {@link RuleParameter}s fall back to their constant values as needed.
     *
     * @param context the execution context whose variable map is checked
     * @return {@code true} if all declared variables that are present have the correct types,
     *         and all required-non-null variables are indeed non-null
     */
    default public boolean validateVariables(RuleContext context) {
        Set<String> requiredNotNullVariables = variablesList();
        Map<String, Class<? extends Object>> expectedTypes = variablesType();
        Set<String> requiredNullableVariables = expectedTypes.keySet();
        boolean isPassed = true;
        for (String variable : requiredNullableVariables) {
            Object value = context.getVariable(variable);
            boolean shouldBeNotNull = requiredNotNullVariables.contains(variable);
            Class<? extends Object> expectedType = expectedTypes.get(variable);
            if (value == null) {
                if (shouldBeNotNull) {
                    isPassed = false;
                    Util.LOGGER.warn("FMinecraftMod: Variable " + variable + " is required to be non-null for event " + getType() + ", but was null. Check your event dispatcher implementation.");
                } 
                continue;
            }
            if (expectedType == null) {
                Util.LOGGER.warn("FMinecraftMod: No expected type declared for variable " + variable + " in event " + getType() + ".");
                isPassed = false;
                continue;
            }
            if (expectedType.isPrimitive()) {
                Util.LOGGER.warn("FMinecraftMod: Due to auto-boxing, type check will always fail for primitives. Please use their boxed counterparts (e.g. Integer instead of int) in variablesType() declarations for your variable " + variable + " in event " + getType() + ".");
            }  
            if (!expectedType.isInstance(value)) {
                Util.LOGGER.warn("FMinecraftMod: Variable " + variable + " is expected to be of type " + expectedType.getName() + " for event " + getType() + ", but was " + value.getClass().getName() + ". Check your event dispatcher implementation.");
                isPassed = false;
            }
        }
        for (String variable : requiredNotNullVariables) {
            if (!requiredNullableVariables.contains(variable)) {
                Util.LOGGER.warn("FMinecraftMod: Variable " + variable + " is required to be non-null for event " + getType() + ", but no expected type was declared in variablesType(). Check your event implementation.");
                isPassed = false;
            }
        }
        return isPassed;
    }
    
    // default Text renderVariablesInfo(String eventI18nKey) {
    //     if (variablesList().isEmpty()) {
    //         return Text.translatable("fmod.rule.event.novar");
    //     }
    //     MutableText result = Text.empty();
    //     int i = 0;
    //     for (String variable : variablesList()) {
    //         if (i > 0) {
    //             result = result.append("\n");
    //         }
    //         result = result.append(variable + ": ").append(Util.parseTranslatableText(eventI18nKey + ".var." + variable.toLowerCase()));
    //         i++;
    //     }
    //     return result;
    // }
}
