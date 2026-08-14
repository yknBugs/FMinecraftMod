/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.ConditionFormulaParser;

/**
 * A composite {@link IterableCondition} that applies a binary logical or bitwise operator
 * to two child {@link RuleCondition} operands.
 *
 * <p>The supported relationships are defined by {@link ConditionRelationship}:
 * <ul>
 *   <li>{@link ConditionRelationship#AND}    – short-circuit logical AND ({@code &&})</li>
 *   <li>{@link ConditionRelationship#OR}     – short-circuit logical OR ({@code ||})</li>
 *   <li>{@link ConditionRelationship#BITAND} – non-short-circuit bitwise AND ({@code &})</li>
 *   <li>{@link ConditionRelationship#BITOR}  – non-short-circuit bitwise OR ({@code |})</li>
 *   <li>{@link ConditionRelationship#XOR}    – XOR ({@code ^})</li>
 *   <li>{@link ConditionRelationship#TRUE} / {@link ConditionRelationship#FALSE} – constant sentinels,
 *       folded away by {@link RuleCondition#optimize(CustomRule)}</li>
 * </ul>
 *
 * <p>Instances are created either programmatically via {@link RuleCondition#and(RuleCondition)},
 * {@link RuleCondition#or(RuleCondition)}, etc., or by deserializing from a formula string through
 * {@link ConditionFormulaParser#parse(String)}. The JSON representation stores only the formula
 * string (see {@link IterableCondition#toJson()}).
 *
 * <p>This class is immutable; {@link #setName(String)} returns a new instance.
 *
 * @see ConditionRelationship
 * @see UnaryConditionExpression
 * @see ConditionFormulaParser
 */
public class BinaryConditionExpression implements IterableCondition {

    /** 
     * User-assigned name for this expression node, used in admin commands and display. 
     */
    private final String name;

    /** 
     * The left-hand operand of the binary expression. 
     */
    private final RuleCondition leftOperand;

    /** 
     * The right-hand operand of the binary expression. 
     */
    private final RuleCondition rightOperand;

    /** 
     * The binary operator applied to the two operands. 
     */
    private final ConditionRelationship relationship;

    /**
     * The type string for this class.
     */
    public static final String TYPE = "BinaryConditionExpression";

    private BinaryConditionExpression(String name, RuleCondition leftOperand, RuleCondition rightOperand, ConditionRelationship relationship) {
        this.name = name;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
        this.relationship = relationship;
    }

    private static String conditionToFormula(RuleCondition condition) {
        if (condition instanceof IterableCondition) {
            IterableCondition nestedIterableCondition = (IterableCondition) condition;
            return "(" + nestedIterableCondition.toFormula() + ")";
        } else if (condition instanceof SourceCondition) {
            SourceCondition sourceCondition = (SourceCondition) condition;
            return sourceCondition.getName();
        } else if (condition instanceof ConditionReference) {
            ConditionReference conditionReference = (ConditionReference) condition;
            return conditionReference.getReferenceName();
        } else if (condition instanceof ConstCondition) {
            ConstCondition constCondition = (ConstCondition) condition;
            return String.valueOf(constCondition.getValue());
        } else {
            Util.LOGGER.error("FMinecraftMod: Unknown condition type " + condition.getClass().getName() + ", defaulting to condition type name");
            return condition.getType();
        }
    }

    private static List<RuleCondition> conditionToChildren(RuleCondition condition) {
        if (condition instanceof IterableCondition) {
            IterableCondition iterableCondition = (IterableCondition) condition;
            return iterableCondition.visit();
        } else {
            List<RuleCondition> children = new ArrayList<>();
            children.add(condition);
            return children;
        }
    }

    /**
     * Creates a new named {@code BinaryConditionExpression}.
     *
     * @param name         the instance name
     * @param leftOperand  the left-hand operand
     * @param rightOperand the right-hand operand
     * @param relationship the binary logical/bitwise operator
     */
    public static BinaryConditionExpression of(String name, RuleCondition leftOperand, RuleCondition rightOperand, ConditionRelationship relationship) {
        return new BinaryConditionExpression(name, leftOperand, rightOperand, relationship);
    }

    /**
     * Creates a new anonymous {@code BinaryConditionExpression} (empty name).
     *
     * @param leftOperand  the left-hand operand
     * @param rightOperand the right-hand operand
     * @param relationship the binary logical/bitwise operator
     */
    public static BinaryConditionExpression of(RuleCondition leftOperand, RuleCondition rightOperand, ConditionRelationship relationship) {
        return new BinaryConditionExpression("", leftOperand, rightOperand, relationship);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        switch (relationship) {
            case AND:
                return leftOperand.evaluate(context) && rightOperand.evaluate(context);
            case OR:
                return leftOperand.evaluate(context) || rightOperand.evaluate(context);
            case BITAND:
                return leftOperand.evaluate(context) & rightOperand.evaluate(context);
            case BITOR:
                return leftOperand.evaluate(context) | rightOperand.evaluate(context);
            case XOR:
                return leftOperand.evaluate(context) ^ rightOperand.evaluate(context);
            case FALSE:
                return false;
            case TRUE:
                return true;
            case NOT:
            default:
                Util.LOGGER.error("FMinecraftMod: Unsupported or invalid condition relationship " + relationship + ", defaulting to false");
                return false;
        }
    }

