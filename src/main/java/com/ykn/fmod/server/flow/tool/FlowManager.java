/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.LogicFlow;

/**
 * Manages a logic flow with editing capabilities and execution control.
 * <p>
 * FlowManager provides high-level operations for:
 * <ul>
 *   <li>Creating, modifying, and connecting nodes</li>
 *   <li>Undo/redo functionality for all editing operations</li>
 *   <li>Enabling/disabling automatic event-based execution</li>
 *   <li>Executing flows and managing execution history</li>
 * </ul>
 * <p>
 * The manager maintains two stacks for undo/redo operations, allowing users to
 * freely modify flows and revert changes. Each editing operation is recorded
 * as a {@link NodeEditPath} that knows how to apply and reverse itself.
 * <p>
 * Key features:
 * <ul>
 *   <li><b>Node management:</b> Create, remove, and rename nodes</li>
 *   <li><b>Connection management:</b> Set inputs (constants or node references) and next node branches</li>
 *   <li><b>Undo/Redo:</b> Full undo/redo support for all operations</li>
 *   <li><b>Execution control:</b> Enable/disable flows and execute them with history tracking</li>
 * </ul>
 * <p>
 * Note: Any kinds of editing operations will automatically disable the flow to prevent unintended executions.
 * <p>
 * Note: This class is designed to be an interface between user interactions and the underlying logic flow data structure.
 * (i.e. Be an intermediate layer between commands and the {@link LogicFlow} class). It is not recommended for developers
 * to use this class for programmatically manipulating flows in code, as it is optimized for user-driven editing with undo/redo 
 * support rather than direct flow manipulation. Instead, please dirctly use the {@link LogicFlow} class for flow manipulation in code.
 * <p>
 * Note: User should not see the node ids, because ids are internal immutable identifiers for nodes and what we used to
 * identify nodes in code. For users, they should always use node names to manage nodes. While node names are mutable and 
 * can be changed by users, command side should always validate the node name to make sure it exists and is unique.
 * <p>
 * Example usage:
 * <pre>
 * FlowManager manager = new FlowManager("MyFlow", "EntityDeathEventNode", "OnDeath");
 * manager.createNode("BinaryArithmeticNode", "AddOne");
 * manager.setConstInput("AddOne", 0, 1.0);
 * manager.setNextNode("OnDeath", 0, "AddOne");
 * manager.isEnabled = true;
 * </pre>
 * 
 * @see LogicFlow
 * @see NodeEditPath
 * @see ExecutionContext
 */
public class FlowManager {

    /**
     * The logic flow being managed.
     * <p>
     * This flow is modified by the manager's editing operations and can be
     * executed when enabled.
     */
    private final LogicFlow flow;

    /**
     * Whether this flow is enabled for automatic event triggering.
     * <p>
     * Only enabled flows will be automatically executed when their triggering
     * events occur (e.g., entity death, player interaction).
     */
    private volatile boolean enabled;

    /**
     * Stack of operations that can be redone.
     * <p>
     * Populated when the user performs undo operations. Cleared whenever a new
     * editing operation is performed (you can't redo after making a new change).
     */
    private final Deque<NodeEditPath> redoPath;

    /**
     * Stack of operations that can be undone.
     * <p>
     * Every editing operation pushes a {@link NodeEditPath} onto this stack,
     * allowing the user to revert changes in reverse order.
     */
    private final Deque<NodeEditPath> undoPath;

    /**
     * Creates a flow manager for an existing logic flow.
     * <p>
     * The manager starts with the flow disabled and empty undo/redo stacks.
     * 
     * @param flow The logic flow to manage
     */
    public FlowManager(LogicFlow flow) {
        this.flow = flow;
        this.enabled = false;
        this.redoPath = new ArrayDeque<>();
        this.undoPath = new ArrayDeque<>();
    }

    /**
     * Creates a new flow manager with a fresh logic flow.
     * <p>
     * This constructor creates a new flow with a single event node as the start node.
     * The flow is initially disabled with empty undo/redo stacks.
     * 
     * @param name The name of the new logic flow
     * @param eventNode The type name of the event node to create (must be registered)
     * @param eventNodeName The display name for the event node
     */
    public FlowManager(String name, String eventNode, String eventNodeName) {
        this.flow = new LogicFlow(name);
        FlowNode startNode = NodeRegistry.createNode(eventNode, flow.generateId(), eventNodeName);
        if (startNode == null) {
            throw new IllegalArgumentException("Unknown event node type: " + eventNode + " for start node " + eventNodeName + ".");
        }
        this.flow.addNode(startNode);
        this.flow.setStartNodeId(startNode.getId());
        this.enabled = false;
        this.redoPath = new ArrayDeque<>();
        this.undoPath = new ArrayDeque<>();
    }

