/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

/**
 * Enumeration of the logical and bitwise operators supported by the rule condition system.
 *
 * <p>A {@link BinaryConditionExpression} uses one of the binary operators ({@link #OR},
 * {@link #AND}, {@link #BITOR}, {@link #XOR}, {@link #BITAND}) to combine two
 * {@link RuleCondition} operands. A {@link UnaryConditionExpression} uses one of the
 * unary operators ({@link #NOT}, {@link #TRUE}, {@link #FALSE}).
 *
 * <p>Operator precedence (lowest → highest) when parsed from a formula string:
 * {@code ||} < {@code &&} < {@code |} < {@code ^} < {@code &} < {@code !}.
 */
public enum ConditionRelationship {

    /** 
     * Short-circuit logical OR ({@code ||}).  Evaluates the right operand only if the left is {@code false}. 
     */
    OR,

    /** 
     * Short-circuit logical AND ({@code &&}).  Evaluates the right operand only if the left is {@code true}. 
     */
    AND,

    /** 
     * Non-short-circuit bitwise OR ({@code |}).  Both operands are always evaluated. 
     */
    BITOR,

    /** 
     * Non-short-circuit XOR ({@code ^}).  Both operands are always evaluated. 
     */
    XOR,

    /** 
     * Non-short-circuit bitwise AND ({@code &}).  Both operands are always evaluated. 
     */
    BITAND,

    /** 
     * Unary logical NOT ({@code !}).  Negates a single operand. 
     */
    NOT,

    /**
     * Constant {@code false} sentinel.
     *
     * <p>Used as an optimisation hint: a binary/unary expression whose relationship is
     * {@code FALSE} is folded to {@link ConstCondition#of(boolean) ConstCondition.of(false)}
     * during {@link RuleCondition#optimize(CustomRule)}.
     */
    FALSE,

    /**
     * Constant {@code true} sentinel.
     *
     * <p>Used as an optimisation hint: a binary/unary expression whose relationship is
     * {@code TRUE} is folded to {@link ConstCondition#of(boolean) ConstCondition.of(true)}
     * during {@link RuleCondition#optimize(CustomRule)}.
     */
    TRUE
    
}
