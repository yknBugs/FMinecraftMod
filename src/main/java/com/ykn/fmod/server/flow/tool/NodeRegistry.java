/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.ykn.fmod.server.flow.node.*;
import com.ykn.fmod.server.flow.logic.FlowNode;

/**
 * Central registry for all node types in the logic flow system.
 * <p>
 * The NodeRegistry maintains two categories of nodes:
 * <ul>
 *   <li><b>Regular nodes:</b> Standard logic nodes that process data and control flow</li>
 *   <li><b>Event nodes:</b> Special nodes that serve as entry points, triggered by game events</li>
 * </ul>
 * <p>
 * Node types are registered with a string identifier and a factory function.
 * This allows dynamic node creation by type name, which is essential for:
 * <ul>
 *   <li>Serialization/deserialization of flows</li>
 *   <li>Node copying and cloning</li>
 *   <li>User-facing node creation interfaces</li>
 * </ul>
 * <p>
 * All nodes must be registered during mod initialization using {@link #registerDefaultNodes()}.
 * <p>
 * Example registration:
 * <pre>
 * NodeRegistry.register("BinaryArithmeticNode", BinaryArithmeticNode::new);
 * NodeRegistry.registerEvent("EntityDeathEventNode", EntityDeathEventNode::new);
 * </pre>
 * 
 * @see FlowNode
 * @see NodeFactory
 */
public class NodeRegistry {

    /**
     * Map of regular node type names to their factory functions.
     */
    private static final Map<String, NodeFactory> nodes = new HashMap<>();

    /**
     * Map of event node type names to their factory functions.
     * Event nodes are kept separate for easier categorization and querying.
     */
    private static final Map<String, NodeFactory> eventNodes = new HashMap<>();

    /**
     * Factory interface for creating node instances.
     * <p>
     * Implementations should construct a node of the appropriate type with the given ID and name.
     * The factory is typically a method reference to the node's constructor.
     */
    public interface NodeFactory {
        /**
         * Creates a new node instance.
         * 
         * @param id The unique ID for the node
         * @param name The user-defined name for the node
         * @return A new FlowNode instance
         */
        FlowNode create(long id, String name);
    }

    /**
     * Registers a new regular node type.
     * <p>
     * The node class must have a constructor with the exact signature: {@code (long id, String name)}.
     * <p>
     * Example:
     * <pre>
     * NodeRegistry.register("BinaryArithmeticNode", BinaryArithmeticNode::new);
     * </pre>
     * 
     * @param type The unique type name for this node (used in serialization and dynamic creation)
     * @param factory The factory function to create instances of this node type
     */
    public static void register(String type, NodeFactory factory) {
        nodes.put(type, factory);
    }

    /**
     * Registers a new event node type.
     * <p>
     * Event nodes are special nodes that serve as entry points for flow execution,
     * typically triggered by game events like entity death or player actions.
     * <p>
     * The node class must have a constructor with the exact signature: {@code (long id, String name)}.
     * <p>
     * Example:
     * <pre>
     * NodeRegistry.registerEvent("EntityDeathEventNode", EntityDeathEventNode::new);
     * </pre>
     * 
     * @param type The unique type name for this event node
     * @param factory The factory function to create instances of this event node type
     */
    public static void registerEvent(String type, NodeFactory factory) {
        eventNodes.put(type, factory);
    }

    /**
     * Creates a new node instance dynamically by type name.
     * <p>
     * This method searches both regular and event node registries.
     * It's used for:
     * <ul>
     *   <li>Deserializing flows from JSON or other formats</li>
     *   <li>Copying nodes while preserving their specific subclass type</li>
     *   <li>User-initiated node creation in editors or UIs</li>
     * </ul>
     * 
     * @param type The registered type name of the node
     * @param id The unique ID to assign to the new node
     * @param name The user-defined name for the new node
     * @return A new FlowNode instance of the specified type, or null if the type is not registered
     */
    @Nullable
    public static FlowNode createNode(String type, long id, String name) {
        NodeFactory factory = nodes.get(type);
        if (factory == null) {
            factory = eventNodes.get(type);
        }
        if (factory == null) {
            return null;
        } else {
            return factory.create(id, name);
        }
    }

