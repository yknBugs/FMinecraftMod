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

    public LogicFlowSuggestion() {

    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Collection<String> flows = Util.getServerData(context.getSource().getServer()).logicFlows.keySet();
        for (String flow : flows) {
            if (flow.startsWith(builder.getRemaining())) {
                builder.suggest(flow);
            }
        }
        return builder.buildFuture();
    }

    public static LogicFlowSuggestion suggest() {
        return new LogicFlowSuggestion();
    }

}
