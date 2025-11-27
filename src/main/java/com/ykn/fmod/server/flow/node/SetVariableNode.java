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
        this.type = "Set Variable Node";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslateableText("fmod.node.setvar.title.name");
        Text description = Util.parseTranslateableText("fmod.node.setvar.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslateableText("fmod.node.setvar.input.varname.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.setvar.input.varname.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.setvar.input.varname.type"));
        inputNames.add(Util.parseTranslateableText("fmod.node.setvar.input.value.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.setvar.input.value.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.setvar.input.value.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslateableText("fmod.node.setvar.output.name"));
        outputDescriptions.add(Util.parseTranslateableText("fmod.node.setvar.output.feat"));
        outputDataTypes.add(Util.parseTranslateableText("fmod.node.setvar.output.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object varNameObj = resolvedInputs.get(0);
        String varName = TypeAdaptor.parseStringLikeObject(varNameObj);
        Object valueObj = resolvedInputs.get(1);
        Object rawValue = context.getVariable(varName);
        context.setVariable(varName, valueObj);
        status.setOutput(0, rawValue);
    }
}
