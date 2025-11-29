package com.ykn.fmod.server.base.command;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

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

    private final boolean needQuote;
    private final int flowNameIndex; // /f flow edit "<flowName>" ...: index 3

    public FlowNodeSuggestion(boolean needQuote, int flowNameIndex) {
        this.needQuote = needQuote;
        this.flowNameIndex = flowNameIndex;
    }

    private String extractFlowName(String input) {
        String[] parts = input.split(" ");
        if (parts.length > flowNameIndex) {
            String flowNamePart = parts[flowNameIndex];
            if (flowNamePart.startsWith("\"")) {
                // Optional spaces inside quotes
                int endQuoteIndex = input.indexOf("\"", input.indexOf(flowNamePart) + 1);
                if (endQuoteIndex != -1) {
                    return input.substring(input.indexOf(flowNamePart) + 1, endQuoteIndex);
                } else {
                    return flowNamePart.substring(1); // No ending quote yet
                }
            } else {
                return flowNamePart;
            }
        }
        return "";
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String flowName = extractFlowName(builder.getInput());
        FlowManager flow = Util.getServerData(context.getSource().getServer()).logicFlows.get(flowName);
        if (flow != null) {
            Collection<FlowNode> nodeNames = flow.flow.getNodes();
            for (FlowNode node : nodeNames) {
                String suggestion = node.name;
                if (needQuote) {
                    suggestion = "\"" + suggestion + "\"";
                }
                if (suggestion.startsWith(builder.getRemaining())) {
                    builder.suggest(suggestion);
                }
            }
        }
        return builder.buildFuture();
    }

    public static FlowNodeSuggestion suggest(boolean needQuote, int flowNameIndex) {
        return new FlowNodeSuggestion(needQuote, flowNameIndex);
    }
}
