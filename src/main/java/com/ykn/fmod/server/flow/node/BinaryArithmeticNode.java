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

import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * A flow node that performs a binary operation on two values.
 * Numbers, vectors, texts, booleans and lists are supported; unsupported combinations fail.
 * Inputs:
 * 1. Number - The first operand for the arithmetic operation.
 * 2. Number - The second operand for the arithmetic operation.
 * 3. String - The arithmetic operation to perform (e.g., +, -, *, /).
 * Outputs:
 * 1. Number - The result of the arithmetic operation.
 * Branches: 1 (Next node)
 */
public class BinaryArithmeticNode extends FlowNode {

    public BinaryArithmeticNode(long id, String name) {
        super(id, name, 3, 1, 1, "BinaryArithmeticNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.bialu.title.name", "fmod.node.bialu.title.feat")
            .input("fmod.node.bialu.input.num1.name", "fmod.node.bialu.input.num1.feat", "fmod.node.bialu.input.num1.type")
            .input("fmod.node.bialu.input.num2.name", "fmod.node.bialu.input.num2.feat", "fmod.node.bialu.input.num2.type")
            .input("fmod.node.bialu.input.op.name", "fmod.node.bialu.input.op.feat", "fmod.node.bialu.input.op.type")
            .output("fmod.node.bialu.output.name", "fmod.node.bialu.output.feat", "fmod.node.bialu.output.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        String operation = TypeAdaptor.parse(inputs.get(2)).asString().strip().toLowerCase(Locale.ROOT);
        status.setOutput(0, FlowConvert.unwrapOutput(apply(inputs.get(0), inputs.get(1), operation)));
    }

    /**
     * Applies an operation after type inference in the documented priority order.
     */
    private static Object apply(Object a, Object b, String operation) throws LogicException {
        String op = operation.strip().toLowerCase(Locale.ROOT);
        // Collection operations retain their port roles: b is a delimiter or a single sought value.
        if (op.equals("join")) {
            List<Object> values = FlowConvert.listInput(a);
            FlowConvert.checkListSize(values.size());
            String separator = TypeAdaptor.parse(b).asString();
            return values.stream().map(value -> TypeAdaptor.parse(value).asString()).collect(Collectors.joining(separator));
        }
        if (op.equals("contains") && !(a instanceof Tag)) {
            List<Object> values = a == null ? List.of() : FlowConvert.listCopy(a);
            if (values != null) {
                return values.contains(b);
            }
        }
        if (a == null || b == null) {
            if (eq(op)) {
                return a == b;
            }
            if (op.equals("!=")) {
                return a != b;
            }
            return a == null ? b : a;
        }
        // Tags must bypass iterable/numeric inference, even ListTag and NumericTag.
        boolean nbt = a instanceof Tag || b instanceof Tag;
        if (!nbt) {
            Vec3 av = TypeAdaptor.parse(a).asVec3d();
            Vec3 bv = TypeAdaptor.parse(b).asVec3d();
            if (av != null || bv != null) {
                return vector(av, bv, a, b, op, false);
            }
            Vec2 a2 = TypeAdaptor.parse(a).asVec2f();
            Vec2 b2 = TypeAdaptor.parse(b).asVec2f();
            if (a2 != null || b2 != null) {
                return vector(a2 == null ? null : new Vec3(a2.x, a2.y, 0),
                        b2 == null ? null : new Vec3(b2.x, b2.y, 0), a, b, op, true);
            }
            List<Object> al = FlowConvert.listCopy(a);
            List<Object> bl = FlowConvert.listCopy(b);
            if (al != null || bl != null) {
                return lists(al == null ? List.of(a) : al, bl == null ? List.of(b) : bl, op);
            }
            Double ad = TypeAdaptor.parse(a).asDouble();
            Double bd = TypeAdaptor.parse(b).asDouble();
            if (ad != null && bd != null) {
                return numbers(ad, bd, op);
            }
            Boolean ab = TypeAdaptor.parse(a).asBoolean();
            Boolean bb = TypeAdaptor.parse(b).asBoolean();
            if (ab != null && bb != null) {
                return switch (op) {
                    case "=", "==", "equals" -> ab.equals(bb);
                    case "!=" -> !ab.equals(bb);
                    case "and", "&&" -> ab && bb;
                    case "or", "||" -> ab || bb;
                    case "xor", "^" -> ab ^ bb;
                    case "nand" -> !(ab && bb);
                    case "nor" -> !(ab || bb);
                    case "implies" -> !ab || bb;
                    default -> throw unsupported(op);
                };
            }
        }
        boolean text = a instanceof Component || b instanceof Component;
        if (text && (op.equals("+") || op.equals("append") || op.equals("concat"))) {
            return Component.empty().append(a instanceof Component c ? c.copy() : Component.literal(string(a)))
                    .append(b instanceof Component c ? c.copy() : Component.literal(string(b)));
        }
        Object result = strings(string(a), b, op);
        return text && result instanceof String s ? Component.literal(s) : result;
    }

    private static boolean eq(String op) {
        return op.equals("=") || op.equals("==") || op.equals("equals");
    }

    private static LogicException unsupported(String op) {
        return new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", op), null);
    }

    private static String string(Object value) {
        return value instanceof Tag tag ? tag.toString() : TypeAdaptor.parse(value).asString();
    }

    private static Object numbers(double a, double b, String op) throws LogicException {
        return switch (op) {
            case "=", "==", "equals" -> a == b;
            case "!=" -> a != b;
            case "+", "add" -> a + b;
            case "-", "subtract" -> a - b;
            case "*", "x", "multiply", "scale", "scale by", "scaleby" -> a * b;
            case "/", "divide" -> a / b;
            case "%", "mod" -> {
                if (b == 0) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.modzero"), null);
                }
                yield a % b;
            }
            case ">" -> a > b;
            case "<" -> a < b;
            case ">=" -> a >= b;
            case "<=" -> a <= b;
            case "^", "pow" -> Math.pow(a, b);
            case "min" -> Math.min(a, b);
            case "max" -> Math.max(a, b);
            case "log" -> Math.log(b) / Math.log(a);
            case "atan2" -> Math.atan2(a, b);
            case "hypot" -> Math.hypot(a, b);
            case "copysign" -> Math.copySign(a, b);
            case "ieeeremainder" -> Math.IEEEremainder(a, b);
            case "distance", "absdiff" -> Math.abs(a - b);
            default -> throw unsupported(op);
        };
    }

