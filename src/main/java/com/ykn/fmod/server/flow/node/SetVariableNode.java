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
        Component displayName = Util.parseTranslatableText("fmod.node.setvar.title.name");
        Component description = Util.parseTranslatableText("fmod.node.setvar.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.setvar.input.varname.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.setvar.input.varname.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.setvar.input.varname.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.setvar.input.value.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.setvar.input.value.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.setvar.input.value.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.setvar.output.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.setvar.output.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.setvar.output.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
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
