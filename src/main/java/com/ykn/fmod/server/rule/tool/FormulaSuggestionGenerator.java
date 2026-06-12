/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import com.ykn.fmod.server.rule.core.RuleCondition;

/**
 * Generates context‑aware autocomplete suggestions for boolean expression formulas.
 *
 * <p>This utility class analyzes partially‑typed boolean expressions composed of
 * condition variable names, logical operators ({@code ||}, {@code &amp;&amp;}, {@code ^}),
 * negation ({@code !}), and parentheses, and produces a sorted, deduplicated list of
 * valid completions for the current cursor position.
 */
public class FormulaSuggestionGenerator {
    
    /**
     * Generate autocomplete suggestions for boolean expressions
     * Formula Structure: ConditionA operator ConditionB
     * 
     * @param variableNames List of available condition/variable names
     * @param existingContent Current input content to autocomplete
     * @return List of autocomplete suggestions
     */
    public static List<String> getAutocompleteSuggestions(Collection<String> variableNames, String existingContent) {
        validateVariableNames(variableNames);

        if (existingContent == null) {
            existingContent = "";
        }
        
        // Check if formula is valid (no unbalanced parentheses)
        if (!isValidFormula(existingContent) || !isValidStructure(existingContent)) {
            return new ArrayList<>();
        }
        
        List<String> suggestions = new ArrayList<>();
        
        // Handle empty input
        if (existingContent.isEmpty()) {
            suggestions.addAll(variableNames);
            suggestions.add("!");
            suggestions.add("(");
            return normalizeSuggestions(suggestions);
        }
        
        boolean endsWithSpace = existingContent.endsWith(" ");
        
        List<String> result;
        if (endsWithSpace) {
            result = getSuggestionsAfterSpace(variableNames, existingContent);
        } else {
            result = getSuggestionsWithoutTrailingSpace(variableNames, existingContent);
        }

        return normalizeSuggestions(result);
    }

    /**
     * Normalize suggestions by deduplicating and sorting alphabetically.
     * Uses TreeSet to automatically sort and deduplicate in a single pass.
     *
     * @param rawSuggestions Raw list of suggestions, possibly containing duplicates
     * @return Sorted and deduplicated list of suggestions
     */
    private static List<String> normalizeSuggestions(List<String> rawSuggestions) {
        return new ArrayList<>(new TreeSet<>(rawSuggestions));
    }

    /**
     * Validate that all variable names conform to required pattern:
     * Must match [a-zA-Z_][a-zA-Z0-9_]* but NOT be 'true' or 'false'.
     *
     * @param variableNames List of variable names to validate
     * @throws IllegalArgumentException if any name is null, empty, or invalid
     */
    private static void validateVariableNames(Collection<String> variableNames) {
        if (variableNames == null) {
            throw new IllegalArgumentException("variableNames must not be null");
        }

        for (String name : variableNames) {
            if (name == null || !RuleCondition.NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid variable name: " + String.valueOf(name));
            }
        }
    }
    
    /**
     * Get suggestions when input ends with space
     * After space, we can add operators, variables, ! or (
     */
    private static List<String> getSuggestionsAfterSpace(Collection<String> variableNames, String existingContent) {
        List<String> suggestions = new ArrayList<>();
        String trimmed = existingContent.trim();
        
        String lastToken = getLastTokenBeforeWhitespace(trimmed);
        
        // After operator or opening paren, suggest variables, !, (
        if (isOperator(lastToken)) {
            // After operator, suggest variables, !, (
            for (String var : variableNames) {
                suggestions.add(trimmed + " " + var);
            }
            suggestions.add(trimmed + " !");
            suggestions.add(trimmed + " (");
        } else if (lastToken.equals("!")) {
            // After !, suggest variables and (
            for (String var : variableNames) {
                suggestions.add(trimmed + var);
            }
            suggestions.add(trimmed + "(");
        } else if (trimmed.endsWith("(")) {
            // After (, suggest variables, !, ( without extra space
            for (String var : variableNames) {
                suggestions.add(trimmed + var);
            }
            suggestions.add(trimmed + "!");
            suggestions.add(trimmed + "(");
        } else {
            // After variable or closing paren, suggest operators
            suggestions.add(trimmed + " ||");
            suggestions.add(trimmed + " |");
            suggestions.add(trimmed + " &&");
            suggestions.add(trimmed + " &");
            suggestions.add(trimmed + " ^");
            
            // Add closing parenthesis if applicable
            int openParens = countOpenParentheses(trimmed);
            if (openParens > 0) {
                suggestions.add(trimmed + ")");
            }
        }
        
        return suggestions;
    }
    
