/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.Locale;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.Ranges;
import com.ykn.fmod.server.base.util.SplitMixRandom;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Numeric data-flow conditions: compute a value and store it in a variable.
 *
 * <p>Five shapes cover most types, so each is one table entry:
 * {@link #binary}, {@link #unary}, {@link #rounding}, {@link #conversion} and {@link #distance}.
 *
 * <p>Results are not range-checked: a division by zero stores {@code NaN} and adds the NaN warning,
 * because that is what the arithmetic produces. Truncating conversions return {@code null} instead,
 * so {@code ToInteger} and the rounding types fail rather than silently clamp.
 *
 * <p>Randomness comes from {@link SplitMixRandom}, an independent SplitMix64 stream. No Minecraft
 * random source is read or advanced, so a rule cannot change monster spawns, loot, or anything else
 * that depends on the world's random sequence.
 */
public final class NumericConditions {

    private static final SplitMixRandom RANDOM = new SplitMixRandom(System.nanoTime() ^ System.currentTimeMillis());

    private NumericConditions() {
    }

    /**
     * Registers every numeric condition type.
     */
    public static void register() {
        binary("ComputeAdd", (a, b) -> a + b);
        binary("ComputeSub", (a, b) -> a - b);
        binary("ComputeMul", (a, b) -> a * b);
        binary("ComputeDiv", (a, b) -> b == 0 ? Double.NaN : a / b);
        binary("ComputeMod", (a, b) -> b == 0 ? Double.NaN : a % b);
        binary("ComputePow", Math::pow);
        binary("ComputeMin", Math::min);
        binary("ComputeMax", Math::max);

        unary("ComputeAbs", Math::abs);
        unary("ComputeSqrt", Math::sqrt);
        unary("ComputeSin", Math::sin);
        unary("ComputeCos", Math::cos);
        unary("ComputeTan", Math::tan);
        unary("ComputeLog", Math::log);
        unary("ComputeExp", Math::exp);
        unary("DegToRad", Math::toRadians);
        unary("RadToDeg", Math::toDegrees);

        rounding("ComputeRound", (x) -> Math.round(x));
        rounding("ComputeFloor", Math::floor);
        rounding("ComputeCeil", Math::ceil);

        conversion("ToString", (value) -> TypeAdaptor.parse(value).asString());
        conversion("ToDouble", NumericConditions::toDouble);
        conversion("ToInteger", NumericConditions::toInteger);

        distance("EuclideanDistance", GameMath::getEuclideanDistance);
        distance("ManhattanDistance", GameMath::getManhattanDistance);
        distance("ChebyshevDistance", GameMath::getChebyshevDistance);
        distance("HorizontalEuclideanDistance", GameMath::getHorizontalEuclideanDistance);
        distance("HorizontalManhattanDistance", GameMath::getHorizontalManhattanDistance);
        distance("HorizontalChebyshevDistance", GameMath::getHorizontalChebyshevDistance);
        distance("VerticalDistance", GameMath::getVerticalDistance);

        // Vector components. Coord* take a Vec3 (ParamKind.POSITION), Rotation* take a Vec2 whose
        // components are pitch then yaw (ParamKind.ROTATION), matching how the event snapshots
        // publish positions and rotations. Both operands of a shift are required, like every other
        // arithmetic writer here.
        Components.writer("CoordShift", Params.of("fmod.rule.condition.coordshift")
                .param("base", ParamKind.POSITION)
                .param("offset", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> call.vector(0).add(call.vector(1)));

        Components.writer("RotationShift", Params.of("fmod.rule.condition.rotationshift")
                .param("base", ParamKind.ROTATION)
                .param("offset", ParamKind.ROTATION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> call.get(0, Vec2.class).add(call.get(1, Vec2.class)));

        Components.writer("GetCoordX", Params.of("fmod.rule.condition.getcoordx")
                .param("coordinate", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> call.vector(0).x);

        Components.writer("GetCoordY", Params.of("fmod.rule.condition.getcoordy")
                .param("coordinate", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> call.vector(0).y);

        Components.writer("GetCoordZ", Params.of("fmod.rule.condition.getcoordz")
                .param("coordinate", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> call.vector(0).z);

        Components.writer("GetRotationPitch", Params.of("fmod.rule.condition.getrotationpitch")
                .param("rotation", ParamKind.ROTATION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> (double) call.get(0, Vec2.class).x);

        Components.writer("GetRotationYaw", Params.of("fmod.rule.condition.getrotationyaw")
                .param("rotation", ParamKind.ROTATION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> (double) call.get(0, Vec2.class).y);

        // allowNulls: a rule may deliberately publish a null value, and AUTO resolves an absent
        // variable to null. The body only passes the value through, so no numeric accessor is
        // unboxed here.
        Components.writer("MakeCoord", Params.of("fmod.rule.condition.makecoord")
                .param("x", ParamKind.DOUBLE)
                .param("y", ParamKind.DOUBLE)
                .param("z", ParamKind.DOUBLE)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> new Vec3(call.number(0), call.number(1), call.number(2)));

        Components.writer("MakeRotation", Params.of("fmod.rule.condition.makerotation")
                .param("pitch", ParamKind.DOUBLE)
                .param("yaw", ParamKind.DOUBLE)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> new Vec2(call.number(0).floatValue(), call.number(1).floatValue()));
        Components.writer("SetVariable", Params.of("fmod.rule.condition.setvariable")
                .output("variable", ParamKind.VARIABLE_NAME)
                .param("value", ParamKind.AUTO)
                .allowNulls(),
                (call) -> call.raw(1));

        Components.writer("RandomDouble", Params.of("fmod.rule.condition.randomdouble")
                .param("min", ParamKind.DOUBLE)
                .param("max", ParamKind.DOUBLE)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    double lower = call.number(0);
                    double upper = call.number(1);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.randomdouble.invalidrange")) {
                        return null;
                    }
                    if (!Double.isFinite(lower) || !Double.isFinite(upper)) {
                        call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
                        return null;
                    }
                    return RANDOM.between(lower, upper);
                });

        Components.writer("RegexCaptureGroup",
                Params.of("fmod.rule.condition.regexcapturegroup")
                        .param("text", ParamKind.STRING)
                        .output("variable", ParamKind.VARIABLE_NAME)
                        .param("index", ParamKind.INT)
                        .param("regex", ParamKind.GREEDY_STRING),
                (call) -> {
                    String regex = call.text(3);
                    try {
                        Matcher matcher = Pattern.compile(regex).matcher(call.text(0));
                        int group = call.integer(2);
                        return group >= 0 && group <= matcher.groupCount() && matcher.find()
                                ? matcher.group(group)
                                : null;
                    } catch (PatternSyntaxException exception) {
                        call.fail(Util.parseTranslatableText(
                                "fmod.rule.condition.regexcapturegroup.invalid",
                                exception.getDescription()));
                        return null;
                    }
                });
    }

    /**
     * Registers a binary numeric operation that stores its result.
     *
     * @param type      the type id; the i18n prefix is derived from it
     * @param operation the operation
     */
    private static void binary(String type, DoubleBinaryOperator operation) {
        Components.writer(type, Params.of(prefix(type))
                .param("a", ParamKind.DOUBLE)
                .param("b", ParamKind.DOUBLE)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> operation.applyAsDouble(call.number(0), call.number(1)));
    }

    /**
     * Registers a unary numeric operation that stores its result.
     *
     * @param type      the type id; the i18n prefix is derived from it
     * @param operation the operation
     */
    private static void unary(String type, DoubleUnaryOperator operation) {
        Components.writer(type, Params.of(prefix(type))
                .param("x", ParamKind.DOUBLE)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> operation.applyAsDouble(call.number(0)));
    }

    /**
     * Registers a rounding operation that stores a truncated integer.
     *
     * @param type      the type id; the i18n prefix is derived from it
     * @param operation the rounding operation
     */
    private static void rounding(String type, DoubleUnaryOperator operation) {
        Components.writer(type, Params.of(prefix(type))
                .param("x", ParamKind.DOUBLE)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> truncate(operation.applyAsDouble(call.number(0))));
    }

    /**
     * Registers a conversion that stores its result.
     *
     * @param type       the type id; the i18n prefix is derived from it
     * @param conversion the conversion, applied to the raw parameter value
     */
    private static void conversion(String type, ObjectConversion conversion) {
        Components.writer(type, Params.of(prefix(type))
                .param("value", ParamKind.AUTO)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    try {
                        return conversion.convert(call.raw(0));
                    } catch (NumberFormatException exception) {
                        call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
                        return null;
                    }
                });
    }

    /**
     * Registers a distance computation that stores its result.
     *
     * @param type      the distance name; the type id is {@code Compute<type>} and the i18n prefix
     *                  is derived from it
     * @param operation the distance function
     */
    private static void distance(String type, ToDoubleBiFunction<Vec3, Vec3> operation) {
        String id = "Compute" + type;
        Components.writer(id, Params.of(prefix(id))
                .param("a", ParamKind.POSITION)
                .param("b", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> operation.applyAsDouble(call.vector(0), call.vector(1)));
    }

    /**
     * Converts text to a double, leaving a non-numeric string to fail as a number-format error.
     *
     * @param value the raw value
     * @return the double
     */
    private static Object toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.valueOf(value.toString());
    }

    /**
     * Converts a value to an integer, truncating a numeric value.
     *
     * @param value the raw value
     * @return the integer, or {@code null} when a number is outside the int range
     */
    private static Object toInteger(Object value) {
        if (value instanceof Number number) {
            return truncate(number.doubleValue());
        }
        return Integer.valueOf(value.toString());
    }

    /**
     * Truncates a finite number to {@code int}.
     *
     * @param value the value to truncate
     * @return the truncated value, or {@code null} when it is not finite or out of range
     */
    private static Integer truncate(double value) {
        if (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return (int) value;
    }

    /**
     * Builds the i18n prefix of a condition type from its id.
     *
     * @param type the type id, e.g. {@code ComputeAdd}
     * @return the prefix, e.g. {@code fmod.rule.condition.computeadd}
     */
    private static String prefix(String type) {
        return "fmod.rule.condition." + type.toLowerCase(Locale.ROOT);
    }

    /**
     * Converts a raw parameter value to the value to store.
     */
    @FunctionalInterface
    private interface ObjectConversion {

        /**
         * Converts the value.
         *
         * @param value the raw parameter value
         * @return the value to store
         */
        Object convert(Object value);
    }
}
