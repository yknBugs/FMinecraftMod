/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.ModVersion;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ConstCondition;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.core.DummyEvent;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleEvent;

/**
 * Handles JSON serialization, deserialization, and file I/O for {@link CustomRule} objects.
 *
 * <p>The JSON format produced by this serializer matches the rule JSON schema described in the
 * project documentation:
 * <pre>{@code
 * {
 *   "name":             "<rule name>",
 *   "version":          "<minecraft version>",
 *   "mod":              "<mod version>",
 *   "event":            "<event type string>",
 *   "condition":        { ... },
 *   "extra":            [ { ... }, ... ],
 *   "actionIfSatisfied": [ { ... }, ... ],
 *   "actionIfViolated":  [ { ... }, ... ]
 * }
 * }</pre>
 *
 * <p>Version compatibility checks are performed during deserialization:
 * a warning is logged if the Minecraft version or mod version stored in the JSON differs
 * from the currently running version. Rules created with a mod version older than
 * {@link #LAST_COMPATIBLE_MOD_VERSION} also produce a warning.
 *
 * <p>All component objects are reconstructed through {@link RuleRegistry}, so custom types
 * registered after {@link RuleRegistry#registerDefault()} will be handled automatically.
 *
 * @see RuleRegistry
 * @see CustomRule
 */
public class RuleSerializer {

    private static final Gson GSON = buildGson();

    /**
     * The oldest mod version whose rules can be loaded without a compatibility warning.
     * Rules produced by an older mod version will still be attempted, but a warning is logged.
     */
    public static final ModVersion LAST_COMPATIBLE_MOD_VERSION = ModVersion.fromString("0.3.9+b1");

