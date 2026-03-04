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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;

public class RuleFileSuggestion implements SuggestionProvider<ServerCommandSource> {

    private static volatile ArrayList<String> cachedRuleList = new ArrayList<>();

    public RuleFileSuggestion() {
        ArrayList<String> newList = new ArrayList<>();
        Path absPath = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID);
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
        cachedRuleList = newList;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
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
    
    public static RuleFileSuggestion suggest() {
        return new RuleFileSuggestion();
    }

    public static int getAvailableRules() {
        return cachedRuleList.size();
    }

    public static List<String> getCachedRuleList() {
        return Collections.unmodifiableList(cachedRuleList);
    }
}
