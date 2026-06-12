/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.commands.CommandSourceStack;

/**
 * Provides command auto-completion suggestions for custom rules registered at runtime.
 * This suggestion provider retrieves rules from the server data and filters them by event type,
 * enabled status, and other criteria before offering them as suggestions.
 */
public class CustomRuleSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * Whether to wrap suggested rule names in double quotes.
     */
    private final boolean needQuote;

    /**
     * Whether to include a wildcard "*" option in suggestions.
     */
    private final boolean allowAll;

    /**
     * If non-null, only rules whose event type matches this filter will be suggested.
     */
    private final String filter;

    /**
     * Whether to only suggest rules that are currently enabled.
     */
    private final boolean enabledOnly;

    /**
     * Constructs a new CustomRuleSuggestion with the specified filtering options.
     *
     * @param needQuote   whether to wrap suggestions in double quotes
     * @param allowAll    whether to include a wildcard "*" option
     * @param filter      optional event type filter; only rules with this event type are suggested (null for no filter)
     * @param enabledOnly whether to only suggest enabled rules
     */
    public CustomRuleSuggestion(boolean needQuote, boolean allowAll, String filter, boolean enabledOnly) {
        this.needQuote = needQuote;
        this.allowAll = allowAll;
        this.filter = filter;
        this.enabledOnly = enabledOnly;
    }

    /**
     * Provides suggestions for custom rules based on the current input.
     * Suggests all matching rule names that start with the remaining input text,
     * subject to the filter and enabled-only settings configured in this instance.
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
            Map<String, RuleManager> customRules = Util.getServerData(context.getSource().getServer()).getCustomRules();
            Collection<String> ruleNames = customRules.keySet();
            for (String ruleName : ruleNames) {
                String suggestion = ruleName;
                RuleManager manager = customRules.get(ruleName);
                if (filter != null) {
                    if (!filter.equals(manager.getRule().getEvent().getType())) {
                        continue;
                    }
                }
                if (enabledOnly && !manager.isEnabled()) {
                    continue;
                }
                if (needQuote) {
                    suggestion = "\"" + suggestion + "\"";
                }
                if (suggestion.startsWith(builder.getRemaining())) {
                    builder.suggest(suggestion);
                }
            }
            if (allowAll && "*".startsWith(builder.getRemaining())) {
                builder.suggest("*");
            }
            return builder.build();
        });
    }

    /**
     * Creates a new CustomRuleSuggestion for general-purpose rule name suggestions.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @return a new CustomRuleSuggestion instance with no event filter and wildcard disabled
     */
    public static CustomRuleSuggestion suggest(boolean needQuote) {
        return new CustomRuleSuggestion(needQuote, false, null, false);
    }

    /**
     * Creates a new CustomRuleSuggestion for use with save commands.
     * Includes the wildcard "*" option to allow saving all rules at once.
     *
     * @return a new CustomRuleSuggestion instance configured for save operations
     */
    public static CustomRuleSuggestion suggestSave() {
        return new CustomRuleSuggestion(false, true, null, false);
    }
    
}
