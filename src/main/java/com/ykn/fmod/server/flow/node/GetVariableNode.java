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
 * A flow node that retrieves the value of a variable from the execution context.
 * Inputs:
 * 1. String - The name of the variable to retrieve.
 * Outputs:
 * 1. Object - The value of the variable.
 * Branches: 1 (Next node)
 */
public class GetVariableNode extends FlowNode {

    public GetVariableNode(long id, String name) {
        super(id, name, 1, 1, 1);
        this.type = "GetVariableNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.getvar.title.name", "fmod.node.getvar.title.feat")
            .input("fmod.node.getvar.input.name", "fmod.node.getvar.input.feat", "fmod.node.getvar.input.type")
            .output("fmod.node.getvar.output.name", "fmod.node.getvar.output.feat", "fmod.node.getvar.output.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object varNameObj = resolvedInputs.get(0);
        String varName = TypeAdaptor.parse(varNameObj).asString();
        Object value = context.getVariable(varName);
        status.setOutput(0, value);
    }
}
