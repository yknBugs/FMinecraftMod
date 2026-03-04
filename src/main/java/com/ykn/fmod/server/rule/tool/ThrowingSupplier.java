/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

/**
 * A functional interface like {@link java.util.function.Supplier} but whose
 * {@link #get()} method is allowed to throw a checked exception of type {@code E}.
 *
 * <p>Used in {@link com.ykn.fmod.server.rule.core.RuleParameter#fromCommandContext} so that
 * Brigadier {@code CommandSyntaxException}s can propagate out of parameter value suppliers
 * without being wrapped in a runtime exception.
 *
 * @param <T> the type of value supplied
 * @param <E> the checked exception type that may be thrown
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
    
    /**
     * Gets the value, potentially throwing a checked exception.
     *
     * @return the supplied value
     * @throws E if the value cannot be produced
     */
    T get() throws E;
    
}