    private static Object vector(Vec3 av, Vec3 bv, Object a, Object b, String op, boolean two) throws LogicException {
        Double ad = av == null ? TypeAdaptor.parse(a).asDouble() : null;
        Double bd = bv == null ? TypeAdaptor.parse(b).asDouble() : null;
        if ((av == null && ad == null) || (bv == null && bd == null)) {
            throw unsupported(op);
        }
        if (av != null && bd != null && (op.equals("norm") || op.equals("pnorm"))) {
            if (bd < 1 || Double.isNaN(bd)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.norm"), null);
            }
            double max = Math.max(Math.abs(av.x), Math.max(Math.abs(av.y), Math.abs(av.z)));
            if (max == 0 || Double.isInfinite(max) || Double.isInfinite(bd)) {
                return max;
            }
            return max * Math.pow(Math.pow(Math.abs(av.x) / max, bd) + Math.pow(Math.abs(av.y) / max, bd)
                    + (two ? 0 : Math.pow(Math.abs(av.z) / max, bd)), 1 / bd);
        }
        if (av != null && bd != null && (op.equals("normalize") || op.equals("setlength"))) {
            double length = Math.hypot(Math.hypot(av.x, av.y), av.z);
            return output(length == 0 ? Vec3.ZERO : av.scale(1 / length).scale(bd), two);
        }
        if (av != null && bv != null) {
            Vec3 delta = av.subtract(bv);
            switch (op) {
                case "distance":
                    return Math.hypot(Math.hypot(delta.x, delta.y), delta.z);
                case "distancesquared":
                    return delta.lengthSqr();
                case "manhattan":
                    return Math.abs(delta.x) + Math.abs(delta.y) + Math.abs(delta.z);
                case "chebyshev":
                    return Math.max(Math.abs(delta.x), Math.max(Math.abs(delta.y), Math.abs(delta.z)));
                case "@", "dot":
                    return av.dot(bv);
                case "#", "cross":
                    return two ? av.x * bv.y - av.y * bv.x : av.cross(bv);
                case "angle": {
                    double denominator = av.length() * bv.length();
                    return denominator == 0 ? Double.NaN
                            : Math.acos(Math.max(-1, Math.min(1, av.dot(bv) / denominator)));
                }
                case "project":
                    return output(bv.lengthSqr() == 0 ? Vec3.ZERO : bv.scale(av.dot(bv) / bv.lengthSqr()), two);
                case "=", "==", "equals":
                    return av.equals(bv);
                case "!=":
                    return !av.equals(bv);
            }
        }
        Vec3 left = av == null ? new Vec3(ad, ad, two ? 0 : ad) : av;
        Vec3 right = bv == null ? new Vec3(bd, bd, two ? 0 : bd) : bv;
        return output(new Vec3(component(left.x, right.x, op), component(left.y, right.y, op),
                two ? 0 : component(left.z, right.z, op)), two);
    }

