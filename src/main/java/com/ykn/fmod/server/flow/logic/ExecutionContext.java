package com.ykn.fmod.server.flow.logic;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.util.Util;

/**
 * Since one LogicFlow can be executed multiple times simultaneously,
 * This class represents a single execution context of a LogicFlow.
 */
public class ExecutionContext {

    /**
     * The logic flow being executed in this context
     */
    private LogicFlow flow;

    /**
     * All nodes in this logic flow, mapped by their IDs
     */
    private Map<Long, FlowNode> nodes;

    /**
     * All the node statuses in this execution context, mapped by their node IDs
     */
    private Map<Long, NodeStatus> nodeStatuses;

    /**
     * Variables in this execution context.
     */
    private Map<String, Object> variables;

    /**
     * A counter to track how many nodes have been executed in this context to avoid dead loops
     */
    private long nodeExecutionCounter;

    /**
     * The exception that occurred during execution, if any
     */
    private LogicException exception;

    public ExecutionContext(LogicFlow flow) {
        this.flow = flow;
        this.nodes = new HashMap<>();
        this.nodeStatuses = new HashMap<>();
        this.variables = new HashMap<>();
        this.nodeExecutionCounter = 0L;
        this.exception = null;
    }

    public LogicFlow getFlow() {
        return flow;
    }

    public void addNode(FlowNode node) {
        this.nodes.put(node.getId(), node);
        this.nodeStatuses.put(node.getId(), new NodeStatus(node));
    }

    public FlowNode getNode(long nodeId) {
        return this.nodes.get(nodeId);
    }

    public NodeStatus getNodeStatus(long nodeId) {
        return this.nodeStatuses.get(nodeId);
    }

    public void setVariable(String name, Object value) {
        this.variables.put(name, value);
    }

    public Object getVariable(String name) {
        return this.variables.get(name);
    }

    public long getNodeExecutionCounter() {
        return nodeExecutionCounter;
    }

    @Nullable
    public LogicException getException() {
        return exception;
    }

    /**
     * Reset the execution status of all nodes and clear all variables
     */
    public void resetExecutionStatus() {
        for (NodeStatus status : this.nodeStatuses.values()) {
            status.reset();
        }
        variables.clear();
    }
    
    /**
     * Execute the flow from its start node up to a specified maximum number of node executions.
     * @param maxAllowedNodes the maximum number of nodes that may be executed before the execution is considered a dead loop and aborted
     */
    public void execute(long maxAllowedNodes) {
        this.resetExecutionStatus();
        this.nodeExecutionCounter = 0L;
        FlowNode currentNode = this.getNode(flow.startNodeId);
        try {
            while (currentNode != null) {
                if (this.nodeExecutionCounter > maxAllowedNodes) {
                    throw new LogicException(null, Util.parseTranslateableText("fmod.flow.error.deadloop", maxAllowedNodes), null);
                }
                currentNode = currentNode.execute(this);
                this.nodeExecutionCounter++;
            }
        } catch (LogicException e) {
            this.exception = e;
            LoggerFactory.getLogger(Util.LOGGERNAME).info("Logic flow " + flow.name + " terminated with exception", e);
        }
    }
}
