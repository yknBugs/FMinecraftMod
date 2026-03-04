/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.Map;
import java.util.Set;

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
     * Returns the set of variable names that the event dispatcher must populate before
     * this rule is evaluated.
     *
     * <p>For example, {@code TickEvent} declares {@code {"tick"}}; the dispatcher is
     * expected to put the current tick counter into the context's variable map under
     * that key.
     *
     * @return an unmodifiable set of required variable names; never {@code null}
     */
    public Set<String> variablesList();

    /**
     * Returns a map from each variable name to its expected {@link Class} type.
     *
     * <p>This map is used by {@link #validateVariables(RuleContext)} to check that every
     * required variable is present and has the correct runtime type.
     *
     * @return an unmodifiable map of variable name -> expected type; never {@code null}
     */
    public Map<String, Class<? extends Object>> variablesType();

    /**
     * Validates that the variables supplied in {@code context} satisfy this event's contract.
     *
     * <p>Checks that every variable listed in {@link #variablesList()} is present in
     * {@link RuleContext#getVariables()} and assignable to its declared type from
     * {@link #variablesType()}.
     *
     * <p>A warning is logged by {@link CustomRule#test(RuleContext)} when this returns
     * {@code false}, but rule execution is not aborted — the condition evaluation proceeds
     * and individual {@link RuleParameter}s fall back to their constant values as needed.
     *
     * @param context the execution context to validate against
     * @return {@code true} if all required variables are present with correct types
     */
    default public boolean validateVariables(RuleContext context) {
        Set<String> requiredVariables = variablesList();
        for (String variable : requiredVariables) {
            Object value = context.getVariable(variable);
            if (value == null) {
                return false;
            }
            Class<? extends Object> expectedType = variablesType().get(variable);
            if (!expectedType.isInstance(value)) {
                return false;
            }
        }
        return true;
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
