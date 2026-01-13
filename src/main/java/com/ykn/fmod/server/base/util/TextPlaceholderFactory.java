/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A utility class for managing and parsing text placeholders. This class allows
 * the creation of placeholder mappings, where placeholders in a text string can
 * be replaced with dynamically generated values based on an input object of type T.
 *
 * <p>Placeholders are defined as key-value pairs, where the key is a string
 * representing the placeholder, and the value is a function that generates a
 * {@link Component} object based on the input object of type T.</p>
 *
 * <p>The class provides methods for adding, removing, and clearing placeholders,
 * as well as for parsing text strings to replace placeholders with their resolved
 * values. Additionally, it includes utility methods for creating default placeholder
 * factories and splitting text with delimiters.</p>
 *
 * @param <T> The type of the object used to resolve placeholder values.
 */
public class TextPlaceholderFactory<T> {

    private HashMap<String, Function<T, Component>> placeholders;

    public TextPlaceholderFactory() {
        this.placeholders = new HashMap<>();
    }

    public TextPlaceholderFactory(HashMap<String, Function<T, Component>> placeholders) {
        this.placeholders = placeholders;
    }

    public TextPlaceholderFactory<T> add(String key, Function<T, Component> value) {
        this.placeholders.put(key, value);
        return this;
    }

    public TextPlaceholderFactory<T> add(String key, Component value) {
        this.placeholders.put(key, t -> value);
        return this;
    }

    public TextPlaceholderFactory<T> add(String key, String value) {
        this.placeholders.put(key, t -> Component.literal(value));
        return this;
    }

    public TextPlaceholderFactory<T> append(HashMap<String, Function<T, Component>> placeholders) {
        this.placeholders.putAll(placeholders);
        return this;
    }

    public TextPlaceholderFactory<T> append(TextPlaceholderFactory<T> placeholders) {
        this.placeholders.putAll(placeholders.placeholders);
        return this;
    }

    public TextPlaceholderFactory<T> clear() {
        this.placeholders.clear();
        return this;
    }

    public TextPlaceholderFactory<T> remove(String key) {
        this.placeholders.remove(key);
        return this;
    }

    /**
     * Splits the given text using the specified delimiter and appends the resulting parts
     * to the provided list. Optionally, the delimiter can be retained between the parts.
     *
     * @param list          The initial list to which the split parts will be appended.
     * @param text          The text to be split using the delimiter.
     * @param delimiter     The delimiter used to split the text.
     * @param keepDelimiter If true, the delimiter will be included between the split parts
     *                      in the resulting list.
     * @return A new list containing the original list elements followed by the split parts
     *         of the text, with or without the delimiter based on the keepDelimiter flag.
     */
    public static List<String> splitAndAppend(List<String> list, String text, String delimiter, boolean keepDelimiter) {
        List<String> result = new ArrayList<>(list);
        // We need to escape the delimiter for regex, so we use Pattern.quote
        String pattern = Pattern.quote(delimiter);
        // We need to preserve the last empty string if the text ends with the delimiter
        String[] parts = text.split(pattern, -1);
        for (int i = 0; i < parts.length; i++) {
            if (keepDelimiter && i > 0) {
                result.add(delimiter);
            }
            result.add(parts[i]);
        }
        return result;
    }

    /**
     * Parses the given text and replaces placeholders with their corresponding values
     * based on the provided mapping and the given object of type T.
     *
     * <p>The method processes the input text by identifying placeholders, splitting
     * the text into parts, and replacing placeholders with their resolved values.
     * Placeholders are identified by keys in the {@code placeholders} map, and their
     * corresponding values are generated using the associated functions.</p>
     *
     * @param text The input text containing placeholders to be replaced.
     * @param t    The object of type T used to resolve placeholder values.
     * @return A {@link MutableComponent} object representing the parsed and resolved text.
     */
    public MutableComponent parse(String text, T t) {
        List<String> keys = new ArrayList<>(this.placeholders.keySet());
        // We must sort the keys by length in descending order to avoid replacing smaller keys first
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        List<String> splitText = new ArrayList<>();
        splitText.add(text);
        for (String key : keys) {
            List<String> currentParts = new ArrayList<>();
            int index = 0;
            for (String part : splitText) {
                if (index % 2 == 0) {
                    // We keep the delimiter here, so the even indexes are the strings that should be splited
                    // and the odd indexes are the delimiters that should be kept
                    currentParts = splitAndAppend(currentParts, part, key, true);
                } else {
                    currentParts.add(part);
                }
                index++;
            }
            splitText = currentParts;
        }

        List<Component> finalText = new ArrayList<>();
        for (int i = 0; i < splitText.size(); i++) {
            String part = splitText.get(i);
            if (i % 2 == 0) {
                // Even indexes are the strings that should be splited
                finalText.add(Component.literal(part));
            } else {
                // Odd indexes are the delimiters that should be kept
                Function<T, Component> placeholderFunction = this.placeholders.get(part);
                if (placeholderFunction == null) {
                    // Unlikely to happen
                    LoggerFactory.getLogger(Util.LOGGERNAME).warn("Missing placeholder: " + part);
                    finalText.add(Component.literal(part));
                } else {
                    try {
                        finalText.add(placeholderFunction.apply(t));
                    } catch (Exception e) {
                        LoggerFactory.getLogger(Util.LOGGERNAME).error("Error while parsing placeholder: " + part, e);
                        finalText.add(Component.literal(part));
                    }
                }
            }
        }
        
        MutableComponent result = Component.empty();
        for (Component part : finalText) {
            result.append(part);
        }
        return result;
    }

