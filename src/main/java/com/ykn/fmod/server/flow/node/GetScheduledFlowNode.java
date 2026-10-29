/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.schedule.ScheduledFlow;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import com.ykn.fmod.server.flow.tool.FlowConvert;

/**
 * A flow node that lists the scheduled flow tasks that match the given filters.
 * Inputs:
 * 1. String - Filter by flow name.
 * 2. String - Filter by start node type.
 * 3. String - Filter by existence of a variable with the given name.
 * 4. Integer - Filter by minimum number of nodes.
 * 5. Integer - Filter by maximum number of nodes.
 * 6. Integer - Filter by minimum ticks left.
 * 7. Integer - Filter by maximum ticks left.
 * Outputs:
 * 1. List[Scheduled Task] - List of matching tasks.
 * 2. Integer - Number of matching tasks.
 * 3. List[Object] - List of variable values for each matching task, mapped by the given variable name filter.
 * 4. List[Integer] - List of node counts for the flow in each matching task.
 * 5. List[Integer] - List of ticks left for each matching task.
 * Branches: 1 (Next node)
 */
public class GetScheduledFlowNode extends FlowNode {

    public GetScheduledFlowNode(long id, String name) {
        super(id, name, 7, 5, 1, "GetScheduledFlowNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.getscheduledflow.title.name", "fmod.node.getscheduledflow.title.feat")
            .input("fmod.node.getscheduledflow.input.flowname.name", "fmod.node.getscheduledflow.input.flowname.feat", "fmod.node.getscheduledflow.input.flowname.type")
            .input("fmod.node.getscheduledflow.input.nodetype.name", "fmod.node.getscheduledflow.input.nodetype.feat", "fmod.node.getscheduledflow.input.nodetype.type")
            .input("fmod.node.getscheduledflow.input.varname.name", "fmod.node.getscheduledflow.input.varname.feat", "fmod.node.getscheduledflow.input.varname.type")
            .input("fmod.node.getscheduledflow.input.minnodes.name", "fmod.node.getscheduledflow.input.minnodes.feat", "fmod.node.getscheduledflow.input.minnodes.type")
            .input("fmod.node.getscheduledflow.input.maxnodes.name", "fmod.node.getscheduledflow.input.maxnodes.feat", "fmod.node.getscheduledflow.input.maxnodes.type")
            .input("fmod.node.getscheduledflow.input.minticks.name", "fmod.node.getscheduledflow.input.minticks.feat", "fmod.node.getscheduledflow.input.minticks.type")
            .input("fmod.node.getscheduledflow.input.maxticks.name", "fmod.node.getscheduledflow.input.maxticks.feat", "fmod.node.getscheduledflow.input.maxticks.type")
            .output("fmod.node.getscheduledflow.output.flows.name", "fmod.node.getscheduledflow.output.flows.feat", "fmod.node.getscheduledflow.output.flows.type")
            .output("fmod.node.getscheduledflow.output.count.name", "fmod.node.getscheduledflow.output.count.feat", "fmod.node.getscheduledflow.output.count.type")
            .output("fmod.node.getscheduledflow.output.variables.name", "fmod.node.getscheduledflow.output.variables.feat", "fmod.node.getscheduledflow.output.variables.type")
            .output("fmod.node.getscheduledflow.output.nodecounts.name", "fmod.node.getscheduledflow.output.nodecounts.feat", "fmod.node.getscheduledflow.output.nodecounts.type")
            .output("fmod.node.getscheduledflow.output.ticksleft.name", "fmod.node.getscheduledflow.output.ticksleft.feat", "fmod.node.getscheduledflow.output.ticksleft.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        // Parse filter inputs
        String flowNameFilter = parseStringFilter(resolvedInputs.get(0));
        String nodeTypeFilter = parseStringFilter(resolvedInputs.get(1));
        String varNameFilter = parseStringFilter(resolvedInputs.get(2));
        Double minNodes = TypeAdaptor.parse(resolvedInputs.get(3)).asDouble();
        Double maxNodes = TypeAdaptor.parse(resolvedInputs.get(4)).asDouble();
        Double minTicks = TypeAdaptor.parse(resolvedInputs.get(5)).asDouble();
        Double maxTicks = TypeAdaptor.parse(resolvedInputs.get(6)).asDouble();
        
        // Collect all scheduled flows and apply filters
        List<ScheduledFlow> matchedFlows = new ArrayList<>();
        List<Object> variableValues = new ArrayList<>();
        List<Integer> nodeCounts = new ArrayList<>();
        List<Integer> ticksLeftList = new ArrayList<>();
        
        List<ScheduledTask> allTasks = Util.getServerData(context.getServer()).getScheduledTasks();
        for (ScheduledTask task : allTasks) {
            if (task instanceof ScheduledFlow flow) {
                // Get flow properties
                LogicFlow logicFlow = flow.getFlowManager().getFlow();
                String flowName = logicFlow.getName();
                FlowNode startNode = logicFlow.getFirstNode();
                String startNodeType = startNode != null ? startNode.getType() : null;
                Map<String, Object> variables = flow.getContextVariables();
                int nodeCount = logicFlow.getNodes().size();
                int ticksLeft = task.getDelay();
                
                // Apply filters
                if (flowNameFilter != null && !flowNameFilter.equals(flowName)) {
                    continue;
                }
                if (nodeTypeFilter != null && !nodeTypeFilter.equals(startNodeType)) {
                    continue;
                }
                if (varNameFilter != null && !variables.containsKey(varNameFilter)) {
                    continue;
                }
                if (minNodes != null && nodeCount < minNodes) {
                    continue;
                }
                if (maxNodes != null && nodeCount > maxNodes) {
                    continue;
                }
                if (minTicks != null && ticksLeft < minTicks) {
                    continue;
                }
                if (maxTicks != null && ticksLeft > maxTicks) {
                    continue;
                }
                
                // This flow matches all filters
                FlowConvert.checkListSize((long) matchedFlows.size() + 1);
                matchedFlows.add(flow);
                nodeCounts.add(nodeCount);
                ticksLeftList.add(ticksLeft);
                
                // Get variable value if filter is specified
                if (varNameFilter != null) {
                    variableValues.add(variables.get(varNameFilter));
                } else {
                    variableValues.add(null);
                }
            }
        }
        
        // Set outputs
        status.setOutput(0, FlowConvert.unwrapOutput(matchedFlows.isEmpty() ? null : matchedFlows));
        status.setOutput(1, matchedFlows.size());
        status.setOutput(2, FlowConvert.unwrapOutput(variableValues.isEmpty() ? null : variableValues));
        status.setOutput(3, FlowConvert.unwrapOutput(nodeCounts.isEmpty() ? null : nodeCounts));
        status.setOutput(4, FlowConvert.unwrapOutput(ticksLeftList.isEmpty() ? null : ticksLeftList));
    }
    
    private String parseStringFilter(Object obj) {
        if (obj == null) {
            return null;
        }
        String str = TypeAdaptor.parse(obj).asString();
        if (str.isBlank()) {
            return null;
        }
        return str;
    }
}
