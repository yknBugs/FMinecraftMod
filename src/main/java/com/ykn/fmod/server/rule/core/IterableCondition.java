/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.network.chat.Component;

/**
 * A {@link RuleCondition} that is composed of child conditions and can be expressed
 * as a printable boolean formula string.
 *
 * <p>All composite conditions ({@link BinaryConditionExpression} and
 * {@link UnaryConditionExpression}) implement this interface. It provides:
 * <ul>
 *   <li>{@link #toFormula()} – serialises the expression back to a human-readable
 *       formula string (e.g. {@code "inZone && !isOp"}) that round-trips through
 *       {@link com.ykn.fmod.server.rule.tool.ConditionFormulaParser}.</li>
 *   <li>{@link #visit()}    – returns the flat list of all leaf child conditions,
 *       used during optimisation to gather references.</li>
 * </ul>
 *
 * <p>The default {@link #toJson()} implementation serialises the formula string under
 * the {@code "value"} key, which is how the JSON rule format stores expressions.
 * The default {@link #render()} shows the formula in chat.
 *
 * @see BinaryConditionExpression
 * @see UnaryConditionExpression
 * @see com.ykn.fmod.server.rule.tool.ConditionFormulaParser
 */
public interface IterableCondition extends RuleCondition {

    /**
     * Returns the boolean formula string that represents this condition.
     *
     * <p>The formula uses Java-style operators: {@code &&}, {@code ||}, {@code &},
     * {@code |}, {@code ^}, and {@code !}, with parentheses for grouping.
     * Leaf operands are represented by their condition names or constant literals.
     *
     * @return a non-null formula string, parseable by
     *         {@link com.ykn.fmod.server.rule.tool.ConditionFormulaParser#parse(String)}
     */
    public String toFormula();

    /**
     * Returns the direct child conditions (leaves) of this composite condition.
     *
     * <p>For a deep tree, each child is itself an {@link IterableCondition} only if
     * it is a nested composite; otherwise it is a leaf ({@link SourceCondition},
     * {@link ConditionReference}, or {@link ConstCondition}).
     *
     * @return a mutable list of child conditions; never {@code null}
     */
    public List<RuleCondition> visit();

    @Override
    default Component render() {
        if (getName().isEmpty()) {
            return Component.literal(toFormula());
        } else {
            return Component.literal(getName() + ": " + toFormula());
        }
    }

    @Override
    default JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("name", getName());
        result.addProperty("type", getType());
        result.addProperty("value", toFormula());
        return result;
    }
}
