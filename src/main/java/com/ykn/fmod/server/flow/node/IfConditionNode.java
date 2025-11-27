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
        this.type = "If Condition Node";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslateableText("fmod.node.if.title.name");
        Text description = Util.parseTranslateableText("fmod.node.if.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslateableText("fmod.node.if.input.name"));
        inputDescriptions.add(Util.parseTranslateableText("fmod.node.if.input.feat"));
        inputDataTypes.add(Util.parseTranslateableText("fmod.node.if.input.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslateableText("fmod.node.if.branch.true.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.if.branch.true.feat"));
        branchNames.add(Util.parseTranslateableText("fmod.node.if.branch.false.name"));
        branchDescriptions.add(Util.parseTranslateableText("fmod.node.if.branch.false.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    public long getNextNodeId(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Object conditionObj = resolvedInputs.get(0);
        Boolean condition = TypeAdaptor.parseBooleanLikeObject(conditionObj);
        if (condition == null) {
            throw new LogicException(null, Util.parseTranslateableText("fmod.node.if.error.classcast", this.name, this.metadata.inputNames.get(0)), null);
        } else if (condition == true) {
            return this.nextNodeIds.get(0); // True branch
        } else {
            return this.nextNodeIds.get(1); // False branch
        }
    }
}
