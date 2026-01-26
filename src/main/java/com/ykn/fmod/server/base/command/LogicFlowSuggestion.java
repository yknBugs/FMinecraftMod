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

import net.minecraft.server.command.ServerCommandSource;

public class LogicFlowSuggestion implements SuggestionProvider<ServerCommandSource> {

    private final boolean needQuote;
    private final boolean allowAll;

    public LogicFlowSuggestion(boolean needQuote, boolean allowAll) {
        this.needQuote = needQuote;
        this.allowAll = allowAll;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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

    public static LogicFlowSuggestion suggest(boolean needQuote) {
        return new LogicFlowSuggestion(needQuote, false);
    }

    public static LogicFlowSuggestion suggestSave() {
        return new LogicFlowSuggestion(false, true);
    }
}
