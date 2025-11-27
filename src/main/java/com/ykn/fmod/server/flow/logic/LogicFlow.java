package com.ykn.fmod.server.flow.logic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * This class represents a custom logic flow that can be executed.
 */
public class LogicFlow {

    /**
     * A counter to generate unique IDs for nodes in this logic flow.
     */
    private long idCounter;

    /**
     * The name of this logic flow. Can be determined by the user, can be renamed at any time.
     */
    public String name;

    /**
     * All nodes in this logic flow, mapped by their IDs
     */
    private Map<Long, FlowNode> nodes;

    /**
     * The first node to be executed in this logic flow.
     */
    public long startNodeId;

    public LogicFlow(String name) {
        this.idCounter = 0L;
        this.name = name;
        this.nodes = new HashMap<>();
        this.startNodeId = -1L;
    }

    public Collection<FlowNode> getNodes() {
        return this.nodes.values();
    }

    public FlowNode getNode(long id) {
        return this.nodes.get(id);
    }

    public void addNode(FlowNode node) {
        this.nodes.put(node.getId(), node);
    }

    public void removeNode(long id) {
        this.nodes.remove(id);
    }

    /**
     * Generate a new unique ID for a node in this logic flow.
     * @return The unique ID
     */
    public long generateId() {
        this.idCounter++;
        return this.idCounter;
    }

    /**
     * Render this logic flow into a text representation for display.
     * It renders the static information about this flow.
     * To render the dynamic status of an execution, use {@link ExecutionContext#render()} instead.
     * @return A text representation of this flow
     */
    public Text render() {
        MutableText title = Text.literal(this.name).append("\n");
        Collection<FlowNode> nodes = this.getNodes();
        for (FlowNode node : nodes) { 
            Text nodeText = node.render(this);
            MutableText nodeEntry = Text.literal("[").append(node.name).append("] ");
            nodeEntry = nodeEntry.styled(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, nodeText))
            );
            title = title.append(nodeEntry);
        }
        return title;
    }
}
