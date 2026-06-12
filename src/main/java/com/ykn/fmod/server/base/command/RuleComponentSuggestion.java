/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.tool.FormulaSuggestionGenerator;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.commands.CommandSourceStack;

/**
 * Provides command auto-completion suggestions for components of a custom rule,
 * such as conditions, actions (if-satisfied / if-violated), variables, placeholders,
 * and boolean expressions (formulas).
 *
 * <p>The suggestion provider reads the rule name from the command input at the specified
 * argument index, looks up the corresponding {@link RuleManager}, and generates domain-specific
 * suggestions based on the selected {@link SuggestionType}.
 */
public class RuleComponentSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * Enumerates the types of rule components for which suggestions can be generated.
     */
    private static enum SuggestionType {
        CONDITION,
        DOIFSATISFIED,
        DOIFVIOLATED,
        VARIABLE,
        PLACEHOLDER,
        FORMULA
    }

    /**
     * Whether to wrap suggested names in double quotes.
     */
    private final boolean needQuote;

    /**
     * The type of suggestion to generate (condition, action, variable, etc.).
     */
    private final SuggestionType type;

    /**
     * The 0-based index within the space‑split command input where the rule name appears.
     */
    private final int ruleNameIndex;

    /**
     * Constructs a new RuleComponentSuggestion.
     *
     * @param needQuote     whether to wrap suggestions in double quotes
     * @param type          the type of rule component to suggest
     * @param ruleNameIndex the argument index of the rule name in the command input
     */
    private RuleComponentSuggestion(boolean needQuote, SuggestionType type, int ruleNameIndex) {
        this.needQuote = needQuote;
        this.type = type;
        this.ruleNameIndex = ruleNameIndex;
    }

    /**
     * Extracts the rule name from the command input string by finding the token at
     * the configured {@link #ruleNameIndex}. Handles both quoted and unquoted names.
     *
     * @param input the full command input string
     * @return the extracted rule name, or an empty string if the name cannot be determined
     */
    private String extractRuleName(String input) {
        String[] parts = input.split(" ");
        if (parts.length > ruleNameIndex) {
            String ruleNamePart = parts[ruleNameIndex];
            if (ruleNamePart.startsWith("\"")) {
                // Handle quoted rule names
                int endQuoteIndex = input.indexOf("\"", input.indexOf(ruleNamePart) + 1);
                if (endQuoteIndex != -1) {
                    return input.substring(input.indexOf(ruleNamePart) + 1, endQuoteIndex);
                } else {
                    return ruleNamePart.substring(1); // No ending quote yet
                }
            } else {
                return ruleNamePart;
            }
        }
        return "";
    }

    /**
     * Provides suggestions for the configured {@link SuggestionType} based on the
     * rule extracted from the command input. The actual suggestion logic is submitted
     * to the server thread for thread safety.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        if (context.getSource() == null || context.getSource().getServer() == null) {
            return builder.buildFuture();
        }
        return context.getSource().getServer().submit(() -> {
            String ruleName = extractRuleName(builder.getInput());
            RuleManager ruleManager = Util.getServerData(context.getSource().getServer()).getCustomRules().get(ruleName);
            if (ruleManager != null) {
                switch (type) {
                    case CONDITION:
                        {
                            List<RuleCondition> conditions = ruleManager.getRule().getExtra();
                            for (RuleCondition condition : conditions) {
                                String suggestion = condition.getName();
                                if (needQuote) {
                                    suggestion = "\"" + suggestion + "\"";
                                }
                                if (suggestion.startsWith(builder.getRemaining())) {
                                    builder.suggest(suggestion);
                                }
                            }
                        }
                        break;
                    case DOIFSATISFIED:
                        {
                            List<RuleAction> actions = ruleManager.getRule().getActionIfSatisfied();
                            for (RuleAction action : actions) {
                                String suggestion = action.getName();
                                if (needQuote) {
                                    suggestion = "\"" + suggestion + "\"";
                                }
                                if (suggestion.startsWith(builder.getRemaining())) {
                                    builder.suggest(suggestion);
                                }
                            }
                        }
                        break;
                    case DOIFVIOLATED:
                        {
                            List<RuleAction> actions = ruleManager.getRule().getActionIfViolated();
                            for (RuleAction action : actions) {
                                String suggestion = action.getName();
                                if (needQuote) {
                                    suggestion = "\"" + suggestion + "\"";
                                }
                                if (suggestion.startsWith(builder.getRemaining())) {
                                    builder.suggest(suggestion);
                                }
                            }
                        }
                        break;
                    case VARIABLE:
                        {
                            Set<String> availableVariables = ruleManager.getRule().getEvent().variablesType().keySet();
                            for (String variable : availableVariables) {
                                String suggestion = variable;
                                if (needQuote) {
                                    suggestion = "\"" + suggestion + "\"";
                                }
                                if (suggestion.startsWith(builder.getRemaining())) {
                                    builder.suggest(suggestion);
                                }
                            }
                        }
                        break;
                    case PLACEHOLDER:
                        {
                            Set<String> availableVariables = ruleManager.getRule().getEvent().variablesType().keySet();
                            String placeholderPrefix = "${";
                            String builderRemaining = builder.getRemaining();
                            int lastIndexOfPlaceholder = builderRemaining.lastIndexOf(placeholderPrefix);
                            if (lastIndexOfPlaceholder >= 0 && lastIndexOfPlaceholder < builderRemaining.length()) {
                                String variablePart = builderRemaining.substring(lastIndexOfPlaceholder);
                                for (String variable : availableVariables) {
                                    String expectedVariable = placeholderPrefix + "var:" + variable + "}";
                                    if (expectedVariable.startsWith(variablePart)) {
                                        String suggestion = builderRemaining.substring(0, lastIndexOfPlaceholder) + expectedVariable;
                                        builder.suggest(suggestion);
                                    }
                                }
                            }
                        }
                        break;
                    case FORMULA:
                         {
                            List<RuleCondition> conditions = ruleManager.getRule().getExtra();
                            List<String> conditionNames = conditions.stream().map(RuleCondition::getName).toList();
                            List<String> availableSuggestions = FormulaSuggestionGenerator.getAutocompleteSuggestions(conditionNames, builder.getRemaining());
                            for (String suggestion : availableSuggestions) {
                                String fullSuggestion = suggestion;
                                if (needQuote) {
                                    fullSuggestion = "\"" + suggestion + "\"";
                                }
                                builder.suggest(fullSuggestion);
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            return builder.build();
        });
    }

    /**
     * Creates a new RuleComponentSuggestion for extra condition names.
     *
     * @param needQuote     whether to wrap suggestions in double quotes
     * @param ruleNameIndex the argument index of the rule name in the command input
     * @return a new RuleComponentSuggestion for condition suggestions
     */
    public static RuleComponentSuggestion suggestCondition(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.CONDITION, ruleNameIndex);
    }

    /**
     * Creates a new RuleComponentSuggestion for "if satisfied" action names.
     *
     * @param needQuote     whether to wrap suggestions in double quotes
     * @param ruleNameIndex the argument index of the rule name in the command input
     * @return a new RuleComponentSuggestion for satisfied‑action suggestions
     */
    public static RuleComponentSuggestion suggestAction(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.DOIFSATISFIED, ruleNameIndex);
    }
    
    /**
     * Creates a new RuleComponentSuggestion for "if violated" (punishment) action names.
     *
     * @param needQuote     whether to wrap suggestions in double quotes
     * @param ruleNameIndex the argument index of the rule name in the command input
     * @return a new RuleComponentSuggestion for violated‑action suggestions
     */
    public static RuleComponentSuggestion suggestPunish(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.DOIFVIOLATED, ruleNameIndex);
    }

    /**
     * Creates a new RuleComponentSuggestion for event variable names.
     *
     * @param needQuote     whether to wrap suggestions in double quotes
     * @param ruleNameIndex the argument index of the rule name in the command input
     * @return a new RuleComponentSuggestion for variable suggestions
     */
    public static RuleComponentSuggestion suggestVariable(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.VARIABLE, ruleNameIndex);
    }

    /**
     * Creates a new RuleComponentSuggestion for placeholder expressions ({@code ${var:name}}).
     * Quotes are never applied for placeholder suggestions.
     *
     * @param ruleNameIndex the argument index of the rule name in the command input
     * @return a new RuleComponentSuggestion for placeholder suggestions
     */
    public static RuleComponentSuggestion suggestPlaceholder(int ruleNameIndex) {
        return new RuleComponentSuggestion(false, SuggestionType.PLACEHOLDER, ruleNameIndex);
    }

    /**
     * Creates a new RuleComponentSuggestion for boolean expression (formula) autocompletion.
     * Quotes are never applied for formula suggestions.
     *
     * @param ruleNameIndex the argument index of the rule name in the command input
     * @return a new RuleComponentSuggestion for formula suggestions
     */
    public static RuleComponentSuggestion suggestFormula(int ruleNameIndex) {
        return new RuleComponentSuggestion(false, SuggestionType.FORMULA, ruleNameIndex);
    }
}
