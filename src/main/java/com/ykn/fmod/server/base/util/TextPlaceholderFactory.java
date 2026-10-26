/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * and the value is a function that generates Component based on the context object.
 * Custom styles use regex patterns to match style markers and apply formatting.
 * Placeholder keys must be non-null and nonempty. Factory registrations are mutable and
 * must not be modified concurrently with parsing.
 * <p>
 * Example usage:
 * <pre>{@code
 * TextPlaceholderFactory<ServerPlayer> factory = TextPlaceholderFactory.ofDefault();
 * MutableComponent result = factory.parse("Hello ${player}! Your health is ${health}.", player);
 * }</pre>
 *
 * @param <T> The type of context object used for placeholder resolution
 */
public class TextPlaceholderFactory<T> {

    /**
     * Map of placeholder keys to their corresponding text generation functions.
     */
    private final Map<String, Function<T, Component>> placeholders;

    /**
     * Map of custom style regex patterns to their corresponding style application functions.
     */
    private final Map<String, StyleDefinition> customStyles;

    private static final int MAX_NESTING = 32;

    /** Immutable definitions can be shared; matchers and active styles belong to each parse. */
    private record StyleDefinition(Pattern pattern, String prefix, boolean richHint, boolean consumesRemainder,
            BiFunction<String, MutableComponent, MutableComponent> function) {}

    private record StyleMatch(String key, StyleDefinition definition, int start, int end, String parameter) {}

    private record ActiveStyle(StyleDefinition definition, String parameter, Component hoverText) {}

    /** Lazily initializes defaults once, while each public factory keeps independent maps. */
    private static final class Defaults {
        private static final TextPlaceholderFactory<ServerPlayer> FACTORY = createDefault();
    }

    /**
     * Constructs a new empty TextPlaceholderFactory with no placeholders or custom styles.
     */
    public TextPlaceholderFactory() {
        this.placeholders = new HashMap<>();
        // Must use LinkedHashMap to make sure the order is deterministic
        this.customStyles = new LinkedHashMap<>();
    }

    /**
     * Constructs a new TextPlaceholderFactory with a shallow copy of the specified placeholders.
     *
     * @param placeholders A map of placeholder keys to their corresponding text generation functions
     */
    public TextPlaceholderFactory(Map<String, Function<T, Component>> placeholders) {
        placeholders.keySet().forEach(TextPlaceholderFactory::requireKey);
        this.placeholders = new HashMap<>(placeholders);
        this.customStyles = new LinkedHashMap<>();
    }

