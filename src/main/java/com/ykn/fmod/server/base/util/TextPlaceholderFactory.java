/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * A factory for creating and parsing text with placeholders and custom styles.
 * <p>
 * This class provides a flexible system for:
 * <ul>
 *   <li>Defining placeholders that can be replaced with dynamic content</li>
 *   <li>Applying custom styles using regex patterns</li>
 *   <li>Parsing text strings to replace placeholders and apply styles</li>
 * </ul>
 * <p>
 * Placeholders are defined as key-value pairs where the key is a string pattern
 * and the value is a function that generates Text based on the context object.
 * Custom styles use regex patterns to match style markers and apply formatting.
 * <p>
 * Example usage:
 * <pre>{@code
 * TextPlaceholderFactory<ServerPlayerEntity> factory = TextPlaceholderFactory.ofDefault();
 * MutableText result = factory.parse("Hello ${player}! Your health is ${health}.", player);
 * }</pre>
 *
 * @param <T> The type of context object used for placeholder resolution
 */
public class TextPlaceholderFactory<T> {

    /**
     * Map of placeholder keys to their corresponding text generation functions.
     */
    private HashMap<String, Function<T, Component>> placeholders;

    /**
     * Map of custom style regex patterns to their corresponding style application functions.
     */
    private HashMap<String, BiFunction<String, MutableComponent, MutableComponent>> customStyles;

    /**
     * Constructs a new empty TextPlaceholderFactory with no placeholders or custom styles.
     */
    public TextPlaceholderFactory() {
        this.placeholders = new HashMap<>();
        // Must use LinkedHashMap to make sure the order is deterministic
        this.customStyles = new LinkedHashMap<>();
    }

