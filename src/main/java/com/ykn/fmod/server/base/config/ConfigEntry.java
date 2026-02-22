/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a field in a {@link ConfigReader} implementation as a
 * configurable entry managed by {@link ServerConfigRegistry}.
 *
 * <p>Each annotated field is automatically registered with the config registry, which
 * then generates the corresponding Brigadier command node and (optionally) a UI widget
 * in {@code OptionScreen}. The annotation attributes control every aspect of the entry:
 * its type, command syntax, i18n keys, allowed value ranges, display helpers, and
 * whether it can be edited through the in-game options screen.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @ConfigEntry(
 *     type = ConfigEntry.ConfigType.BOOLEAN,
 *     codeEntry = "myFlag",
 *     commandEntry = "my_flag",
 *     i18nEntry = "myFlag"
 * )
 * private boolean myFlag = true;
 * }</pre>
 *
 * @see ServerConfigRegistry
 * @see ConfigReader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigEntry {

    /**
     * The data type of this config entry.
     * Determines which Brigadier argument type and UI widget are used.
     *
     * @return the {@link ConfigType} of the annotated field
     */
    ConfigType type() default ConfigType.STRING;

    /**
     * The key used internally to identify this entry in {@link ServerConfigRegistry}.
     * If left empty, the field name is used as the code entry key.
     *
     * @return the internal code entry name, or empty string to use the field name
     */
    String codeEntry() default "";

    /**
     * The literal name of the sub-command generated for this entry under {@code /f options}.
     * If left empty, {@link #codeEntry()} (or the field name) is used instead.
     *
     * @return the command literal for this entry, or empty string to derive from codeEntry
     */
    String commandEntry() default "";

    /**
     * The suffix used to construct i18n translation keys for this entry.
     * The title key will be {@code fmod.options.<i18nEntry>} and the hint key will be
     * {@code fmod.options.hint.<i18nEntry>}.
     * If left empty, {@link #codeEntry()} (or the field name) is used instead.
     *
     * @return the i18n entry suffix, or empty string to derive from codeEntry
     */
    String i18nEntry() default "";

    /**
     * The minimum value accepted by the Brigadier command for a {@link ConfigType#DOUBLE} entry.
     * Defaults to {@link Double#NEGATIVE_INFINITY} (no lower bound).
     *
     * @return the minimum double value for command input
     */
    double minCommandDouble() default Double.NEGATIVE_INFINITY;

    /**
     * The maximum value accepted by the Brigadier command for a {@link ConfigType#DOUBLE} entry.
     * Defaults to {@link Double#POSITIVE_INFINITY} (no upper bound).
     *
     * @return the maximum double value for command input
     */
    double maxCommandDouble() default Double.POSITIVE_INFINITY;

    /**
     * The minimum value mapped to the left end of the slider widget for a
     * {@link ConfigType#DOUBLE} entry in the options screen UI.
     *
     * @return the minimum double value for the slider
     */
    double minSliderDouble() default 0.0;

    /**
     * The maximum value mapped to the right end of the slider widget for a
     * {@link ConfigType#DOUBLE} entry in the options screen UI.
     *
     * @return the maximum double value for the slider
     */
    double maxSliderDouble() default 100.0;

    /**
     * The minimum value accepted by the Brigadier command for an {@link ConfigType#INTEGER} entry.
     * Defaults to {@link Integer#MIN_VALUE} (no lower bound).
     *
     * @return the minimum integer value for command input
     */
    int minCommandInt() default Integer.MIN_VALUE;

    /**
     * The maximum value accepted by the Brigadier command for an {@link ConfigType#INTEGER} entry.
     * Defaults to {@link Integer#MAX_VALUE} (no upper bound).
     *
     * @return the maximum integer value for command input
     */
    int maxCommandInt() default Integer.MAX_VALUE;

    /**
     * The minimum value mapped to the left end of the slider widget for an
     * {@link ConfigType#INTEGER} entry in the options screen UI.
     *
     * @return the minimum integer value for the slider
     */
    int minSliderInt() default 0;

    /**
     * The maximum value mapped to the right end of the slider widget for an
     * {@link ConfigType#INTEGER} entry in the options screen UI.
     *
     * @return the maximum integer value for the slider
     */
    int maxSliderInt() default 100;

    /**
     * The maximum number of characters allowed for a {@link ConfigType#STRING} entry,
     * both in command input and in the options screen text field.
     *
     * @return the maximum string length, default 1024
     */
    int maxStringLength() default 1024;

    /**
     * Whether this entry can be modified through the in-game options screen UI.
     * When {@code false} the corresponding widget is rendered but disabled.
     *
     * @return {@code true} if the entry is editable in the UI (default), {@code false} otherwise
     */
    boolean isEditableInUI() default true;

    /**
     * An i18n translation key for the tooltip shown when this entry is not editable in the UI.
     * Only used when {@link #isEditableInUI()} returns {@code false}.
     * If empty, no tooltip is shown.
     *
     * @return the translation key for the "not editable" reason tooltip, or empty string
     */
    String notEditableReason() default "";

    /**
     * The name of a method on the owning {@link ConfigReader} instance that converts a raw
     * config value to a display {@link net.minecraft.text.Text} for the options screen and
     * command feedback. The method signature must be {@code Text methodName(T value)} where
     * {@code T} is the type of the field.
     * If empty, {@link String#valueOf(Object)} is used as a fallback.
     *
     * @return the display-value getter method name, or empty string to use default string conversion
     */
    String displayValueGetter() default "";

    /**
     * The placeholder name shown for the value argument in the command syntax hint,
     * e.g. {@code /f options myEntry <value>}.
     *
     * @return the command argument hint label, default {@code "value"}
     */
    String commandValueHint() default "value";

    /**
     * The name of a method on the owning {@link ConfigReader} instance that converts the
     * raw command input (as parsed by Brigadier) into the true value to be stored in the
     * config field. The method signature must be {@code T methodName(T input)}.
     * If empty, the command input is stored directly without conversion.
     *
     * @return the conversion method name, or empty string to skip conversion
     */
    String commandInputToTrueValue() default "";

    /**
     * The name of a method on the owning {@link ConfigReader} instance that converts a
     * config value (as a {@code double}) into the normalised slider position in {@code [0.0, 1.0]}.
     * The method signature must be {@code double methodName(double configValue)}.
     * If empty, linear interpolation between {@link #minSliderDouble()} / {@link #minSliderInt()}
     * and {@link #maxSliderDouble()} / {@link #maxSliderInt()} is used.
     *
     * @return the to-slider conversion method name, or empty string to use default linear mapping
     */
    String toSliderValue() default "";

    /**
     * The name of a method on the owning {@link ConfigReader} instance that converts a
     * normalised slider position ({@code double} in {@code [0.0, 1.0]}) back into the true
     * config value. The method signature must be {@code T methodName(double sliderValue)}.
     * If empty, the inverse of the linear interpolation is used.
     *
     * @return the from-slider conversion method name, or empty string to use default linear mapping
     */
    String fromSliderValue() default "";

    /**
     * Enumerates all supported data types for a config entry.
     *
     * <p>The type determines:
     * <ul>
     *   <li>Which Brigadier argument type is registered for the {@code /f options} command.</li>
     *   <li>Which widget is created in the options screen UI.</li>
     * </ul>
     */
    enum ConfigType {
        /** 
         * A {@code double}-typed config entry using a slider widget and double argument. 
         */
        DOUBLE,

        /** 
         * An {@code int}-typed config entry using a slider widget and integer argument. 
         */
        INTEGER,

        /** 
         * A {@link String}-typed config entry using a text field widget and greedy-string argument. 
         */
        STRING,

        /** 
         * A {@code boolean}-typed config entry using a toggle button widget and bool argument. 
         */
        BOOLEAN,

        /**
         * A {@link com.ykn.fmod.server.base.util.ServerMessageType}-typed entry that exposes
         * sub-commands and buttons for {@code main} location, {@code other} location, and
         * {@code receiver} fields.
         */
        SERVERMESSAGE,
        
        /**
         * A {@link com.ykn.fmod.server.base.util.PlayerMessageType}-typed entry that exposes
         * sub-commands and buttons for {@code main} location, {@code other} location, and
         * {@code receiver} fields.
         */
        PLAYERMESSAGE
    }
}
