package com.ykn.fmod.server.base.command;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.command.ServerCommandSource;

public class FlowNodeSuggestion implements SuggestionProvider<ServerCommandSource> {

    public FlowNodeSuggestion() {

    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String flowName = StringArgumentType.getString(context, "name");
        FlowManager flow = Util.getServerData(context.getSource().getServer()).logicFlows.get(flowName);
        if (flow != null) {
            Collection<FlowNode> nodeNames = flow.flow.getNodes();
            for (FlowNode node : nodeNames) {
                builder.suggest(node.name);
            }
        }
        return builder.buildFuture();
    }

    public static FlowNodeSuggestion suggest() {
        return new FlowNodeSuggestion();
    }
}
