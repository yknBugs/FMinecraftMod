/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicFlow;

/**
 * Utility class for serializing and deserializing logic flows to/from JSON.
 * <p>
 * FlowSerializer provides comprehensive functionality for:
 * <ul>
 *   <li>Converting LogicFlow objects to JSON format</li>
 *   <li>Reconstructing LogicFlow objects from JSON</li>
 *   <li>Saving flows to files with atomic replacement support</li>
 *   <li>Loading flows from files with error handling</li>
 * </ul>
 * <p>
 * <b>JSON Format Specification:</b>
 * <p>
 * <b>Root Object:</b>
 * <pre>
 * {
 *   "name": String,           // Flow name
 *   "version": String,        // Minecraft version
 *   "mod": String,            // Mod version
 *   "startNodeId": long,      // ID of the start node
 *   "nodes": Array[Object]    // Array of serialized nodes
 * }
 * </pre>
 * <p>
 * <b>Node Object:</b>
 * <pre>
 * {
 *   "id": long,               // Unique node ID
 *   "type": String,           // Node type (for NodeRegistry)
 *   "name": String,           // Display name
 *   "inputs": Array[Object],  // Array of DataReference objects
 *   "nextNodes": Array[long]  // Array of next node IDs
 * }
 * </pre>
 * <p>
 * <b>DataReference Object:</b>
 * <p>
 * For constant values:
 * <pre>
 * {
 *   "type": "const",
 *   "value": String           // String representation of value
 * }
 * </pre>
 * <p>
 * For node output references:
 * <pre>
 * {
 *   "type": "reference",
 *   "id": long,               // Referenced node ID
 *   "index": int              // Referenced output index
 * }
 * </pre>
 * <p>
 * The serializer supports various value types including Vec3d, Vec2f, Double,
 * Boolean, and String. Values are automatically parsed during deserialization.
 * <p>
 * File operations use atomic writes (via temporary files) when replacing existing
 * files to prevent data corruption if the operation is interrupted.
 * 
 * @see LogicFlow
 * @see FlowNode
 * @see DataReference
 * @see NodeRegistry
 */
public class FlowSerializer {

    /**
     * Parses a string representation of a constant value into a DataReference.
     * <p>
     * This method attempts to parse the string as various types in order:
     * <ol>
     *   <li>null (if string is "null" or null)</li>
     *   <li>Vec3d (3D vector)</li>
     *   <li>Vec2f (2D float vector)</li>
     *   <li>List (comma-separated values in square brackets)</li>
     *   <li>Double (numeric value)</li>
     *   <li>Boolean (true/false)</li>
     *   <li>String (fallback)</li>
     * </ol>
     * 
     * @param valueStr The string representation of the value
     * @return A DataReference containing the parsed constant value
     */
    public static DataReference parseConstDataReference(String valueStr) {
        if (valueStr == null || "null".equals(valueStr)) {
            return DataReference.createEmptyReference();
        } else {
            return DataReference.createConstantReference(TypeAdaptor.parse(valueStr).autoCast());
        }
    }

    /**
     * Serializes a DataReference into a JSON object.
     * <p>
     * The JSON format depends on the reference type:
     * <ul>
     *   <li>CONSTANT: {"type": "const", "value": String}</li>
     *   <li>NODE_OUTPUT: {"type": "reference", "id": long, "index": int}</li>
     * </ul>
     * 
     * @param ref The DataReference to serialize
     * @return A JsonObject representing the reference
     */
    private static JsonObject serializeDataReference(DataReference ref) {
        JsonObject json = new JsonObject();
        switch (ref.type) {
            case CONSTANT:
                json.addProperty("type", "const");
                json.addProperty("value", String.valueOf(ref.value));
                break;
            case NODE_OUTPUT:
                json.addProperty("type", "reference");
                json.addProperty("id", ref.referenceId);
                json.addProperty("index", ref.referenceIndex);
                break;
            default:
                json.addProperty("type", "const");
                json.addProperty("value", "null");
                break;
        }
        return json;
    }