    /**
     * Get suggestions when input does not end with space
     * Here we're completing a partial token
     */
    private static List<String> getSuggestionsWithoutTrailingSpace(Collection<String> variableNames, String existingContent) {
        List<String> suggestions = new ArrayList<>();
        
        if (existingContent.isEmpty()) {
            suggestions.addAll(variableNames);
            suggestions.add("!");
            suggestions.add("(");
            return suggestions;
        }
        
        String trimmed = existingContent.trim();
        if (trimmed.isEmpty()) {
            suggestions.addAll(variableNames);
            suggestions.add("!");
            suggestions.add("(");
            return suggestions;
        }
        
        // Get last non-space character
        char lastChar = trimmed.charAt(trimmed.length() - 1);
        
        // If ends with closing paren, just confirm
        if (lastChar == ')') {
            suggestions.add(existingContent);
            return suggestions;
        }
        
        // If ends with opening paren, suggest variables, !, and another (
        if (lastChar == '(') {
            for (String var : variableNames) {
                suggestions.add(trimmed + var);
            }
            // Only suggest ! if the input doesn't already end with !
            if (!trimmed.endsWith("!(")) {
                suggestions.add(trimmed + "!");
            }
            suggestions.add(trimmed + "(");
            return suggestions;
        }
        
        // If ends with !, suggest variables and (
        if (lastChar == '!') {
            for (String var : variableNames) {
                suggestions.add(trimmed + var);
            }
            suggestions.add(trimmed + "(");
            return suggestions;
        }
        
        // Extract last token (excluding parentheses and spaces)
        String lastToken = extractLastTokenWithoutParens(trimmed);
        String beforeLastToken = trimmed.substring(0, trimmed.length() - lastToken.length());
        
        if (isOperator(lastToken)) {
            // For top-level input like "c1 |", keep partial first.
            // Inside parenthesized context like "c1 || (c2 |", keep completed first.
            String completed = completeOperator(lastToken);
            boolean inParenthesizedContext = beforeLastToken.contains("(");

            if (inParenthesizedContext) {
                suggestions.add(beforeLastToken + completed);
                if (!lastToken.equals(completed)) {
                    suggestions.add(beforeLastToken + lastToken);
                }
            } else {
                suggestions.add(beforeLastToken + lastToken);
                if (!lastToken.equals(completed)) {
                    suggestions.add(beforeLastToken + completed);
                }
            }
        } else if (lastToken.startsWith("!")) {
            // After !, suggest matching variables and (
            String varPrefix = lastToken.substring(1);
            
            for (String var : variableNames) {
                if (var.startsWith(varPrefix)) {
                    suggestions.add(beforeLastToken + "!" + var);
                }
            }
            
            // Suggest !( if appropriate
            if (varPrefix.isEmpty() || "(".startsWith(varPrefix)) {
                suggestions.add(beforeLastToken + "!(");
            }
        } else {
            // Variable name - check if complete or partial
            if (variableNames.contains(lastToken)) {
                // Complete variable, confirm it
                suggestions.add(existingContent);
            } else {
                // Partial variable, suggest matching completions
                for (String var : variableNames) {
                    if (var.startsWith(lastToken)) {
                        suggestions.add(beforeLastToken + var);
                    }
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Extract the last token without including parentheses
     */
    private static String extractLastTokenWithoutParens(String s) {
        int i = s.length() - 1;
        int end = i;
        
        // Move back to find start of token (stop at space or paren)
        while (i >= 0 && s.charAt(i) != ' ' && s.charAt(i) != '(' && s.charAt(i) != ')') {
            i--;
        }
        
        return s.substring(i + 1, end + 1);
    }
    
    /**
     * Get the last token before whitespace, used for checking what comes before a space
     */
    private static String getLastTokenBeforeWhitespace(String s) {
        int i = s.length() - 1;
        // Skip trailing spaces
        while (i >= 0 && s.charAt(i) == ' ') {
            i--;
        }
        
        if (i < 0) return "";
        
        int end = i;
        // Move back while we have "token characters"
        while (i >= 0 && s.charAt(i) != ' ' && s.charAt(i) != '(' && s.charAt(i) != ')') {
            i--;
        }
        
        return s.substring(i + 1, end + 1);
    }
    
    /**
     * Check if formula has valid parentheses (can be incomplete with unmatched opening parens)
     */
    private static boolean isValidFormula(String formula) {
        int openParens = 0;
        for (char c : formula.toCharArray()) {
            if (c == '(') {
                openParens++;
            } else if (c == ')') {
                openParens--;
                // More closing than opening is invalid
                if (openParens < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validates that the tokenized structure of the expression is syntactically plausible.
     * Ensures operands (variables, parentheses, negation) and operators alternate correctly,
     * and that operators are not placed in operand positions (or vice versa).
     *
     * @param expr the trimmed expression to validate
     * @return {@code true} if the token sequence is structurally valid
     */
    private static boolean isValidStructure(String expr) {
        if (expr.isEmpty()) return true;
        String trimmed = expr.trim();
        if (trimmed.isEmpty()) return true;

        List<String> tokens = tokenize(trimmed);
        if (tokens.size() <= 1) return true;

        boolean expectOperand = true;

        for (String token : tokens) {
            if (expectOperand) {
                if (isOperator(token)) return false;
                if (token.equals(")")) return false;
                if (token.equals("(")) {
                    // still expect operand inside parens
                } else if (token.equals("!")) {
                    // still expect operand after !
                } else {
                    // variable
                    expectOperand = false;
                }
            } else {
                if (token.equals("(")) return false;
                if (token.equals("!")) return false;
                if (token.equals(")")) {
                    // after ), still expect operator
                } else if (isOperator(token)) {
                    expectOperand = true;
                } else {
                    // variable after variable/) without operator
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Tokenize a trimmed expression into meaningful tokens
     * (variables, operators, parentheses, !)
     */
    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == ' ') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else if (c == '(' || c == ')' || c == '!') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                tokens.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }
    
    /**
     * Count unclosed opening parentheses
     */
    private static int countOpenParentheses(String s) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
            }
        }
        return Math.max(count, 0);
    }
    
    /**
     * Check if a string is an operator (|, &, or ^)
     */
    private static boolean isOperator(String s) {
        return s.length() > 0 && s.matches("[|&^]+");
    }
    
    /**
     * Complete partial operator (| -> ||, & -> &&)
     */
    private static String completeOperator(String op) {
        if (op.equals("|")) {
            return "||";
        }
        if (op.equals("&")) {
            return "&&";
        }
        return op;
    }
}