/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

/**
 * A flow node that sets the value of a variable in the execution context.
 * Inputs:
 * 1. String - The name of the variable to set.
 * 2. Object - The value to set the variable to.
 * Outputs:
 * 1. Object - The previous value of the variable.
 * Branches: 1 (Next node)
 */
public class SetVariableNode extends FlowNode {

    public SetVariableNode(long id, String name) {
        super(id, name, 2, 1, 1);
        this.type = "SetVariableNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.setvar.title.name", "fmod.node.setvar.title.feat")
            .input("fmod.node.setvar.input.varname.name", "fmod.node.setvar.input.varname.feat", "fmod.node.setvar.input.varname.type")
            .input("fmod.node.setvar.input.value.name", "fmod.node.setvar.input.value.feat", "fmod.node.setvar.input.value.type")
            .output("fmod.node.setvar.output.name", "fmod.node.setvar.output.feat", "fmod.node.setvar.output.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object varNameObj = resolvedInputs.get(0);
        String varName = TypeAdaptor.parse(varNameObj).asString();
        Object valueObj = resolvedInputs.get(1);
        Object rawValue = context.getVariable(varName);
        context.setVariable(varName, valueObj);
        status.setOutput(0, rawValue);
    }
}
