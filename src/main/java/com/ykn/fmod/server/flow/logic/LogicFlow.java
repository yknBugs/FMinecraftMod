/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Represents a complete logic flow consisting of interconnected nodes.
 * <p>
 * A LogicFlow is a directed graph of {@link FlowNode}s that can be executed to perform
 * complex logic sequences. Key features:
 * <ul>
 *   <li>Manages a collection of nodes with unique IDs</li>
 *   <li>Tracks a start node that begins execution</li>
 *   <li>Provides node lookup, sorting, and graph traversal capabilities</li>
 *   <li>Can be serialized, copied, and rendered for display</li>
 * </ul>
 * <p>
 * Nodes in the flow are connected through their branch outputs, forming a potentially
 * cyclic graph. The flow can be executed multiple times simultaneously using separate
 * {@link ExecutionContext} instances.
 * <p>
 * Note: Since flows need to access Minecraft mutable objects, all flows and nodes are designed 
 * to run on the main server thread. And they are expected to finish execution in one tick.
 * Long running tasks should be split into multiple flows and executed by specific nodes.
 * <p>
 * Example usage:
 * <pre>
 * LogicFlow flow = new LogicFlow("MyFlow");
 * FlowNode node1 = new SomeNode(flow.generateId(), "Node1");
 * FlowNode node2 = new SomeNode(flow.generateId(), "Node2");
 * flow.addNode(node1);
 * flow.addNode(node2);
 * node1.setNextNodeId(0, node2.getId());
 * flow.startNodeId = node1.getId();
 * </pre>
 * 
 * @see FlowNode
 * @see ExecutionContext
 */
public class LogicFlow implements Cloneable {

    /**
     * A counter used to generate unique IDs for nodes in this logic flow.
     * Incremented each time {@link #generateId()} is called.
     */
    private long idCounter;

    /**
     * The user-defined name of this logic flow.
     * Can be changed at any time and is used for display and identification.
     */
    private String name;

    /**
     * All nodes in this logic flow, mapped by their unique IDs.
     * Provides O(1) lookup by node ID.
     */
    private Map<Long, FlowNode> nodes;

    /**
     * The ID of the first node to execute when this flow runs.
     * A value of -1 indicates no start node has been set.
     * Event nodes are typically used as start nodes.
     */
    private long startNodeId;

    /**
     * Creates a new logic flow with the specified name.
     * <p>
     * The flow is initialized with:
     * <ul>
     *   <li>An empty node collection</li>
     *   <li>An ID counter starting at 0</li>
     *   <li>No start node (startNodeId = -1)</li>
     * </ul>
     * 
     * @param name The name of this logic flow
     */
    public LogicFlow(String name) {
        this.idCounter = 0L;
        this.name = name;
        this.nodes = new HashMap<>();
        this.startNodeId = -1L;
    }

    /**
     * Gets all nodes in this logic flow.
     * 
     * @return A collection of all FlowNodes in this flow
     */
    public Collection<FlowNode> getNodes() {
        return Collections.unmodifiableCollection(this.nodes.values());
    }

    /**
     * Gets a specific node by its ID.
     * 
     * @param id The unique ID of the node
     * @return The FlowNode with the specified ID, or null if not found
     */
    @Nullable
    public FlowNode getNode(long id) {
        return this.nodes.get(id);
    }

    /**
     * Gets the start node of this flow.
     * 
     * @return The FlowNode designated as the start node, or null if not set
     */
    @Nullable
    public FlowNode getFirstNode() {
        return this.nodes.get(this.startNodeId);
    }