    /**
     * Constructs a new TextPlaceholderFactory with the specified placeholders.
     *
     * @param placeholders A map of placeholder keys to their corresponding text generation functions
     */
    public TextPlaceholderFactory(HashMap<String, Function<T, Component>> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Adds a placeholder with a dynamic value function.
     *
     * @param key The placeholder key (e.g., "${player}")
     * @param value A function that generates Text based on the context object
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> add(String key, Function<T, Component> value) {
        this.placeholders.put(key, value);
        return this;
    }

    /**
     * Adds a placeholder with a static Text value.
     *
     * @param key The placeholder key (e.g., "${greeting}")
     * @param value The static Text value to use for this placeholder
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> add(String key, Component value) {
        this.placeholders.put(key, t -> value);
        return this;
    }

    /**
     * Adds a placeholder with a static string value.
     *
     * @param key The placeholder key (e.g., "${name}")
     * @param value The static string value to use for this placeholder
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> add(String key, String value) {
        this.placeholders.put(key, t -> Component.literal(value));
        return this;
    }

    /**
     * Adds a custom style pattern that can be applied to text.
     * <p>
     * The style pattern is a regex that matches style markers in the text.
     * When matched, the style function is applied to transform the text.
     * The first capturing group in the regex (if present) is passed as the
     * parameter to the style function.
     * <p>
     * Example: {@code .style("\\$\\{color:([0-9A-Fa-f]+)\\}", (color, text) -> text.styled(s -> s.withColor(Integer.parseInt(color, 16))))}
     *
     * @param styleRegex A regex pattern to match style markers in the text
     * @param styleFunction A function that applies the style, taking the captured parameter and the text to style
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> style(String styleRegex, BiFunction<String, MutableComponent, MutableComponent> styleFunction) {
        this.customStyles.put(styleRegex, styleFunction);
        return this;
    }

    /**
     * Appends all placeholders from the given map to this factory.
     * Existing placeholders with the same keys will be overwritten.
     *
     * @param placeholders A map of placeholders to add
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> append(HashMap<String, Function<T, Component>> placeholders) {
        this.placeholders.putAll(placeholders);
        return this;
    }

    /**
     * Appends all placeholders and custom styles from another factory to this factory.
     * Existing placeholders and styles with the same keys will be overwritten.
     *
     * @param placeholders The factory whose placeholders and styles to append
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> append(TextPlaceholderFactory<T> placeholders) {
        this.placeholders.putAll(placeholders.placeholders);
        this.customStyles.putAll(placeholders.customStyles);
        return this;
    }

    /**
     * Removes all placeholders and custom styles from this factory.
     *
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> clear() {
        this.placeholders.clear();
        this.customStyles.clear();
        return this;
    }

    /**
     * Removes a placeholder with the specified key.
     *
     * @param key The placeholder key to remove
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> remove(String key) {
        this.placeholders.remove(key);
        return this;
    }
    
    /**
     * Removes a custom style with the specified regex key.
     *
     * @param key The style regex key to remove
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> reset(String key) {
        this.customStyles.remove(key);
        return this;
    }

    /**
     * Replaces the element at the specified index in the given list with the elements from the newElements list.
     * @param <U> The type of elements in the list.
     * @param list The original list.
     * @param index The index of the element to be replaced.
     * @param newElements The list of new elements to insert at the specified index.
     * @return A new list with the element at the specified index replaced by the new elements.
     */
    public static <U> List<U> replaceElementToList(List<U> list, int index, List<U> newElements) {
        List<U> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i == index) {
                result.addAll(newElements);
            } else {
                result.add(list.get(i));
            }
        }
        return result;
    }

    /**
     * Expands the inner list at the given index by producing one result per element in
     * {@code newElements}. For each element {@code e} in {@code newElements}, the method
     * creates a shallow copy of {@code list.get(index)} with {@code e} appended and
     * includes it in the returned outer list. All other inner lists are included as
     * shallow copies in their original order.
     *
     * @param list the outer list containing inner lists to copy/expand (must not be null)
     * @param index the position of the inner list to expand; if out of range no expansion occurs
     * @param newElements elements to append to the selected inner list to create expanded variants (must not be null)
     * @param <U> the element type of the inner lists
     * @return a new outer list containing shallow-copied inner lists with the specified expansions applied
     */
    public static <U> List<List<U>> expandElementToList(List<List<U>> list, int index, List<U> newElements) {
        List<List<U>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i == index) {
                List<U> current = list.get(i);
                for (U element : newElements) {
                    List<U> copiedCurrent = new ArrayList<>(current);
                    if (element != null) {
                        copiedCurrent.add(element);
                    }
                    result.add(copiedCurrent);
                }
            } else {
                result.add(new ArrayList<>(list.get(i)));
            }
        }
        return result;
    }

    /**
     * Tokenizes the given text by splitting it based on custom style patterns.
     * <p>
     * This method splits the input text wherever a custom style pattern matches,
     * creating text tokens and tracking which styles apply to each token.
     * The results are accumulated into the provided lists.
     *
     * @param text The text to tokenize
     * @param textTokens Output list to receive the text fragments
     * @param styleTokens Output list to receive the style regex patterns applicable to each text fragment
     * @param styleParams Output list to receive the captured parameters from each style pattern match
     */
    public void tokenizeText(String text, List<String> textTokens, List<List<String>> styleTokens, List<List<String>> styleParams) {
        Set<String> keys = this.customStyles.keySet();
        List<String> splitText = new ArrayList<>();
        List<List<String>> splitStyles = new ArrayList<>();
        List<List<String>> splitParams = new ArrayList<>();
        splitText.add(text);
        splitStyles.add(new ArrayList<>());
        splitParams.add(new ArrayList<>());
        for (String styleRegex : keys) {
            Pattern pattern = Pattern.compile(styleRegex);
            for (int i = 0; i < splitText.size(); i++) {
                String part = splitText.get(i);
                Matcher matcher = pattern.matcher(part);
                int lastIndex = 0;

                // Split the part by the pattern
                List<String> splittedParts = new ArrayList<>();
                List<String> fullMatchedParts = new ArrayList<>();
                List<String> firstMatchingGroups = new ArrayList<>();
                // Make sure splittedParts and fullMatchedParts have the same length
                fullMatchedParts.add(null);
                firstMatchingGroups.add(null);
                while (matcher.find()) {
                    splittedParts.add(part.substring(lastIndex, matcher.start()));
                    fullMatchedParts.add(styleRegex);
                    firstMatchingGroups.add(matcher.groupCount() < 1 ? "" : matcher.group(1));
                    lastIndex = matcher.end();
                }
                splittedParts.add(part.substring(lastIndex));

                // Write back to splitText and splitStyles and update the index accordingly
                int indexOffset = splittedParts.size() - 1;
                splitText = replaceElementToList(splitText, i, splittedParts);
                splitStyles = expandElementToList(splitStyles, i, fullMatchedParts);
                splitParams = expandElementToList(splitParams, i, firstMatchingGroups);
                i += indexOffset;
            }
        }
        textTokens.addAll(splitText);
        styleTokens.addAll(splitStyles);
        styleParams.addAll(splitParams);
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
     * Parses and replaces all placeholders in the given text.
     * <p>
     * This method splits the text by placeholder keys and replaces each placeholder
     * with the result of its associated function. Placeholders are processed in order
     * of decreasing key length to avoid replacing smaller keys that are part of larger keys.
     * <p>
     * Note: This method does not apply custom styles. Use {@link #parse(String, Object)}
     * for full parsing with both placeholders and styles.
     *
     * @param text The text containing placeholders to replace
     * @param t The context object passed to placeholder functions
     * @return The parsed text with all placeholders replaced
     */
    public MutableComponent parsePlaceholders(String text, T t) {
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
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Missing placeholder: " + part);
                    finalText.add(Component.literal(part));
                } else {
                    try {
                        finalText.add(placeholderFunction.apply(t));
                    } catch (Exception e) {
                        LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Error while parsing placeholder: " + part, e);
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

    /**
     * Parses the given text by applying both custom styles and placeholder replacements.
     * <p>
     * This is the main parsing method that performs a two-step process:
     * <ol>
     *   <li>Tokenizes the text based on custom style patterns</li>
     *   <li>For each token, replaces placeholders and applies accumulated styles</li>
     * </ol>
     * <p>
     * Styles are applied in the order they appear in the text and accumulate from left to right.
     * Once a style is applied, it remains active for all subsequent text until the end.
     *
     * @param text The text to parse, containing both style markers and placeholders
     * @param t The context object passed to placeholder functions
     * @return The fully parsed and styled text
     */
    public MutableComponent parse(String text, T t) {
        // Tokenize the text first based on custom styles
        List<MutableComponent> finalTexts = new ArrayList<>();
        List<String> textTokens = new ArrayList<>();
        List<List<String>> styleTokens = new ArrayList<>();
        List<List<String>> styleParams = new ArrayList<>();
        this.tokenizeText(text, textTokens, styleTokens, styleParams);
        // Broadcast the styles from left to the end of the text
        HashMap<String, String> activeStyles = new LinkedHashMap<>();
        for (int i = 0; i < textTokens.size(); i++) {
            String currentText = textTokens.get(i);
            List<String> styles = styleTokens.get(i);
            List<String> params = styleParams.get(i);
            // Get the last style and param for this text token
            // Only the last style matters, because the previous styles are already saved in activeStyles
            String lastStyle = styles.size() > 0 ? styles.get(styles.size() - 1) : null;
            String lastParam = params.size() > 0 ? params.get(params.size() - 1) : null;
            if (lastStyle != null && lastParam != null) {
                if (activeStyles.containsKey(lastStyle)) {
                    // Remove the style if it is already active, to make sure the latest style is always at the end of the LinkedHashMap
                    activeStyles.remove(lastStyle);
                }
                activeStyles.put(lastStyle, lastParam);
            }
            
            // Apply the active styles to the text token
            MutableComponent textToken = parsePlaceholders(currentText, t);
            Set<String> styleKeys = activeStyles.keySet();
            for (String styleKey : styleKeys) {
                BiFunction<String, MutableComponent, MutableComponent> styleFunction = this.customStyles.get(styleKey);
                if (styleFunction == null) {
                    // Unlikely to happen
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Missing custom style: " + styleKey);
                    continue;
                }
                String param = activeStyles.get(styleKey);
                try {
                    textToken = styleFunction.apply(param, textToken);
                } catch (Exception e) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Error while applying custom style: " + styleKey + " with param: " + param, e);
                }
            }
            finalTexts.add(textToken);
        }
        // Combine all final texts
        MutableComponent result = Component.empty();
        for (Component part : finalTexts) {
            result.append(part);
        }
        return result;
    }

    /**
     * Creates a new empty TextPlaceholderFactory with no placeholders or styles.
     *
     * @param <U> The type of context object for placeholder resolution
     * @return A new empty factory instance
     */
    public static <U> TextPlaceholderFactory<U> empty() {
        return new TextPlaceholderFactory<U>();
    }

    /**
     * Creates a new TextPlaceholderFactory with the specified placeholders.
     *
     * @param <U> The type of context object for placeholder resolution
     * @param placeholders A map of placeholder keys to their corresponding text generation functions
     * @return A new factory instance with the given placeholders
     */
    public static <U> TextPlaceholderFactory<U> of(HashMap<String, Function<U, Component>> placeholders) {
        return new TextPlaceholderFactory<U>(placeholders);
    }

    /**
     * Creates a new TextPlaceholderFactory with a single placeholder.
     *
     * @param <U> The type of context object for placeholder resolution
     * @param key The placeholder key
     * @param value A function that generates Text based on the context object
     * @return A new factory instance with the single placeholder
     */
    public static <U> TextPlaceholderFactory<U> of(String key, Function<U, Component> value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    /**
     * Creates a new TextPlaceholderFactory with a single static Text placeholder.
     *
     * @param <U> The type of context object for placeholder resolution
     * @param key The placeholder key
     * @param value The static Text value for this placeholder
     * @return A new factory instance with the single placeholder
     */
    public static <U> TextPlaceholderFactory<U> of(String key, Component value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    /**
     * Creates a new TextPlaceholderFactory with a single static string placeholder.
     *
     * @param <U> The type of context object for placeholder resolution
     * @param key The placeholder key
     * @param value The static string value for this placeholder
     * @return A new factory instance with the single placeholder
     */
    public static <U> TextPlaceholderFactory<U> of(String key, String value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    /**
     * Creates a pre-configured TextPlaceholderFactory for ServerPlayerEntity with common placeholders and styles.
     * <p>
     * Includes the following style patterns:
     * <ul>
     *   <li>{@code &0-&f, &k-&o, &r} - Minecraft formatting codes (colors and formatting)</li>
     *   <li>{@code ${markdown}} - Parse the text content as Markdown</li>
     *   <li>{@code ${color:RRGGBB}} - Apply a hex color</li>
     *   <li>{@code ${link:url}} - Create a clickable URL link</li>
     *   <li>{@code ${copy:text}} - Create a click-to-copy text</li>
     *   <li>{@code ${hint:text}} - Add hover text</li>
     *   <li>{@code ${suggest:command}} - Suggest a command on click</li>
     * </ul>
     * <p>
     * Includes the following player-related placeholders:
     * <ul>
     *   <li>{@code ${player}} - Player display name</li>
     *   <li>{@code ${health}, ${hp}} - Current health</li>
     *   <li>{@code ${maxhealth}, ${maxhp}} - Maximum health</li>
     *   <li>{@code ${level}} - Experience level</li>
     *   <li>{@code ${hunger}} - Hunger level</li>
     *   <li>{@code ${saturation}} - Saturation level</li>
     *   <li>{@code ${x}, ${y}, ${z}} - Player coordinates</li>
     *   <li>{@code ${pitch}, ${yaw}} - Player rotation</li>
     *   <li>{@code ${biome}} - Current biome</li>
     *   <li>{@code ${coord}} - Formatted coordinates</li>
     *   <li>{@code ${mainhand}, ${offhand}} - Item in hand</li>
     * </ul>
     *
     * @return A pre-configured factory for ServerPlayerEntity context
     */
    public static TextPlaceholderFactory<ServerPlayer> ofDefault() {
        return new TextPlaceholderFactory<ServerPlayer>()
            .style("&0", (param, text) -> text.withStyle(ChatFormatting.BLACK))
            .style("&1", (param, text) -> text.withStyle(ChatFormatting.DARK_BLUE))
            .style("&2", (param, text) -> text.withStyle(ChatFormatting.DARK_GREEN))
            .style("&3", (param, text) -> text.withStyle(ChatFormatting.DARK_AQUA))
            .style("&4", (param, text) -> text.withStyle(ChatFormatting.DARK_RED))
            .style("&5", (param, text) -> text.withStyle(ChatFormatting.DARK_PURPLE))
            .style("&6", (param, text) -> text.withStyle(ChatFormatting.GOLD))
            .style("&7", (param, text) -> text.withStyle(ChatFormatting.GRAY))  
            .style("&8", (param, text) -> text.withStyle(ChatFormatting.DARK_GRAY))
            .style("&9", (param, text) -> text.withStyle(ChatFormatting.BLUE))  
            .style("&[aA]", (param, text) -> text.withStyle(ChatFormatting.GREEN))
            .style("&[bB]", (param, text) -> text.withStyle(ChatFormatting.AQUA))
            .style("&[cC]", (param, text) -> text.withStyle(ChatFormatting.RED))
            .style("&[dD]", (param, text) -> text.withStyle(ChatFormatting.LIGHT_PURPLE))
            .style("&[eE]", (param, text) -> text.withStyle(ChatFormatting.YELLOW))
            .style("&[fF]", (param, text) -> text.withStyle(ChatFormatting.WHITE))
            .style("&[kK]", (param, text) -> text.withStyle(ChatFormatting.OBFUSCATED))
            .style("&[lL]", (param, text) -> text.withStyle(ChatFormatting.BOLD))
            .style("&[oO]", (param, text) -> text.withStyle(ChatFormatting.ITALIC))
            .style("&[nN]", (param, text) -> text.withStyle(ChatFormatting.UNDERLINE))
            .style("&[mM]", (param, text) -> text.withStyle(ChatFormatting.STRIKETHROUGH))
            .style("&[rR]", (param, text) -> text.setStyle(Style.EMPTY))
            .style("\\$\\{markdown\\}", (param, text) -> Component.empty().append(MarkdownToTextConverter.parseMarkdownToText(text.getString())))
            .style("\\$\\{color:([0-9A-Fa-f]{1,8})\\}", (param, text) -> {
                try {
                    int colorInt = Integer.parseInt(param.strip(), 16);
                    return text.withStyle(style -> style.withColor(colorInt));
                } catch (NumberFormatException e) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Invalid color code: " + param.strip(), e);
                    return text;
                }
            })
            .style("\\$\\{link:(.*)\\}", (param, text) -> 
                text.withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.OPEN_URL, param.strip())
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.openurl", Component.literal(param.strip()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN))
                ))
            )
            .style("\\$\\{copy:(.*)\\}", (param, text) -> 
                text.withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, param.strip())
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.copyto", param.strip()).withStyle(ChatFormatting.GREEN))
                ))
            )
            .style("\\$\\{hint:(.*)\\}", (param, text) -> 
                text.withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(param.strip()))
                ))
            )
            .style("\\$\\{suggest:(.*)\\}", (param, text) -> 
                text.withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, param.strip())
                ))
            )
            .add("${Ciallo}", t -> Component.literal("Ciallo\uff5e(\u2220\u30fb\u03c9< )\u2312\u2606")
                .withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Ciallo\uff5e(\u2220\u30fb\u03c9< )\u2312\u2606").withStyle(
                        styled -> styled.withColor(Integer.parseInt("c0721c", 16))
                    ))
                ).withClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "Ciallo\uff5e(\u2220\u30fb\u03c9< )\u2312\u2606")
                ).withColor(Integer.parseInt("c0721c", 16)))
            )
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
            .add("${coord}", t -> Util.parseCoordText(t))
            .add("${mainhand}", t -> {
                ItemStack item = t.getMainHandItem();
                if (item == null || item.isEmpty()) {
                    return Util.parseTranslatableText("fmod.command.get.emptyslot");
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
                    return Util.parseTranslatableText("fmod.command.get.emptyslot");
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
