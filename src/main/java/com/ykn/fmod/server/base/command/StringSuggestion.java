package com.ykn.fmod.server.base.command;

import java.util.ArrayList;
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

    public StringSuggestion(Collection<String> list) {
        this.stringList = list;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String item : stringList) {
            if (item.startsWith(builder.getRemaining())) {
                builder.suggest(item);
            }
        }
        return builder.buildFuture();
    }

    public static StringSuggestion suggest(Collection<String> list) {
        return new StringSuggestion(list);
    }

    public static StringSuggestion suggestSelf() {
        Collection<String> self = new ArrayList<>();
        self.add("this.server");
        self.add("this.entity");
        self.add("this.position");
        self.add("this.world");
        self.add("this.displayName");
        self.add("this.name");
        self.add("null");
        self.add("true");
        self.add("false");
        return new StringSuggestion(self);
    }

    public StringSuggestion update(Collection<String> list) {
        this.stringList = list;
        return this;
    }
}
