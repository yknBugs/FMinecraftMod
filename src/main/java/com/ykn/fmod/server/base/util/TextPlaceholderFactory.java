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

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A utility class for managing and parsing text placeholders. This class allows
 * the creation of placeholder mappings, where placeholders in a text string can
 * be replaced with dynamically generated values based on an input object of type T.
 *
 * <p>Placeholders are defined as key-value pairs, where the key is a string
 * representing the placeholder, and the value is a function that generates a
 * {@link Text} object based on the input object of type T.</p>
 * 
 * <p>In addition to basic placeholder replacement, the class also supports
 * custom styles that can be applied to text segments based on regex patterns.</p>
 *
 * <p>The class provides methods for adding, removing, and clearing placeholders,
 * as well as for parsing text strings to replace placeholders with their resolved
 * values. Additionally, it includes utility methods for creating default placeholder
 * factories and splitting text with delimiters.</p>
 *
 * @param <T> The type of the object used to resolve placeholder values.
 */
public class TextPlaceholderFactory<T> {

    private HashMap<String, Function<T, Text>> placeholders;
    private HashMap<String, BiFunction<String, MutableText, MutableText>> customStyles;

    public TextPlaceholderFactory() {
        this.placeholders = new HashMap<>();
        // Must use LinkedHashMap to make sure the order is deterministic
        this.customStyles = new LinkedHashMap<>();
    }

    public TextPlaceholderFactory(HashMap<String, Function<T, Text>> placeholders) {
        this.placeholders = placeholders;
    }

    public TextPlaceholderFactory<T> add(String key, Function<T, Text> value) {
        this.placeholders.put(key, value);
        return this;
    }

    public TextPlaceholderFactory<T> add(String key, Text value) {
        this.placeholders.put(key, t -> value);
        return this;
    }

    public TextPlaceholderFactory<T> add(String key, String value) {
        this.placeholders.put(key, t -> Text.literal(value));
        return this;
    }

    public TextPlaceholderFactory<T> style(String styleRegex, BiFunction<String, MutableText, MutableText> styleFunction) {
        this.customStyles.put(styleRegex, styleFunction);
        return this;
    }

    public TextPlaceholderFactory<T> append(HashMap<String, Function<T, Text>> placeholders) {
        this.placeholders.putAll(placeholders);
        return this;
    }

    public TextPlaceholderFactory<T> append(TextPlaceholderFactory<T> placeholders) {
        this.placeholders.putAll(placeholders.placeholders);
        this.customStyles.putAll(placeholders.customStyles);
        return this;
    }

    public TextPlaceholderFactory<T> clear() {
        this.placeholders.clear();
        this.customStyles.clear();
        return this;
    }

    public TextPlaceholderFactory<T> remove(String key) {
        this.placeholders.remove(key);
        return this;
    }
    
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
     * Tokenizes the given text by applying every regular-expression key in this.customStyles.
     *
     * The method iterates over the keys returned by this.customStyles.keySet(), treats each key
     * as a regular-expression pattern, and sequentially splits existing text segments wherever
     * that pattern matches. Each split is propagated to parallel lists that record the matching
     * style (the regex string) and the first capture-group value for that match.
     *
     * Result mapping:
     * - textTokens receives the sequence of text fragments (substrings) produced by all splits.
     * - styleTokens is a parallel List<List<String>> where each inner list contains style-regex
     *   entries corresponding to the match that followed the associated text fragment (an inner
     *   element is the regex string that matched; a null entry indicates "no match" for that fragment
     *   for the current regex during processing).
     * - styleParams is a parallel List<List<String>> where each inner list contains the first
     *   capturing-group value for the corresponding entry in styleTokens (empty string if the
     *   pattern has no capturing groups). Entries line up 1:1 with the inner lists of styleTokens.
     *
     * @param text the input string to tokenize
     * @param textTokens destination list to which the resulting text fragments will be appended
     * @param styleTokens destination parallel list; each element is a list of style-regex strings
     *                    corresponding to the fragment at the same index in textTokens
     * @param styleParams destination parallel list; each element is a list of first-capture-group
     *                    values corresponding to the entries in the same-index inner list of
     *                    styleTokens (empty string if no group)
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
     * @return A {@link MutableText} object representing the parsed and resolved text.
     */
    public MutableText parsePlaceholders(String text, T t) {
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

        List<Text> finalText = new ArrayList<>();
        for (int i = 0; i < splitText.size(); i++) {
            String part = splitText.get(i);
            if (i % 2 == 0) {
                // Even indexes are the strings that should be splited
                finalText.add(Text.literal(part));
            } else {
                // Odd indexes are the delimiters that should be kept
                Function<T, Text> placeholderFunction = this.placeholders.get(part);
                if (placeholderFunction == null) {
                    // Unlikely to happen
                    LoggerFactory.getLogger(Util.LOGGERNAME).warn("Missing placeholder: " + part);
                    finalText.add(Text.literal(part));
                } else {
                    try {
                        finalText.add(placeholderFunction.apply(t));
                    } catch (Exception e) {
                        LoggerFactory.getLogger(Util.LOGGERNAME).error("Error while parsing placeholder: " + part, e);
                        finalText.add(Text.literal(part));
                    }
                }
            }
        }
        
        MutableText result = Text.empty();
        for (Text part : finalText) {
            result.append(part);
        }
        return result;
    }