    /**
     * Registers all default node types that come with the mod.
     * <p>
     * This method should be called during mod initialization to make all built-in
     * node types available for use. It registers:
     * <ul>
     *   <li>Logic nodes (BinaryArithmeticNode, IfConditionNode, etc.)</li>
     *   <li>Variable nodes (GetVariableNode, SetVariableNode)</li>
     *   <li>Action nodes (BroadcastMessageNode, RunFlowNode)</li>
     *   <li>Event nodes (EntityDeathEventNode, EntityDamageEventNode, etc.)</li>
     * </ul>
     * <p>
     * Custom mods can register additional node types after this method is called.
     */
    public static void registerDefaultNodes() {
        NodeRegistry.register("AddVariableToScheduledFlowNode", AddVariableToScheduledFlowNode::new);
        NodeRegistry.register("BinaryArithmeticNode", BinaryArithmeticNode::new);
        NodeRegistry.register("BroadcastMessageNode", BroadcastMessageNode::new);
        NodeRegistry.register("CancelScheduledTaskNode", CancelScheduledTaskNode::new);
        NodeRegistry.register("ExecuteCommandNode", ExecuteCommandNode::new);
        NodeRegistry.register("GatherEntityNode", GatherEntityNode::new);
        NodeRegistry.register("GetBlockNode", GetBlockNode::new);
        NodeRegistry.register("GetEntityDataNode", GetEntityDataNode::new);
        NodeRegistry.register("GetNbtValueNode", GetNbtValueNode::new);
        NodeRegistry.register("GetObjectAtIndexNode", GetObjectAtIndexNode::new);
        NodeRegistry.register("GetScheduledFlowNode", GetScheduledFlowNode::new);
        NodeRegistry.register("GetVariableNode", GetVariableNode::new);
        NodeRegistry.register("GetWorldListNode", GetWorldListNode::new);
        NodeRegistry.register("IfConditionNode", IfConditionNode::new);
        NodeRegistry.register("SetObjectAtIndexNode", SetObjectAtIndexNode::new);
        NodeRegistry.register("SetVariableNode", SetVariableNode::new);
        NodeRegistry.register("RunFlowNode", RunFlowNode::new);
        NodeRegistry.register("UnaryArithmeticNode", UnaryArithmeticNode::new);

        NodeRegistry.registerEvent("DummyNode", DummyNode::new);
        NodeRegistry.registerEvent("EntityDamageEventNode", EntityDamageEventNode::new);
        NodeRegistry.registerEvent("EntityDeathEventNode", EntityDeathEventNode::new);
        NodeRegistry.registerEvent("ProjectileHitEntityEventNode", ProjectileHitEntityEventNode::new);
        NodeRegistry.registerEvent("TriggerNode", TriggerNode::new);
    }

    /**
     * Gets the list of all registered regular node type names.
     * <p>
     * This can be used to:
     * <ul>
     *   <li>Display available node types in a UI</li>
     *   <li>Validate node type names during deserialization</li>
     *   <li>Generate documentation or help text</li>
     * </ul>
     * 
     * @return A collection of all registered regular node type names
     */
    public static Collection<String> getNodeList() {
        return List.copyOf(nodes.keySet());
    }

    /**
     * Gets the list of all registered event node type names.
     * <p>
     * Event nodes are kept in a separate list because they are typically
     * used as flow entry points and may be presented differently in UIs.
     * 
     * @return A collection of all registered event node type names
     */
    public static Collection<String> getEventNodeList() {
        return List.copyOf(eventNodes.keySet());
    }
}
