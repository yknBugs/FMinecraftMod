/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Represents a single execution instance of a {@link LogicFlow}.
 * <p>
 * Since a LogicFlow can be executed multiple times simultaneously (potentially with different
 * parameters or states), each execution requires its own context to track:
 * <ul>
 *   <li>Node execution states and output values</li>
 *   <li>Flow variables that can be read and written by nodes</li>
 *   <li>The execution sequence for debugging and analysis</li>
 *   <li>Any exceptions that occur during execution</li>
 * </ul>
 * <p>
 * The ExecutionContext creates a deep copy of the LogicFlow at construction time,
 * ensuring that the original flow remains unmodified. It manages the execution loop,
 * tracks node execution to prevent infinite loops, and provides access to the
 * Minecraft server for nodes that need to interact with the game world.
 * <p>
 * Typical usage:
 * <pre>
 * ExecutionContext context = new ExecutionContext(flow, server, 1000);
 * context.execute(startOutputs, initialVariables);
 * if (context.getException() != null) {
 *     // Handle execution error
 * }
 * </pre>
 * 
 * @see LogicFlow
 * @see FlowNode
 * @see NodeStatus
 */
public class ExecutionContext {

    /**
     * The logic flow being executed in this context.
     * This is a deep copy of the original flow to ensure isolation between executions.
     */
    private LogicFlow flow;

    /**
     * The Minecraft server instance where this flow is executed.
     * Nodes can use this to interact with the game world, players, and entities.
     */
    private MinecraftServer server;

    /**
     * All node execution statuses in this context, mapped by node IDs.
     * Each NodeStatus tracks inputs, outputs, and execution state for one node.
     */
    private Map<Long, NodeStatus> nodeStatuses;

    /**
     * Variables that can be read and written during flow execution.
     * Nodes like SetVariableNode and GetVariableNode use this map to share data.
     */
    private Map<String, Object> variables;

    /**
     * The maximum number of node executions allowed in this context.
     * Used to detect and prevent infinite loops during execution.
     */
    private int maxAllowedNodes;

    /**
     * The maximum allowed recursion depth for flow executions.
     * Used to prevent infinite recursive calls when flows invoke other flows.
     */
    private int maxAllowedRecursions;

    /**
     * Counter tracking the total number of node executions in this context.
     * Used to detect and prevent infinite loops by limiting total executions.
     */
    private int nodeExecutionCounter;

    /**
     * The chronological sequence of executed nodes with their final states.
     * Each entry is a snapshot of a NodeStatus after that node completed execution.
     * Used for debugging, visualization, and flow analysis.
     */
    private List<NodeStatus> executedSequence;

    /**
     * The parent execution contexts that led to this context, if any.
     * This is used to track call stacks when flows invoke other flows.
     */
    private List<ExecutionContext> parentContexts;

    /**
     * The exception that occurred during execution, if any.
     * If execution completes normally, this remains null.
     * If a LogicException is thrown by any node, it is captured here.
     */
    private LogicException exception;

    /**
     * Creates a new execution context for the specified logic flow.
     * <p>
     * This constructor:
     * <ul>
     *   <li>Creates a deep copy of the flow to ensure isolation</li>
     *   <li>Initializes a NodeStatus for each node in the flow</li>
     *   <li>Initializes empty variable storage</li>
     *   <li>Resets all counters and state tracking</li>
     * </ul>
     * 
     * @param flow The logic flow to execute (will be copied)
     * @param server The Minecraft server instance for game world interaction
     * @param maxAllowedNodes The maximum number of node executions allowed to prevent infinite loops
     * @param maxAllowedRecursions The maximum allowed recursion depth for flow executions
     */
    public ExecutionContext(LogicFlow flow, MinecraftServer server, int maxAllowedNodes, int maxAllowedRecursions) {
        this.flow = flow.copy();
        this.server = server;
        this.nodeStatuses = new HashMap<>();
        Collection<FlowNode> nodes = this.flow.getNodes();
        for (FlowNode node : nodes) {
            this.nodeStatuses.put(node.getId(), new NodeStatus(node));
        }
        this.variables = new HashMap<>();
        this.maxAllowedNodes = maxAllowedNodes;
        this.maxAllowedRecursions = maxAllowedRecursions;
        this.nodeExecutionCounter = 0;
        this.executedSequence = new ArrayList<>();
        this.parentContexts = new ArrayList<>();
        this.exception = null;
    }

