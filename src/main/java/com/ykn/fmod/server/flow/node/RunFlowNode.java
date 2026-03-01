/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ScheduledFlow;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.MinecraftServer;

/**
 * A flow node that triggers the execution of another flow.
 * Inputs:
 * 1. String - The name of the flow to run.
 * 2. Number - The delay before running the flow (in ticks). Default is 0.
 * 3. Boolean - Whether to keep all the variables from the current flow. Default is true.
 * Outputs:
 * 1. ScheduledTask - The scheduled task representing the running flow.
 * Branches: 1 (Next node)
 */
public class RunFlowNode extends FlowNode {

    public RunFlowNode(long id, String name) {
        super(id, name, 3, 1, 1, "RunFlowNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.runflow.title.name", "fmod.node.runflow.title.feat")
            .input("fmod.node.runflow.input.name.name", "fmod.node.runflow.input.name.feat", "fmod.node.runflow.input.name.type")
            .input("fmod.node.runflow.input.delay.name", "fmod.node.runflow.input.delay.feat", "fmod.node.runflow.input.delay.type")
            .input("fmod.node.runflow.input.keepvar.name", "fmod.node.runflow.input.keepvar.feat", "fmod.node.runflow.input.keepvar.type")
            .output("fmod.node.runflow.output.name", "fmod.node.runflow.output.feat", "fmod.node.runflow.output.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        String flowName = parseFlowName(resolvedInputs.get(0));
        int delayInput = parseDelay(resolvedInputs.get(1));
        boolean keepVariables = parseKeepVariables(resolvedInputs.get(2));

        MinecraftServer server = context.getServer();
        ServerData data = Util.getServerData(server);
        FlowManager targetFlow = data.getLogicFlows().get(flowName);
        if (targetFlow == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.runflow.error.noflow", flowName), null);
        }
        if (!targetFlow.isEnabled()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.runflow.error.disabled", flowName), null);
        }
        if (delayInput <= 0) {
            try {
                if (keepVariables) {
                    targetFlow.execute(data, context, context.getMaxAllowedNodes() - context.getNodeExecutionCounter(), context.getMaxAllowedRecursions(), null, context.getVariables());
                } else {
                    targetFlow.execute(data, context, context.getMaxAllowedNodes() - context.getNodeExecutionCounter(), context.getMaxAllowedRecursions(), null, null);
                }
            } catch (StackOverflowError e) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.flow.error.overflow"), null);
            } catch (LogicException e) {
                throw e;
            }
            status.setOutput(0, null);
        } else {
            ScheduledTask scheduledFlow = new ScheduledFlow(targetFlow, null, keepVariables ? context.getVariables() : null, server, delayInput);
            data.submitScheduledTask(scheduledFlow);
            status.setOutput(0, scheduledFlow);
        }
    }

    private String parseFlowName(Object flowObj) throws LogicException {
        if (flowObj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else {
            return TypeAdaptor.parse(flowObj).asString();
        }
    }

    private int parseDelay(Object delayObj) throws LogicException {
        if (delayObj == null) {
            return 0;
        }
        Double delay = TypeAdaptor.parse(delayObj).asDouble();
        if (delay == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
        }
        int delayInt = delay.intValue();
        if (delayInt < 0) {
            delayInt = 0;
        }
        return delayInt;
    }

    private boolean parseKeepVariables(Object keepVarObj) throws LogicException {
        if (keepVarObj == null) {
            return true;
        }
        Boolean keepVar = TypeAdaptor.parse(keepVarObj).asBoolean();
        if (keepVar == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(2), this.metadata.inputDataTypes.get(2)), null);
        }
        return keepVar;
    }
}
