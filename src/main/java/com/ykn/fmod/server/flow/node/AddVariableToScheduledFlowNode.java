/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.schedule.ScheduledFlow;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

/**
 * A flow node that adds a variable to a scheduled flow.
 * Inputs:
 * 1. ScheduledFlow - The scheduled flow to add the variable to.
 * 2. String - The name of the variable to add, if not provided, it will add all variables from the current flow.
 * 3. Object - The value of the variable to add, if not provided, it will use the value from the current flow.
 * Outputs:
 * 1. Number - The number of variables in the scheduled flow after adding.
 * Branches: 1 (Next node)
 */
public class AddVariableToScheduledFlowNode extends FlowNode {

    public AddVariableToScheduledFlowNode(long id, String name) {
        super(id, name, 3, 1, 1, "AddVariableToScheduledFlowNode");
    }
    
    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.addvarflow.title.name", "fmod.node.addvarflow.title.feat")
            .input("fmod.node.addvarflow.input.flow.name", "fmod.node.addvarflow.input.flow.feat", "fmod.node.addvarflow.input.flow.type")
            .input("fmod.node.addvarflow.input.varname.name", "fmod.node.addvarflow.input.varname.feat", "fmod.node.addvarflow.input.varname.type")
            .input("fmod.node.addvarflow.input.value.name", "fmod.node.addvarflow.input.value.feat", "fmod.node.addvarflow.input.value.type")
            .output("fmod.node.addvarflow.output.name", "fmod.node.addvarflow.output.feat", "fmod.node.addvarflow.output.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        ScheduledFlow flow = parseScheduledFlow(resolvedInputs.get(0));
        Object varObj = resolvedInputs.get(1);
        if (varObj == null) {
            flow.addContextVariables(context.getVariables());
            status.setOutput(0, flow.getContextVariables().keySet().size());
            return;
        }
        String varName = TypeAdaptor.parse(varObj).asString();
        Object valueObj = resolvedInputs.get(2);
        if (valueObj == null) {
            Object currentValue = context.getVariable(varName);
            if (currentValue != null) {
                flow.addContextVariable(varName, currentValue);
            }
            status.setOutput(0, flow.getContextVariables().keySet().size());
            return;
        }
        flow.addContextVariable(varName, valueObj);
        status.setOutput(0, flow.getContextVariables().keySet().size());
    }

    private ScheduledFlow parseScheduledFlow(Object flowObj) throws LogicException {
        if (flowObj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (flowObj instanceof ScheduledFlow) {
            return (ScheduledFlow) flowObj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }
}
