package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.List;

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
     * All nodes in this logic flow.
     */
    public List<FlowNode> nodes;

    /**
     * The first node to be executed in this logic flow.
     */
    public long startNodeId;

    public LogicFlow(String name) {
        this.idCounter = 0L;
        this.name = name;
        this.nodes = new ArrayList<>();
        this.startNodeId = -1L;
    }

    /**
     * Generate a new unique ID for a node in this logic flow.
     * @return The unique ID
     */
    public long generateId() {
        this.idCounter++;
        return this.idCounter;
    }
}
