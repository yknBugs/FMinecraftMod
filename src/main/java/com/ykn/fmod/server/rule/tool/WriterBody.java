/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import com.ykn.fmod.server.rule.core.ComponentCall;

/**
 * The body of a data-flow condition: it computes a value that the shared wrapper stores in the
 * variable named by the component's {@link com.ykn.fmod.server.rule.core.Params#output output} parameter.
 *
 * <p>Registered through {@link com.ykn.fmod.server.rule.core.Components#writer}. The wrapper
 * evaluates to {@code true} when the value was stored and to {@code false} when it was not
 * (a missing parameter, an invalid variable name, a {@code null} result), after recording a
 * warning. Exceptions are not caught: a body that can fail for an expected reason must return
 * {@code null} instead of throwing.
 *
 * <p>That makes {@code &&} an evaluation-order operator for writers:
 * {@code ComputeA && ComputeB && Check} stops at the first failed compute, while {@code |} runs
 * them all regardless. A writer therefore does not need to hide its failure to stay usable.
 *
 * <p>Returning {@code null} means "nothing to store" and fails the component. Report the concrete
 * reason with {@link ComponentCall#fail} before returning {@code null}; the wrapper then skips its
 * generic message instead of appending a second, less useful one.
 *
 * @see ComponentCall
 * @see com.ykn.fmod.server.rule.core.Params
 */
@FunctionalInterface
public interface WriterBody {

    /**
     * Computes the value to store.
     *
     * @param call the resolved parameter values for this evaluation
     * @return the value to assign, or {@code null} when the computation produced nothing
     */
    Object compute(ComponentCall call);
}
