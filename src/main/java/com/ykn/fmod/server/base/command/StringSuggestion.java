package com.ykn.fmod.server.base.command;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;

public class StringSuggestion implements SuggestionProvider<ServerCommandSource> {

    public Collection<String> stringList;
    private final boolean needQuote;

    public StringSuggestion(Collection<String> list, boolean needQuote) {
        this.stringList = list;
        this.needQuote = needQuote;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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

    public static StringSuggestion suggest(Collection<String> list, boolean needQuote) {
        return new StringSuggestion(list, needQuote);
    }

    public StringSuggestion update(Collection<String> list) {
        this.stringList = list;
        return this;
    }
}