    /**
     * Gets the logic flow being executed in this context.
     * 
     * @return The (copied) LogicFlow for this execution
     */
    public LogicFlow getFlow() {
        return this.flow;
    }

    /**
     * Gets the Minecraft server instance for this execution.
     * 
     * @return The MinecraftServer where this flow is running
     */
    public MinecraftServer getServer() {
        return this.server;
    }

    /**
     * Gets the execution status for a specific node.
     * 
     * @param nodeId The ID of the node
     * @return The NodeStatus for the specified node, or null if the node doesn't exist
     */
    @Nullable
    public NodeStatus getNodeStatus(long nodeId) {
        return this.nodeStatuses.get(nodeId);
    }

    /**
     * Gets the first node to execute in this flow (the start node).
     * 
     * @return The start node, or null if not set
     */
    @Nullable
    public FlowNode getStartNode() {
        return this.flow.getFirstNode();
    }

    /**
     * Gets the number of outputs from the start node.
     * <p>
     * This is useful when initializing the execution with output values
     * for the start node (e.g., event parameters).
     * 
     * @return The number of output ports on the start node, or 0 if no start node exists
     */
    public int getStartNodeOutputNumber() {
        FlowNode startNode = this.flow.getFirstNode();
        if (startNode != null) {
            return startNode.getMetadata().outputNumber;
        }
        return 0;
    }

    /**
     * Sets a variable in this execution context.
     * <p>
     * Variables can be used to share data between nodes during execution.
     * If a variable with the same name already exists, it will be overwritten.
     * 
     * @param name The variable name
     * @param value The value to store (can be null)
     */
    public void setVariable(String name, Object value) {
        this.variables.put(name, value);
    }

    /**
    * Adds multiple variables to this execution context.
    * <p>
    * This is a convenience method for adding multiple variables at once.
    * If any variable names already exist, they will be overwritten.
    * 
    * @param newVariables A map of variable names to values to add
    */
    public void addVariables(Map<String, Object> newVariables) {
        this.variables.putAll(newVariables);
    }

    /**
     * Gets a variable from this execution context.
     * 
     * @param name The variable name
     * @return The variable value, or null if the variable doesn't exist
     */
    @Nullable
    public Object getVariable(String name) {
        return this.variables.get(name);
    }

    /**
     * Gets all variables in this execution context.
     * 
     * @return A map of all variable names to their values
     */
    public Map<String, Object> getVariables() {
        return this.variables;
    }

    /**
     * Gets the maximum number of node executions allowed in this context.
     * <p>
     * This limit is used to detect and prevent infinite loops during execution.
     * 
     * @return The maximum allowed node executions
     */
    public int getMaxAllowedNodes() {
        return this.maxAllowedNodes;
    }

    /**
     * Sets the maximum number of node executions allowed in this context.
     * <p>
     * This limit is used to detect and prevent infinite loops during execution.
     * 
     * @param maxAllowedNodes The maximum allowed node executions
     */
    public void setMaxAllowedNodes(int maxAllowedNodes) {
        if (maxAllowedNodes < 0) {
            this.maxAllowedNodes = 0;
        } else {
            this.maxAllowedNodes = maxAllowedNodes;
        }
    }

    /**
     * Gets the total number of node executions in this context.
     * <p>
     * This counter is used to detect potential infinite loops.
     * 
     * @return The number of times nodes have been executed
     */
    public int getNodeExecutionCounter() {
        return this.nodeExecutionCounter;
    }