    public static <U> TextPlaceholderFactory<U> empty() {
        return new TextPlaceholderFactory<U>();
    }

    public static <U> TextPlaceholderFactory<U> of(HashMap<String, Function<U, Component>> placeholders) {
        return new TextPlaceholderFactory<U>(placeholders);
    }

    public static <U> TextPlaceholderFactory<U> of(String key, Function<U, Component> value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    public static <U> TextPlaceholderFactory<U> of(String key, Component value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    public static <U> TextPlaceholderFactory<U> of(String key, String value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    /**
     * Creates a default {@link TextPlaceholderFactory} for {@link ServerPlayer} with predefined placeholders.
     * 
     * @return A {@link TextPlaceholderFactory} instance with the default placeholders for {@link ServerPlayer}.
     */
    public static TextPlaceholderFactory<ServerPlayer> ofDefault() {
        return new TextPlaceholderFactory<ServerPlayer>()
            .add("&", t -> Component.literal("\u00a7"))
            .add("${player}", t -> t.getDisplayName())
            .add("${health}", t -> Component.literal(String.format("%.2f", t.getHealth())))
            .add("${hp}", t -> Component.literal(String.format("%.2f", t.getHealth())))
            .add("${maxhealth}", t -> Component.literal(String.format("%.2f", t.getMaxHealth())))
            .add("${maxhp}", t -> Component.literal(String.format("%.2f", t.getMaxHealth())))
            .add("${level}", t -> Component.literal(String.valueOf(t.experienceLevel)))
            .add("${hunger}", t -> Component.literal(String.valueOf(t.getFoodData().getFoodLevel())))
            .add("${saturation}", t -> Component.literal(String.format("%.2f", t.getFoodData().getSaturationLevel())))
            .add("${x}", t -> Component.literal(String.format("%.2f", t.getX())))
            .add("${y}", t -> Component.literal(String.format("%.2f", t.getY())))
            .add("${z}", t -> Component.literal(String.format("%.2f", t.getZ())))
            .add("${pitch}", t -> Component.literal(String.format("%.2f", t.getXRot())))
            .add("${yaw}", t -> Component.literal(String.format("%.2f", t.getYRot())))
            .add("${biome}", t -> Util.getBiomeText(t))
            .add("${coord}", t -> {
                Component biomeText = Util.getBiomeText(t);
                String strX = String.format("%.2f", t.getX());
                String strY = String.format("%.2f", t.getY());
                String strZ = String.format("%.2f", t.getZ());
                Component x = Component.literal(strX);
                Component y = Component.literal(strY);
                Component z = Component.literal(strZ);
                MutableComponent result = Component.literal("[").append(biomeText).append("] (").append(x).append(", ").append(y).append(", ").append(z).append(")");
                result = result.withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + strX + " " + strY + " " + strZ)
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
                ));
                return result;
            })
            .add("${mainhand}", t -> {
                ItemStack item = t.getMainHandItem();
                if (item == null || item.isEmpty()) {
                    return Component.translatable("fmod.command.get.emptyslot");
                } else {
                    if (item.getCount() > 1) {
                        return Component.empty().append(item.getDisplayName()).append("x").append(Component.literal(String.valueOf(item.getCount())));
                    } else {
                        return item.getDisplayName();
                    }
                }
            })
            .add("${offhand}", t -> {
                ItemStack item = t.getOffhandItem();
                if (item == null || item.isEmpty()) {
                    return Component.translatable("fmod.command.get.emptyslot");
                } else {
                    if (item.getCount() > 1) {
                        return Component.empty().append(item.getDisplayName()).append("x").append(Component.literal(String.valueOf(item.getCount())));
                    } else {
                        return item.getDisplayName();
                    }
                }
            });
    }
}
