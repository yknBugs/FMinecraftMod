/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.BinaryConditionExpression;
import com.ykn.fmod.server.rule.core.ConditionReference;
import com.ykn.fmod.server.rule.core.ConditionRelationship;
import com.ykn.fmod.server.rule.core.ConstCondition;
import com.ykn.fmod.server.rule.core.IterableCondition;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.UnaryConditionExpression;

/**
 * Parses a boolean condition formula string into a {@link RuleCondition} tree.
 *
 * <p>Supported syntax:
 * <ul>
 *   <li>{@code a || b}  – logical OR (short-circuit)</li>
 *   <li>{@code a && b}  – logical AND (short-circuit)</li>
 *   <li>{@code a | b}   – bitwise OR (no short-circuit)</li>
 *   <li>{@code a ^ b}   – XOR (no short-circuit)</li>
 *   <li>{@code a & b}   – bitwise AND (no short-circuit)</li>
 *   <li>{@code !a}      – logical NOT</li>
 *   <li>{@code (expr)}  – grouping</li>
 *   <li>{@code true} / {@code false} – boolean constants</li>
 *   <li>{@code identifier} – reference to a named condition</li>
 * </ul>
 *
 * Operator precedence (lowest → highest): {@code ||}, {@code &&}, {@code |}, {@code ^}, {@code &}, {@code !}.
 *
 * @throws IllegalArgumentException if the formula is null, empty, or contains invalid syntax
 */
public class ConditionFormulaParser {

    private final String formula;
    private int pos;

    private ConditionFormulaParser(String formula) {
        this.formula = formula;
        this.pos = 0;
    }

    /**
     * Parses {@code formula} and returns the root {@link RuleCondition}.
     *
     * @param formula the boolean formula string to parse
     * @return the parsed condition tree, never {@code null}
     * @throws IllegalArgumentException if the formula is null, empty, or contains invalid syntax
     */
    public static RuleCondition parse(String formula) {
        if (formula == null || formula.strip().isEmpty()) {
            throw new IllegalArgumentException("At position 0 in formula: '" + formula + "': Formula cannot be null or empty.");
        }
        ConditionFormulaParser parser = new ConditionFormulaParser(formula.strip());
        RuleCondition result = parser.parseOr();
        parser.skipWhitespace();
        if (parser.pos < parser.formula.length()) {
            throw new IllegalArgumentException("At position " + parser.pos + " in formula: '" + formula + "': Found unexpected characters '" + parser.formula.substring(parser.pos) + "' at the end of the formula.");
        }
        return result;
    }

    /**
     * Reconstructs a formula string for {@code condition}, the inverse of {@link #parse(String)}.
     *
     * <p>Composite nodes ({@link IterableCondition}) delegate to {@link IterableCondition#toFormula()};
     * leaves render as their name ({@link SourceCondition}), reference name
     * ({@link ConditionReference}), or literal ({@link ConstCondition}).
     *
     * @param condition the condition tree to render
     * @return a formula string parseable by {@link #parse(String)}
     */
    public static String toFormula(RuleCondition condition) {
        if (condition instanceof IterableCondition) {
            return ((IterableCondition) condition).toFormula();
        } else if (condition instanceof SourceCondition) {
            return ((SourceCondition) condition).getName();
        } else if (condition instanceof ConditionReference) {
            return ((ConditionReference) condition).getReferenceName();
        } else if (condition instanceof ConstCondition) {
            return String.valueOf(((ConstCondition) condition).getValue());
        } else {
            Util.LOGGER.warn("Unknown RuleCondition type: " + condition.getClass().getName());
            return condition.getType();
        }
    }

    /**
     * Skip whitespace characters starting from the current position.
     */
    private void skipWhitespace() {
        while (pos < formula.length() && Character.isWhitespace(formula.charAt(pos))) {
            pos++;
        }
    }

    /** 
     * Peek at the character at the current position without consuming it. Returns {@code '\0'} at EOF. 
     */
    private char peek() {
        return pos < formula.length() ? formula.charAt(pos) : '\0';
    }