    /**
     * Creates a new node and adds it to the flow.
     * <p>
     * The node is created using {@link NodeRegistry} with an auto-generated ID.
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param type The type name of the node to create (must be registered in NodeRegistry)
     * @param name The display name for the new node
     */
    public void createNode(String type, String name) {
        this.redoPath.clear();
        FlowNode node = NodeRegistry.createNode(type, flow.generateId(), name);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node type: " + type + " for node " + name + ".");
        }
        this.flow.addNode(node);
        this.undoPath.add(new NodeEditPath(f -> f.addNode(node), f -> f.removeNode(node.getId())));
        this.enabled = false;
    }

    /**
     * Removes a node from the flow by name.
     * <p>
     * If a node with the specified name exists, it is removed from the flow.
     * This operation is recorded in the undo stack and clears the redo stack.
     * <p>
     * Note: This does not automatically disconnect references to this node from other nodes.
     * 
     * @param name The name of the node to remove
     */
    public void removeNode(String name) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            this.flow.removeNode(node.getId());
            this.undoPath.add(new NodeEditPath(f -> f.removeNode(node.getId()), f -> f.addNode(node)));
            this.enabled = false;
        }
    }

    /**
     * Replaces the starting event node with a new event node of the specified type.
     * <p>
     * The old starting node is removed and replaced with a new node created
     * using {@link NodeRegistry}. The new node becomes the start node of the flow.
     * This operation is recorded in the undo stack and clears the redo stack.
     * <p>
     * Note: Since the start node cannot be deleted, this method provides a way to change the event type.
     * 
     * @param type The type name of the new event node to create (must be registered)
     * @param name The display name for the new event node
     */
    public void replaceEventNode(String type, String name) {
        this.redoPath.clear();
        FlowNode oldNode = this.flow.getFirstNode();
        FlowNode newNode = NodeRegistry.createNode(type, flow.generateId(), name);
        if (newNode == null) {
            throw new IllegalArgumentException("Unknown event node type: " + type + " for event node " + name + ".");
        }
        if (oldNode != null) {
            this.flow.addNode(newNode);
            this.flow.setStartNodeId(newNode.getId());
            this.flow.removeNode(oldNode.getId());
            this.undoPath.add(new NodeEditPath(
                f -> {
                    f.addNode(newNode);
                    f.setStartNodeId(newNode.getId());
                    f.removeNode(oldNode.getId());
                },
                f -> {
                    f.addNode(oldNode);
                    f.setStartNodeId(oldNode.getId());
                    f.removeNode(newNode.getId());
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Renames a node in the flow.
     * <p>
     * Changes the display name of the specified node. Node IDs remain unchanged.
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param oldName The current name of the node to rename
     * @param newName The new name for the node
     */
    public void renameNode(String oldName, String newName) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(oldName);
        if (node != null) {
            node.setName(newName);
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setName(newName);
                    }
                }, 
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setName(oldName);
                    }
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Sets an input port to a constant value.
     * <p>
     * The input will reference the constant value directly rather than another node's output.
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param name The name of the node to modify
     * @param index The index of the input port (0-based)
     * @param value The constant value to set
     */
    public void setConstInput(String name, int index, Object value) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            DataReference oldValue = node.getInput(index);
            node.setInput(index, DataReference.createConstantReference(value));
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, DataReference.createConstantReference(value));
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, oldValue);
                    }
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Sets an input port to reference another node's output.
     * <p>
     * This creates a data connection from the specified output of one node
     * to the specified input of another node.
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param name The name of the node to modify
     * @param index The index of the input port (0-based)
     * @param refNode The name of the node whose output to reference
     * @param refIndex The index of the output port on the referenced node (0-based)
     */
    public void setReferenceInput(String name, int index, String refNode, int refIndex) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        FlowNode ref = this.flow.getNodeByName(refNode);
        if (node != null && ref != null) {
            DataReference oldValue = node.getInput(index);
            node.setInput(index, DataReference.createNodeOutputReference(ref.getId(), refIndex));
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    FlowNode r = f.getNode(ref.getId());
                    if (n != null && r != null) {
                        n.setInput(index, DataReference.createNodeOutputReference(r.getId(), refIndex));
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, oldValue);
                    }
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Disconnects an input port by setting it to an empty reference.
     * <p>
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param name The name of the node to modify
     * @param index The index of the input port to disconnect (0-based)
     */
    public void disconnectInput(String name, int index) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            DataReference oldValue = node.getInput(index);
            node.setInput(index, DataReference.createEmptyReference());
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, DataReference.createEmptyReference());
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setInput(index, oldValue);
                    }
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Sets the next node for a specific branch.
     * <p>
     * This creates a control flow connection from one node to another.
     * Nodes with multiple branches (e.g., conditional nodes) use different
     * branch indices for different execution paths.
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param name The name of the source node
     * @param index The branch index (0-based)
     * @param next The name of the next node to execute
     */
    public void setNextNode(String name, int index, String next) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        FlowNode nextNode = this.flow.getNodeByName(next);
        if (node != null && nextNode != null) {
            long oldNextId = node.getNextNodeIds().get(index);
            node.setNextNodeId(index, nextNode.getId());
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    FlowNode nn = f.getNode(nextNode.getId());
                    if (n != null && nn != null) {
                        n.setNextNodeId(index, nn.getId());
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setNextNodeId(index, oldNextId);
                    }
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Disconnects a branch by setting its next node to -1 (no connection).
     * <p>
     * This effectively makes the branch a termination point - the flow will
     * end if this branch is taken during execution.
     * This operation is recorded in the undo stack and clears the redo stack.
     * 
     * @param name The name of the node to modify
     * @param index The branch index to disconnect (0-based)
     */
    public void disconnectNextNode(String name, int index) {
        this.redoPath.clear();
        FlowNode node = this.flow.getNodeByName(name);
        if (node != null) {
            long oldNextId = node.getNextNodeIds().get(index);
            node.setNextNodeId(index, -1L);
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setNextNodeId(index, -1L);
                    }
                },
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.setNextNodeId(index, oldNextId);
                    }
                }
            ));
            this.enabled = false;
        }
    }

    /**
     * Redoes the most recently undone operation.
     * <p>
     * Moves the top operation from the redo stack to the undo stack and applies it.
     * Does nothing if the redo stack is empty.
     */
    public void redo() {
        if (!this.redoPath.isEmpty()) {
            NodeEditPath edit = this.redoPath.pop();
            edit.redo(this.flow);
            this.undoPath.push(edit);
            this.enabled = false;
        }
    }

    /**
     * Undoes the most recent operation.
     * <p>
     * Moves the top operation from the undo stack to the redo stack and reverses it.
     * Does nothing if the undo stack is empty.
     */
    public void undo() {
        if (!this.undoPath.isEmpty()) {
            NodeEditPath edit = this.undoPath.pop();
            edit.undo(this.flow);
            this.redoPath.push(edit);
            this.enabled = false;
        }
    }

    /**
     * Executes this flow and stores the execution context in the server's history.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates a new ExecutionContext for this flow</li>
     *   <li>Executes the flow with the specified parameters</li>
     *   <li>Stores the execution context in the server's history</li>
     *   <li>Trims the history if it exceeds the configured limit</li>
     *   <li>Returns any exception that occurred during execution</li>
     * </ol>
     * <p>
     * The execution respects the max flow length configuration to prevent infinite loops.
     * 
     * @param serverData The server data containing the server instance and execution history
     * @param startNodeOutputs Optional output values to pre-populate for the start node (e.g., event parameters)
     * @param initialVariables Optional initial variables for the execution context
     * @return The LogicException that terminated execution, or null if execution completed successfully
     */
    @Nullable
    public LogicException execute(@Nonnull ServerData serverData, @Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) {
        return this.execute(serverData, Util.getServerConfig().getMaxFlowLength(), Util.getServerConfig().getMaxFlowRecursionDepth(), startNodeOutputs, initialVariables);
    }

    /**
     * Executes this flow with a specified maximum flow length and stores the execution context in the server's history.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates a new ExecutionContext for this flow</li>
     *   <li>Executes the flow with the specified parameters</li>
     *   <li>Stores the execution context in the server's history</li>
     *   <li>Trims the history if it exceeds the configured limit</li>
     *   <li>Returns any exception that occurred during execution</li>
     * </ol>
     * 
     * @param serverData The server data containing the server instance and execution history
     * @param maxFlowLength The maximum number of nodes to execute before forcibly stopping
     * @param maxRecursionDepth The maximum allowed recursion depth for flow executions
     * @param startNodeOutputs Optional output values to pre-populate for the start node (e.g., event parameters)
     * @param initialVariables Optional initial variables for the execution context
     * @return The LogicException that terminated execution, or null if execution completed successfully
     */
    @Nullable
    public LogicException execute(@Nonnull ServerData serverData, int maxFlowLength, int maxRecursionDepth, @Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) {
        ExecutionContext executionContext = new ExecutionContext(this.flow, serverData.getServer(), maxFlowLength, maxRecursionDepth);
        executionContext.execute(startNodeOutputs, initialVariables);
        int historyLimit = Util.getServerConfig().getMaxFlowHistorySize();
        serverData.addExecuteHistory(executionContext, historyLimit);
        return executionContext.getException();
    }

    /**
     * Executes this flow within a parent execution context and stores the execution context in the server's history.
     * <p>
     * This method:
     * <ol>
     *   <li>Creates a new ExecutionContext for this flow</li>
     *   <li>Executes the flow within the provided parent context</li>
     *   <li>Stores the execution context in the server's history</li>
     *   <li>Trims the history if it exceeds the configured limit</li>
     * </ol>
     * 
     * @param serverData The server data containing the server instance and execution history
     * @param parentContext The parent execution context to inherit from
     * @param maxFlowLength The maximum number of nodes to execute before forcibly stopping
     * @param maxRecursionDepth The maximum allowed recursion depth for flow executions
     * @param startNodeOutputs Optional output values to pre-populate for the start node (e.g., event parameters)
     * @param initialVariables Optional initial variables for the execution context
     * @throws LogicException If an error occurs during flow execution
     */
    public void execute(@Nonnull ServerData serverData, ExecutionContext parentContext, int maxFlowLength, int maxRecursionDepth, @Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) throws LogicException {
        // If the flow is executed by another flow i.e. in another execution context
        // Exception will not be caught and returned, but directly thrown to let the parent flow to handle it
        // Therefore, we specially need to put history before execution only in this special case
        // Or exception thrown by this flow will prevent the history from being recorded, which may cause issues for debugging
        ExecutionContext executionContext = new ExecutionContext(this.flow, serverData.getServer(), maxFlowLength, maxRecursionDepth);
        serverData.addExecuteHistory(executionContext, Util.getServerConfig().getMaxFlowHistorySize());
        executionContext.execute(parentContext, startNodeOutputs, initialVariables);
    }

    /**
     * Gets the logic flow being managed.
     * <p>
     * This flow is modified by the manager's editing operations and can be
     * executed when enabled.
     * 
     * @return The logic flow being managed
     */
    public LogicFlow getFlow() {
        return this.flow;
    }

    /**
    * Checks whether this flow is enabled for automatic event triggering.
    * <p>
    * Only enabled flows will be automatically executed when their triggering
    * events occur (e.g., entity death, player interaction).
    * 
    * @return True if the flow is enabled, false otherwise
    */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether this flow is enabled for automatic event triggering.
     * <p>
     * Only enabled flows will be automatically executed when their triggering
     * events occur (e.g., entity death, player interaction).
     * 
     * @param enabled True to enable the flow, false to disable it
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if there are operations available to redo.
     * <p>
     * Returns true if the redo stack is not empty, indicating that there are
     * operations that can be reapplied after an undo.
     * 
     * @return True if redo operations are available, false otherwise
     */
    public boolean canRedo() {
        return !this.redoPath.isEmpty();
    }

    /**
     * Checks if there are operations available to undo.
     * <p>
     * Returns true if the undo stack is not empty, indicating that there are
     * operations that can be reversed to revert changes made to the flow.
     * 
     * @return True if undo operations are available, false otherwise
     */
    public boolean canUndo() {
        return !this.undoPath.isEmpty();
    }
}