    private static double component(double a, double b, String op) throws LogicException {
        Object result = numbers(a, b, op);
        if (!(result instanceof Double d)) {
            throw unsupported(op);
        }
        return d;
    }

    private static Object output(Vec3 v, boolean two) {
        return two ? new Vec2((float) v.x, (float) v.y) : v;
    }

    private static Object lists(List<Object> a, List<Object> b, String op) throws LogicException {
        if (eq(op)) {
            return a.equals(b);
        }
        if (op.equals("!=")) {
            return !a.equals(b);
        }
        if (op.equals("contains") || op.equals("containsall")) {
            return a.containsAll(b);
        }
        if (op.equals("intersects")) {
            return a.stream().anyMatch(b::contains);
        }
        List<Object> result = new ArrayList<>();
        switch (op) {
            case "+", "append", "concat":
                FlowConvert.checkListSize((long) a.size() + b.size());
                result.addAll(a);
                result.addAll(b);
                break;
            case "-", "difference", "remove":
                for (Object item : a) {
                    if (!b.contains(item)) {
                        add(result, item);
                    }
                }
                break;
            case "intersection":
                for (Object item : a) {
                    if (b.contains(item) && !result.contains(item)) {
                        add(result, item);
                    }
                }
                break;
            case "union":
                for (Object item : a) {
                    if (!result.contains(item)) {
                        add(result, item);
                    }
                }
                for (Object item : b) {
                    if (!result.contains(item)) {
                        add(result, item);
                    }
                }
                break;
            default:
                throw unsupported(op);
        }
        return result;
    }

    private static void add(List<Object> result, Object item) throws LogicException {
        FlowConvert.checkListSize((long) result.size() + 1);
        result.add(item);
    }

    private static Object strings(String a, Object b, String op) throws LogicException {
        String s = string(b);
        return switch (op) {
            case "=", "==", "equals" -> a.equals(s);
            case "!=" -> !a.equals(s);
            case "equalsignorecase" -> a.equalsIgnoreCase(s);
            case "+", "append", "concat" -> a + s;
            case "contains" -> a.contains(s);
            case "startswith" -> a.startsWith(s);
            case "endswith" -> a.endsWith(s);
            case "split" -> FlowConvert.split(a, s);
            case "indexof" -> (double) a.indexOf(s);
            case "lastindexof" -> (double) a.lastIndexOf(s);
            case "compare" -> (double) a.compareTo(s);
            case "compareignorecase" -> (double) a.compareToIgnoreCase(s);
            case "regex-match", "regexmatch", "matches" -> regex(a, s, false);
            case "regex-find", "regexfind" -> regex(a, s, true);
            case "remove" -> a.replace(s, "");
            default -> throw unsupported(op);
        };
    }

    /**
     * Tests {@code input} against {@code pattern}, either as a full match or as a search.
     *
     * <p>A malformed pattern is bad user input, not a crash: the compiler's exception is reported as
     * a {@link LogicException} carrying the offending text, so the flow shows a readable message
     * instead of the empty one the executor's catch-all would produce.
     *
     * @param input   the text to test
     * @param pattern the regular expression, as typed by the flow author
     * @param find    {@code true} for a search, {@code false} for a full match
     * @return whether the text matched
     * @throws LogicException when the pattern cannot be compiled
     */
    private static boolean regex(String input, String pattern, boolean find) throws LogicException {
        try {
            Matcher matcher = Pattern.compile(pattern).matcher(input);
            return find ? matcher.find() : matcher.matches();
        } catch (PatternSyntaxException exception) {
            throw new LogicException(exception,
                    Util.parseTranslatableText("fmod.node.bialu.error.regex", pattern), null);
        }
    }
}