    /**
     * Deserializes a DataReference from a JSON object.
     * <p>
     * Reconstructs the appropriate reference type based on the "type" field.
     * 
     * @param json The JsonObject containing the serialized reference
     * @return A DataReference reconstructed from the JSON
     */
    private static DataReference deserializeDataReference(JsonObject json) {
        String type = json.get("type").getAsString();
        if ("const".equals(type)) {
            String valueStr = json.get("value").getAsString();
            return parseConstDataReference(valueStr);
        } else if ("reference".equals(type)) {
            long id = json.get("id").getAsLong();
            int index = json.get("index").getAsInt();
            return DataReference.createNodeOutputReference(id, index);
        } else {
            return DataReference.createEmptyReference();
        }
    }

    /**
     * Serializes a FlowNode into a JSON object.
     * <p>
     * The JSON includes:
     * <ul>
     *   <li>Node ID, type, and name</li>
     *   <li>All input references as an array</li>
     *   <li>All next node IDs as an array</li>
     * </ul>
     * 
     * @param node The FlowNode to serialize
     * @return A JsonObject representing the node
     */
    private static JsonObject serializeNode(FlowNode node) {
        JsonObject json = new JsonObject();
        json.addProperty("id", node.getId());
        json.addProperty("type", node.getType());
        json.addProperty("name", node.name);
        JsonArray inputsArray = new JsonArray();
        for (int i = 0; i < node.getMetadata().inputNumber; i++) {
            DataReference ref = node.getInput(i);
            JsonObject refJson = serializeDataReference(ref);
            inputsArray.add(refJson);
        }
        json.add("inputs", inputsArray);
        JsonArray nextNodeArray = new JsonArray();
        for (int i = 0; i < node.getMetadata().branchNumber; i++) {
            long nextNodeId = node.nextNodeIds.get(i);
            nextNodeArray.add(nextNodeId);
        }
        json.add("nextNodes", nextNodeArray);
        return json;
    }

    /**
     * Deserializes a FlowNode from a JSON object.
     * <p>
     * This method:
     * <ol>
     *   <li>Extracts the node type and uses NodeRegistry to create the node</li>
     *   <li>Deserializes and sets all input references</li>
     *   <li>Deserializes and sets all next node IDs</li>
     * </ol>
     * 
     * @param json The JsonObject containing the serialized node
     * @return A FlowNode reconstructed from the JSON
     */
    private static FlowNode deserializeNode(JsonObject json) {
        long id = json.get("id").getAsLong();
        String type = json.get("type").getAsString();
        String name = json.get("name").getAsString();
        FlowNode node = NodeRegistry.createNode(type, id, name);
        JsonArray inputsArray = json.getAsJsonArray("inputs");
        for (int i = 0; i < node.getMetadata().inputNumber; i++) {
            JsonObject refJson = inputsArray.get(i).getAsJsonObject();
            DataReference ref = deserializeDataReference(refJson);
            node.setInput(i, ref);
        }
        JsonArray nextNodeArray = json.getAsJsonArray("nextNodes");
        for (int i = 0; i < node.getMetadata().branchNumber; i++) {
            long nextNodeId = nextNodeArray.get(i).getAsLong();
            node.setNextNodeId(i, nextNodeId);
        }
        return node;
    }

    /**
     * Serializes a LogicFlow into a JsonObject.
     * <p>
     * The resulting JSON includes:
     * <ul>
     *   <li>Flow name and start node ID</li>
     *   <li>Version information (Minecraft and mod versions)</li>
     *   <li>All nodes in sorted order with their full configuration</li>
     * </ul>
     * 
     * @param flow The LogicFlow to serialize (must not be null)
     * @return A JsonObject representing the complete serialized LogicFlow
     */
    public static JsonObject toJson(LogicFlow flow) {
        JsonObject json = new JsonObject();
        json.addProperty("name", flow.name);
        json.addProperty("version", Util.getMinecraftVersion());
        json.addProperty("mod", Util.getModVersion());
        json.addProperty("startNodeId", flow.startNodeId);
        JsonArray nodesArray = new JsonArray();
        List<FlowNode> nodes = flow.getSortedNodes();
        for (FlowNode node : nodes) {
            JsonObject nodeJson = serializeNode(node);
            nodesArray.add(nodeJson);
        }
        json.add("nodes", nodesArray);
        return json;
    }