    public MutableText parse(String text, T t) {
        // Tokenize the text first based on custom styles
        List<Text> finalTexts = new ArrayList<>();
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
            MutableText textToken = parsePlaceholders(currentText, t);
            Set<String> styleKeys = activeStyles.keySet();
            for (String styleKey : styleKeys) {
                BiFunction<String, MutableText, MutableText> styleFunction = this.customStyles.get(styleKey);
                if (styleFunction == null) {
                    // Unlikely to happen
                    LoggerFactory.getLogger(Util.LOGGERNAME).warn("Missing custom style: " + styleKey);
                    continue;
                }
                String param = activeStyles.get(styleKey);
                try {
                    textToken = styleFunction.apply(param, textToken);
                } catch (Exception e) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("Error while applying custom style: " + styleKey + " with param: " + param, e);
                }
            }
            finalTexts.add(textToken);
        }
        // Combine all final texts
        MutableText result = Text.empty();
        for (Text part : finalTexts) {
            result.append(part);
        }
        return result;
    }

    public static <U> TextPlaceholderFactory<U> empty() {
        return new TextPlaceholderFactory<U>();
    }

    public static <U> TextPlaceholderFactory<U> of(HashMap<String, Function<U, Text>> placeholders) {
        return new TextPlaceholderFactory<U>(placeholders);
    }

    public static <U> TextPlaceholderFactory<U> of(String key, Function<U, Text> value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    public static <U> TextPlaceholderFactory<U> of(String key, Text value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    public static <U> TextPlaceholderFactory<U> of(String key, String value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    /**
     * Creates a default {@link TextPlaceholderFactory} for {@link ServerPlayerEntity} with predefined placeholders.
     * 
     * @return A {@link TextPlaceholderFactory} instance with the default placeholders for {@link ServerPlayerEntity}.
     */
    public static TextPlaceholderFactory<ServerPlayerEntity> ofDefault() {
        return new TextPlaceholderFactory<ServerPlayerEntity>()
            .style("&0", (param, text) -> text.formatted(Formatting.BLACK))
            .style("&1", (param, text) -> text.formatted(Formatting.DARK_BLUE))
            .style("&2", (param, text) -> text.formatted(Formatting.DARK_GREEN))
            .style("&3", (param, text) -> text.formatted(Formatting.DARK_AQUA))
            .style("&4", (param, text) -> text.formatted(Formatting.DARK_RED))
            .style("&5", (param, text) -> text.formatted(Formatting.DARK_PURPLE))
            .style("&6", (param, text) -> text.formatted(Formatting.GOLD))
            .style("&7", (param, text) -> text.formatted(Formatting.GRAY))  
            .style("&8", (param, text) -> text.formatted(Formatting.DARK_GRAY))
            .style("&9", (param, text) -> text.formatted(Formatting.BLUE))  
            .style("&[aA]", (param, text) -> text.formatted(Formatting.GREEN))
            .style("&[bB]", (param, text) -> text.formatted(Formatting.AQUA))
            .style("&[cC]", (param, text) -> text.formatted(Formatting.RED))
            .style("&[dD]", (param, text) -> text.formatted(Formatting.LIGHT_PURPLE))
            .style("&[eE]", (param, text) -> text.formatted(Formatting.YELLOW))
            .style("&[fF]", (param, text) -> text.formatted(Formatting.WHITE))
            .style("&[kK]", (param, text) -> text.formatted(Formatting.OBFUSCATED))
            .style("&[lL]", (param, text) -> text.formatted(Formatting.BOLD))
            .style("&[oO]", (param, text) -> text.formatted(Formatting.ITALIC))
            .style("&[nN]", (param, text) -> text.formatted(Formatting.UNDERLINE))
            .style("&[mM]", (param, text) -> text.formatted(Formatting.STRIKETHROUGH))
            .style("&[rR]", (param, text) -> text.setStyle(Style.EMPTY))
            .style("\\$\\{color: ([0-9A-Fa-f]{1,8})\\}", (param, text) -> {
                try {
                    int colorInt = Integer.parseInt(param, 16);
                    return text.styled(style -> style.withColor(colorInt));
                } catch (NumberFormatException e) {
                    LoggerFactory.getLogger(Util.LOGGERNAME).error("Invalid color code: " + param, e);
                    return text;
                }
            })
            .style("\\$\\{link: (.*)\\}", (param, text) -> 
                text.styled(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.OPEN_URL, param)
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.openurl", param))
                ))
            )
            .style("\\$\\{copy: (.*)\\}", (param, text) -> 
                text.styled(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, param)
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.copyto", param))
                ))
            )
            .style("\\$\\{hint: (.*)\\}", (param, text) -> 
                text.styled(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(param))
                ))
            )
            .add("${Ciallo}", t -> Text.literal("Ciallo\uff5e(\u2220\u30fb\u03c9< )\u2312\u2606")
                .styled(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Ciallo\uff5e(\u2220\u30fb\u03c9< )\u2312\u2606").styled(
                        styled -> styled.withColor(Integer.parseInt("000721", 16))
                    ))
                ).withClickEvent(
                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "Ciallo\uff5e(\u2220\u30fb\u03c9< )\u2312\u2606")
                ).withColor(Integer.parseInt("000721", 16)))
            )
            .add("${player}", t -> t.getDisplayName())
            .add("${health}", t -> Text.literal(String.format("%.2f", t.getHealth())))
            .add("${hp}", t -> Text.literal(String.format("%.2f", t.getHealth())))
            .add("${maxhealth}", t -> Text.literal(String.format("%.2f", t.getMaxHealth())))
            .add("${maxhp}", t -> Text.literal(String.format("%.2f", t.getMaxHealth())))
            .add("${level}", t -> Text.literal(String.valueOf(t.experienceLevel)))
            .add("${hunger}", t -> Text.literal(String.valueOf(t.getHungerManager().getFoodLevel())))
            .add("${saturation}", t -> Text.literal(String.format("%.2f", t.getHungerManager().getSaturationLevel())))
            .add("${x}", t -> Text.literal(String.format("%.2f", t.getX())))
            .add("${y}", t -> Text.literal(String.format("%.2f", t.getY())))
            .add("${z}", t -> Text.literal(String.format("%.2f", t.getZ())))
            .add("${pitch}", t -> Text.literal(String.format("%.2f", t.getPitch())))
            .add("${yaw}", t -> Text.literal(String.format("%.2f", t.getYaw())))
            .add("${biome}", t -> Util.getBiomeText(t))
            .add("${coord}", t -> {
                Text biomeText = Util.getBiomeText(t);
                String strDim = t.getWorld().getRegistryKey().getValue().toString();
                String strX = String.format("%.2f", t.getX());
                String strY = String.format("%.2f", t.getY());
                String strZ = String.format("%.2f", t.getZ());
                String strPitch = String.format("%.2f", t.getPitch());
                String strYaw = String.format("%.2f", t.getYaw());
                Text x = Text.literal(strX);
                Text y = Text.literal(strY);
                Text z = Text.literal(strZ);
                MutableText result = Text.literal("[").append(biomeText).append("] (").append(x).append(", ").append(y).append(", ").append(z).append(")");
                result = result.styled(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + strDim + " run tp @s " + strX + " " + strY + " " + strZ + " " + strYaw + " " + strPitch)
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
                ));
                return result;
            })
            .add("${mainhand}", t -> {
                ItemStack item = t.getMainHandStack();
                if (item == null || item.isEmpty()) {
                    return Text.translatable("fmod.command.get.emptyslot");
                } else {
                    if (item.getCount() > 1) {
                        return Text.empty().append(item.toHoverableText()).append("x").append(Text.literal(String.valueOf(item.getCount())));
                    } else {
                        return item.toHoverableText();
                    }
                }
            })
            .add("${offhand}", t -> {
                ItemStack item = t.getOffHandStack();
                if (item == null || item.isEmpty()) {
                    return Text.translatable("fmod.command.get.emptyslot");
                } else {
                    if (item.getCount() > 1) {
                        return Text.empty().append(item.toHoverableText()).append("x").append(Text.literal(String.valueOf(item.getCount())));
                    } else {
                        return item.toHoverableText();
                    }
                }
            });
    }
}
