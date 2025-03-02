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

public class SongFileSuggestion implements SuggestionProvider<ServerCommandSource> {

    public ArrayList<String> cachedSongList;

    public SongFileSuggestion() {
        this.cachedSongList = new ArrayList<>();
        Path absPath = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID);
        try {
            if (!Files.exists(absPath)) {
                Files.createDirectories(absPath);
            }
            Files.list(absPath).filter(path -> path.toString().endsWith(".nbs")).forEach(path -> {
                this.cachedSongList.add(path.getFileName().toString());
            });
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.MODID).error("Error while getting .nbs file list", e);
        }
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        for (String song : this.cachedSongList) {
            if (song.startsWith(builder.getRemaining())) {
                builder.suggest(song);
            }
        }
        return builder.buildFuture();
    }

    public static SongFileSuggestion suggest() {
        return new SongFileSuggestion();
    }

    public static int getAvailableSongs() {
        return suggest().cachedSongList.size();
    }
}
