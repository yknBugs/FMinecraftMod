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
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Provides command auto-completion suggestions for logic flow names.
 * This suggestion provider retrieves all loaded logic flows from the server data
 * and suggests their names for command auto-completion. Supports optional quote wrapping
 * and wildcard "*" suggestion.
 */
public class LogicFlowSuggestion implements SuggestionProvider<ServerCommandSource> {

    /**
     * Whether to wrap suggestions in double quotes.
     */
    private final boolean needQuote;
    
    /**
     * Whether to allow "*" as a wildcard suggestion.
     */
    private final boolean allowAll;

    /**
     * An optional filter to limit suggestions to specific types of logic flows.
     */
    private final String filter;

    /**
     * Whether to suggest only enabled logic flows.
     */
    private final boolean enabledOnly;

    /**
     * Constructs a new LogicFlowSuggestion with the specified configuration.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @param allowAll whether to allow "*" as a wildcard suggestion
     */
    public LogicFlowSuggestion(boolean needQuote, boolean allowAll, String filter, boolean enabledOnly) {
        this.needQuote = needQuote;
        this.allowAll = allowAll;
        this.filter = filter;
        this.enabledOnly = enabledOnly;
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
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Collection<String> flows = Util.getServerData(context.getSource().getServer()).logicFlows.keySet();
        for (String flow : flows) {
            String suggestion = flow;
            FlowManager manager = Util.getServerData(context.getSource().getServer()).logicFlows.get(flow);
            if (filter != null) {
                if (manager.flow.getFirstNode() == null || filter.equals(manager.flow.getFirstNode().getType()) == false) {
                    continue;
                }
            }
            if (enabledOnly && manager.isEnabled == false) {
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
        return builder.buildFuture();
    }

    /**
     * Creates a new LogicFlowSuggestion instance with quote wrapping option.
     * The wildcard "*" suggestion is disabled. There is no filter applied and both enabled and disabled flows are suggested.
     *
     * @param needQuote whether to wrap suggestions in double quotes
     * @return a new LogicFlowSuggestion instance
     */
    public static LogicFlowSuggestion suggest(boolean needQuote) {
        return new LogicFlowSuggestion(needQuote, false, null, false);
    }

    /**
     * Creates a new LogicFlowSuggestion instance for save operations.
     * Quote wrapping is disabled and wildcard "*" suggestion is enabled.
     * There is no filter applied and both enabled and disabled flows are suggested.
     *
     * @return a new LogicFlowSuggestion instance for save operations
     */
    public static LogicFlowSuggestion suggestSave() {
        return new LogicFlowSuggestion(false, true, null, false);
    }

    /**
     * Creates a new LogicFlowSuggestion instance for trigger operations.
     * Quote wrapping and wildcard "*" suggestion are both disabled.
     * There is a filter applied for "TriggerNode" and only enabled flows are suggested.
     * 
     * @return a new LogicFlowSuggestion instance for trigger operations
     */
    public static LogicFlowSuggestion suggestTrigger() {
        return new LogicFlowSuggestion(false, false, "TriggerNode", true);
    }
}
