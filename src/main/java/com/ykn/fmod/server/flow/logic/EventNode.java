/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import java.util.List;

/**
 * Base class for all event nodes that serve as entry points for logic flow execution.
 * <p>
 * Event nodes are special nodes that:
 * <ul>
 *   <li>Must always be the starting point of a logic flow</li>
 *   <li>Are automatically triggered by game events (e.g., entity death, player interaction)</li>
 *   <li>Have their outputs populated by the event system rather than computation</li>
 *   <li>Do not perform computation in {@link #onExecute} - their outputs come from event parameters</li>
 * </ul>
 * <p>
 * Event nodes differ from regular nodes in that:
 * <ul>
 *   <li>They return {@code true} from {@link #isEventNode()}</li>
 *   <li>Their output values are set by {@link ExecutionContext} before execution starts</li>
 *   <li>They typically have no inputs (inputs are ignored)</li>
 *   <li>They serve as the bridge between game events and logic flows</li>
 * </ul>
 * <p>
 * Subclasses should:
 * <ul>
 *   <li>Define appropriate outputs in their metadata (e.g., entity, damage amount, location)</li>
 *   <li>Register themselves with {@link com.ykn.fmod.server.flow.tool.NodeRegistry#registerEvent}</li>
 *   <li>Be triggered by the appropriate event listener code</li>
 * </ul>
 * <p>
 * Example subclasses: EntityDeathEventNode, EntityDamageEventNode, ProjectileHitEntityEventNode
 * 
 * @see FlowNode
 * @see ExecutionContext#execute(long, List, java.util.Map)
 * @see com.ykn.fmod.server.flow.tool.NodeRegistry#registerEvent
 */
public class EventNode extends FlowNode {

    /**
     * Creates a new event node with the specified configuration.
     * <p>
     * Event nodes typically have no inputs (inputNumber = 0) since they receive data
     * directly from game events rather than from other nodes.
     * 
     * @param id The unique identifier for this node
     * @param name The user-defined name for this node
     * @param inputNumber The number of input ports (typically 0 for event nodes)
     * @param outputNumber The number of output ports (event parameters)
     * @param branchNumber The number of branch connections (typically 1)
     */
    public EventNode(long id, String name, int inputNumber, int outputNumber, int branchNumber) {
        super(id, name, inputNumber, outputNumber, branchNumber);
        this.type = "AbstractEventNode";
    }

    /**
     * Event nodes do not perform computation during execution.
     * <p>
     * The output values for event nodes are set by {@link ExecutionContext}
     * before the flow begins, using event parameter data. This method intentionally
     * does nothing since the outputs are already populated.
     * 
     * @param context The execution context
     * @param status The node status with pre-populated outputs
     * @param resolvedInputs The resolved inputs (typically empty for event nodes)
     * @throws LogicException Not thrown by event nodes
     */
    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        // Do nothing, because the event node's output is managed by the ExecutionContext
    }

    /**
     * Identifies this node as an event node.
     * <p>
     * Event nodes are special entry point nodes that can be used as flow start nodes
     * and are automatically triggered by game events.
     * 
     * @return Always returns {@code true} for event nodes
     */
    @Override
    public boolean isEventNode() {
        return true;
    }
}
