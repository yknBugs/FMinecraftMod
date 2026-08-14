/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;

/**
 * A lazy reference to a named condition declared in a rule's {@code extra} list.
 *
 * <p>Rule authors write boolean formulas such as {@code "inZone && !isOp"}. The identifiers
 * in the formula ({@code inZone}, {@code isOp}) are initially parsed into
 * {@code ConditionReference} nodes by {@link com.ykn.fmod.server.rule.tool.ConditionFormulaParser}.
 * Each reference is resolved at evaluation time by looking up its
 * {@linkplain #getReferenceName() reference name} in the rule's
 * {@link CustomRule#getExtra() extra} condition list.
 *
 * <p>During {@link CustomRule#optimize()}, unresolvable references are left as-is, while
 * resolvable ones are inlined ({@link #optimize(CustomRule)}) to avoid repeated list
 * traversals at evaluation time.
 *
 * <p>JSON representation uses a {@code "require"} field (not {@code "value"}) to store the name:
 * <pre>{@code
 * {"name": "check", "type": "ConditionReference", "require": "inZone"}
 * }</pre>
 *
 * <p>This class is immutable; {@link #setName(String)} returns a new instance.
 *
 * @see CustomRule#optimize()
 * @see com.ykn.fmod.server.rule.tool.ConditionFormulaParser
 */
public class ConditionReference implements RuleCondition {

    // Useful when you want to write boolean expression first and add conditions later

    private final String name;

    private final String referenceName;

    public static final String TYPE = "ConditionReference";

    private ConditionReference(String name, String referenceName) {
        this.name = name;
        this.referenceName = referenceName;
    }

    /**
     * Creates a named {@code ConditionReference}.
     *
     * @param name          the instance name
     * @param referenceName the name of the condition to look up in the rule's {@code extra} list
     * @return a new {@code ConditionReference}
     */
    public static ConditionReference of(String name, String referenceName) {
        return new ConditionReference(name, referenceName);
    }

    /**
     * Creates an anonymous {@code ConditionReference} (empty name).
     *
     * @param referenceName the name of the condition to look up
     * @return a new {@code ConditionReference}
     */
    public static ConditionReference need(String referenceName) {
        return new ConditionReference("", referenceName);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        CustomRule rule = context.getRule();
        if (referenceName.isEmpty()) {
            context.setErrorMessage(Util.parseTranslatableText("fmod.rule.error.condition.empty", rule.getName(), this.getName()));
            return false;
        }
        for (RuleCondition condition : rule.getExtra()) {
            if (condition.getName().equals(referenceName)) {
                return condition.evaluate(context);
            }
        }
        context.setErrorMessage(Util.parseTranslatableText("fmod.rule.error.condition.notexist", rule.getName(), this.getName(), referenceName));
        return false;
    }
    
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new ConditionReference(name, referenceName);
    }

    public String getReferenceName() {
        return referenceName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Nullable
    public RuleCondition collapse(@Nonnull CustomRule rule) {
        for (RuleCondition condition : rule.getExtra()) {
            if (condition.getName().equals(referenceName)) {
                return condition;
            }
        }
        return null;
    }

    /**
     * Recursively resolves chained {@code ConditionReference} nodes until a non-reference
     * condition is reached, or returns {@code null} if any link in the chain is unresolvable.
     *
     * @param rule the rule used to resolve each reference
     * @return the first non-reference condition in the chain, or {@code null}
     */
    @Nullable
    public RuleCondition collapseToLeaf(@Nonnull CustomRule rule) {
        RuleCondition condition = collapse(rule);
        if (condition == null) {
            return null;
        }
        HashSet<RuleCondition> visitedNode = new HashSet<>();
        visitedNode.add(this);
        while (condition instanceof ConditionReference) {
            ConditionReference reference = (ConditionReference) condition;
            if (visitedNode.contains(reference)) {
                // Circular reference detected; bail out to prevent infinite recursion.
                return null;
            }
            visitedNode.add(reference);
            condition = reference.collapse(rule);
            if (condition == null) {
                return null;
            }
        }
        return condition;
    }

    @Override
    public RuleCondition onOptimize(CustomRule rule, HashSet<RuleCondition> optimizingConditions) {
        RuleCondition resolved = collapseToLeaf(rule);
        if (resolved != null) {
            return resolved;
        }
        return this;
    }

    @Override
    public Component render() {
        if (name.isEmpty()) {
            return Component.literal(referenceName);
        } else {
            return Component.literal(name + ": " + referenceName);
        }
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", getName());
        json.addProperty("type", getType());
        json.addProperty("value", referenceName);
        return json;
    }

    public static JsonObject toJson(ConditionReference condition) {
        return condition.toJson();
    }

    /**
     * Deserializes a {@code ConditionReference} from its JSON representation.
     *
     * <p>The JSON must contain a {@code "value"} string field. An optional {@code "name"}
     * field may be present; if omitted the name defaults to empty string.
     *
     * @param json the JSON object to deserialise
     * @return a new {@code ConditionReference}
     */
    public static ConditionReference fromJson(JsonObject json) {
        if (json.has("value") && json.get("value").isJsonPrimitive()) {
            String referenceName = json.get("value").getAsString();
            String name = json.has("name") ? json.get("name").getAsString() : "";
            return ConditionReference.of(name, referenceName);
        } else {
            Util.LOGGER.warn("FMinecraftMod: Invalid ConditionReference JSON: " + json.toString() + ". 'value' field is missing or not a string. Defaulting to null reference.");
            return ConditionReference.of("", "null");
        }
    }
}
