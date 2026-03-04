/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.regex.Pattern;

import com.google.gson.JsonObject;

/**
 * Represents a boolean condition that can be evaluated in the context of a rule execution.
 *
 * <p>Conditions form a tree: leaf nodes implement {@link SourceCondition} (concrete game-state
 * checks), while composite nodes implement {@link IterableCondition} ({@link BinaryConditionExpression}
 * and {@link UnaryConditionExpression}). Lazy name references use {@link ConditionReference}
 * and are resolved at optimisation time. The constant {@link ConstCondition} is always
 * {@code true} or always {@code false}.
 *
 * <p>To create a new condition type, implement {@link SourceCondition} (most common) or
 * directly this interface, then register it with
 * {@link com.ykn.fmod.server.rule.tool.RuleRegistry}.
 *
 * <p>Default helper methods ({@link #and}, {@link #or}, {@link #not}, etc.) allow conditions
 * to be composed in Java code without manually constructing expression nodes.
 *
 * @see SourceCondition
 * @see IterableCondition
 * @see BinaryConditionExpression
 * @see UnaryConditionExpression
 * @see ConditionReference
 * @see ConstCondition
 */
public interface RuleCondition extends RuleComponent {

    /**
     * Regex pattern that valid condition names must match: starts with a letter or underscore,
     * followed by one or more letters, digits, or underscores.
     *
     * <p>Names are referenced inside boolean formula strings (e.g. {@code "inZone && !isOp"}),
     * so they must be valid identifiers.
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * Evaluates this condition in the given execution context.
     *
     * @param context the current rule execution context, providing access to the server,
     *                event-supplied variables, and the owning rule
     * @return {@code true} if the condition is satisfied, {@code false} otherwise
     */
    public boolean evaluate(RuleContext context);

    /**
     * Combines this condition with {@code operand} using the given logical/bitwise relationship,
     * returning a new {@link BinaryConditionExpression}.
     *
     * @param operand      the right-hand operand
     * @param relationship the operator to apply (e.g. {@link ConditionRelationship#AND})
     * @return a new composite {@link IterableCondition}
     */
    default public IterableCondition append(RuleCondition operand, ConditionRelationship relationship) {
        return BinaryConditionExpression.of(this, operand, relationship);
    }

    /**
     * Returns a new condition that is the logical negation ({@code !}) of this one.
     *
     * @return a new {@link UnaryConditionExpression} that negates this condition
     */
    default public IterableCondition not() {
        return UnaryConditionExpression.of(this, ConditionRelationship.NOT);
    }

    /**
     * Returns a new short-circuit logical AND ({@code &&}) condition.
     *
     * @param operand the right-hand operand
     * @return a new {@link BinaryConditionExpression} with relationship {@link ConditionRelationship#AND}
     */
    default public IterableCondition and(RuleCondition operand) {
        return append(operand, ConditionRelationship.AND);
    }

    /**
     * Returns a new short-circuit logical OR ({@code ||}) condition.
     *
     * @param operand the right-hand operand
     * @return a new {@link BinaryConditionExpression} with relationship {@link ConditionRelationship#OR}
     */
    default public IterableCondition or(RuleCondition operand) {
        return append(operand, ConditionRelationship.OR);
    }

    /**
     * Returns a new non-short-circuit bitwise AND ({@code &}) condition.
     *
     * @param operand the right-hand operand
     * @return a new {@link BinaryConditionExpression} with relationship {@link ConditionRelationship#BITAND}
     */
    default public IterableCondition bitAnd(RuleCondition operand) {
        return append(operand, ConditionRelationship.BITAND);
    }

    /**
     * Returns a new non-short-circuit bitwise OR ({@code |}) condition.
     *
     * @param operand the right-hand operand
     * @return a new {@link BinaryConditionExpression} with relationship {@link ConditionRelationship#BITOR}
     */
    default public IterableCondition bitOr(RuleCondition operand) {
        return append(operand, ConditionRelationship.BITOR);
    }

    /**
     * Returns a new XOR ({@code ^}) condition.
     *
     * @param operand the right-hand operand
     * @return a new {@link BinaryConditionExpression} with relationship {@link ConditionRelationship#XOR}
     */
    default public IterableCondition xor(RuleCondition operand) {
        return append(operand, ConditionRelationship.XOR);
    }

    /**
     * Returns an optimised version of this condition for the given rule.
     *
     * <p>The default implementation returns {@code this} unchanged. Composite conditions
     * (e.g. {@link BinaryConditionExpression}) override this to recursively inline
     * {@link ConditionReference} nodes, eliminating repeated map look-ups during evaluation.
     *
     * @param rule the rule whose {@code extra} list is used to resolve references
     * @return an optimised substitute for this condition, or {@code this} if no optimisation applies
     */
    default public RuleCondition optimize(CustomRule rule) {
        return this;
    }

    /**
     * Returns the unique name of this condition instance within its rule.
     *
     * <p>Names are referenced in boolean formula strings (e.g. {@code "inZone && !isOp"})
     * and in command operations.
     *
     * @return the non-null name of this condition instance
     */
    public String getName();

    /**
     * Returns a copy of this condition with the given name.
     *
     * <p>Conditions are immutable by convention; this method returns a new instance
     * rather than mutating the current one.
     *
     * @param name the new name to assign
     * @return a new {@code RuleCondition} that is otherwise identical to this one
     */
    public RuleCondition setName(String name);

    /**
     * Serializes this condition to a {@link JsonObject} that can be embedded in a rule JSON file.
     *
     * @return a non-null {@link JsonObject} representing this condition
     */
    public JsonObject toJson();
}
