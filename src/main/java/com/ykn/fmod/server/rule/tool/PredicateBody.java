/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import com.ykn.fmod.server.rule.core.ComponentCall;

/**
 * The body of a rule condition that answers a yes/no question.
 *
 * <p>Registered through {@link com.ykn.fmod.server.rule.core.Components#predicate}. The shared
 * wrapper resolves and validates every declared parameter first, so a strict body can use its
 * accessors without null checks. A body registered with {@link com.ykn.fmod.server.rule.core.Params#allowNulls()}
 * receives possibly-null values and decides for itself.
 *
 * <p>Exceptions are not caught by the wrapper. A body that can fail for an expected reason must
 * check that reason and return {@code false} itself; anything else reaches
 * {@link com.ykn.fmod.server.rule.core.RuleContext#test()}, which logs a stack trace and aborts the
 * current evaluation instead of hiding a bug behind a {@code false}.
 *
 * @see com.ykn.fmod.server.rule.core.ComponentCall
 * @see com.ykn.fmod.server.rule.core.Components
 */
@FunctionalInterface
public interface PredicateBody {

    /**
     * Evaluates the condition.
     *
     * @param call the resolved parameter values for this evaluation
     * @return {@code true} when the condition holds
     */
    boolean test(ComponentCall call);
}
