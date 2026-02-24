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

/**
 * A flow node that evaluates a boolean condition and branches accordingly.
 * Inputs:
 * 1. Boolean - The condition to evaluate.
 * Outputs: None
 * Branches:
 * 1. True branch
 * 2. False branch
 */
public class IfConditionNode extends FlowNode {

    public IfConditionNode(long id, String name) {
        super(id, name, 1, 0, 2);
        this.type = "IfConditionNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.if.title.name", "fmod.node.if.title.feat")
            .input("fmod.node.if.input.name", "fmod.node.if.input.feat", "fmod.node.if.input.type")
            .branch("fmod.node.if.branch.true.name", "fmod.node.if.branch.true.feat")
            .branch("fmod.node.if.branch.false.name", "fmod.node.if.branch.false.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    public long getNextNodeId(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object conditionObj = resolvedInputs.get(0);
        Boolean condition = TypeAdaptor.parse(conditionObj).asBoolean();
        if (condition == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.if.error.classcast", this.name, this.metadata.inputNames.get(0)), null);
        } else if (condition == true) {
            return this.getNextNodeIds().get(0); // True branch
        } else {
            return this.getNextNodeIds().get(1); // False branch
        }
    }
}