    /** 
     * Peek at the character at {@code pos + offset}. Returns {@code '\0'} if out of range. 
     */
    private char peek(int offset) {
        int idx = pos + offset;
        return idx < formula.length() ? formula.charAt(idx) : '\0';
    }

    // Level 1 – lowest precedence: ||
    private RuleCondition parseOr() {
        RuleCondition left = parseAnd();
        while (true) {
            skipWhitespace();
            if (peek() == '|' && peek(1) == '|') {
                pos += 2;
                RuleCondition right = parseAnd();
                left = BinaryConditionExpression.of(left, right, ConditionRelationship.OR);
            } else {
                break;
            }
        }
        return left;
    }

    // Level 2: &&
    private RuleCondition parseAnd() {
        RuleCondition left = parseBitOr();
        while (true) {
            skipWhitespace();
            if (peek() == '&' && peek(1) == '&') {
                pos += 2;
                RuleCondition right = parseBitOr();
                left = BinaryConditionExpression.of(left, right, ConditionRelationship.AND);
            } else {
                break;
            }
        }
        return left;
    }

    // Level 3: | (bitwise OR, not followed by another |)
    private RuleCondition parseBitOr() {
        RuleCondition left = parseXor();
        while (true) {
            skipWhitespace();
            if (peek() == '|' && peek(1) != '|') {
                pos += 1;
                RuleCondition right = parseXor();
                left = BinaryConditionExpression.of(left, right, ConditionRelationship.BITOR);
            } else {
                break;
            }
        }
        return left;
    }

    // Level 4: ^ (XOR)
    private RuleCondition parseXor() {
        RuleCondition left = parseBitAnd();
        while (true) {
            skipWhitespace();
            if (peek() == '^') {
                pos += 1;
                RuleCondition right = parseBitAnd();
                left = BinaryConditionExpression.of(left, right, ConditionRelationship.XOR);
            } else {
                break;
            }
        }
        return left;
    }

    // Level 5: & (bitwise AND, not followed by another &)
    private RuleCondition parseBitAnd() {
        RuleCondition left = parseNot();
        while (true) {
            skipWhitespace();
            if (peek() == '&' && peek(1) != '&') {
                pos += 1;
                RuleCondition right = parseNot();
                left = BinaryConditionExpression.of(left, right, ConditionRelationship.BITAND);
            } else {
                break;
            }
        }
        return left;
    }

    // Level 6: unary !
    private RuleCondition parseNot() {
        skipWhitespace();
        if (peek() == '!') {
            pos++;
            RuleCondition operand = parseNot(); // right-associative
            return UnaryConditionExpression.of(operand, ConditionRelationship.NOT);
        }
        return parseAtom();
    }

    // Level 6: atoms – parenthesised expr, true/false constants, identifiers
    private RuleCondition parseAtom() {
        skipWhitespace();
        if (pos >= formula.length()) {
            throw new IllegalArgumentException("At position " + pos + " in formula: '" + formula + "': Unexpected end of formula, expected condition.");
        }
        char c = peek();
        // Parenthesised expression
        if (c == '(') {
            pos++; // consume '('
            RuleCondition inner = parseOr();
            skipWhitespace();
            if (peek() == ')') {
                pos++; // consume ')'
            } else {
                throw new IllegalArgumentException("At position " + pos + " in formula: '" + formula + "': Expected ')' to close the parenthesised expression.");
            }
            return inner;
        }
        // Identifier or keyword
        if (Character.isLetter(c) || c == '_') {
            int start = pos;
            while (pos < formula.length() && (Character.isLetterOrDigit(formula.charAt(pos)) || formula.charAt(pos) == '_')) {
                pos++;
            }
            String name = formula.substring(start, pos);
            switch (name) {
                case "true":
                    return ConstCondition.of(true);
                case "false":
                    return ConstCondition.of(false);
                default:
                    return ConditionReference.need(name);
            }
        }
        throw new IllegalArgumentException("At position " + pos + " in formula: '" + formula + "': Unexpected character '" + c + "', expected '(', identifier, or boolean constant.");
    }
}
