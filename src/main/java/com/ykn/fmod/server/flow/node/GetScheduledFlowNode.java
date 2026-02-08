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
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.network.chat.Component;

/**
 * A node to get a list of all the scheduled flows, which are flows that are submitted by RunFlowNode but not executed yet
 * Inputs:
 * 1. String - Filter, if not null, only the flows with the same name will be returned
 * 2. String - Filter, if not null, only the flows with the same start node type will be returned
 * 3. String - Filter, if not null, only the flows containing the same context variable name will be returned
 * 4. Number - Filter, if not null, only the flows with the same or more nodes will be returned
 * 5. Number - Filter, if not null, only the flows with the same or less nodes will be returned
 * 6. Number - Filter, if not null, only the flows with the same or more ticks left will be returned
 * 7. Number - Filter, if not null, only the flows with the same or less ticks left will be returned
 * Outputs:
 * 1. List - A list of ScheduledFlow that satisfy the filter conditions, or the ScheduledFlow object if only one flow satisfies.
 * 2. Integer - The number of flows that satisfy the filter conditions
 * 3. List - A list of context variable values of the flows that satisfy the filter conditions, or else null.
 * 4. List - The number of nodes of the flows that satisfy the filter conditions.
 * 5. List - The ticks left of the flows that satisfy the filter conditions.
 * Branches: 1 (Next node)
 */
public class GetScheduledFlowNode extends FlowNode {

    public GetScheduledFlowNode(long id, String name) {
        super(id, name, 7, 5, 1);
        this.type = "GetScheduledFlowNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.getscheduledflow.title.name");
        Component description = Util.parseTranslatableText("fmod.node.getscheduledflow.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.flowname.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.flowname.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.flowname.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.nodetype.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.nodetype.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.nodetype.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.varname.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.varname.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.varname.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.minnodes.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.minnodes.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.minnodes.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.maxnodes.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.maxnodes.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.maxnodes.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.minticks.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.minticks.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.minticks.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.maxticks.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.maxticks.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.input.maxticks.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.flows.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.flows.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.flows.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.count.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.count.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.count.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.variables.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.variables.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.variables.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.nodecounts.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.nodecounts.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.nodecounts.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.ticksleft.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.ticksleft.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getscheduledflow.output.ticksleft.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
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
            if (task instanceof ScheduledFlow) {
                ScheduledFlow flow = (ScheduledFlow) task;
                
                // Get flow properties
                String flowName = flow.getFlowManager().flow.name;
                FlowNode startNode = flow.getFlowManager().flow.getFirstNode();
                String startNodeType = startNode != null ? startNode.getType() : null;
                Map<String, Object> variables = flow.getContextVariables();
                int nodeCount = flow.getFlowManager().flow.getNodes().size();
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
        if (matchedFlows.isEmpty()) {
            status.setOutput(0, null);
        } else if (matchedFlows.size() == 1) {
            status.setOutput(0, matchedFlows.get(0));
        } else {
            status.setOutput(0, matchedFlows);
        }
        
        status.setOutput(1, matchedFlows.size());

        if (variableValues.isEmpty()) {
            status.setOutput(2, null);
        } else if (variableValues.size() == 1) {
            status.setOutput(2, variableValues.get(0));
        } else {
            status.setOutput(2, variableValues);
        }

        if (nodeCounts.isEmpty()) {
            status.setOutput(3, null);
        } else if (nodeCounts.size() == 1) {
            status.setOutput(3, nodeCounts.get(0));
        } else {
            status.setOutput(3, nodeCounts);
        }

        if (ticksLeftList.isEmpty()) {
            status.setOutput(4, null);
        } else if (ticksLeftList.size() == 1) {
            status.setOutput(4, ticksLeftList.get(0));
        } else {
            status.setOutput(4, ticksLeftList);
        }
    }
    
    private String parseStringFilter(Object obj) {
        if (obj == null) {
            return null;
        }
        String str = TypeAdaptor.parse(obj).asString();
        if (str.strip().isEmpty()) {
            return null;
        }
        return str;
    }
}
