/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

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
        return NodeMetadata.builder("fmod.node.canceltask.title.name", "fmod.node.canceltask.title.feat")
            .input("fmod.node.canceltask.input.task.name", "fmod.node.canceltask.input.task.feat", "fmod.node.canceltask.input.task.type")
            .output("fmod.node.canceltask.output.ticks.name", "fmod.node.canceltask.output.ticks.feat", "fmod.node.canceltask.output.ticks.type")
            .output("fmod.node.canceltask.output.duration.name", "fmod.node.canceltask.output.duration.feat", "fmod.node.canceltask.output.duration.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
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
