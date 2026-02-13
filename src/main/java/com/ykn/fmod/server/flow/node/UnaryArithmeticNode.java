/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

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

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * A flow node that performs unary arithmetic operations on one numeric input.
 * Inputs:
 * 1. Number - The operand.
 * 2. String - The operation to perform (e.g., "-", "!", "abs", "sin").
 * Outputs:
 * 1. Number - The result of the operation.
 * Branches: 1 (Next node)
 */
public class UnaryArithmeticNode extends FlowNode {
	public UnaryArithmeticNode(long id, String name) {
		super(id, name, 2, 1, 1);
		this.type = "UnaryArithmeticNode";
	}

	@Override
	protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
		Component displayName = Util.parseTranslatableText("fmod.node.unalu.title.name");
		Component description = Util.parseTranslatableText("fmod.node.unalu.title.feat");
		List<Component> inputNames = new ArrayList<>();
		List<Component> inputDescriptions = new ArrayList<>();
		List<Component> inputDataTypes = new ArrayList<>();
		inputNames.add(Util.parseTranslatableText("fmod.node.unalu.input.num.name"));
		inputDescriptions.add(Util.parseTranslatableText("fmod.node.unalu.input.num.feat"));
		inputDataTypes.add(Util.parseTranslatableText("fmod.node.unalu.input.num.type"));
		inputNames.add(Util.parseTranslatableText("fmod.node.unalu.input.op.name"));
		inputDescriptions.add(Util.parseTranslatableText("fmod.node.unalu.input.op.feat"));
		inputDataTypes.add(Util.parseTranslatableText("fmod.node.unalu.input.op.type"));
		List<Component> outputNames = new ArrayList<>();
		List<Component> outputDescriptions = new ArrayList<>();
		List<Component> outputDataTypes = new ArrayList<>();
		outputNames.add(Util.parseTranslatableText("fmod.node.unalu.output.name"));
		outputDescriptions.add(Util.parseTranslatableText("fmod.node.unalu.output.feat"));
		outputDataTypes.add(Util.parseTranslatableText("fmod.node.unalu.output.type"));
		List<Component> branchNames = new ArrayList<>();
		List<Component> branchDescriptions = new ArrayList<>();
		branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
		branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
		return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description,
			inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
	}

	@Override
	protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
		Object numObj = resolvedInputs.get(0);
		Object opObj = resolvedInputs.get(1);
		String operation = TypeAdaptor.parse(opObj).asString().strip().toLowerCase();

        // Special handling for null inputs
		if (numObj == null) {
			status.setOutput(0, null);
			return;
		}

        Double tryDouble = TypeAdaptor.parse(numObj).asDouble();

        // Try Vec3d operations
        Vec3 tryVec3d = TypeAdaptor.parse(numObj).asVec3d();
        if (tryVec3d != null) {
            switch (operation) {
                case "length":
                case "len":
                    status.setOutput(0, tryVec3d.length());
                    return;
                case "normalize":
                case "norm":
                    status.setOutput(0, tryVec3d.normalize());
                    return;
                case "-":
                    status.setOutput(0, tryVec3d.scale(-1));
                    return;
                case "x":
                    status.setOutput(0, tryVec3d.x);
                    return;
                case "y":
                    status.setOutput(0, tryVec3d.y);
                    return;
                case "z":
                    status.setOutput(0, tryVec3d.z);    
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
            }
        }

        // Try Vec2f operations
        Vec2 tryVec2f = TypeAdaptor.parse(numObj).asVec2f();
        if (tryVec2f != null) {
            switch (operation) {
                case "length":
                case "len":
                    status.setOutput(0, tryVec2f.length());
                    return;
                case "normalize":
                case "norm":
                    status.setOutput(0, tryVec2f.normalized());
                    return;
                case "-":
                    status.setOutput(0, new Vec2(-tryVec2f.x, -tryVec2f.y));
                    return;
                case "x":
                    status.setOutput(0, tryVec2f.x);
                    return;
                case "y":
                    status.setOutput(0, tryVec2f.y);
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
            }
        }

        // Try double operations
        if (tryDouble != null) {
            switch (operation) {
                case "-":
                    status.setOutput(0, -tryDouble);
                    return;
                case "~":
                    status.setOutput(0, (double) ~tryDouble.longValue());
                    return;
                case "abs":
                    status.setOutput(0, Math.abs(tryDouble));
                    return;
                case "sqrt":
                    status.setOutput(0, Math.sqrt(tryDouble));
                    return;
                case "cbrt":
                    status.setOutput(0, Math.cbrt(tryDouble));
                    return;
                case "exp":
                    status.setOutput(0, Math.exp(tryDouble));
                    return;
                case "ln":
                    status.setOutput(0, Math.log(tryDouble));
                    return;
                case "lg":
                    status.setOutput(0, Math.log10(tryDouble));
                    return;
                case "sin":
                    status.setOutput(0, Math.sin(tryDouble));
                    return;
                case "cos":
                    status.setOutput(0, Math.cos(tryDouble));
                    return;
                case "tan":
                    status.setOutput(0, Math.tan(tryDouble));
                    return;
                case "asin":
                    status.setOutput(0, Math.asin(tryDouble));
                    return;
                case "acos":
                    status.setOutput(0, Math.acos(tryDouble));
                    return;
                case "atan":
                    status.setOutput(0, Math.atan(tryDouble));
                    return;
                case "sinh":
                    status.setOutput(0, Math.sinh(tryDouble));
                    return;
                case "cosh":
                    status.setOutput(0, Math.cosh(tryDouble));
                    return;
                case "tanh":
                    status.setOutput(0, Math.tanh(tryDouble));
                    return;
                case "ceil":
                    status.setOutput(0, Math.ceil(tryDouble));
                    return;
                case "floor":
                    status.setOutput(0, Math.floor(tryDouble));
                    return;
                case "round":
                    status.setOutput(0, (double) Math.round(tryDouble));
                    return;
                case "rad":
                    status.setOutput(0, Math.toRadians(tryDouble));
                    return;
                case "deg":
                    status.setOutput(0, Math.toDegrees(tryDouble));
                    return;
                case "sgn":
                    status.setOutput(0, Math.signum(tryDouble));
                    return;
                case "erf":
                    // Java's Math class does not have an erf function
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
                    // status.setOutput(0, Math.erf(tryDouble));
                    // return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
            }
        }

        // Try boolean operations
        Boolean tryBool = TypeAdaptor.parse(numObj).asBoolean();
        if (tryBool != null) {
            switch (operation) {
                case "!":
                case "not":
                    status.setOutput(0, !tryBool);
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
            }
        }

        // Special Use case for String and Text
        if (numObj instanceof Component) {
            Component text = (Component) numObj;
            switch (operation) {
                case "length":
                case "len":
                    status.setOutput(0, text.getString().length());
                    return;
                case "lower":
                    status.setOutput(0, text.getString().toLowerCase());
                    return;
                case "isempty":
                    status.setOutput(0, text.getString().isEmpty());
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
            }
        }

        String tryString = TypeAdaptor.parse(numObj).asString();
        if (tryString != null) {
            switch (operation) {
                case "length":
                case "len":
                    status.setOutput(0, tryString.length());
                    return;
                case "lower":
                    status.setOutput(0, tryString.toLowerCase());
                    return;
                case "isempty":
                    status.setOutput(0, tryString.isEmpty());
                    return;
                case "strip":
                    status.setOutput(0, tryString.strip());
                    return;
                default:
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
            }
        }

        // If we reach here, the input type is unsupported
        throw new LogicException(null, Util.parseTranslatableText("fmod.node.unalu.error.unsupported", this.name, operation), null);
	}
}
