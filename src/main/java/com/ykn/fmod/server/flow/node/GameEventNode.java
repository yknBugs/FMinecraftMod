/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.tool.FlowEvents;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

/**
 * The start node of every flow that runs on a game event.
 * <p>
 * One instance exists per event type. The node carries no logic of its own: the dispatcher in
 * {@link com.ykn.fmod.server.flow.tool.FlowEventHooks} writes the event's outputs into the
 * execution context, and {@link EventNode} intentionally does nothing when executed.
 * <p>
 * The node's outputs are the outputs of its {@link FlowEvents.Definition}, in declaration order,
 * and the definition is handed to the constructor rather than looked up from the node's type
 * string. A type string is an identifier in saved flows; it must never be parsed to recover the
 * schema, because a rename or an alias would then resolve to the wrong definition - or to none.
 *
 * Inputs: None
 * Outputs:
 * 1. One port per output that {@link FlowEvents} declares for this node's event type, in that order.
 * Branches: 1 (Next node)
 */
public final class GameEventNode extends EventNode {

    /**
     * Creates an event node for one event declaration.
     *
     * @param id    the unique node id inside its flow
     * @param name  the user-defined node name
     * @param event the event declaration this node starts a flow for; supplies the node type and
     *              the output ports
     */
    public GameEventNode(long id, String name, FlowEvents.Definition event) {
        super(id, name, event.nodeType(), buildMetadata(event));
    }

    /**
     * Builds the metadata of an event node from its event declaration.
     * <p>
     * Must be static: it runs as part of the {@code super(...)} argument list, before this
     * instance's fields exist.
     *
     * @param event the event declaration
     * @return metadata with no inputs, one output per declared event output, and one branch
     */
    private static NodeMetadata buildMetadata(FlowEvents.Definition event) {
        NodeMetadata.Builder builder = NodeMetadata.builder(event.key() + ".name", event.key() + ".desc");
        for (FlowEvents.Output port : event.outputs()) {
            String key = event.key() + ".output." + port.name();
            builder.output(key + ".name", key + ".desc", key + ".type");
        }
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(0, event.outputs().size(), 1);
    }

    /**
     * Registers one node type per flow event, named {@link FlowEvents.Definition#nodeType()}.
     *
     * <p>Called from {@link NodeRegistry#registerDefaultNodes()}. The factory closes over the
     * declaration, so no instance ever has to look its event up by type name.
     */
    public static void register() {
        for (FlowEvents.Definition event : FlowEvents.all()) {
            NodeRegistry.registerEvent(event.nodeType(), (id, name) -> new GameEventNode(id, name, event));
        }
    }
}
