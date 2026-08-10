/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

/**
 * A functional interface like {@link java.util.function.BiFunction} but whose
 * {@link #apply(Object, Object)} method is allowed to throw a checked exception of type {@code E}.
 *
 * <p>Used by {@link ParamKind} so that Brigadier {@code CommandSyntaxException}s can propagate
 * out of a parameter's value extractor without being wrapped in a runtime exception.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <R> the type of the result
 * @param <E> the checked exception type that may be thrown
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R, E extends Throwable> {

    /**
     * Applies this function to the given arguments, potentially throwing a checked exception.
     *
     * @param t the first argument
     * @param u the second argument
     * @return the function result
     * @throws E if the result cannot be produced
     */
    R apply(T t, U u) throws E;

}
