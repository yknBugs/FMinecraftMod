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
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Provides command auto-completion suggestions for flow node names within a specific flow.
 * This suggestion provider extracts the flow name from the command input and suggests
 * available node names from that flow. Supports optional quote wrapping for suggestions.
 */
public class FlowNodeSuggestion implements SuggestionProvider<ServerCommandSource> {

    /**
     * Whether to wrap suggestions in double quotes.
     */
    private final boolean needQuote;
    
    /**
     * The index position of the flow name in the command input.
     * For example, in "/f flow edit "<flowName>" ...", the index is 3.
     */
    private final int flowNameIndex;

    /**
     * Constructs a new FlowNodeSuggestion with the specified configuration.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @param flowNameIndex the index position of the flow name in the command
     */
    public FlowNodeSuggestion(boolean needQuote, int flowNameIndex) {
        this.needQuote = needQuote;
        this.flowNameIndex = flowNameIndex;
    }

    /**
     * Extracts the flow name from the command input string.
     * Handles flow names with or without quotes. If the flow name is quoted,
     * it extracts the content between the quotes, including spaces.
     *
     * @param input the full command input string
     * @return the extracted flow name, or an empty string if not found
     */
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

    /**
     * Provides suggestions for flow node names based on the extracted flow name and current input.
     * Retrieves the flow from the server data and suggests all node names that start with
     * the remaining input text.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
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

    /**
     * Creates a new FlowNodeSuggestion instance with the specified configuration.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @param flowNameIndex the index position of the flow name in the command
     * @return a new FlowNodeSuggestion instance
     */
    public static FlowNodeSuggestion suggest(boolean needQuote, int flowNameIndex) {
        return new FlowNodeSuggestion(needQuote, flowNameIndex);
    }
}
