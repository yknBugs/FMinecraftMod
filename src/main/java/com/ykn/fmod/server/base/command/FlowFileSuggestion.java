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
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Provides command auto-completion suggestions for .flow files in the config directory.
 * This suggestion provider scans the mod's config directory and caches the list of available
 * .flow files for use in command auto-completion.
 */
public class FlowFileSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * Static cache of available .flow file names in the config directory.
     * Marked {@code volatile} so that the reference update performed by the constructor
     * (which runs on the server thread) is immediately visible to the network thread that
     * calls {@link #getSuggestions}.
     */
    private static volatile ArrayList<String> cachedFlowList = new ArrayList<>();

    /**
     * Constructs a new FlowFileSuggestion and refreshes the cached list of .flow files.
     * Scans the mod's config directory for files with the .flow extension.
     * <p>
     * The new list is built into a local variable first and then assigned to
     * {@code cachedFlowList} in a single write, so the network thread never sees
     * a partially-populated list.
     */
    public FlowFileSuggestion() {
        // Build the list locally to avoid exposing a partially-populated ArrayList
        // to the network thread that may call getSuggestions() concurrently.
        ArrayList<String> newList = new ArrayList<>();
        Path absPath = FMLPaths.CONFIGDIR.get().resolve(Util.MODID);
        try {
            if (!Files.exists(absPath)) {
                Files.createDirectories(absPath);
            }
            try (Stream<Path> stream = Files.list(absPath)) {
                stream.filter(path -> path.toString().endsWith(".flow")).forEach(path -> {
                    newList.add(path.getFileName().toString());
                });
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Error while getting .flow file list", e);
        }
        // Single volatile write – the network thread either sees the old complete list
        // or the new complete list, never a half-populated one.
        cachedFlowList = newList;
    }

    /**
     * Provides suggestions for .flow files based on the current input.
     * Suggests all cached .flow files that start with the remaining input text.
     * Also suggests "*" as a wildcard option.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String flow : cachedFlowList) {
            if (flow.startsWith(builder.getRemaining())) {
                builder.suggest(flow);
            }
        }
        if ("*".startsWith(builder.getRemaining())) {
            builder.suggest("*");
        }
        return builder.buildFuture();
    }

    /**
     * Creates a new FlowFileSuggestion instance.
     *
     * @return a new FlowFileSuggestion with refreshed .flow file cache
     */
    public static FlowFileSuggestion suggest() {
        return new FlowFileSuggestion();
    }

    /**
     * Returns the number of available .flow files in the cache.
     *
     * @return the count of cached .flow files
     */
    public static int getAvailableFlows() {
        return cachedFlowList.size();
    }

    /**
     * Returns an unmodifiable list of cached .flow file names.
     *
     * @return an unmodifiable list of cached .flow file names
     */
    public static List<String> getCachedFlowList() {
        return Collections.unmodifiableList(cachedFlowList);
    }
}
