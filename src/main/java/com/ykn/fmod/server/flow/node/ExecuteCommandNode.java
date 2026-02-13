/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.RedirectedCommandOutput;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

/**
 * Node that executes a command string
 * Inputs:
 * 1. Entity - The source entity.
 * 2. String - The command to execute.
 * Outputs:
 * 1. Text - The feedback message from command execution, if any.
 * 2. String - The raw output from command execution, if any.
 * 3. Integer - The result code from command execution, if any.
 * Branches: 1 (Next node)
 */
public class ExecuteCommandNode extends FlowNode {

    public ExecuteCommandNode(long id, String name) {
        super(id, name, 2, 3, 1);
        this.type = "ExecuteCommandNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslatableText("fmod.node.runcommand.title.name");
        Text description = Util.parseTranslatableText("fmod.node.runcommand.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.runcommand.input.source.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.runcommand.input.source.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.runcommand.input.source.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.runcommand.input.command.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.runcommand.input.command.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.runcommand.input.command.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.runcommand.output.feedback.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.runcommand.output.feedback.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.runcommand.output.feedback.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.runcommand.output.raw.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.runcommand.output.raw.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.runcommand.output.raw.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.runcommand.output.result.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.runcommand.output.result.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.runcommand.output.result.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description,
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Entity sourceEntity = parseEntity(resolvedInputs.get(0));
        String command = parseCommand(resolvedInputs.get(1));
        RedirectedCommandOutput output = RedirectedCommandOutput.create();
        int result = Util.runCommand(output, sourceEntity, command, 4);
        status.setOutput(0, output.getAllMessage());
        status.setOutput(1, output.getRawOutput());
        status.setOutput(2, result);
    }

    private Entity parseEntity(Object entityObject) throws LogicException {
        if (entityObject == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (entityObject instanceof Entity) {
            return (Entity) entityObject;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }

    private String parseCommand(Object commandObject) throws LogicException {
        if (commandObject == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(1)), null);
        }
        String command = TypeAdaptor.parse(commandObject).asString().strip();
        if (command.isEmpty()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(1)), null);
        }
        return command;
    }
}
