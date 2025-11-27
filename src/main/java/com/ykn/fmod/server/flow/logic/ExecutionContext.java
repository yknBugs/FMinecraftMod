package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

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
     * The sequence of executed nodes in this context, for debugging purposes
     */
    private List<NodeStatus> executedSequence;

    /**
     * The exception that occurred during execution, if any
     */
    private LogicException exception;

    public ExecutionContext(LogicFlow flow) {
        this.flow = flow;
        this.nodeStatuses = new HashMap<>();
        Collection<FlowNode> nodes = flow.getNodes();
        for (FlowNode node : nodes) {
            this.nodeStatuses.put(node.getId(), new NodeStatus(node));
        }
        this.variables = new HashMap<>();
        this.nodeExecutionCounter = 0L;
        this.executedSequence = new ArrayList<>();
        this.exception = null;
    }

    public LogicFlow getFlow() {
        return flow;
    }

    @Nullable
    public NodeStatus getNodeStatus(long nodeId) {
        return this.nodeStatuses.get(nodeId);
    }

    public void setVariable(String name, Object value) {
        this.variables.put(name, value);
    }

    @Nullable
    public Object getVariable(String name) {
        return this.variables.get(name);
    }

    public long getNodeExecutionCounter() {
        return nodeExecutionCounter;
    }

    public List<NodeStatus> getExecutedSequence() {
        return executedSequence;
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
        this.nodeExecutionCounter = 0L;
        executedSequence.clear();
        this.exception = null;
    }
    
    /**
     * Execute the flow from its start node up to a specified maximum number of node executions.
     * @param maxAllowedNodes the maximum number of nodes that may be executed before the execution is considered a dead loop and aborted
     */
    public void execute(long maxAllowedNodes) {
        this.resetExecutionStatus();
        FlowNode currentNode = this.flow.getNode(flow.startNodeId);
        try {
            while (currentNode != null) {
                if (this.nodeExecutionCounter > maxAllowedNodes) {
                    throw new LogicException(null, Util.parseTranslateableText("fmod.flow.error.deadloop", maxAllowedNodes), null);
                }
                FlowNode nextNode = currentNode.execute(this);
                this.nodeExecutionCounter++;
                this.executedSequence.add(this.nodeStatuses.get(currentNode.getId()).copy());
                currentNode = nextNode;
            }
        } catch (LogicException e) {
            this.exception = e;
            LoggerFactory.getLogger(Util.LOGGERNAME).info("Logic flow " + flow.name + " terminated with exception", e);
        }
    }

    /**
     * Render this execution status into a text representation for display.
     * It renders the dynamic status of this execution.
     * To render the static information about this flow, use {@link LogicFlow#render()} instead.
     * @return A text representation of this execution status.
     */
    public Text render() {
        MutableText title = Text.literal(this.flow.name).append("\n");
        for (int i = 0; i < this.executedSequence.size(); i++) { 
            NodeStatus node = this.executedSequence.get(i);
            Text nodeText = node.render(i + 1, this.flow);
            MutableText nodeEntry = Text.literal("[").append(node.node.name).append("] ");
            nodeEntry = nodeEntry.styled(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nodeText))
            );
            title = title.append(nodeEntry);
        }
        if (this.exception != null) {
            title = title.append("\n").append(exception.getMessageText());
        }
        return title;
    }
}
