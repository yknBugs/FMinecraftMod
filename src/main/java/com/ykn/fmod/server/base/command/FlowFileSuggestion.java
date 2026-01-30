/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ykn.fmod.server.base.util.Util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Provides command auto-completion suggestions for .flow files in the config directory.
 * This suggestion provider scans the mod's config directory and caches the list of available
 * .flow files for use in command auto-completion.
 */
public class FlowFileSuggestion implements SuggestionProvider<ServerCommandSource> {

    /**
     * Static cache of available .flow file names in the config directory.
     */
    public static ArrayList<String> cachedFlowList = new ArrayList<>();

    /**
     * Constructs a new FlowFileSuggestion and refreshes the cached list of .flow files.
     * Scans the mod's config directory for files with the .flow extension.
     */
    public FlowFileSuggestion() {
        // Refresh the list of .flow files in the config directory
        cachedFlowList = new ArrayList<>();
        Path absPath = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID);
        try {
            if (!Files.exists(absPath)) {
                Files.createDirectories(absPath);
            }
            Files.list(absPath).filter(path -> path.toString().endsWith(".flow")).forEach(path -> {
                cachedFlowList.add(path.getFileName().toString());
            });
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Error while getting .flow file list", e);
        }
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
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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
}
