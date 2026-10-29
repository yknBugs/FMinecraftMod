/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ComponentCall;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.Ranges;

import net.minecraft.server.level.ServerPlayer;

/**
 * String predicates.
 *
 * <p>Every type here resolves its inputs as text, so a missing value becomes the empty string with
 * a warning instead of failing the whole condition: an empty prefix or suffix is a meaningful
 * comparison, and {@code StringLengthBetween} then measures length 0.
 *
 * <p>An invalid regular expression is user input, not a bug: it is reported through
 * {@code fmod.rule.condition.regexmatch.invalid} and the condition is {@code false}.
 */
public final class StringConditions {

    private StringConditions() {
    }

    /**
     * Registers every string condition type.
     */
    public static void register() {
        Components.predicate("StringPrefix", Params.of("fmod.rule.condition.stringprefix")
                .param("target", ParamKind.STRING)
                .param("substring", ParamKind.STRING)
                .allowNulls(),
                (call) -> text(call, 0).startsWith(text(call, 1)));

        Components.predicate("StringSuffix", Params.of("fmod.rule.condition.stringsuffix")
                .param("target", ParamKind.STRING)
                .param("substring", ParamKind.STRING)
                .allowNulls(),
                (call) -> text(call, 0).endsWith(text(call, 1)));

        Components.predicate("StringLengthBetween",
                Params.of("fmod.rule.condition.stringlengthbetween")
                        .param("target", ParamKind.STRING)
                        .param("min", ParamKind.INT)
                        .param("max", ParamKind.INT)
                        .allowNulls(),
                (call) -> {
                    String target = text(call, 0);
                    int lower = Ranges.lowerInt(call, 1);
                    int upper = Ranges.upperInt(call, 2);
                    if (!Ranges.requireOrder(call, lower, upper,
                            "fmod.rule.condition.stringlengthbetween.invalidrange")) {
                        return false;
                    }
                    int length = target.length();
                    return length >= lower && length <= upper;
                });

        Components.predicate("RegexMatch", Params.of("fmod.rule.condition.regexmatch")
                .param("target", ParamKind.STRING)
                .param("regex", ParamKind.GREEDY_STRING)
                .allowNulls(),
                (call) -> {
                    String target = text(call, 0);
                    String regex = call.text(1);
                    if (regex == null) {
                        call.warnNull(1);
                        return false;
                    }
                    try {
                        return Pattern.compile(regex).matcher(target).matches();
                    } catch (PatternSyntaxException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.condition.regexmatch.invalid",
                                exception.getDescription()));
                        return false;
                    }
                });

        Components.predicate("StringContainsOnlinePlayerName",
                Params.of("fmod.rule.condition.stringcontainsonlineplayername")
                        .param("target", ParamKind.STRING)
                        .allowNulls(),
                (call) -> {
                    String target = text(call, 0);
                    if (target.isEmpty()) {
                        return false;
                    }
                    String normalized = target.toLowerCase(Locale.ROOT);
                    for (ServerPlayer player : call.context().getServer().getPlayerList().getPlayers()) {
                        if (normalized.contains(
                                player.getGameProfile().getName().toLowerCase(Locale.ROOT))) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Reads a string parameter, substituting the empty string for a missing value.
     *
     * @param call  the current evaluation
     * @param index the parameter index
     * @return the resolved text, or an empty string when the parameter is missing
     */
    private static String text(ComponentCall call, int index) {
        String value = call.text(index);
        if (value == null) {
            call.warnNull(index);
            return "";
        }
        return value;
    }
}