    /**
     * Gets the sequence of executed nodes in chronological order.
     * <p>
     * Each entry is a snapshot of the node's state after it completed execution.
     * 
     * @return A list of NodeStatus snapshots representing the execution history
     */
    public List<NodeStatus> getExecutedSequence() {
        return this.executedSequence;
    }

    /**
     * Retrieves the list of parent execution contexts.
     *
     * @return a list of parent {@link ExecutionContext} objects, or an empty list if no parent contexts exist
     */
    public List<ExecutionContext> getParentContexts() {
        return this.parentContexts;
    }

    /**
     * Sets the parent context for this execution context.
     * This method clears any existing parent contexts and replaces them with
     * the parent context hierarchy from the provided parent, then adds the
     * parent context itself to the chain.
     *
     * @param parent the parent ExecutionContext to set. Must not be null.
     *               The parent's context chain will be copied to this context.
     */
    public void setParentContext(ExecutionContext parent) {
        this.parentContexts.clear();
        this.parentContexts.addAll(parent.getParentContexts());
        this.parentContexts.add(parent);
    }

    /**
     * Gets the maximum allowed recursion depth for flow executions.
     * <p>
     * This limit is used to prevent infinite recursive calls when flows invoke other flows.
     * 
     * @return The maximum allowed recursion depth
     */
    public int getMaxAllowedRecursions() {
        return this.maxAllowedRecursions;
    }

    /**
     * Sets the maximum allowed recursion depth for flow executions.
     * <p>
     * This limit is used to prevent infinite recursive calls when flows invoke other flows.
     * 
     * @param maxAllowedRecursions The maximum allowed recursion depth
     */
    public void setMaxAllowedRecursions(int maxAllowedRecursions) {
        if (maxAllowedRecursions < 0) {
            this.maxAllowedRecursions = 0;
        } else {
            this.maxAllowedRecursions = maxAllowedRecursions;
        }
    }

    /**
     * Gets the exception that occurred during execution, if any.
     * 
     * @return The LogicException that terminated execution, or null if execution completed normally
     */
    @Nullable
    public LogicException getException() {
        return this.exception;
    }

    /**
     * Resets the execution status of all nodes and clears all variables.
     * <p>
     * This method:
     * <ul>
     *   <li>Resets all node statuses to their initial state</li>
     *   <li>Clears all variables</li>
     *   <li>Resets the execution counter to 0</li>
     *   <li>Clears the execution sequence</li>
     *   <li>Clears any stored exception</li>
     * </ul>
     * <p>
     * Note: It is strongly recommended to create a new ExecutionContext for each execution
     * instead of reusing an existing one.
     */
    public void resetExecutionStatus() {
        for (NodeStatus status : this.nodeStatuses.values()) {
            status.reset();
        }
        this.variables.clear();
        this.nodeExecutionCounter = 0;
        this.executedSequence.clear();
        this.exception = null;
    }

