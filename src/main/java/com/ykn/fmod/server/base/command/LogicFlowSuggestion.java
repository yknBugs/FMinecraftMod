/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.commands.CommandSourceStack;

/**
 * Provides command auto-completion suggestions for logic flow names.
 * This suggestion provider retrieves all loaded logic flows from the server data
 * and suggests their names for command auto-completion. Supports optional quote wrapping
 * and wildcard "*" suggestion.
 */
public class LogicFlowSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * Whether to wrap suggestions in double quotes.
     */
    private final boolean needQuote;
    
    /**
     * Whether to allow "*" as a wildcard suggestion.
     */
    private final boolean allowAll;

    /**
     * Constructs a new LogicFlowSuggestion with the specified configuration.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @param allowAll whether to allow "*" as a wildcard suggestion
     */
    public LogicFlowSuggestion(boolean needQuote, boolean allowAll) {
        this.needQuote = needQuote;
        this.allowAll = allowAll;
    }

    /**
     * Provides suggestions for logic flow names based on the current input.
     * Suggests all loaded flow names that start with the remaining input text.
     * If allowAll is true, also suggests "*" as a wildcard option.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Collection<String> flows = Util.getServerData(context.getSource().getServer()).logicFlows.keySet();
        for (String flow : flows) {
            String suggestion = flow;
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
        return builder.buildFuture();
    }

    /**
     * Creates a new LogicFlowSuggestion instance with quote wrapping option.
     * The wildcard "*" suggestion is disabled.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @return a new LogicFlowSuggestion instance
     */
    public static LogicFlowSuggestion suggest(boolean needQuote) {
        return new LogicFlowSuggestion(needQuote, false);
    }

    /**
     * Creates a new LogicFlowSuggestion instance for save operations.
     * Quote wrapping is disabled and wildcard "*" suggestion is enabled.
     *
     * @return a new LogicFlowSuggestion instance for save operations
     */
    public static LogicFlowSuggestion suggestSave() {
        return new LogicFlowSuggestion(false, true);
    }
}
