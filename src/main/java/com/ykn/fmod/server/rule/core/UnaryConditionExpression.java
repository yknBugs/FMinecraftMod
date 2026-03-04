/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.ConditionFormulaParser;

/**
 * A composite {@link IterableCondition} that applies a unary operator to a single
 * child {@link RuleCondition} operand.
 *
 * <p>The only meaningful runtime operator is {@link ConditionRelationship#NOT} ({@code !}).
 * {@link ConditionRelationship#TRUE} and {@link ConditionRelationship#FALSE} are constant
 * sentinels that are folded away during {@link RuleCondition#optimize(CustomRule)}.
 *
 * <p>Instances are created programmatically via {@link RuleCondition#not()} or by
 * deserializing from a formula string through
 * {@link com.ykn.fmod.server.rule.tool.ConditionFormulaParser#parse(String)}.
 * The JSON representation stores only the formula string.
 *
 * <p>This class is immutable; {@link #setName(String)} returns a new instance.
 *
 * @see BinaryConditionExpression
 * @see ConditionRelationship
 * @see com.ykn.fmod.server.rule.tool.ConditionFormulaParser
 */
public class UnaryConditionExpression implements IterableCondition {

    /** 
     * User-assigned name for this expression node, used in admin commands and display. 
     */
    private final String name;

    /** 
     * The single operand to which the unary operator is applied. 
     */
    private final RuleCondition operand;

    /** 
     * The unary operator (currently only {@link ConditionRelationship#NOT} is evaluated). 
     */
    private final ConditionRelationship relationship;
    
    private UnaryConditionExpression(String name, RuleCondition operand, ConditionRelationship relationship) {
        this.name = name;
        this.operand = operand;
        this.relationship = relationship;
    }

    /**
     * Creates a new named {@code UnaryConditionExpression}.
     *
     * @param name         the instance name
     * @param operand      the single child operand
     * @param relationship the unary operator (use {@link ConditionRelationship#NOT})
     */
    public static UnaryConditionExpression of(String name, RuleCondition operand, ConditionRelationship relationship) {
        return new UnaryConditionExpression(name, operand, relationship);
    }

    /**
     * Creates a new anonymous {@code UnaryConditionExpression} (empty name).
     *
     * @param operand      the single child operand
     * @param relationship the unary operator (use {@link ConditionRelationship#NOT})
     */
    public static UnaryConditionExpression of(RuleCondition operand, ConditionRelationship relationship) {
        return new UnaryConditionExpression("", operand, relationship);
    }

    @Override
    public boolean evaluate(RuleContext context) {
        switch (relationship) {
            case NOT:
                return !operand.evaluate(context);
            case TRUE:
                return true;
            case FALSE:
                return false;
            case AND:
            case OR:
            case XOR:
            case BITAND:
            case BITOR:
            default:
                throw new IllegalArgumentException("Unsupported unary condition relationship: " + relationship);
        }
    }
    
    @Override
    public String toFormula() {
        switch (relationship) {
            case NOT:
                {
                    if (operand instanceof IterableCondition) {
                        IterableCondition nestedIterableCondition = (IterableCondition) operand;
                        return "!(" + nestedIterableCondition.toFormula() + ")";
                    } else if (operand instanceof SourceCondition) {
                        SourceCondition sourceCondition = (SourceCondition) operand;
                        return "!" + sourceCondition.getName();
                    } else if (operand instanceof ConditionReference) {
                        ConditionReference conditionReference = (ConditionReference) operand;
                        return "!" + conditionReference.getReferenceName();
                    } else if (operand instanceof ConstCondition) {
                        ConstCondition constCondition = (ConstCondition) operand;
                        return "!" + String.valueOf(constCondition.getValue());
                    } else {
                        Util.LOGGER.error("FMinecraftMod: Unknown condition type " + operand.getClass().getName() + ", defaulting to condition type name");
                        return "!" + operand.getType();
                    }
                }
            case TRUE:
                return "true";
            case FALSE:
                return "false";
            case AND:
            case OR:
            case XOR:
            case BITAND:
            case BITOR:
            default:
                throw new IllegalArgumentException("Unsupported unary condition relationship: " + relationship);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new UnaryConditionExpression(name, operand, relationship);
    }

    @Override
    public String getType() {
        return "UnaryConditionExpression";
    }

    @Override
    public List<RuleCondition> visit() {
        if (operand instanceof IterableCondition) {
            IterableCondition iterableOperand = (IterableCondition) operand;
            return iterableOperand.visit();
        } else {
            List<RuleCondition> children = new ArrayList<>();
            children.add(operand);
            return children;
        }
    }

    @Override
    public RuleCondition optimize(CustomRule rule) {
        if (this.relationship == ConditionRelationship.TRUE) {
            return ConstCondition.of(true);
        } else if (this.relationship == ConditionRelationship.FALSE) {
            return ConstCondition.of(false);
        }
        RuleCondition operand = this.operand.optimize(rule);
        if (operand == this.operand) {
            return this;
        }
        return UnaryConditionExpression.of(getName(), operand, relationship);
    }

    /**
     * Returns the single operand of this expression.
     *
     * @return the child {@link RuleCondition}
     */
    public RuleCondition getOperand() {
        return operand;
    }

    /**
     * Returns the unary operator applied by this expression.
     *
     * @return the {@link ConditionRelationship}
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
    public static JsonObject toJson(UnaryConditionExpression expression) {
        return expression.toJson();
    }

    /**
     * Deserializes a {@code UnaryConditionExpression} from its JSON representation.
     *
     * <p>The JSON must contain a {@code "value"} string field holding the formula
     * (e.g. {@code "!isOp"}), re-parsed via {@link ConditionFormulaParser}.
     * If the result is not a unary expression an {@link IllegalArgumentException} is thrown.
     *
     * @param json the JSON object to deserialise
     * @return a new {@code UnaryConditionExpression}
     * @throws IllegalArgumentException if {@code "value"} does not parse to a unary expression
     */
    public static UnaryConditionExpression fromJson(JsonObject json) {
        try {
            String name = json.has("name") ? json.get("name").getAsString() : "";
            String formula = json.has("value") ? json.get("value").getAsString() : "";
            RuleCondition condition = ConditionFormulaParser.parse(formula);
            if (condition instanceof UnaryConditionExpression) {
                UnaryConditionExpression unaryCondition = (UnaryConditionExpression) condition;
                return new UnaryConditionExpression(name, unaryCondition.getOperand(), unaryCondition.getRelationship());
            } else {
                throw new IllegalArgumentException("JSON does not represent a valid UnaryConditionExpression, but represent " + condition.getType() + " instead.");
            }
        } catch (Exception e) {
            Util.LOGGER.warn("FMinecraftMod: Failed to parse UnaryConditionExpression from JSON, defaulting to false.", e);
            return new UnaryConditionExpression("", ConstCondition.of(false), ConditionRelationship.FALSE);
        }
    }
}