    private static Gson buildGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.disableHtmlEscaping();
        Gson gson = builder.create();
        return gson;
    }

    /**
     * Utility method to safely extract a string value from a JsonObject with a default fallback.
     * <p>
     * This method checks if the key exists and is not null before attempting to retrieve the value.
     * If the key is missing or null, it logs a warning and returns the provided default value.
     * 
     * @param json The JsonObject to extract from
     * @param key The key to look for
     * @param defaultValue The default value to return if the key is missing or null
     * @return The extracted string value, or the default value if not found
     */
    private static String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            Util.LOGGER.warn("FMinecraftMod: Missing or null key '" + key + "' in JSON. Using default value: " + defaultValue);
            return defaultValue;
        }
        return json.get(key).getAsString();
    }

    /**
     * Utility method to safely extract a JsonArray from a JsonObject with an empty array fallback.
     * <p>
     * This method checks if the key exists and is a JsonArray before attempting to retrieve it.
     * If the key is missing, null, or not an array, it logs a warning and returns an empty JsonArray.
     * 
     * @param json The JsonObject to extract from
     * @param key The key to look for
     * @return The extracted JsonArray, or an empty array if not found or invalid
     */
    private static JsonArray getArrayOrEmpty(JsonObject json, String key) {
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            Util.LOGGER.warn("FMinecraftMod: Missing or invalid key '" + key + "' in JSON. Using empty array.");
            return new JsonArray();
        }
        return json.getAsJsonArray(key);
    }

    /**
     * Serializes a {@link CustomRule} to a {@link com.google.gson.JsonObject}.
     *
     * <p>The resulting object includes the current Minecraft and mod versions as
     * {@code "version"} and {@code "mod"} metadata fields for future compatibility checks.
     *
     * @param rule the rule to serialize
     * @return a non-null JSON representation of the rule
     */
    public static JsonObject toJson(CustomRule rule) {
        JsonObject json = new JsonObject();
        json.addProperty("name", rule.getName());
        json.addProperty("version", Util.getMinecraftVersion());
        json.addProperty("mod", Util.MOD_VERSION.toString());
        json.addProperty("event", rule.getEvent().getType());
        json.add("condition", rule.getCondition().toJson());
        JsonArray extraArray = new JsonArray();
        for (RuleCondition extra : rule.getExtra()) {
            extraArray.add(extra.toJson());
        }
        JsonArray actionIfSatisfiedArray = new JsonArray();
        for (RuleAction action : rule.getActionIfSatisfied()) {
            actionIfSatisfiedArray.add(action.toJson());
        }
        JsonArray actionIfViolatedArray = new JsonArray();
        for (RuleAction action : rule.getActionIfViolated()) {
            actionIfViolatedArray.add(action.toJson());
        }
        json.add("extra", extraArray);
        json.add("actionIfSatisfied", actionIfSatisfiedArray);
        json.add("actionIfViolated", actionIfViolatedArray);
        return json;
    }

    /**
     * Deserializes a {@link CustomRule} from a {@link com.google.gson.JsonObject}.
     *
     * <p>Logs warnings if the stored Minecraft version or mod version differs from the
     * current environment. The rule's components are instantiated via {@link RuleRegistry};
     * unrecognised type strings will result in {@code null} entries and potential
     * {@link NullPointerException}s when the rule is later evaluated.
     *
     * @param json the rule JSON object
     * @return the reconstructed {@link CustomRule}
     */
    public static CustomRule fromJson(JsonObject json) {
        String name = getStringOrDefault(json, "name", "Unnamed Rule");
        String version = getStringOrDefault(json, "version", "unknown");
        String mod = getStringOrDefault(json, "mod", "unknown");
        if (!Util.getMinecraftVersion().equals(version)) {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " was created in Minecraft version " + version + ", but the current version is " + Util.getMinecraftVersion() + ". This may cause compatibility issues.");
        }
        ModVersion ruleVersion = null;
        try {
            ruleVersion = ModVersion.fromString(mod);
        } catch (Exception e) {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an invalid mod version string: " + mod + ". This may cause compatibility issues.", e);
        }
        if (ruleVersion != null && ruleVersion.compareTo(LAST_COMPATIBLE_MOD_VERSION) < 0) {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " was created with mod version " + mod + ", but the current version is " + Util.MOD_VERSION.toString() + ". This may cause compatibility issues.");
        }
        if (ruleVersion != null && Util.MOD_VERSION.compareTo(ruleVersion) < 0) {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " was created with a newer mod version " + mod + ", but the current version is " + Util.MOD_VERSION.toString() + ". This may cause compatibility issues.");
        }
        
        String eventType = getStringOrDefault(json, "event", "Dummy");
        RuleEvent event = RuleRegistry.createRuleEvent(eventType);
        if (event == null) {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an unrecognized event type: " + eventType + ". Defaulting to Dummy.");
            event = DummyEvent.getInstance();
        }

        RuleCondition condition = null;
        if (json.has("condition") && json.get("condition").isJsonObject()) {
            condition = RuleRegistry.createCondition(json.getAsJsonObject("condition"));
        } else {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " is missing a valid 'condition' object. Defaulting to false.");
            condition = ConstCondition.of(false);
        }
        if (condition == null) {
            Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an unrecognized condition. Defaulting to false.");
            condition = ConstCondition.of(false);
        }

        List<RuleCondition> extraList = new ArrayList<>();
        JsonArray extraArray = getArrayOrEmpty(json, "extra");
        for (JsonElement el : extraArray) {
            if (!el.isJsonObject()) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an invalid extra condition entry that is not a JSON object. Skipping.");
                continue;
            }
            RuleCondition extraCondition = RuleRegistry.createCondition(el.getAsJsonObject());
            if (extraCondition == null) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an unrecognized extra condition. Skipping.");
                continue;
            }
            if (!RuleCondition.NAME_PATTERN.matcher(extraCondition.getName()).matches()) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an invalid extra condition name: " + extraCondition.getName() + ".");
            }
            extraList.add(extraCondition);
        }

        List<RuleAction> satisfiedActions = new ArrayList<>();
        JsonArray satisfiedArray = getArrayOrEmpty(json, "actionIfSatisfied");
        for (JsonElement el : satisfiedArray) {
            if (!el.isJsonObject()) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an invalid actionIfSatisfied entry that is not a JSON object. Skipping.");
                continue;
            }
            RuleAction ruleAction = RuleRegistry.createRuleAction(el.getAsJsonObject());
            if (ruleAction == null) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an unrecognized actionIfSatisfied. Skipping.");
                continue;
            }
            satisfiedActions.add(ruleAction);
        }

        List<RuleAction> violatedActions = new ArrayList<>();
        JsonArray violatedArray = getArrayOrEmpty(json, "actionIfViolated");
        for (JsonElement el : violatedArray) {
            if (!el.isJsonObject()) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an invalid actionIfViolated entry that is not a JSON object. Skipping.");
                continue;
            }
            RuleAction ruleAction = RuleRegistry.createRuleAction(el.getAsJsonObject());
            if (ruleAction == null) {
                Util.LOGGER.warn("FMinecraftMod: The rule " + name + " has an unrecognized actionIfViolated. Skipping.");
                continue;
            }
            violatedActions.add(ruleAction);
        }

        CustomRule rule = CustomRule.build(name, event, condition, satisfiedActions, violatedActions);
        for (RuleCondition extra : extraList) {
            rule.addCondition(extra);
        }
        return rule;
    }

    /**
     * Serializes a rule to its pretty-printed JSON string representation.
     *
     * @param rule the rule to serialize
     * @return a non-null JSON string
     */
    public static String serializeToString(CustomRule rule) {
        return GSON.toJson(toJson(rule));
    }

    /**
     * Deserializes a rule from its JSON string representation.
     *
     * @param json a JSON string previously produced by {@link #serializeToString(CustomRule)}
     * @return the reconstructed {@link CustomRule}
     */
    public static CustomRule deserializeFromString(String json) {
        return fromJson(GSON.fromJson(json, JsonObject.class));
    }

    /**
     * Writes a rule to a JSON file.
     *
     * @param rule    the rule to persist
     * @param path    the target file path
     * @param replace if {@code false} and the file already exists, the write is skipped and
     *                {@code false} is returned
     * @return {@code true} if the file was written successfully
     */
    public static boolean saveFile(CustomRule rule, Path path, boolean replace) {
        JsonObject json = toJson(rule);
        return Util.saveFile(writer -> GSON.toJson(json, writer), path, replace);
    }

    /**
     * Reads and deserializes a rule from a JSON file.
     *
     * @param path the path to the rule JSON file
     * @return the deserialized {@link CustomRule}, or {@code null} if the file does not exist
     *         or an I/O or parse error occurs
     */
    public static CustomRule loadFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return fromJson(json);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Could not read the rule from file " + path.toString(), e);
            return null;
        }
    }
}