    /**
     * Executes the logic flow in this context.
     * 
     * @param startNodeOutputs Optional list of output values to pre-populate for the start node
     * @param initialVariables Optional map of variables to initialize before execution
     * @throws Exception If any error occurs during execution
     */
    private void executeFlow(@Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) throws Exception {
        if (startNodeOutputs != null) {
            NodeStatus startNodeStatus = this.nodeStatuses.get(this.flow.startNodeId);
            if (startNodeStatus != null) {
                for (int i = 0; i < startNodeOutputs.size() && i < startNodeStatus.node.getMetadata().outputNumber; i++) {
                    startNodeStatus.setOutput(i, startNodeOutputs.get(i));
                }
            }
        }
        if (initialVariables != null) {
            this.variables.putAll(initialVariables);
        }
        FlowNode currentNode = this.flow.getFirstNode();
        if (this.parentContexts.size() >= this.maxAllowedRecursions) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.flow.error.recursion"), null);
        }
        if (currentNode == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.flow.error.nullstart"), null);
        }
        while (currentNode != null) {
            if (this.nodeExecutionCounter >= this.maxAllowedNodes) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.flow.error.deadloop", this.maxAllowedNodes), null);
            }
            FlowNode nextNode = currentNode.execute(this);
            this.nodeExecutionCounter++;
            this.executedSequence.add(this.nodeStatuses.get(currentNode.getId()).copy());
            currentNode = nextNode;
        }
    }
    
    /**
     * Executes the logic flow in this context.
     * <p>
     * The execution process:
     * <ol>
     *   <li>Resets the execution status</li>
     *   <li>Initializes start node outputs if provided</li>
     *   <li>Initializes variables if provided</li>
     *   <li>Executes nodes sequentially, following branch connections</li>
     *   <li>Continues until reaching a terminal node (null) or an error</li>
     *   <li>Enforces a maximum execution limit to prevent infinite loops</li>
     * </ol>
     * <p>
     * If any node throws a {@link LogicException}, execution terminates and the exception
     * is stored in this context. After execution, check {@link #getException()} to determine
     * if the flow completed successfully.
     * 
     * @param startNodeOutputs Optional list of output values to pre-populate for the start node
     *                         (useful for passing event parameters)
     * @param initialVariables Optional map of variables to initialize before execution
     */
    public void execute(@Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) {
        this.resetExecutionStatus();
        try {
            this.executeFlow(startNodeOutputs, initialVariables);
        } catch (LogicException e) {
            this.exception = e;
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Logic flow " + flow.name + " terminated with exception", e);
        } catch (StackOverflowError e) {
            this.exception = new LogicException(null, Util.parseTranslatableText("fmod.flow.error.overflow"), null);
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Logic flow " + flow.name + " terminated with stack overflow", e);
        } catch (Exception e) {
            this.exception = new LogicException(e, null, null);
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Logic flow " + flow.name + " terminated with unexpected exception", e);
        }
    }

    /**
     * Executes the logic flow in this context within the context of a parent execution.
     * <p>
     * If any node throws a {@link LogicException}, execution terminates and the exception
     * is stored in this context. After execution, check {@link #getException()} to determine
     * if the flow completed successfully.
     * 
     * @param parentContext The parent execution context invoking this flow
     * @param startNodeOutputs Optional list of output values to pre-populate for the start node
     *                         (useful for passing event parameters)
     * @param initialVariables Optional map of variables to initialize before execution
     * @throws LogicException If any error occurs during execution
     */
    public void execute(@NotNull ExecutionContext parentContext, @Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) throws LogicException {
        this.resetExecutionStatus();
        this.setParentContext(parentContext);
        try {
            this.executeFlow(startNodeOutputs, initialVariables);
        } catch (LogicException e) {
            this.exception = e;
            throw this.exception;
        } catch (StackOverflowError e) {
            this.exception = new LogicException(null, Util.parseTranslatableText("fmod.flow.error.overflow"), null);
            throw this.exception;
        } catch (Exception e) {
            this.exception = new LogicException(e, null, null);
            throw this.exception;
        }
    }

    /**
     * Renders the dynamic execution status of this context as displayable text.
     * <p>
     * The rendered text includes:
     * <ul>
     *   <li>The flow name</li>
     *   <li>A sequence of executed nodes (hoverable for details)</li>
     *   <li>Any exception message if execution failed</li>
     * </ul>
     * <p>
     * Each node in the sequence can be hovered over to see its execution details
     * (inputs, outputs, and next node).
     * <p>
     * Note: This renders dynamic execution state. To render static flow information,
     * use {@link LogicFlow#render()} instead.
     * 
     * @return A Text object suitable for display in Minecraft with hover events
     */
    public Text render() {
        MutableText title = Text.literal(this.flow.name).append(" ");
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
            title = title.append(" ").append(exception.getMessageText());
        }
        return title;
    }
}
