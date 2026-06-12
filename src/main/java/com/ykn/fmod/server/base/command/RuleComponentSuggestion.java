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
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.commands.CommandSourceStack;

public class RuleComponentSuggestion implements SuggestionProvider<CommandSourceStack> {

    private static enum SuggestionType {
        CONDITION,
        DOIFSATISFIED,
        DOIFVIOLATED,
        VARIABLE,
        PLACEHOLDER
    }

    private final boolean needQuote;

    private final SuggestionType type;

    private final int ruleNameIndex;

    private RuleComponentSuggestion(boolean needQuote, SuggestionType type, int ruleNameIndex) {
        this.needQuote = needQuote;
        this.type = type;
        this.ruleNameIndex = ruleNameIndex;
    }

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
                    default:
                        break;
                }
            }
            return builder.build();
        });
    }

    public static RuleComponentSuggestion suggestCondition(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.CONDITION, ruleNameIndex);
    }

    public static RuleComponentSuggestion suggestAction(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.DOIFSATISFIED, ruleNameIndex);
    }
    
    public static RuleComponentSuggestion suggestPunish(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.DOIFVIOLATED, ruleNameIndex);
    }

    public static RuleComponentSuggestion suggestVariable(boolean needQuote, int ruleNameIndex) {
        return new RuleComponentSuggestion(needQuote, SuggestionType.VARIABLE, ruleNameIndex);
    }

    public static RuleComponentSuggestion suggestPlaceholder(int ruleNameIndex) {
        return new RuleComponentSuggestion(false, SuggestionType.PLACEHOLDER, ruleNameIndex);
    }
}
