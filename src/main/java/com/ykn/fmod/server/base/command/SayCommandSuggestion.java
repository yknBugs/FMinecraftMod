/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Provides command auto-completion suggestions for say commands with special formatting.
 * This suggestion provider maintains a map of keys to suggestion lists, enabling
 * context-aware auto-completion for formatting codes (e.g., "&" for color codes,
 * "${" for variable placeholders).
 */
public class SayCommandSuggestion implements SuggestionProvider<ServerCommandSource> {

    /**
     * Map storing keys and their associated suggestion lists.
     * Keys represent special formatting prefixes (e.g., "&", "${").
     */
    private Map<String, List<String>> suggestionsMap;

    /**
     * Constructs a new SayCommandSuggestion with an empty suggestions map.
     */
    public SayCommandSuggestion() {
        this.suggestionsMap = new LinkedHashMap<>();
    }

    /**
     * Provides suggestions based on the current input and the suggestions map.
     * Analyzes the input to find matching keys and suggests completions.
     *
     * @param context the command context
     * @param builder the suggestions builder
     * @return a completable future containing the suggestions
     */
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String input = builder.getRemaining();
        List<String> suggestions = generateSuggestionList(input);
        suggestions.forEach(suggestion -> builder.suggest(input + suggestion));
        return builder.buildFuture();
    }

    /**
     * Generates a list of suggestions based on the given input string.
     *
     * @param input The input string for which suggestions are to be generated. 
     *              It can be null or empty, in which case an empty list is returned.
     * @return A list of suggestions that match the input string. If no matching 
     *         suggestions are found, an empty list is returned.
     */
    @NotNull
    public List<String> generateSuggestionList(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        String key = findLongestMatchingKey(input);
        if (key == null) {
            return List.of();
        }

        List<String> suggestions = suggestionsMap.getOrDefault(key, List.of());
        int position = Math.min(input.length(), input.lastIndexOf(key) + key.length());
        String suffix = input.substring(position);
        List<String> result = suggestions.stream()
                .filter(suggestion -> suggestion.startsWith(suffix))
                .map(suggestion -> suggestion.substring(suffix.length()))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Finds the longest matching key from the suggestions map that is a substring of the given input.
     * The method searches for keys in the map that appear in the input string and returns the key
     * that ends at the farthest position in the input. If multiple keys end at the same position,
     * the longest key is returned.
     *
     * @param input The input string to search for matching keys. Can be null or empty.
     * @return The longest matching key that ends at the farthest position in the input,
     *         or null if no matching key is found or if the input is null/empty.
     */
    @Nullable
    public String findLongestMatchingKey(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String result = null;
        int position = 0;
        for (String key : suggestionsMap.keySet()) {
            int keyPosition = input.lastIndexOf(key);
            if (keyPosition < 0) {
                continue;
            }
            keyPosition += key.length();
            if (keyPosition > position) {
                position = keyPosition;
                result = key;
            } else if (keyPosition == position) {
                if (result == null || result.length() < key.length()) {
                    position = keyPosition;
                    result = key;
                }
            }
        }
        return result;
    }

    /**
     * Adds a list of suggestions associated with a specific key.
     *
     * @param key the key (formatting prefix) to associate with the suggestions
     * @param suggestions the list of suggestion values
     * @return this SayCommandSuggestion instance for method chaining
     */
    public SayCommandSuggestion add(String key, List<String> suggestions) {
        if (key == null || suggestions == null || suggestions.isEmpty()) {
            return this;
        }
        suggestionsMap.put(key, suggestions);
        return this;
    }

    /**
     * Adds multiple suggestions associated with a specific key using varargs.
     *
     * @param key the key (formatting prefix) to associate with the suggestions
     * @param suggestions variable number of suggestion values
     * @return this SayCommandSuggestion instance for method chaining
     */
    public SayCommandSuggestion add(String key, String... suggestions) {
        if (key == null || suggestions == null || suggestions.length == 0) {
            return this;
        }
        for (String suggestion : suggestions) {
            if (suggestion != null && !suggestion.isEmpty()) {
                this.suggestionsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(suggestion);
            }
        }
        return this;
    }

    /**
     * Clears all suggestions from the suggestions map.
     *
     * @return this SayCommandSuggestion instance for method chaining
     */
    public SayCommandSuggestion clear() {
        this.suggestionsMap.clear();
        return this;
    }

    /**
     * Creates a new empty SayCommandSuggestion instance.
     *
     * @return a new SayCommandSuggestion with an empty suggestions map
     */
    public static SayCommandSuggestion suggest() {
        return new SayCommandSuggestion();
    }

    /**
     * Creates a new SayCommandSuggestion instance with default suggestions.
     * Includes suggestions for color codes ("&") and variable placeholders ("${").
     *
     * @return a new SayCommandSuggestion with default formatting suggestions
     */
    public static SayCommandSuggestion suggestDefault() {
        return new SayCommandSuggestion()
                .add("&", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "a", "b", "c", "d", "e", "f", "r", "l", "k", "m", "n", "o")
                .add("${", "player}", "health}", "hp}", "maxhealth}", "maxhp}", "level}", "hunger}", "saturation}",
                     "x}", "y}", "z}", "biome}", "pitch}", "yaw}", "coord}", "mainhand}", "offhand}", "color:", "link:", 
                     "copy:", "hint:", "suggest:", "markdown}");
    }
}
