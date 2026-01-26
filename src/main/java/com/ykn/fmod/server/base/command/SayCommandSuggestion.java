/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.util.LinkedHashMap;
import java.util.LinkedList;
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

import net.minecraft.commands.CommandSourceStack;

public class SayCommandSuggestion implements SuggestionProvider<CommandSourceStack> {

    private Map<String, List<String>> suggestionsMap;

    public SayCommandSuggestion() {
        this.suggestionsMap = new LinkedHashMap<>();
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
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
     * Adds a value to a list associated with a specific key in the given map. 
     * If the key does not exist in the map, a new list is created and added to the map.
     *
     * @param <K>  The type of keys in the map.
     * @param <T>  The type of elements in the list.
     * @param map  The map where the key-value pair will be added.
     * @param key  The key to which the value should be associated.
     * @param value The value to be added to the list associated with the key.
     * @return The updated map with the new key-value association.
     */
    public static <K, T> Map<K, List<T>> put(Map<K, List<T>> map, K key, T value) {
        List<T> list = map.get(key);
        if (list == null) {
            list = new LinkedList<>();
            map.put(key, list);
        }
        list.add(value);
        return map;
    }

    public SayCommandSuggestion add(String key, List<String> suggestions) {
        if (key == null || suggestions == null || suggestions.isEmpty()) {
            return this;
        }
        suggestionsMap.put(key, suggestions);
        return this;
    }

    public SayCommandSuggestion add(String key, String... suggestions) {
        if (key == null || suggestions == null || suggestions.length == 0) {
            return this;
        }
        for (String suggestion : suggestions) {
            if (suggestion != null && !suggestion.isEmpty()) {
                put(this.suggestionsMap, key, suggestion);
            }
        }
        return this;
    }

    public SayCommandSuggestion clear() {
        this.suggestionsMap.clear();
        return this;
    }

    public static SayCommandSuggestion suggest() {
        return new SayCommandSuggestion();
    }

    public static SayCommandSuggestion suggestDefault() {
        return new SayCommandSuggestion()
                .add("&", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "a", "b", "c", "d", "e", "f", "r", "l", "k", "m", "n", "o")
                .add("${", "player}", "health}", "hp}", "maxhealth}", "maxhp}", "level}", "hunger}", "saturation}",
                     "x}", "y}", "z}", "biome}", "pitch}", "yaw}", "coord}", "mainhand}", "offhand}", "color:", "link:", "copy:", "hint:");
    }
}
