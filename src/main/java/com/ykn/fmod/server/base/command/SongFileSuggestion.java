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
 * Provides command auto-completion suggestions for .nbs (Note Block Song) files in the config directory.
 * This suggestion provider scans the mod's config directory and caches the list of available
 * .nbs files for use in command auto-completion.
 */
public class SongFileSuggestion implements SuggestionProvider<ServerCommandSource> {

    /**
     * Static cache of available .nbs file names in the config directory.
     */
    public static ArrayList<String> cachedSongList = new ArrayList<>();

    /**
     * Constructs a new SongFileSuggestion and refreshes the cached list of .nbs files.
     * Scans the mod's config directory for files with the .nbs extension.
     */
    public SongFileSuggestion() {
        // Refresh the list of .nbs files in the config directory
        cachedSongList = new ArrayList<>();
        Path absPath = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID);
        try {
            if (!Files.exists(absPath)) {
                Files.createDirectories(absPath);
            }
            Files.list(absPath).filter(path -> path.toString().endsWith(".nbs")).forEach(path -> {
                cachedSongList.add(path.getFileName().toString());
            });
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Error while getting .nbs file list", e);
        }
    }

    /**
     * Provides suggestions for .nbs files based on the current input.
     * Suggests all cached .nbs files that start with the remaining input text.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     * @throws CommandSyntaxException if there's a syntax error in the command
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String song : cachedSongList) {
            if (song.startsWith(builder.getRemaining())) {
                builder.suggest(song);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Creates a new SongFileSuggestion instance.
     *
     * @return a new SongFileSuggestion with refreshed .nbs file cache
     */
    public static SongFileSuggestion suggest() {
        return new SongFileSuggestion();
    }

    /**
     * Returns the number of available .nbs files in the cache.
     *
     * @return the count of cached .nbs files
     */
    public static int getAvailableSongs() {
        return cachedSongList.size();
    }
}