    /**
     * Finds a node by its name.
     * <p>
     * Note: If multiple nodes have the same name, only the first match is returned.
     * 
     * @param name The name to search for
     * @return The first FlowNode with the specified name, or null if not found
     */
    @Nullable
    public FlowNode getNodeByName(String name) {
        for (FlowNode node : this.nodes.values()) {
            if (name.equals(node.name)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns all nodes in a deterministic sorted order suitable for display.
     * <p>
     * The sorting algorithm:
     * <ol>
     *   <li>Starts from the designated start node and performs depth-first traversal</li>
     *   <li>After exhausting reachable nodes, picks remaining nodes with zero in-degree</li>
     *   <li>If cycles exist, picks any remaining unvisited node</li>
     *   <li>All choices use ID-based sorting for determinism</li>
     * </ol>
     * <p>
     * This ordering is primarily used for rendering and display purposes,
     * ensuring consistent output across multiple calls.
     * 
     * @return A list of all nodes in a deterministic display order
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
            for (long neighborId : node.getNextNodeIds()) {
                if (!this.nodes.containsKey(neighborId)) {
                    nodeInDegrees.put(neighborId, nodeInDegrees.getOrDefault(-1, 0) + 1);
                } else {
                    nodeInDegrees.put(neighborId, nodeInDegrees.getOrDefault(neighborId, 0) + 1);
                }
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
                        for (int i = currentNode.getNextNodeIds().size() - 1; i >= 0; i--) {
                            long neighborId = currentNode.getNextNodeIds().get(i);
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

    /**
     * Adds a node to this logic flow.
     * <p>
     * If the node's ID is greater than or equal to the current ID counter,
     * the counter is updated to match the node's ID to prevent future ID collisions.
     * 
     * @param node The FlowNode to add to this flow
     */
    public void addNode(FlowNode node) {
        this.nodes.put(node.getId(), node);
        if (node.getId() >= this.idCounter) {
            this.idCounter = node.getId();
        }
    }

    /**
     * Removes a node from this logic flow.
     * <p>
     * Note: This does not update references to this node in other nodes' nextNodeIds.
     * Callers should ensure dangling references are handled appropriately.
     * 
     * @param id The ID of the node to remove
     */
    public void removeNode(long id) {
        this.nodes.remove(id);
    }

    /**
     * Generates a new unique ID for a node in this flow.
     * <p>
     * The ID counter is incremented each time this method is called.
     * 
     * @return A new unique node ID
     */
    public long generateId() {
        this.idCounter++;
        return this.idCounter;
    }

    /**
     * Creates a deep copy of this logic flow.
     * <p>
     * The copy includes:
     * <ul>
     *   <li>All nodes (each node is copied)</li>
     *   <li>The ID counter state</li>
     *   <li>The start node ID</li>
     *   <li>The flow name</li>
     * </ul>
     * <p>
     * The copy is completely independent - modifying the copy does not affect the original.
     * 
     * @return A new LogicFlow instance with copied nodes and configuration
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
     * Gets the name of this logic flow.
     * 
     * @return The name of this flow
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this logic flow.
     * <p>
     * The name is used for display and identification purposes. It can be changed at any time.
     * 
     * @param name The new name for this flow
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the start node ID for this flow.
     * <p>
     * The start node is the first node that will be executed when this flow runs.
     * It is typically an event node. Setting a start node ID that does not exist in the flow
     * will log a warning but will still set the ID.
     * 
     * @param startNodeId The ID of the node to set as the start node
     */
    public void setStartNodeId(long startNodeId) {
        if (startNodeId > 0 && !this.nodes.containsKey(startNodeId)) {
            Util.LOGGER.warn("FMinecraftMod: Setting startNodeId to " + startNodeId + " which does not exist in the flow " + this.name);
        }
        this.startNodeId = startNodeId;
    }

    /**
     * Gets the start node ID for this flow.
     * <p>
     * The start node is the first node that will be executed when this flow runs.
     * A value of -1 indicates that no start node has been set.
     */
    public long getStartNodeId() {
        return this.startNodeId;
    }

    /**
     * Verifies the integrity of the flow and all its nodes.
     * 
     * Performs comprehensive validation checks including:
     * <ul>
     *   <li>Verifies each node's internal integrity</li>
     *   <li>Validates that all node IDs do not exceed the flow's idCounter</li>
     *   <li>Ensures node IDs match their corresponding keys in the nodes map</li>
     *   <li>Validates all input data references:
     *     <ul>
     *       <li>Referenced nodes exist in the flow</li>
     *       <li>Nodes do not reference themselves</li>
     *       <li>Referenced output indices are within valid bounds</li>
     *     </ul>
     *   </li>
     *   <li>Validates all next node references exist in the flow</li>
     *   <li>Validates that the start node ID exists in the flow if set</li>
     * </ul>
     * 
     * Logs a warning message for each validation failure encountered.
     * 
     * @return {@code true} if all integrity checks pass, {@code false} otherwise
     */
    public boolean verifyIntegrity() {
        boolean passed = true;
        for (Map.Entry<Long, FlowNode> entry : this.nodes.entrySet()) {
            FlowNode node = entry.getValue();
            passed = node.verifyIntegrity() && passed;
            if (node.getId() > this.idCounter) {
                passed = false;
                Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has node " + node.name + " with ID " + node.getId() + " which is greater than the flow's idCounter " + this.idCounter);
            }
            if (node.getId() != entry.getKey()) {
                passed = false;
                Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has node " + node.name + " with ID " + node.getId() + " which does not match its key " + entry.getKey() + " in the flow's nodes map");
            }
            for (DataReference input : node.getInputs()) {
                if (input.getType() == DataReference.ReferenceType.NODE_OUTPUT) {
                    FlowNode referencedNode = this.nodes.get(input.getReferenceId());
                    if (referencedNode == null) {
                        passed = false;
                        Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has node " + node.name + " with input referencing node ID " + input.getReferenceId() + " which does not exist in the flow");
                    } else if (referencedNode == node) {
                        passed = false;
                        Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has node " + node.name + " with input referencing itself with node ID " + input.getReferenceId());
                    } else if (input.getReferenceIndex() < 0 || input.getReferenceIndex() >= referencedNode.getMetadata().outputNumber) {
                        passed = false;
                        Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has node " + node.name + " with input referencing node ID " + input.getReferenceId() + " with invalid output index " + input.getReferenceIndex());
                    }
                }
            }
            for (Long nextNodeId : node.getNextNodeIds()) {
                if (nextNodeId > 0 && !this.nodes.containsKey(nextNodeId)) {
                    passed = false;
                    Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has node " + node.name + " with next node ID " + nextNodeId + " which does not exist in the flow");
                }
            }
        }
        if (this.startNodeId > 0 && !this.nodes.containsKey(this.startNodeId)) {
            passed = false;
            Util.LOGGER.warn("FMinecraftMod: Flow " + this.name + " has startNodeId " + this.startNodeId + " which does not exist in the flow");
        }
        return passed;
    }

    /**
     * Renders the static information about this flow as displayable text.
     * <p>
     * The rendered text includes:
     * <ul>
     *   <li>The flow name</li>
     *   <li>A sequence of all nodes in sorted order (hoverable for details)</li>
     * </ul>
     * <p>
     * Each node in the sequence can be hovered over to see its static configuration
     * (inputs, outputs, and branch connections).
     * <p>
     * Note: This renders static flow structure. To render dynamic execution status,
     * use {@link ExecutionContext#render()} instead.
     * 
     * @return A Text object suitable for display in Minecraft with hover events
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
