package com.ykn.fmod.server.flow.tool;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ykn.fmod.server.flow.node.*;

import com.ykn.fmod.server.flow.logic.FlowNode;

/**
 * This class is responsible for registering and creating different types of FlowNode instances.
 */
public class NodeRegistry {

    private static Map<String, NodeFactory> registry = new HashMap<>();

    public interface NodeFactory {
        FlowNode create(long id, String name);
    }

    /**
     * To register a new node type, please use {@code NodeRegistry.register("Node Type", NodeType::new);}
     * Note that the {@code NodeType} must have a constructor exactly with the parameters {@code (long id, String name)}.
     * @param type The type name of the node
     * @param factory The factory to create instances of the node
     */
    public static void register(String type, NodeFactory factory) {
        registry.put(type, factory);
    }

    /**
     * You can create a new node dynamically just by providing its type name, as long as it has been registered.
     * @param type The type name of the node
     * @param id The unique identifier of the node
     * @param name The name of the node
     * @return The created FlowNode instance
     */
    public static FlowNode createNode(String type, long id, String name) {
        return registry.get(type).create(id, name);
    }

    /**
     * Register default nodes here.
     * Example registration: {@code NodeRegistry.register("Example Node", ExampleNode::new);}
     * Or: {@code NodeRegistry.register((new ExampleNode(-2, "Temp Node")).getType(), ExampleNode::new);}
     */
    public static void registerDefaultNodes() {
        NodeRegistry.register("BinaryArithmeticNode", BinaryArithmeticNode::new);
        NodeRegistry.register("BroadcastMessageNode", BroadcastMessageNode::new);
        NodeRegistry.register("GetVariableNode", GetVariableNode::new);
        NodeRegistry.register("IfConditionNode", IfConditionNode::new);
        NodeRegistry.register("SetVariableNode", SetVariableNode::new);
    }

    public static Collection<String> getNodeList() {
        return registry.keySet();
    }
}
