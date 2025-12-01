package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * This class represents a custom logic flow that can be executed.
 */
public class LogicFlow implements Cloneable {

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

    public FlowNode getNodeByName(String name) {
        for (FlowNode node : this.nodes.values()) {
            if (name.equals(node.name)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Produces a deterministic, fully-populated ordering of the graph's nodes.
     *
     * @return a deterministic list containing every node in this flow exactly once,
     *         ordered by a DFS-driven traversal seeded by the nodes' IDs.
     */
    public List<FlowNode> getSortedNodes() {
        List<FlowNode> sortedNodes = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Stack<Long> dfsStack = new Stack<>();
        Map<Long, Integer> nodeInDegrees = new HashMap<>();

        // Gather all the nodes and sort them based on their IDs to get deterministic results   
        List<FlowNode> nodeList = this.nodes.values().stream().sorted(Comparator.comparingLong(FlowNode::getId)).toList();

        // Calculate in-degrees of all nodes
        for (FlowNode node : nodeList) {
            nodeInDegrees.putIfAbsent(node.getId(), 0);
            for (long neighborId : node.nextNodeIds) {
                nodeInDegrees.put(neighborId, nodeInDegrees.getOrDefault(neighborId, 0) + 1);
            }
        }

        // Collect nodes with zero in-degrees as starting points
        List<FlowNode> zeroInDegreeNodes = new ArrayList<>();
        for (FlowNode node : nodeList) {
            if (nodeInDegrees.get(node.getId()) == 0) {
                zeroInDegreeNodes.add(node);
            }
        }
        
        FlowNode startNode = this.nodes.get(this.startNodeId);
        while (sortedNodes.size() < this.nodes.size()) {
            // Visit nodes connected from the start node
            if (startNode != null) {
                dfsStack.push(startNode.getId());
                while (!dfsStack.isEmpty()) {
                    long currentNodeId = dfsStack.pop();
                    if (visited.contains(currentNodeId)) {
                        continue;
                    }
                    visited.add(currentNodeId);
                    FlowNode currentNode = this.nodes.get(currentNodeId);
                    if (currentNode != null) {
                        sortedNodes.add(currentNode);
                        for (int i = currentNode.nextNodeIds.size() - 1; i >= 0; i--) {
                            long neighborId = currentNode.nextNodeIds.get(i);
                            if (!visited.contains(neighborId)) {
                                dfsStack.push(neighborId);
                            }
                        }
                    }
                }
            }

            // Choose another unvisited node with zero in-degree as the new start node
            startNode = null;
            for (FlowNode node : zeroInDegreeNodes) {
                if (!visited.contains(node.getId())) {
                    startNode = node;
                    break;
                }
            }

            // If no zero in-degree node is found, pick any unvisited node since it is inside a isolated cycle
            if (startNode == null) {
                for (FlowNode node : nodeList) {
                    if (!visited.contains(node.getId())) {
                        startNode = node;
                        break;
                    }
                }
            }
        }

        return sortedNodes;
    }

    public void addNode(FlowNode node) {
        this.nodes.put(node.getId(), node);
        if (node.getId() >= this.idCounter) {
            this.idCounter = node.getId() + 1;
        }
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
     * Create a copy of this logic flow that has the same structure.
     * @return A new LogicFlow object
     */
    public LogicFlow copy() {
        LogicFlow newFlow = new LogicFlow(this.name);
        newFlow.idCounter = this.idCounter;
        Map<Long, FlowNode> newNodes = new HashMap<>();
        for (FlowNode node : this.nodes.values()) {
            FlowNode newNode = node.copy();
            newNodes.put(newNode.getId(), newNode);
        }
        newFlow.nodes = newNodes;
        newFlow.startNodeId = this.startNodeId;
        return newFlow;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }

    /**
     * Render this logic flow into a text representation for display.
     * It renders the static information about this flow.
     * To render the dynamic status of an execution, use {@link ExecutionContext#render()} instead.
     * @return A text representation of this flow
     */
    public Text render() {
        MutableText title = Text.literal(this.name).append(" ");
        List<FlowNode> nodes = this.getSortedNodes();
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
