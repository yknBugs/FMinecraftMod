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

import net.minecraft.text.Text;

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
        Text displayName = Util.parseTranslatableText("fmod.node.getvar.title.name");
        Text description = Util.parseTranslatableText("fmod.node.getvar.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.getvar.input.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getvar.input.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getvar.input.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.getvar.output.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getvar.output.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getvar.output.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object varNameObj = resolvedInputs.get(0);
        String varName = TypeAdaptor.parse(varNameObj).asString();
        Object value = context.getVariable(varName);
        status.setOutput(0, value);
    }
}
