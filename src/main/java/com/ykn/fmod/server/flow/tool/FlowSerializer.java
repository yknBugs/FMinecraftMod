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

import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

/**
 * LogicFlow Serializer
 *
 * JSON format (high level)
 * - Root object:
 *   - "name" : String           -- flow name
 *   - "startNodeId" : long      -- id of the start node
 *   - "nodes" : Array[Object]   -- array of serialized nodes
 *
 * - Node object:
 *   - "id" : long               -- node id
 *   - "type" : String           -- node type (used with NodeRegistry to re-create the node)
 *   - "name" : String           -- node display name
 *   - "inputs" : Array[Object]  -- array of serialized DataReference (length == node.metadata.inputNumber)
 *   - "nextNodes" : Array[long] -- array of next node ids (length == node.metadata.branchNumber)
 *
 * - DataReference object (constant or node output reference):
 *   - if constant:
 *       - "type" : "const"
 *       - "value" : String (the serialized value; booleans and numbers are stored as string representations)
 *   - if node output reference:
 *       - "type" : "reference"
 *       - "id" : long         -- referenced node id
 *       - "index" : int       -- referenced output index
 */
public class FlowSerializer {

    public static DataReference parseConstDataReference(String valueStr) {
        Vec3d valueVec3d = TypeAdaptor.parse(valueStr).asVec3d();
        Vec2f valueVec2f = TypeAdaptor.parse(valueStr).asVec2f();
        Double valueDouble = TypeAdaptor.parse(valueStr).asDouble();
        Boolean boolValue = TypeAdaptor.parse(valueStr).asBoolean();
        if (valueStr == null || "null".equals(valueStr)) {
            return DataReference.createEmptyReference();
        } else if (valueVec3d != null) {
            return DataReference.createConstantReference(valueVec3d);
        } else if (valueVec2f != null) {
            return DataReference.createConstantReference(valueVec2f);
        } else if (valueDouble != null) {
            return DataReference.createConstantReference(valueDouble);
        } else if (boolValue != null) {
            return DataReference.createConstantReference(boolValue);
        } else {
            return DataReference.createConstantReference(valueStr);
        }
    }

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
     * @param flow the LogicFlow to serialize (must not be null)
     * @return a JsonObject representing the serialized LogicFlow
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
     * Deserialize a LogicFlow from its JSON representation.
     *
     * @param json the JsonObject containing the serialized LogicFlow
     * @return a LogicFlow instance populated from the provided JSON
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
     * Serialize a LogicFlow to a JSON string.
     * @param flow the LogicFlow to serialize
     * @return the JSON string representation of the LogicFlow
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
     * Deserialize a LogicFlow from a JSON string.
     * @param jsonString the JSON string representation of the LogicFlow
     * @return the deserialized LogicFlow
     */
    public static LogicFlow deserializeFromString(String jsonString) {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(jsonString, JsonObject.class);
        LogicFlow flow = fromJson(json);
        return flow;
    }

    /**
     * Save a LogicFlow to a file in JSON format.
     * @param flow the LogicFlow to save
     * @param path the file path to save the LogicFlow to
     * @param replace whether to replace the file atomically if it already exists
     * @return true if the save operation was successful, false otherwise
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
     * Load a LogicFlow from a JSON file.
     * @param path the file path to load the LogicFlow from
     * @return the loaded LogicFlow, or null if loading failed
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
