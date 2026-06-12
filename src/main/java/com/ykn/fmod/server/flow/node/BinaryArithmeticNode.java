/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * A flow node that performs binary arithmetic operations on two numeric inputs.
 * Inputs:
 * 1. Number - The first operand.
 * 2. Number - The second operand.
 * 3. String - The operation to perform (e.g., "+", "-", "*", "/").
 * Outputs:
 * 1. Number - The result of the operation.
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
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object num1Obj = resolvedInputs.get(0);
        Object num2Obj = resolvedInputs.get(1);
        Object opObj = resolvedInputs.get(2);
        String operation = TypeAdaptor.parse(opObj).asString().strip().toLowerCase();

        // Special handling for "==" operator
        if ("=".equals(operation) || "==".equals(operation)) {
            if (num1Obj == null && num2Obj == null) {
                status.setOutput(0, true);
            } else if (num1Obj == null || num2Obj == null) {
                status.setOutput(0, false);
            } else {
                status.setOutput(0, num1Obj.equals(num2Obj));
            }
            return;
        }

        if ("!=".equals(operation)) {
            if (num1Obj == null && num2Obj == null) {
                status.setOutput(0, false);
            } else if (num1Obj == null || num2Obj == null) {
                status.setOutput(0, true);
            } else {
                status.setOutput(0, !num1Obj.equals(num2Obj));
            }
            return;
        }
        
        // Special handling for null inputs
        if (num1Obj == null && num2Obj == null) {
            status.setOutput(0, null);
            return;
        } else if (num1Obj == null && num2Obj != null) {
            status.setOutput(0, num2Obj);
            return;
        } else if (num1Obj != null && num2Obj == null) {
            status.setOutput(0, num1Obj);
            return;
        }

        // Hanlding Unique cases for some specific data types
        Double tryDoubleNum1 = TypeAdaptor.parse(num1Obj).asDouble();
        Double tryDoubleNum2 = TypeAdaptor.parse(num2Obj).asDouble();

        // Try Vec3d operations
        Vec3 tryVec3dNum1 = TypeAdaptor.parse(num1Obj).asVec3d();
        Vec3 tryVec3dNum2 = TypeAdaptor.parse(num2Obj).asVec3d();
        if (tryVec3dNum1 != null && tryVec3dNum2 != null) {
            Vec3 result;
            switch (operation) {
                case "+":
                    result = tryVec3dNum1.add(tryVec3dNum2);
                    break;
                case "-":
                    result = tryVec3dNum1.subtract(tryVec3dNum2);
                    break;
                case "*":
                case "x":
                    result = new Vec3(tryVec3dNum1.x * tryVec3dNum2.x, tryVec3dNum1.y * tryVec3dNum2.y, tryVec3dNum1.z * tryVec3dNum2.z);
                    break;
                case "/":
                    // NaN is valid here to represent division by zero
                    result = new Vec3(tryVec3dNum1.x / tryVec3dNum2.x, tryVec3dNum1.y / tryVec3dNum2.y, tryVec3dNum1.z / tryVec3dNum2.z);
                    break;
                case "@":
                    // dot product
                    status.setOutput(0, tryVec3dNum1.dot(tryVec3dNum2));
                    return;
                case "#":
                    // cross product
                    result = tryVec3dNum1.cross(tryVec3dNum2);
                    break;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }

        if (tryVec3dNum1 != null && tryDoubleNum2 != null) {
            Vec3 result;
            switch (operation) {
                case "+":
                    result = new Vec3(tryVec3dNum1.x + tryDoubleNum2, tryVec3dNum1.y + tryDoubleNum2, tryVec3dNum1.z + tryDoubleNum2);
                    break;
                case "-":
                    result = new Vec3(tryVec3dNum1.x - tryDoubleNum2, tryVec3dNum1.y - tryDoubleNum2, tryVec3dNum1.z - tryDoubleNum2);
                    break;
                case "*":
                case "x":
                    result = new Vec3(tryVec3dNum1.x * tryDoubleNum2, tryVec3dNum1.y * tryDoubleNum2, tryVec3dNum1.z * tryDoubleNum2);
                    break;
                case "/":
                    // NaN is valid here to represent division by zero
                    result = new Vec3(tryVec3dNum1.x / tryDoubleNum2, tryVec3dNum1.y / tryDoubleNum2, tryVec3dNum1.z / tryDoubleNum2);
                    break;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }
        if (tryDoubleNum1 != null && tryVec3dNum2 != null) {
            Vec3 result;
            switch (operation) {
                case "+":
                    result = new Vec3(tryDoubleNum1 + tryVec3dNum2.x, tryDoubleNum1 + tryVec3dNum2.y, tryDoubleNum1 + tryVec3dNum2.z);
                    break;
                case "-":
                    result = new Vec3(tryDoubleNum1 - tryVec3dNum2.x, tryDoubleNum1 - tryVec3dNum2.y, tryDoubleNum1 - tryVec3dNum2.z);
                    break;
                case "*":
                case "x":
                    result = new Vec3(tryDoubleNum1 * tryVec3dNum2.x, tryDoubleNum1 * tryVec3dNum2.y, tryDoubleNum1 * tryVec3dNum2.z);
                    break;
                case "/":
                    // NaN is valid here to represent division by zero
                    result = new Vec3(tryDoubleNum1 / tryVec3dNum2.x, tryDoubleNum1 / tryVec3dNum2.y, tryDoubleNum1 / tryVec3dNum2.z);
                    break;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }

        // Try Vec2f operations
        Vec2 tryVec2fNum1 = TypeAdaptor.parse(num1Obj).asVec2f();
        Vec2 tryVec2fNum2 = TypeAdaptor.parse(num2Obj).asVec2f();

        if (tryVec2fNum1 != null && tryVec2fNum2 != null) {
            Vec2 result;
            switch (operation) {
                case "+":
                    result = tryVec2fNum1.add(tryVec2fNum2);
                    break;
                case "-":
                    result = new Vec2(tryVec2fNum1.x - tryVec2fNum2.x, tryVec2fNum1.y - tryVec2fNum2.y);
                    break;
                case "*":
                case "x":
                    result = new Vec2(tryVec2fNum1.x * tryVec2fNum2.x, tryVec2fNum1.y * tryVec2fNum2.y);
                    break;
                case "/":
                    result = new Vec2(tryVec2fNum1.x / tryVec2fNum2.x, tryVec2fNum1.y / tryVec2fNum2.y);
                    break;
                case "@":
                    // dot product
                    status.setOutput(0, tryVec2fNum1.dot(tryVec2fNum2));
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }

        if (tryVec2fNum1 != null && tryDoubleNum2 != null) {
            float scalarF = tryDoubleNum2.floatValue();
            Vec2 result;
            switch (operation) {
                case "+":
                    result = new Vec2(tryVec2fNum1.x + scalarF, tryVec2fNum1.y + scalarF);
                    break;
                case "-":
                    result = new Vec2(tryVec2fNum1.x - scalarF, tryVec2fNum1.y - scalarF);
                    break;
                case "*":
                case "x":
                    result = new Vec2(tryVec2fNum1.x * scalarF, tryVec2fNum1.y * scalarF);
                    break;
                case "/":
                    result = new Vec2(tryVec2fNum1.x / scalarF, tryVec2fNum1.y / scalarF);
                    break;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }
        if (tryDoubleNum1 != null && tryVec2fNum2 != null) {
            float scalarF = tryDoubleNum1.floatValue();
            Vec2 result;
            switch (operation) {
                case "+":
                    result = new Vec2(scalarF + tryVec2fNum2.x, scalarF + tryVec2fNum2.y);
                    break;
                case "-":
                    result = new Vec2(scalarF - tryVec2fNum2.x, scalarF - tryVec2fNum2.y);
                    break;
                case "*":
                case "x":
                    result = new Vec2(scalarF * tryVec2fNum2.x, scalarF * tryVec2fNum2.y);
                    break;
                case "/":
                    result = new Vec2(scalarF / tryVec2fNum2.x, scalarF / tryVec2fNum2.y);
                    break;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }

        // Double operations
        if (tryDoubleNum1 != null && tryDoubleNum2 != null) {
            try {
                switch (operation) {
                    case "+":
                        status.setOutput(0, tryDoubleNum1 + tryDoubleNum2);
                        return;
                    case "-":
                        status.setOutput(0, tryDoubleNum1 - tryDoubleNum2);
                        return;
                    case "*":
                    case "x":
                        status.setOutput(0, tryDoubleNum1 * tryDoubleNum2);
                        return;
                    case "/":
                        // NaN is valid here to represent division by zero
                        status.setOutput(0, tryDoubleNum1 / tryDoubleNum2);
                        return;
                    case "%":
                        if (tryDoubleNum2 == 0) {
                            throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.divzero", this.name), null);
                        }
                        status.setOutput(0, tryDoubleNum1 % tryDoubleNum2);
                        return;
                    case ">":
                        status.setOutput(0, tryDoubleNum1 > tryDoubleNum2);
                        return;
                    case "<":
                        status.setOutput(0, tryDoubleNum1 < tryDoubleNum2);
                        return;
                    case ">=":
                        status.setOutput(0, tryDoubleNum1 >= tryDoubleNum2);
                        return;
                    case "<=":
                        status.setOutput(0, tryDoubleNum1 <= tryDoubleNum2);
                        return;
                    case "^":
                        status.setOutput(0, Math.pow(tryDoubleNum1, tryDoubleNum2));
                        return;
                    case "max":
                        status.setOutput(0, Math.max(tryDoubleNum1, tryDoubleNum2));
                        return;
                    case "min":
                        status.setOutput(0, Math.min(tryDoubleNum1, tryDoubleNum2));
                        return;
                    case "log":
                        status.setOutput(0, Math.log(tryDoubleNum2) / Math.log(tryDoubleNum1));
                        return;
                    case "atan2":
                        status.setOutput(0, Math.atan2(tryDoubleNum1, tryDoubleNum2));
                        return;
                    case "hypot":
                        status.setOutput(0, Math.hypot(tryDoubleNum1, tryDoubleNum2));
                        return;
                    default:
                        throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
                }
            } catch (ArithmeticException ex) {
                throw new LogicException(ex, Util.parseTranslatableText("fmod.node.bialu.error.arithmetic", this.name), null);
            }
        }

        // Logic expressions for boolean inputs
        Boolean tryBoolNum1 = TypeAdaptor.parse(num1Obj).asBoolean();
        Boolean tryBoolNum2 = TypeAdaptor.parse(num2Obj).asBoolean();

        if (tryBoolNum1 != null && tryBoolNum2 != null) {
            switch (operation) {
                case "and":
                case "&&":
                    status.setOutput(0, tryBoolNum1 && tryBoolNum2);
                    return;
                case "or":
                case "||":
                    status.setOutput(0, tryBoolNum1 || tryBoolNum2);
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        }

        // String and Text support append operation
        if (num1Obj instanceof Component && num2Obj instanceof Component) {
            Component text1 = (Component) num1Obj;
            Component text2 = (Component) num2Obj;
            MutableComponent result = Component.empty();
            if ("+".equals(operation)) {
                result = result.append(text1).append(text2);
                status.setOutput(0, result);
                return;
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        } else if (num1Obj instanceof Component) {
            String str2 = TypeAdaptor.parse(num2Obj).asString();
            Component text1 = (Component) num1Obj;
            MutableComponent result = Component.empty();
            if ("+".equals(operation)) {
                result = result.append(text1).append(str2);
                status.setOutput(0, result);
                return;
            } else {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        }

        String str1 = TypeAdaptor.parse(num1Obj).asString();
        String str2 = TypeAdaptor.parse(num2Obj).asString();
        switch (operation) {
            case "+":
            case "append":
                status.setOutput(0, str1 + str2);
                return;
            case "contains":
                status.setOutput(0, str1.contains(str2));
                return;
            case "startswith":
                status.setOutput(0, str1.startsWith(str2));
                return;
            case "endswith":
                status.setOutput(0, str1.endsWith(str2));
                return;
            case "split":
                try {
                    String[] parts = str1.split(Pattern.quote(str2));
                    List<String> listResult = new ArrayList<>();
                    for (String part : parts) {
                        listResult.add(part);
                    }
                    status.setOutput(0, listResult);
                } catch (Exception ex) {
                    throw new LogicException(ex, Util.parseTranslatableText("fmod.node.bialu.error.arithmetic", this.name), null);
                }
                return;
            case "indexof":
                status.setOutput(0, str1.indexOf(str2));
                return;
            case "lastindexof":
                status.setOutput(0, str1.lastIndexOf(str2));
                return;
            default:
                 throw new LogicException(null, Util.parseTranslatableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
        }
    }
}
