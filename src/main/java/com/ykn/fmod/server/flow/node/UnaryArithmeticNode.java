/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.flow.tool.FlowConvert;

import com.ykn.fmod.server.base.util.GameMath;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Set;
import java.util.Arrays;

/**
 * A flow node that performs a unary operation on one value.
 * Inputs:
 * 1. Number - The operand for the unary operation.
 * 2. String - The unary operation to perform (e.g., -, abs, sqrt).
 * Outputs:
 * 1. Number - The result of the unary operation.
 * Branches: 1 (Next node)
 */
public class UnaryArithmeticNode extends FlowNode {
	public UnaryArithmeticNode(long id, String name) {
		super(id, name, 2, 1, 1, "UnaryArithmeticNode");
	}

	@Override
	protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
		return NodeMetadata.builder("fmod.node.unalu.title.name", "fmod.node.unalu.title.feat")
            .input("fmod.node.unalu.input.num.name", "fmod.node.unalu.input.num.feat", "fmod.node.unalu.input.num.type")
            .input("fmod.node.unalu.input.op.name", "fmod.node.unalu.input.op.feat", "fmod.node.unalu.input.op.type")
            .output("fmod.node.unalu.output.name", "fmod.node.unalu.output.feat", "fmod.node.unalu.output.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
	}

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        String op = TypeAdaptor.parse(inputs.get(1)).asString().strip().toLowerCase(Locale.ROOT);
        status.setOutput(0, FlowConvert.unwrapOutput(apply(inputs.get(0), op)));
    }

    /**
     * Infers null, Vec3, Vec2, List, Double, Boolean, then text/string. NBT is string data.
     */
    private static Object apply(Object value, String operation) throws LogicException {
        if (value == null) {
            return null;
        }

        String op = operation.strip().toLowerCase(Locale.ROOT);
        if (!(value instanceof Tag)) {
            Vec3 v = TypeAdaptor.parse(value).asVec3d();
            if (v != null) {
                return vector(v, op, false);
            }
            Vec2 v2 = TypeAdaptor.parse(value).asVec2f();
            if (v2 != null) {
                if (Set.of("direction", "todirection", "tovec3").contains(op)) {
                    // Inverse of GameMath.getPitch/getYaw (the older direction helper reverses Z).
                    double pitch = Math.toRadians(v2.x);
                    double yaw = Math.toRadians(v2.y);
                    return new Vec3(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
                }
                return vector(new Vec3(v2.x, v2.y, 0), op, true);
            }
            List<Object> elements = FlowConvert.listCopy(value);
            if (elements != null) {
                return list(elements, op);
            }
            Double numeric = TypeAdaptor.parse(value).asDouble();
            if (numeric != null) {
                return number(numeric, op);
            }
            Boolean bool = TypeAdaptor.parse(value).asBoolean();
            if (bool != null) {
                if (op.equals("!") || op.equals("not")) {
                    return !bool;
                }
                throw unsupported(op);
            }
        }
        
        // As in the original unary node, text transformations return plain strings.
        String text = value instanceof Tag tag ? tag.toString() : TypeAdaptor.parse(value).asString();
        return switch (op) {
            case "length", "len", "size" -> (double) text.length();
            case "upper", "uppercase" -> text.toUpperCase(Locale.ROOT);
            case "lower", "lowercase" -> text.toLowerCase(Locale.ROOT);
            case "trim", "strip" -> text.strip();
            case "stripstart" -> text.stripLeading();
            case "stripend" -> text.stripTrailing();
            case "isempty" -> text.isEmpty();
            case "isblank" -> text.isBlank();
            case "reverse" -> new StringBuilder(text).reverse().toString();
            case "split", "words" -> words(text);
            case "lines" -> collect(text.lines().iterator());
            default -> throw unsupported(op);
        };
    }

    private static LogicException unsupported(String op) {
        return new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", op), null);
    }

    private static Object vector(Vec3 v, String op, boolean two) throws LogicException {
        double length = Math.hypot(Math.hypot(v.x, v.y), v.z);
        return switch (op) {
            case "length", "len" -> length;
            case "lengthsquared", "lensquared" -> v.lengthSqr();
            case "x" -> v.x;
            case "y" -> v.y;
            case "z" -> {
                if (two) {
                    throw unsupported(op);
                }
                yield v.z;
            }
            case "pitch" -> {
                if (two) {
                    throw unsupported(op);
                }
                yield GameMath.getPitch(v);
            }
            case "yaw" -> {
                if (two) {
                    throw unsupported(op);
                }
                yield GameMath.getYaw(v);
            }
            case "rotation", "torotation", "tovec2" -> {
                if (two) {
                    throw unsupported(op);
                }
                yield new Vec2((float) GameMath.getPitch(v), (float) GameMath.getYaw(v));
            }
            case "normalize", "norm" -> {
                double scale = Math.max(Math.abs(v.x), Math.max(Math.abs(v.y), Math.abs(v.z)));
                if (scale == 0) {
                    yield output(Vec3.ZERO, two);
                }
                Vec3 scaled = new Vec3(v.x / scale, v.y / scale, v.z / scale);
                double magnitude = Math.hypot(Math.hypot(scaled.x, scaled.y), scaled.z);
                yield output(new Vec3(scaled.x / magnitude, scaled.y / magnitude, scaled.z / magnitude), two);
            }
            case "-", "negate" -> output(v.scale(-1), two);
            case "abs" -> output(new Vec3(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z)), two);
            case "floor", "ceil", "round" ->
                output(new Vec3((Double) number(v.x, op), (Double) number(v.y, op), (Double) number(v.z, op)), two);
            default -> throw unsupported(op);
        };
    }

    private static Object output(Vec3 v, boolean two) {
        return two ? new Vec2((float) v.x, (float) v.y) : v;
    }

    private static Object number(double n, String op) throws LogicException {
        return switch (op) {
            case "-", "negate" -> -n;
            case "~" -> (double) ~(long) n;
            case "abs" -> Math.abs(n);
            case "sqrt" -> Math.sqrt(n);
            case "cbrt" -> Math.cbrt(n);
            case "square" -> n * n;
            case "reciprocal" -> 1 / n;
            case "exp" -> Math.exp(n);
            case "expm1" -> Math.expm1(n);
            case "ln", "log" -> Math.log(n);
            case "lg", "log10" -> Math.log10(n);
            case "log1p" -> Math.log1p(n);
            case "sin" -> Math.sin(n);
            case "cos" -> Math.cos(n);
            case "tan" -> Math.tan(n);
            case "asin" -> Math.asin(n);
            case "acos" -> Math.acos(n);
            case "atan" -> Math.atan(n);
            case "sinh" -> Math.sinh(n);
            case "cosh" -> Math.cosh(n);
            case "tanh" -> Math.tanh(n);
            case "ceil" -> Math.ceil(n);
            case "floor" -> Math.floor(n);
            case "round" -> (double) Math.round(n);
            case "rint" -> Math.rint(n);
            case "trunc" -> n < 0 ? Math.ceil(n) : Math.floor(n);
            case "rad", "degtorad" -> Math.toRadians(n);
            case "deg", "radtodeg" -> Math.toDegrees(n);
            case "sgn", "sign", "signum" -> Math.signum(n);
            case "isfinite" -> Double.isFinite(n);
            case "isnan" -> Double.isNaN(n);
            case "isinfinite" -> Double.isInfinite(n);
            default -> throw unsupported(op);
        };
    }

    private static Object list(List<Object> values, String op) throws LogicException {
        switch (op) {
            case "size", "length", "len":
                return (double) values.size();
            case "isempty":
                return values.isEmpty();
            case "reverse":
                Collections.reverse(values);
                return values;
            case "distinct", "unique":
                return new ArrayList<>(new LinkedHashSet<>(values));
            case "first":
                return values.isEmpty() ? null : values.get(0);
            case "last":
                return values.isEmpty() ? null : values.get(values.size() - 1);
        }
        if (!Set.of("sum", "min", "max", "mean", "avg", "median", "std", "variance", "samplestd", "samplevariance",
                "sort").contains(op)) {
            throw unsupported(op);
        }
        double[] numbers = new double[values.size()];
        double scale = 0;
        for (int i = 0; i < numbers.length; i++) {
            if (!(values.get(i) instanceof Number n) || !Double.isFinite(n.doubleValue())) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.elements"), null);
            }
            numbers[i] = n.doubleValue();
            scale = Math.max(scale, Math.abs(numbers[i]));
        }
        if (op.equals("sort")) {
            Arrays.sort(numbers);
            return Arrays.stream(numbers).boxed().toList();
        }
        if (numbers.length == 0) {
            if (op.equals("sum")) {
                return 0.0;
            }
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.empty"), null);
        }
        if (op.equals("min")) {
            return Arrays.stream(numbers).min().orElseThrow();
        }
        if (op.equals("max")) {
            return Arrays.stream(numbers).max().orElseThrow();
        }
        if (op.equals("median")) {
            Arrays.sort(numbers);
            int middle = numbers.length / 2;
            return numbers.length % 2 == 1 ? numbers[middle] : numbers[middle - 1] / 2 + numbers[middle] / 2;
        }
        if (op.equals("sum")) {
            return Arrays.stream(numbers).sum();
        }
        // Scale before online variance to avoid overflow in deltas for large finite inputs.
        double mean = 0, m2 = 0;
        int count = 0;
        for (double n : numbers) {
            double scaled = scale == 0 ? 0 : n / scale;
            double delta = scaled - mean;
            mean += delta / ++count;
            m2 += delta * (scaled - mean);
        }
        if (op.equals("mean") || op.equals("avg")) {
            return mean * scale;
        }
        boolean sample = op.startsWith("sample");
        if (sample && count < 2) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.sample"), null);
        }
        double std = Math.sqrt(Math.max(0, m2 / (sample ? count - 1 : count))) * scale;
        return op.equals("std") || op.equals("samplestd") ? std : std * std;
    }

    private static List<Object> words(String text) throws LogicException {
        Matcher matcher = Pattern.compile("\\S+", Pattern.UNICODE_CHARACTER_CLASS).matcher(text);
        List<Object> result = new ArrayList<>();
        while (matcher.find()) {
            FlowConvert.checkListSize((long) result.size() + 1);
            result.add(matcher.group());
        }
        return result;
    }

    private static List<Object> collect(Iterator<String> iterator) throws LogicException {
        List<Object> result = new ArrayList<>();
        while (iterator.hasNext()) {
            FlowConvert.checkListSize((long) result.size() + 1);
            result.add(iterator.next());
        }
        return result;
    }
}