    /**
     * Adds a placeholder with a dynamic value function.
     *
     * @param key The placeholder key (e.g., "${player}")
     * @param value A function that generates Component based on the context object
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> add(String key, Function<T, Component> value) {
        this.placeholders.put(requireKey(key), value);
        return this;
    }

    /**
     * Adds a placeholder with a static Component value.
     *
     * @param key The placeholder key (e.g., "${greeting}")
     * @param value The static Component value to use for this placeholder
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> add(String key, Component value) {
        this.placeholders.put(requireKey(key), t -> value);
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
        this.placeholders.put(requireKey(key), t -> Component.literal(value));
        return this;
    }

    /**
     * Adds a custom style pattern that can be applied to text.
     * <p>
     * The style pattern is compiled at registration and reused for subsequent parses.
     * Invalid patterns fail at registration. The regex matches atomic style markers in the text.
     * When matched, the style function is applied to transform the text.
     * The first capturing group in the regex (if present) is passed as the
     * parameter to the style function.
     * <p>
     * Example: {@code .style("\\$\\{color:([0-9A-Fa-f]+)\\}", (color, text) -> text.withStyle(s -> s.withColor(Integer.parseInt(color, 16))))}
     *
     * @param styleRegex A regex pattern to match style markers in the text
     * @param styleFunction A function that applies the style, taking the captured parameter and the text to style
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> style(String styleRegex, BiFunction<String, MutableComponent, MutableComponent> styleFunction) {
        Objects.requireNonNull(styleFunction, "styleFunction");
        StyleDefinition existing = this.customStyles.get(styleRegex);
        Pattern pattern = existing == null ? Pattern.compile(styleRegex) : existing.pattern();
        this.customStyles.put(styleRegex, new StyleDefinition(pattern, null, false, false, styleFunction));
        return this;
    }

    /**
     * Appends all placeholders from the given map to this factory.
     * Existing placeholders with the same keys will be overwritten.
     *
     * @param placeholders A map of placeholders to add
     * @return This factory instance for method chaining
     */
    public TextPlaceholderFactory<T> append(Map<String, Function<T, Component>> placeholders) {
        placeholders.keySet().forEach(TextPlaceholderFactory::requireKey);
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

    private static String requireKey(String key) {
        if (Objects.requireNonNull(key, "key").isEmpty()) {
            throw new IllegalArgumentException("Placeholder keys must not be empty");
        }
        return key;
    }

    /** Registers a default expression whose parameter may contain balanced ${...} expressions. */
    private TextPlaceholderFactory<T> expression(String regex, String prefix, boolean richHint,
            BiFunction<String, MutableComponent, MutableComponent> function) {
        style(regex, function);
        StyleDefinition definition = customStyles.get(regex);
        customStyles.put(regex, new StyleDefinition(definition.pattern(), prefix, richHint, false, function));
        return this;
    }

    /** Markdown owns the remainder so its code spans, links and block structure stay intact. */
    private TextPlaceholderFactory<T> markdownStyle() {
        String regex = "\\$\\{markdown\\}";
        style(regex, (param, text) -> Component.empty()
            .append(MarkdownToTextConverter.parseMarkdownToText(text.getString())).setStyle(text.getStyle()));
        StyleDefinition definition = customStyles.get(regex);
        customStyles.put(regex, new StyleDefinition(definition.pattern(), null, false, true, definition.function()));
        return this;
    }

    /** Returns the exclusive end of a balanced expression, or -1 if it is unterminated. */
    private static int expressionEnd(String text, int start) {
        int depth = 1;
        for (int i = start + 2; i < text.length(); i++) {
            if (text.startsWith("${", i)) {
                depth++;
                i++;
            } else if (text.charAt(i) == '}' && --depth == 0) {
                return i + 1;
            }
        }
        return -1;
    }

    /** Finds the earliest nonempty marker; registration order breaks ties. */
    private StyleMatch nextStyle(String text, int offset) {
        StyleMatch best = null;
        for (Map.Entry<String, StyleDefinition> entry : customStyles.entrySet()) {
            StyleDefinition definition = entry.getValue();
            int start;
            int end;
            String parameter;
            if (definition.prefix() != null) {
                start = text.indexOf(definition.prefix(), offset);
                if (start < 0) {
                    continue;
                }
                end = expressionEnd(text, start);
                // Keep an unterminated default expression literal, including formatting codes.
                if (end < 0) {
                    if (best == null || start < best.start()) {
                        best = new StyleMatch(entry.getKey(), null, start, text.length(), "");
                    }
                    continue;
                }
                parameter = text.substring(start + definition.prefix().length(), end - 1);
            } else {
                Matcher matcher = definition.pattern().matcher(text);
                boolean found = matcher.find(offset);
                while (found && matcher.start() == matcher.end()) {
                    found = matcher.find();
                }
                if (!found) {
                    continue;
                }
                start = matcher.start();
                end = matcher.end();
                parameter = matcher.groupCount() == 0 ? "" : matcher.group(1);
            }
            if (best == null || start < best.start()) {
                best = new StyleMatch(entry.getKey(), definition, start, end, parameter);
            }
        }
        return best;
    }

    /**
     * Replaces literal placeholder keys without interpreting replacement components as markup.
     * At each position the longest matching key wins. Exceptions from a resolver leave its key
     * literal. The context is passed through unchanged and may be null if resolvers support it.
     *
     * @param text non-null input
     * @param t placeholder context
     * @return a new component containing resolved placeholders and literal text
     */
    public MutableComponent parsePlaceholders(String text, T t) {
        List<String> keys = new ArrayList<>(placeholders.keySet());
        keys.sort((a, b) -> Integer.compare(b.length(), a.length()));
        MutableComponent result = Component.empty();
        int offset = 0;
        while (offset < text.length()) {
            int next = text.length();
            String matched = null;
            for (String key : keys) {
                int position = text.indexOf(key, offset);
                if (position >= 0 && (matched == null || position < next)) {
                    next = position;
                    matched = key;
                }
            }
            if (next > offset) {
                result.append(Component.literal(text.substring(offset, next)));
            }
            if (matched == null) {
                break;
            }
            try {
                result.append(Objects.requireNonNull(placeholders.get(matched).apply(t)));
            } catch (Exception e) {
                Util.LOGGER.error("FMinecraftMod: Error while parsing placeholder: " + matched, e);
                result.append(Component.literal(matched));
            }
            offset = next + matched.length();
        }
        return result;
    }

    /**
     * Parses atomic style markers from left to right, resolving placeholders in visible text.
     * Styles accumulate in occurrence order; repeating a style replaces its parameter and moves
     * it to the end of the active order. Registration order breaks matches at the same position.
     * Empty regex matches are ignored. Custom regex parameters are passed through unchanged
     * (an unmatched optional first group is null).
     * <p>
     * Default link, copy and suggest parameters are literal. Default hints recursively parse
     * their balanced payload with independent style state, up to 32 nested hints; deeper content
     * remains literal. Unterminated default parameter expressions remain literal.
     * The default ${markdown} marker renders the remaining text as one Markdown document,
     * after placeholder substitution. Further fmod style markers are literal Markdown input.
     *
     * @param text non-null text containing markers and placeholders
     * @param t context passed unchanged to placeholder functions
     * @return a new component containing the parsed text
     */
    public MutableComponent parse(String text, T t) {
        return parse(text, t, 0);
    }

    private MutableComponent parse(String text, T t, int depth) {
        MutableComponent result = Component.empty();
        Map<String, ActiveStyle> active = new LinkedHashMap<>();
        int offset = 0;
        while (offset < text.length()) {
            StyleMatch match = nextStyle(text, offset);
            int end = match == null ? text.length() : match.start();
            if (end > offset) {
                result.append(applyStyles(parsePlaceholders(text.substring(offset, end), t), active));
            }
            if (match == null) {
                break;
            }
            if (match.definition() == null) {
                result.append(applyStyles(Component.literal(text.substring(match.start(), match.end())), active));
            } else {
                Component hover = null;
                if (match.definition().richHint()) {
                    String parameter = match.parameter().strip();
                    hover = depth < MAX_NESTING ? parse(parameter, t, depth + 1) : Component.literal(parameter);
                }
                active.remove(match.key());
                active.put(match.key(), new ActiveStyle(match.definition(), match.parameter(), hover));
                if (match.definition().consumesRemainder()) {
                    result.append(applyStyles(parsePlaceholders(text.substring(match.end()), t), active));
                    break;
                }
            }
            offset = match.end();
        }
        return result;
    }

    private MutableComponent applyStyles(MutableComponent text, Map<String, ActiveStyle> active) {
        for (Map.Entry<String, ActiveStyle> entry : active.entrySet()) {
            ActiveStyle style = entry.getValue();
            try {
                if (style.definition().richHint()) {
                    text = text.withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, style.hoverText())));
                } else {
                    text = style.definition().function().apply(style.parameter(), text);
                }
            } catch (Exception e) {
                Util.LOGGER.error("FMinecraftMod: Error while applying custom style: " + entry.getKey(), e);
            }
        }
        return text;
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
    public static <U> TextPlaceholderFactory<U> of(Map<String, Function<U, Component>> placeholders) {
        return new TextPlaceholderFactory<U>(placeholders);
    }

    /**
     * Creates a new TextPlaceholderFactory with a single placeholder.
     *
     * @param <U> The type of context object for placeholder resolution
     * @param key The placeholder key
     * @param value A function that generates Component based on the context object
     * @return A new factory instance with the single placeholder
     */
    public static <U> TextPlaceholderFactory<U> of(String key, Function<U, Component> value) {
        return new TextPlaceholderFactory<U>().add(key, value);
    }

    /**
     * Creates a new TextPlaceholderFactory with a single static Component placeholder.
     *
     * @param <U> The type of context object for placeholder resolution
     * @param key The placeholder key
     * @param value The static Component value for this placeholder
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
     * Creates a pre-configured TextPlaceholderFactory for ServerPlayer with common placeholders and styles.
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
     * @return A new factory with independent registration maps and shared immutable style definitions
     */
    public static TextPlaceholderFactory<ServerPlayer> ofDefault() {
        return new TextPlaceholderFactory<ServerPlayer>().append(Defaults.FACTORY);
    }

    private static TextPlaceholderFactory<ServerPlayer> createDefault() {
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
            .markdownStyle()
            .style("\\$\\{color:([0-9A-Fa-f]{1,8})\\}", (param, text) -> {
                try {
                    int colorInt = Integer.parseInt(param.strip(), 16);
                    return text.withStyle(style -> style.withColor(colorInt));
                } catch (NumberFormatException e) {
                    Util.LOGGER.debug("FMinecraftMod: Invalid color code: " + param.strip(), e);
                    return text;
                }
            })
            .expression("\\$\\{link:([^}]*)\\}", "${link:", false, (param, text) ->
                text.withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.OPEN_URL, param.strip())
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.openurl", Component.literal(param.strip()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN))
                ))
            )
            .expression("\\$\\{copy:([^}]*)\\}", "${copy:", false, (param, text) ->
                text.withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, param.strip())
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.copyto", param.strip()).withStyle(ChatFormatting.GREEN))
                ))
            )
            .expression("\\$\\{hint:([^}]*)\\}", "${hint:", true, (param, text) ->
                text.withStyle(style -> style.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(param.strip()))
                ))
            )
            .expression("\\$\\{suggest:([^}]*)\\}", "${suggest:", false, (param, text) ->
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
            .add("${player}", t -> t == null ? Component.literal("${player}") : t.getDisplayName())
            .add("${health}", t -> t == null ? Component.literal("${health}") : Component.literal(String.format("%.2f", t.getHealth())))
            .add("${hp}", t -> t == null ? Component.literal("${hp}") : Component.literal(String.format("%.2f", t.getHealth())))
            .add("${maxhealth}", t -> t == null ? Component.literal("${maxhealth}") : Component.literal(String.format("%.2f", t.getMaxHealth())))
            .add("${maxhp}", t -> t == null ? Component.literal("${maxhp}") : Component.literal(String.format("%.2f", t.getMaxHealth())))
            .add("${level}", t -> t == null ? Component.literal("${level}") : Component.literal(String.valueOf(t.experienceLevel)))
            .add("${hunger}", t -> t == null ? Component.literal("${hunger}") : Component.literal(String.valueOf(t.getFoodData().getFoodLevel())))
            .add("${saturation}", t -> t == null ? Component.literal("${saturation}") : Component.literal(String.format("%.2f", t.getFoodData().getSaturationLevel())))
            .add("${x}", t -> t == null ? Component.literal("${x}") : Component.literal(String.format("%.2f", t.getX())))
            .add("${y}", t -> t == null ? Component.literal("${y}") : Component.literal(String.format("%.2f", t.getY())))
            .add("${z}", t -> t == null ? Component.literal("${z}") : Component.literal(String.format("%.2f", t.getZ())))
            .add("${pitch}", t -> t == null ? Component.literal("${pitch}") : Component.literal(String.format("%.2f", t.getXRot())))
            .add("${yaw}", t -> t == null ? Component.literal("${yaw}") : Component.literal(String.format("%.2f", t.getYRot())))
            .add("${biome}", t -> t == null ? Component.literal("${biome}") : Util.getBiomeText(t))
            .add("${coord}", t -> t == null ? Component.literal("${coord}") : Util.parseCoordText(t))
            .add("${mainhand}", t -> {
                if (t == null) {
                    return Component.literal("${mainhand}");
                }
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
                if (t == null) {
                    return Component.literal("${offhand}");
                }
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
