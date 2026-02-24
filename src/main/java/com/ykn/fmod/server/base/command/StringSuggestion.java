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

import net.minecraft.commands.CommandSourceStack;

/**
 * Provides command auto-completion suggestions from a custom collection of strings.
 * This is a generic suggestion provider that can suggest any collection of strings
 * with optional quote wrapping for command auto-completion.
 */
public class StringSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * The collection of strings to suggest.
     * Marked {@code volatile} so that a reference swap performed by
     * {@link #update(Collection)} on the server thread is immediately visible
     * to the network thread calling {@link #getSuggestions}.
     */
    private volatile Collection<String> stringList;
    
    /**
     * Whether to wrap suggestions in double quotes.
     */
    private final boolean needQuote;

    /**
     * Constructs a new StringSuggestion with the specified string collection and quote setting.
     *
     * @param list the collection of strings to suggest
     * @param needQuote whether to wrap suggestions in double quotes
     */
    public StringSuggestion(Collection<String> list, boolean needQuote) {
        this.stringList = list;
        this.needQuote = needQuote;
    }

    /**
     * Provides suggestions from the string collection based on the current input.
     * Suggests all strings that start with the remaining input text.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String item : stringList) {
            String suggestion = item;
            if (needQuote) {
                suggestion = "\"" + suggestion + "\"";
            }
            if (suggestion.startsWith(builder.getRemaining())) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Creates a new StringSuggestion instance with the specified configuration.
     *
     * @param list the collection of strings to suggest
     * @param needQuote whether to wrap suggestions in double quotes
     * @return a new StringSuggestion instance
     */
    public static StringSuggestion suggest(Collection<String> list, boolean needQuote) {
        return new StringSuggestion(list, needQuote);
    }

    /**
     * Updates the string collection for this suggestion provider.
     *
     * @param list the new collection of strings to suggest
     * @return this StringSuggestion instance for method chaining
     */
    public StringSuggestion update(Collection<String> list) {
        this.stringList = list;
        return this;
    }
}
