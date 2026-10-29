/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import com.ykn.fmod.server.rule.core.ComponentCall;

/**
 * The body of a rule action: it performs one side effect.
 *
 * <p>Registered through {@link com.ykn.fmod.server.rule.core.Components#action}. The shared
 * wrapper resolves and validates every declared parameter first; a parameter that resolves to
 * {@code null} is reported and the action is skipped, but execution of later actions continues.
 *
 * <p>Returning {@code false} means "this action could not do its job". The wrapper then reports
 * {@code fmod.rule.warning.operationfailed} - unless the body already called
 * {@link ComponentCall#fail} with a more specific reason - and still continues with the next
 * action, so a single failed effect never cancels the rest of the rule.
 *
 * @see ComponentCall
 */
@FunctionalInterface
public interface ActionBody {

    /**
     * Executes the action.
     *
     * @param call the resolved parameter values for this evaluation
     * @return {@code true} when the action did what it was asked to do
     */
    boolean run(ComponentCall call);
}
