/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public LogicFlow flow;

    /**
     * Whether this flow is enabled for automatic event triggering.
     * <p>
     * Only enabled flows will be automatically executed when their triggering
     * events occur (e.g., entity death, player interaction).
     */
    public boolean isEnabled;

    /**
     * Stack of operations that can be redone.
     * <p>
     * Populated when the user performs undo operations. Cleared whenever a new
     * editing operation is performed (you can't redo after making a new change).
     */
    public Stack<NodeEditPath> redoPath;

    /**
     * Stack of operations that can be undone.
     * <p>
     * Every editing operation pushes a {@link NodeEditPath} onto this stack,
     * allowing the user to revert changes in reverse order.
     */
    public Stack<NodeEditPath> undoPath;

    /**
     * Creates a flow manager for an existing logic flow.
     * <p>
     * The manager starts with the flow disabled and empty undo/redo stacks.
     * 
     * @param flow The logic flow to manage
     */
    public FlowManager(LogicFlow flow) {
        this.flow = flow;
        this.isEnabled = false;
        this.redoPath = new Stack<>();
        this.undoPath = new Stack<>();
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
        this.flow.addNode(startNode);
        this.flow.startNodeId = startNode.getId();
        this.isEnabled = false;
        this.redoPath = new Stack<>();
        this.undoPath = new Stack<>();
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
        this.flow.addNode(node);
        this.undoPath.add(new NodeEditPath(f -> f.addNode(node), f -> f.removeNode(node.getId())));
        this.isEnabled = false;
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
            this.isEnabled = false;
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
        if (oldNode != null) {
            this.flow.addNode(newNode);
            this.flow.startNodeId = newNode.getId();
            this.flow.removeNode(oldNode.getId());
            this.undoPath.add(new NodeEditPath(
                f -> {
                    f.addNode(newNode);
                    f.startNodeId = newNode.getId();
                    f.removeNode(oldNode.getId());
                },
                f -> {
                    f.addNode(oldNode);
                    f.startNodeId = oldNode.getId();
                    f.removeNode(newNode.getId());
                }
            ));
            this.isEnabled = false;
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
            node.name = newName;
            this.undoPath.add(new NodeEditPath(
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.name = newName;
                    }
                }, 
                f -> {
                    FlowNode n = f.getNode(node.getId());
                    if (n != null) {
                        n.name = oldName;
                    }
                }
            ));
            this.isEnabled = false;
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
            this.isEnabled = false;
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
            this.isEnabled = false;
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
            this.isEnabled = false;
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
            long oldNextId = node.nextNodeIds.get(index);
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
            this.isEnabled = false;
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
            long oldNextId = node.nextNodeIds.get(index);
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
            this.isEnabled = false;
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
            this.isEnabled = false;
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
            this.isEnabled = false;
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
    public LogicException execute(@NotNull ServerData serverData, @Nullable List<Object> startNodeOutputs, @Nullable Map<String, Object> initialVariables) {
        ExecutionContext executionContext = new ExecutionContext(this.flow, serverData.server);
        executionContext.execute(Util.serverConfig.getMaxFlowLength(), startNodeOutputs, initialVariables);
        serverData.executeHistory.add(executionContext);
        int historyLimit = Util.serverConfig.getKeepFlowHistoryNumber();
        while (serverData.executeHistory.size() > historyLimit) {
            serverData.executeHistory.remove(0);
        }
        return executionContext.getException();
    }
}
