/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Provides command auto-completion suggestions for .nbs (Note Block Song) files in the config directory.
 * This suggestion provider scans the mod's config directory and caches the list of available
 * .nbs files for use in command auto-completion.
 */
public class SongFileSuggestion implements SuggestionProvider<CommandSourceStack> {

    /**
     * Static cache of available .nbs file names in the config directory.
     * Marked {@code volatile} so that the reference update performed by the constructor
     * (which runs on the server thread) is immediately visible to the network thread that
     * calls {@link #getSuggestions}.
     */
    private static volatile ArrayList<String> cachedSongList = new ArrayList<>();

    /**
     * Constructs a new SongFileSuggestion and refreshes the cached list of .nbs files.
     * Scans the mod's config directory for files with the .nbs extension.
     * <p>
     * The new list is built into a local variable first and then assigned to
     * {@code cachedSongList} in a single write, so the network thread never sees
     * a partially-populated list.
     */
    public SongFileSuggestion() {
        // Build the list locally to avoid exposing a partially-populated ArrayList
        // to the network thread that may call getSuggestions() concurrently.
        ArrayList<String> newList = new ArrayList<>();
        Path absPath = FMLPaths.CONFIGDIR.get().resolve(Util.MODID);
        try {
            if (!Files.exists(absPath)) {
                Files.createDirectories(absPath);
            }
            try (Stream<Path> stream = Files.list(absPath)) {
                stream.filter(path -> path.toString().endsWith(".nbs")).forEach(path -> {
                    newList.add(path.getFileName().toString());
                });
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Error while getting .nbs file list", e);
        }
        // Single volatile write – the network thread either sees the old complete list
        // or the new complete list, never a half-populated one.
        cachedSongList = newList;
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
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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
