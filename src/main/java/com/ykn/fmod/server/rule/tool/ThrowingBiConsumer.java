/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

/**
 * A functional interface like {@link java.util.function.BiConsumer} but whose
 * {@link #accept(Object, Object)} method is allowed to throw a checked exception of type {@code E}.
 *
 * <p>Used by {@link RecursiveCommandBuilder#executes(ThrowingBiConsumer)} so that
 * {@code CommandSyntaxException}s thrown by {@link RecursiveCommandBuilder#resolveParameter}
 * can propagate out of the command action without a manual try/catch at every call site.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <E> the checked exception type that may be thrown
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Throwable> {

    /**
     * Performs this operation on the given arguments, potentially throwing a checked exception.
     *
     * @param t the first argument
     * @param u the second argument
     * @throws E if the operation cannot be completed
     */
    void accept(T t, U u) throws E;

}
