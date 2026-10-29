/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ComponentCall;

/**
 * Bound resolution shared by range conditions.
 *
 * <p>A range condition tolerates a missing bound: an absent {@code min} means "no lower bound" and
 * an absent {@code max} means "no upper bound". The gap is never silent - {@link ComponentCall#warnNull}
 * reports it - so a typo in a parameter still shows up, while a one-sided range stays usable.
 *
 * <p>The single exception is {@link com.ykn.fmod.server.rule.cond.CheckConditions}'
 * {@code CheckPermission}: permission checks are security-relevant, so that type stays strict.
 *
 * @see ComponentCall
 */
public final class Ranges {

    private Ranges() {
    }

    /**
     * Resolves a lower {@code double} bound, treating a missing value as unbounded.
     *
     * @param call  the current evaluation
     * @param index the parameter index
     * @return the bound, or {@link Double#NEGATIVE_INFINITY} when it is missing
     */
    public static double lowerDouble(ComponentCall call, int index) {
        Double value = call.number(index);
        if (value == null) {
            call.warnNull(index);
            return Double.NEGATIVE_INFINITY;
        }
        return value;
    }

    /**
     * Resolves an upper {@code double} bound, treating a missing value as unbounded.
     *
     * @param call  the current evaluation
     * @param index the parameter index
     * @return the bound, or {@link Double#POSITIVE_INFINITY} when it is missing
     */
    public static double upperDouble(ComponentCall call, int index) {
        Double value = call.number(index);
        if (value == null) {
            call.warnNull(index);
            return Double.POSITIVE_INFINITY;
        }
        return value;
    }

    /**
     * Resolves a lower {@code int} bound, treating a missing value as unbounded.
     *
     * @param call  the current evaluation
     * @param index the parameter index
     * @return the bound, or {@link Integer#MIN_VALUE} when it is missing
     */
    public static int lowerInt(ComponentCall call, int index) {
        Integer value = call.integer(index);
        if (value == null) {
            call.warnNull(index);
            return Integer.MIN_VALUE;
        }
        return value;
    }

    /**
     * Resolves an upper {@code int} bound, treating a missing value as unbounded.
     *
     * @param call  the current evaluation
     * @param index the parameter index
     * @return the bound, or {@link Integer#MAX_VALUE} when it is missing
     */
    public static int upperInt(ComponentCall call, int index) {
        Integer value = call.integer(index);
        if (value == null) {
            call.warnNull(index);
            return Integer.MAX_VALUE;
        }
        return value;
    }

    /**
     * Rejects a {@code double} range whose lower bound is above its upper bound.
     *
     * <p>The check is exactly {@code lower > upper}. {@code NaN} never satisfies it, so a
     * {@code NaN} bound is reported by {@link ComponentCall}'s NaN warning only, not as an
     * inverted range.
     *
     * @param call  the current evaluation
     * @param lower the resolved lower bound
     * @param upper the resolved upper bound
     * @param key   the i18n key describing an inverted range for this component type
     * @return {@code true} when the range is usable
     */
    public static boolean requireOrder(ComponentCall call, double lower, double upper, String key) {
        if (lower > upper) {
            call.warn(Util.parseTranslatableText(key));
            return false;
        }
        return true;
    }

    /**
     * Rejects an {@code int} range whose lower bound is above its upper bound.
     *
     * @param call  the current evaluation
     * @param lower the resolved lower bound
     * @param upper the resolved upper bound
     * @param key   the i18n key describing an inverted range for this component type
     * @return {@code true} when the range is usable
     */
    public static boolean requireOrder(ComponentCall call, int lower, int upper, String key) {
        if (lower > upper) {
            call.warn(Util.parseTranslatableText(key));
            return false;
        }
        return true;
    }
}