    @Override
    public String toFormula() {
        switch (relationship) {
            case AND:
                return conditionToFormula(leftOperand) + " && " + conditionToFormula(rightOperand);
            case OR:
                return conditionToFormula(leftOperand) + " || " + conditionToFormula(rightOperand);
            case BITAND:
                return conditionToFormula(leftOperand) + " & " + conditionToFormula(rightOperand);
            case BITOR:
                return conditionToFormula(leftOperand) + " | " + conditionToFormula(rightOperand);
            case XOR:
                return conditionToFormula(leftOperand) + " ^ " + conditionToFormula(rightOperand);
            case FALSE:
                return "false";
            case TRUE:
                return "true";
            case NOT:
            default:
                Util.LOGGER.error("FMinecraftMod: Unsupported or invalid condition relationship " + relationship + ", defaulting to false");
                return "";
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new BinaryConditionExpression(name, leftOperand, rightOperand, relationship);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public List<RuleCondition> visit() {
        List<RuleCondition> children = new ArrayList<>();
        children.addAll(conditionToChildren(leftOperand));
        children.addAll(conditionToChildren(rightOperand));
        return children;
    }

    @Override
    public RuleCondition onOptimize(CustomRule rule, HashSet<RuleCondition> optimizingConditions) {
        if (this.relationship == ConditionRelationship.TRUE) {
            return ConstCondition.of(true);
        } else if (this.relationship == ConditionRelationship.FALSE) {
            return ConstCondition.of(false);
        }
        RuleCondition left = leftOperand.optimize(rule, optimizingConditions);
        RuleCondition right = rightOperand.optimize(rule, optimizingConditions);
        if (left == leftOperand && right == rightOperand) {
            return this;
        }
        return BinaryConditionExpression.of(getName(), left, right, relationship);
    }

    /**
     * Returns the left-hand operand of this expression.
     *
     * @return the left-hand {@link RuleCondition}
     */
    public RuleCondition getLeftOperand() {
        return leftOperand;
    }

    /**
     * Returns the right-hand operand of this expression.
     *
     * @return the right-hand {@link RuleCondition}
     */
    public RuleCondition getRightOperand() {
        return rightOperand;
    }

    /**
     * Returns the binary operator applied by this expression.
     *
     * @return the {@link ConditionRelationship} used to combine the two operands
     */
    public ConditionRelationship getRelationship() {
        return relationship;
    }

    /**
     * Convenience overload; delegates to {@link #toJson()}.
     *
     * @param expression the expression to serialise
     * @return the JSON representation
     */
    public static JsonObject toJson(BinaryConditionExpression expression) {
        return expression.toJson();
    }

    /**
     * Deserializes a {@code BinaryConditionExpression} from its JSON representation.
     *
     * <p>The JSON must contain a {@code "value"} string field holding the formula
     * (e.g. {@code "inZone && !isOp"}), which is re-parsed via
     * {@link ConditionFormulaParser}. If the result is not a binary expression an
     * {@link IllegalArgumentException} is thrown.
     *
     * @param json the JSON object to deserialise
     * @return a new {@code BinaryConditionExpression}
     */
    public static BinaryConditionExpression fromJson(JsonObject json) {
        try {
            String name = json.has("name") ? json.get("name").getAsString() : "";
            String formula = json.has("value") ? json.get("value").getAsString() : "";
            RuleCondition condition = ConditionFormulaParser.parse(formula);
            if (condition instanceof BinaryConditionExpression) {
                BinaryConditionExpression binaryCondition = (BinaryConditionExpression) condition;
                return new BinaryConditionExpression(name, binaryCondition.getLeftOperand(), binaryCondition.getRightOperand(), binaryCondition.getRelationship());
            } else {
                throw new IllegalArgumentException("JSON does not represent a valid BinaryConditionExpression, but represent " + condition.getType() + " instead.");
            }
        } catch (Exception e) {
            Util.LOGGER.warn("FMinecraftMod: Failed to parse BinaryConditionExpression from JSON, defaulting to false.", e);
            return new BinaryConditionExpression("", ConstCondition.of(false), ConstCondition.of(false), ConditionRelationship.FALSE);
        }
    }
}
