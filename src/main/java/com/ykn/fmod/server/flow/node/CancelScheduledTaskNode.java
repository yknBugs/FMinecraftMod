/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.text.Text;

/**
 * A node that cancels a scheduled task.
 * Inputs:
 * 1. ScheduledTask - The scheduled task to cancel.
 * Outputs:
 * 1. Integer - The number of ticks left before the task is executed.
 * 2. Integer - The number of ticks left before the task is finished.
 * Branches: 1 (Next node)
 */
public class CancelScheduledTaskNode extends FlowNode {

    public CancelScheduledTaskNode(long id, String name) {
        super(id, name, 1, 2, 1);
        this.type = "CancelScheduledTaskNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Text displayName = Util.parseTranslatableText("fmod.node.canceltask.title.name");
        Text description = Util.parseTranslatableText("fmod.node.canceltask.title.feat");
        List<Text> inputNames = new ArrayList<>();
        List<Text> inputDescriptions = new ArrayList<>();
        List<Text> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.canceltask.input.task.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.canceltask.input.task.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.canceltask.input.task.type"));
        List<Text> outputNames = new ArrayList<>();
        List<Text> outputDescriptions = new ArrayList<>();
        List<Text> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.canceltask.output.ticks.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.canceltask.output.ticks.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.canceltask.output.ticks.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.canceltask.output.duration.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.canceltask.output.duration.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.canceltask.output.duration.type"));
        List<Text> branchNames = new ArrayList<>();
        List<Text> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        ScheduledTask task = parseScheduledTask(resolvedInputs.get(0));
        int ticksLeft = task.getDelay();
        int durationLeft = task.getDuration();
        task.cancel();
        status.setOutput(0, ticksLeft);
        status.setOutput(1, durationLeft);
    }

    private ScheduledTask parseScheduledTask(Object obj) throws LogicException {
        if (obj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (obj instanceof ScheduledTask) {
            return (ScheduledTask) obj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }
    
}
