/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;

/**
 * A {@link RuleCondition} that always evaluates to a fixed boolean constant.
 *
 * <p>{@code ConstCondition} serves several purposes:
 * <ul>
 *   <li><b>Default placeholder</b> – {@link CustomRule#build(String)} initialises
 *       the rule condition to {@code ConstCondition.of(false)} so that a newly
 *       created rule does nothing until a real condition is configured.</li>
 *   <li><b>Formula literals</b> – the formula parser emits {@code ConstCondition} nodes
 *       for the {@code true} and {@code false} keywords.</li>
 *   <li><b>Optimisation result</b> – {@link ConditionRelationship#TRUE}/{@link ConditionRelationship#FALSE}
 *       sentinels are folded into {@code ConstCondition} by
 *       {@link BinaryConditionExpression#optimize(CustomRule)}.</li>
 * </ul>
 *
 * <p>This class is immutable; {@link #setName(String)} returns a new instance.
 */
public class ConstCondition implements RuleCondition {

    private final String name;

    private final boolean value;

    public static final String TYPE = "ConstCondition";

    private ConstCondition(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a {@code ConstCondition} whose name is the string representation of {@code value}.
     *
     * @param value the constant boolean value
     * @return a new {@code ConstCondition}
     */
    public static ConstCondition of(boolean value) {
        return new ConstCondition(String.valueOf(value), value);
    }

    /**
     * Creates a named {@code ConstCondition}.
     *
     * @param name  the instance name
     * @param value the constant boolean value
     * @return a new {@code ConstCondition}
     */
    public static ConstCondition of(String name, boolean value) {
        return new ConstCondition(name, value);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        return value;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new ConstCondition(name, value);
    }

    @Override
    public String getType() {
        return TYPE;
    }
    
    /**
     * Returns the constant value held by this condition.
     *
     * @return {@code true} or {@code false}
     */
    public boolean getValue() {
        return value;
    }

    @Override
    public Component render() {
        if (name.isEmpty()) {
            return Component.literal(String.valueOf(value));
        } else {
            return Component.literal(name + ": " + String.valueOf(value));
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", getName());
        json.addProperty("type", getType());
        json.addProperty("value", value);
        return json;
    }

    public static JsonObject toJson(ConstCondition condition) {
        return condition.toJson();
    }

    /**
     * Deserializes a {@code ConstCondition} from its JSON representation.
     *
     * <p>The JSON must contain a boolean {@code "value"} field. An optional {@code "name"}
     * field may be present; if omitted the name defaults to the string form of the value.
     *
     * @param json the JSON object to deserialise
     * @return a new {@code ConstCondition}
     * @throws IllegalArgumentException if the required {@code "value"} field is absent
     */
    public static ConstCondition fromJson(JsonObject json) {
        if (json.has("value") && json.get("value").isJsonPrimitive() && json.get("value").getAsJsonPrimitive().isBoolean()) {
            boolean value = json.get("value").getAsBoolean();
            String name = json.has("name") ? json.get("name").getAsString() : String.valueOf(value);
            return ConstCondition.of(name, value);
        } else {
            Util.LOGGER.warn("FMinecraftMod: Invalid ConstCondition JSON: " + json.toString() + ". 'value' field is missing or not a boolean. Defaulting to false.");
            return ConstCondition.of(false);
        }
    }
}
