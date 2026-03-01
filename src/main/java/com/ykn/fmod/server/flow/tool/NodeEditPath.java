/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.function.Consumer;

import com.ykn.fmod.server.flow.logic.LogicFlow;

/**
 * Represents a reversible editing operation on a logic flow.
 * <p>
 * NodeEditPath encapsulates both the forward (redo) and reverse (undo) logic
 * for a single editing operation. This enables full undo/redo functionality
 * in {@link FlowManager}.
 * <p>
 * Each edit path stores two lambda functions:
 * <ul>
 *   <li><b>Redo logic:</b> Applies the edit operation to a flow</li>
 *   <li><b>Undo logic:</b> Reverses the edit operation on a flow</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * NodeEditPath edit = new NodeEditPath(
 *     flow -> flow.addNode(newNode),      // Redo: add the node
 *     flow -> flow.removeNode(nodeId)     // Undo: remove the node
 * );
 * edit.redo(flow);  // Applies the operation
 * edit.undo(flow);  // Reverses the operation
 * </pre>
 * <p>
 * This design pattern (Command pattern) allows complex editing operations
 * to be easily made reversible by capturing both the operation and its inverse.
 * 
 * @see FlowManager
 * @see LogicFlow
 */
public class NodeEditPath {

    /**
     * The lambda function that applies this edit operation to a flow.
     * <p>
     * This function is called when redoing the operation after it was undone.
     */
    private final Consumer<LogicFlow> redoLogic;

    /**
     * The lambda function that reverses this edit operation on a flow.
     * <p>
     * This function is called when undoing the operation, restoring the
     * flow to its previous state.
     */
    private final Consumer<LogicFlow> undoLogic;

    /**
     * Creates a new reversible edit operation.
     * 
     * @param redo The lambda function that applies this edit to a flow
     * @param undo The lambda function that reverses this edit on a flow
     */
    public NodeEditPath(Consumer<LogicFlow> redo, Consumer<LogicFlow> undo) {
        this.redoLogic = redo;
        this.undoLogic = undo;
    }

    /**
     * Applies this edit operation to the specified flow.
     * <p>
     * This executes the redo logic, which performs the forward operation.
     * 
     * @param flow The logic flow to modify
     */
    public void redo(LogicFlow flow) {
        this.redoLogic.accept(flow);
    }

    /**
     * Reverses this edit operation on the specified flow.
     * <p>
     * This executes the undo logic, which restores the flow to its state
     * before this operation was applied.
     * 
     * @param flow The logic flow to modify
     */
    public void undo(LogicFlow flow) {
        this.undoLogic.accept(flow);
    }

}
