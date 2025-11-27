package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

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
        super(id, name, 3, 1, 1);
        this.type = "Binary Arithmetic Node";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslateableText("fmod.node.bialu.title.name");
        Text description = Util.parseTranslateableText("fmod.node.bialu.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslateableText("fmod.node.bialu.input.num1.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.bialu.input.num1.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.bialu.input.num1.type"));
        inputNames.add(Util.parseTranslateableText("fmod.node.bialu.input.num2.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.bialu.input.num2.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.bialu.input.num2.type"));
        inputNames.add(Util.parseTranslateableText("fmod.node.bialu.input.op.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.bialu.input.op.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.bialu.input.op.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslateableText("fmod.node.bialu.output.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.bialu.output.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.bialu.output.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object num1Obj = resolvedInputs.get(0);
        Object num2Obj = resolvedInputs.get(1);
        Object opObj = resolvedInputs.get(2);
        String operation = TypeAdaptor.parseStringLikeObject(opObj);

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
        if (num1Obj instanceof Vec3d && num2Obj instanceof Vec3d) {
            Vec3d vec1 = (Vec3d) num1Obj;
            Vec3d vec2 = (Vec3d) num2Obj;
            Vec3d result;
            switch (operation.toLowerCase()) {
                case "+":
                    result = vec1.add(vec2);
                    break;
                case "-":
                    result = vec1.subtract(vec2);
                    break;
                case "*":
                case "x":
                    result = new Vec3d(vec1.x * vec2.x, vec1.y * vec2.y, vec1.z * vec2.z);
                    break;
                case "/":
                    // NaN is valid here to represent division by zero
                    result = new Vec3d(vec1.x / vec2.x, vec1.y / vec2.y, vec1.z / vec2.z);
                    break;
                case "@":
                    // dot product
                    status.setOutput(0, vec1.dotProduct(vec2));
                    return;
                case "#":
                    // cross product
                    result = vec1.crossProduct(vec2);
                    break;
                default:
                    throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }

        if (num1Obj instanceof Vec2f && num2Obj instanceof Vec2f) {
            Vec2f vec1 = (Vec2f) num1Obj;
            Vec2f vec2 = (Vec2f) num2Obj;
            Vec2f result;
            switch (operation.toLowerCase()) {
                case "+":
                    result = vec1.add(vec2);
                    break;
                case "-":
                    result = new Vec2f(vec1.x - vec2.x, vec1.y - vec2.y);
                    break;
                case "*":
                case "x":
                    result = new Vec2f(vec1.x * vec2.x, vec1.y * vec2.y);
                    break;
                case "/":
                    result = new Vec2f(vec1.x / vec2.x, vec1.y / vec2.y);
                    break;
                case "@":
                    // dot product
                    status.setOutput(0, vec1.dot(vec2));
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
            status.setOutput(0, result);
            return;
        }

        // String and Text support append operation
        if (num1Obj instanceof String && num2Obj instanceof String) {
            String str1 = (String) num1Obj;
            String str2 = (String) num2Obj;
            if ("+".equals(operation)) {
                status.setOutput(0, str1 + str2);
                return;
            } else {
                throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        }

        if (num1Obj instanceof Text && num2Obj instanceof Text) {
            Text text1 = (Text) num1Obj;
            Text text2 = (Text) num2Obj;
            MutableText result = Text.empty();
            if ("+".equals(operation)) {
                result = result.append(text1).append(text2);
                status.setOutput(0, result);
                return;
            } else {
                throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        }

        // Logic expressions for boolean inputs
        if (num1Obj instanceof Boolean && num2Obj instanceof Boolean) {
            Boolean bool1 = (Boolean) num1Obj;
            Boolean bool2 = (Boolean) num2Obj;
            switch (operation.toLowerCase()) {
                case "and":
                case "&&":
                    status.setOutput(0, bool1 && bool2);
                    return;
                case "or":
                case "||":
                    status.setOutput(0, bool1 || bool2);
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        }

        // Fallback to number parsing and arithmetic
        Double number1 = TypeAdaptor.parseNumberLikeObject(num1Obj);
        Double number2 = TypeAdaptor.parseNumberLikeObject(num2Obj);
        try {
            if (number1 == null) {
                throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.classcast", this.name, this.metadata.inputNames.get(0)), null);
            }
            if (number2 == null) {
                throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.classcast", this.name, this.metadata.inputNames.get(1)), null);
            }

            switch (operation.toLowerCase()) {
                case "+":
                    status.setOutput(0, number1 + number2);
                    return;
                case "-":
                    status.setOutput(0, number1 - number2);
                    return;
                case "*":
                case "x":
                    status.setOutput(0, number1 * number2);
                    return;
                case "/":
                    // NaN is valid here to represent division by zero
                    status.setOutput(0, number1 / number2);
                    return;
                case "%":
                    if (number2 == 0) {
                        throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.divzero", this.name), null);
                    }
                    status.setOutput(0, number1 % number2);
                    return;
                case ">":
                    status.setOutput(0, number1 > number2);
                    return;
                case "<":
                    status.setOutput(0, number1 < number2);
                    return;
                case ">=":
                    status.setOutput(0, number1 >= number2);
                    return;
                case "<=":
                    status.setOutput(0, number1 <= number2);
                    return;
                case "^":
                    status.setOutput(0, Math.pow(number1, number2));
                    return;
                case "max":
                    status.setOutput(0, Math.max(number1, number2));
                    return;
                case "min":
                    status.setOutput(0, Math.min(number1, number2));
                    return;
                case "log":
                    status.setOutput(0, Math.log(number2) / Math.log(number1));
                    return;
                case "atan2":
                    status.setOutput(0, Math.atan2(number1, number2));
                    return;
                case "hypot":
                    status.setOutput(0, Math.hypot(number1, number2));
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslateableText("fmod.node.bialu.error.unsupported", this.name, operation), null);
            }
        } catch (ArithmeticException ex) {
            throw new LogicException(ex, Util.parseTranslateableText("fmod.node.bialu.error.arithmetic", this.name), null);
        }
    }
}
