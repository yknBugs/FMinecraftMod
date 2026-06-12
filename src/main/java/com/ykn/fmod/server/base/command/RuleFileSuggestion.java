/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.commands.CommandSourceStack;

/**
 * Provides command auto-completion suggestions for .rule files in the config directory.
 * This suggestion provider scans the mod's config directory and caches the list of available
 * .rule files for use in command auto-completion.
 */
public class RuleFileSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * Static cache of available .rule file names in the config directory.
     * Marked {@code volatile} so that the reference update performed by the constructor
     * (which runs on the server thread) is immediately visible to the network thread that
     * calls {@link #getSuggestions}.
     */
    private static volatile ArrayList<String> cachedRuleList = new ArrayList<>();

    /**
     * Constructs a new RuleFileSuggestion and refreshes the cached list of .rule files.
     * Scans the mod's config directory for files with the .rule extension.
     * <p>
     * The new list is built into a local variable first and then assigned to
     * {@code cachedRuleList} in a single write, so the network thread never sees
     * a partially-populated list.
     */
    public RuleFileSuggestion() {
        ArrayList<String> newList = new ArrayList<>();
        Path absPath = Util.getConfigDir();
        try {
            if (!Files.exists(absPath)) {
                Files.createDirectories(absPath);
            }
            try (Stream<Path> stream = Files.list(absPath)) {
                stream.filter(path -> path.toString().endsWith(".rule")).forEach(path -> {
                    newList.add(path.getFileName().toString());
                });
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Error while getting .rule file list", e);
        }
        // Single volatile write – the network thread either sees the old complete list
        // or the new complete list, never a half-populated one.
        cachedRuleList = newList;
    }

    /**
     * Provides suggestions for .rule files based on the current input.
     * Suggests all cached .rule files that start with the remaining input text.
     * Also suggests "*" as a wildcard option.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String rule : cachedRuleList) {
            if (rule.startsWith(builder.getRemaining())) {
                builder.suggest(rule);
            }
        }
        if ("*".startsWith(builder.getRemaining())) {
            builder.suggest("*");
        }
        return builder.buildFuture();
    }
    
    /**
     * Creates a new RuleFileSuggestion instance.
     *
     * @return a new RuleFileSuggestion with refreshed .rule file cache
     */
    public static RuleFileSuggestion suggest() {
        return new RuleFileSuggestion();
    }

    /**
     * Returns the number of available .rule files in the cache.
     *
     * @return the count of cached .rule files
     */
    public static int getAvailableRules() {
        return cachedRuleList.size();
    }

    /**
     * Returns an unmodifiable list of cached .rule file names.
     *
     * @return an unmodifiable list of cached .rule file names
     */
    public static List<String> getCachedRuleList() {
        return Collections.unmodifiableList(cachedRuleList);
    }
}