    /**
     * Deserializes a LogicFlow from its JSON representation.
     * <p>
     * This method reconstructs the complete flow structure including:
     * <ul>
     *   <li>Flow name and start node</li>
     *   <li>All nodes with their types, connections, and configurations</li>
     *   <li>All data references between nodes</li>
     * </ul>
     * <p>
     * Node types must be registered in NodeRegistry or deserialization will fail.
     * 
     * @param json The JsonObject containing the serialized LogicFlow
     * @return A LogicFlow instance fully reconstructed from the JSON
     */
    public static LogicFlow fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        LogicFlow flow = new LogicFlow(name);
        flow.startNodeId = json.get("startNodeId").getAsLong();
        JsonArray nodesArray = json.getAsJsonArray("nodes");
        for (int i = 0; i < nodesArray.size(); i++) {
            JsonObject nodeJson = nodesArray.get(i).getAsJsonObject();
            FlowNode node = deserializeNode(nodeJson);
            flow.addNode(node);
        }
        return flow;
    }

    /**
     * Serializes a LogicFlow to a pretty-printed JSON string.
     * <p>
     * The output is formatted with indentation for human readability.
     * This is useful for debugging or displaying flows in text format.
     * 
     * @param flow The LogicFlow to serialize
     * @return A formatted JSON string representation of the LogicFlow
     */
    public static String serializeToString(LogicFlow flow) {
        JsonObject json = toJson(flow);
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson gson = builder.create();
        String jsonString = gson.toJson(json);
        return jsonString;
    }

    /**
     * Deserializes a LogicFlow from a JSON string.
     * <p>
     * This is the inverse of {@link #serializeToString(LogicFlow)}.
     * Node types must be registered in NodeRegistry.
     * 
     * @param jsonString The JSON string representation of the LogicFlow
     * @return The deserialized LogicFlow
     * @throws com.google.gson.JsonSyntaxException If the JSON is malformed
     */
    public static LogicFlow deserializeFromString(String jsonString) {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(jsonString, JsonObject.class);
        LogicFlow flow = fromJson(json);
        return flow;
    }

    /**
     * Saves a LogicFlow to a file in pretty-printed JSON format.
     * <p>
     * This method provides two modes:
     * <ul>
     *   <li><b>Atomic replacement (replace=true):</b> Writes to a temporary file first,
     *       then atomically moves it to the target path. This prevents data corruption
     *       if the write is interrupted. Will overwrite existing files.</li>
     *   <li><b>Safe creation (replace=false):</b> Creates the file only if it doesn't
     *       exist. Returns false if the file already exists.</li>
     * </ul>
     * <p>
     * Parent directories are created automatically if they don't exist.
     * Errors are logged but also returned as boolean status.
     * 
     * @param flow The LogicFlow to save
     * @param path The file path to save the LogicFlow to
     * @param replace Whether to replace the file atomically if it already exists
     * @return {@code true} if the save operation was successful, {@code false} otherwise
     */
    public static boolean saveFile(LogicFlow flow, Path path, boolean replace) {
        JsonObject json = toJson(flow);
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        Gson gson = builder.create();
        if (replace) {
            Path dir = path.getParent();
            Path tmp = dir.resolve(path.getFileName() + ".tmp");
            try {
                Files.createDirectories(dir);
                try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                    gson.toJson(json, writer);
                    writer.flush();
                } catch (Exception e) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not write the logic flow " + flow.name + " to temporary file " + tmp.toString(), e);
                    return false;
                }
                Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception e) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not move temporary file to " + path.toString(), e);
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception ex) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not delete temporary file " + tmp.toString(), ex);
                }
                return false;
            }
        }
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Cannot overwrite existing file " + path.toString());
                return false;
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                gson.toJson(json, writer);
            } catch (Exception e) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not write the logic flow " + flow.name + " to file " + path.toString(), e);
                return false;
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not create the target directory " + path.getParent().toString(), e);
            return false;
        }
        return true;
    }

    /**
     * Loads a LogicFlow from a JSON file.
     * <p>
     * This method:
     * <ol>
     *   <li>Checks if the path is a valid regular file</li>
     *   <li>Reads and parses the JSON content</li>
     *   <li>Deserializes the flow using {@link #fromJson(JsonObject)}</li>
     * </ol>
     * <p>
     * Returns null if the file doesn't exist, isn't a regular file, or if
     * deserialization fails. Errors are logged.
     * 
     * @param path The file path to load the LogicFlow from
     * @return The loaded LogicFlow, or {@code null} if loading failed
     */
    public static LogicFlow loadFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            LogicFlow flow = fromJson(json);
            return flow;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not read the logic flow from file " + path.toString(), e);
            return null;
        }
    }
}
