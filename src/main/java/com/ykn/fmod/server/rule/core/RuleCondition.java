/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.HashSet;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.ykn.fmod.server.base.util.Util;

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
     * followed by one or more letters, digits, or underscores, and is not the boolean literal
     * {@code true} or {@code false}.
     *
     * <p>Names are referenced inside boolean formula strings (e.g. {@code "inZone && !isOp"}),
     * so they must be valid identifiers.  The keywords {@code true} and {@code false} are
     * reserved by {@link com.ykn.fmod.server.rule.tool.ConditionFormulaParser} as constant
     * literals and must not be used as condition names to avoid ambiguity during parsing.
     *
     * <p>Names that merely begin with these words (e.g. {@code trueZone}, {@code falseAlarm})
     * are still accepted; only the exact strings {@code "true"} and {@code "false"} are rejected.
     * This is achieved by the negative lookahead {@code (?!true$|false$)} at the start of the
     * pattern, where the anchored {@code $} restricts rejection to whole-word matches.
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("(?!true$|false$)[a-zA-Z_][a-zA-Z0-9_]*");

    /**
     * Evaluates this condition in the given execution context, with cyclic-reference protection.
     *
     * <p>This is a <em>template method</em>: it registers {@code this} in
     * {@link RuleContext#addTestingConditions(RuleCondition)} before delegating to
     * {@link #onEvaluate(RuleContext)}, and removes it afterwards.
     * If the same condition instance is already being evaluated on the current call stack
     * (i.e. a cycle is detected), the method immediately sets an error message on the
     * context and returns {@code false} - preventing a {@link StackOverflowError}.
     *
     * <p><b>Do not override this method.</b>  Override {@link #onEvaluate(RuleContext)} instead.
     *
     * @param context the current rule execution context, providing access to the server,
     *                event-supplied variables, and the owning rule
     * @return {@code true} if the condition is satisfied; {@code false} if unsatisfied
     *         <em>or</em> if a cyclic reference was detected
     */
    default public boolean evaluate(RuleContext context) {
        boolean isNotTesting = context.addTestingConditions(this);
        if (!isNotTesting) {
            context.setErrorMessage(Util.parseTranslatableText("fmod.rule.error.condition.cyclic", context.getRule().getName(), this.getName()));
            return false;
        }
        boolean result = this.onEvaluate(context);
        context.removeTestingConditions(this);
        return result;
    };

    /**
     * Performs the actual condition evaluation logic.
     *
     * <p>This is the <em>implementation hook</em> called by {@link #evaluate(RuleContext)}
     * after cycle detection has been performed.  Condition implementors should override
     * this method rather than {@link #evaluate(RuleContext)}.
     *
     * <p>When this method is invoked, it is guaranteed that {@code this} has already been
     * added to the {@code testingConditions} set of {@code context}.  Recursive calls to
     * evaluate other conditions are safe - any cycle will be caught by
     * {@link #evaluate(RuleContext)} before reaching infinite recursion.
     *
     * @param context the current rule execution context
     * @return {@code true} if the condition is satisfied, {@code false} otherwise
     */
    public boolean onEvaluate(RuleContext context);

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
     * Returns an optimised version of this condition for the given rule, with cyclic-reference
     * protection.
     *
     * <p>This is a <em>template method</em>: it checks whether {@code this} is already present
     * in {@code optimizingConditions} (indicating a cycle).  If so, it returns {@code this}
     * immediately to prevent infinite recursion.  Otherwise it registers {@code this},
     * delegates to {@link #onOptimize(CustomRule, HashSet)}, then de-registers.
     *
     * <p>The default implementation returns {@code this} unchanged via {@link #onOptimize}.
     * Composite conditions (e.g. {@link BinaryConditionExpression}) override
     * {@link #onOptimize} to recursively inline {@link ConditionReference} nodes, eliminating
     * repeated map look-ups at evaluation time.
     *
     * <p><b>Do not override this method.</b>  Override {@link #onOptimize(CustomRule, HashSet)}
     * instead.
     *
     * @param rule                 the rule whose {@code extra} condition list is used to resolve
     *                             {@link ConditionReference} nodes
     * @param optimizingConditions the set of conditions currently being optimised on the call
     *                             stack; used for cycle detection - pass the same set through
     *                             all recursive calls
     * @return an optimised substitute for this condition, or {@code this} if no optimisation
     *         applies or if a cycle is detected
     */
    default public RuleCondition optimize(CustomRule rule, HashSet<RuleCondition> optimizingConditions) {
        if (optimizingConditions.contains(this)) {
            // Circular reference detected; bail out to prevent infinite recursion.
            return this;
        }
        optimizingConditions.add(this);
        RuleCondition optimized = onOptimize(rule, optimizingConditions);
        optimizingConditions.remove(this);
        return optimized;
    }

    /**
     * Performs the actual optimisation logic.
     *
     * <p>This is the <em>implementation hook</em> called by
     * {@link #optimize(CustomRule, HashSet)} after cycle detection has been performed.
     * Condition implementors should override this method rather than
     * {@link #optimize(CustomRule, HashSet)}.
     *
     * <p>The default implementation simply returns {@code this}.
     *
     * @param rule                 the rule whose {@code extra} condition list is used to resolve
     *                             {@link ConditionReference} nodes
     * @param optimizingConditions the cycle-detection set - must be forwarded unchanged to
     *                             any recursive calls to {@link #optimize(CustomRule, HashSet)}
     * @return an optimised substitute for this condition, or {@code this} if no optimisation
     *         applies
     */
    default public RuleCondition onOptimize(CustomRule rule, HashSet<RuleCondition> optimizingConditions) {
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
