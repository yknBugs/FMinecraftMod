package com.ykn.fmod.server.rule.core;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Describes the parameters required by a {@link RuleAction} or {@link SourceCondition}.
 *
 * <p>Each registered parameter is represented by an {@link Entry} that declares its
 * {@link ParamKind}, argument names, and i18n keys for display. A single
 * {@code RequiredParamMetadata} instance serves two purposes:
 *
 * <ul>
 *   <li>Consumed by {@link com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder} to
 *       generate the Brigadier command tree for {@code /f rule edit} commands.</li>
 *   <li>Exposed via {@link RuleAction#getParameters()} and
 *       {@link SourceCondition#getParameters()} so that external consumers can inspect
 *       each component's parameter schema without parsing command nodes or JSON.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * private static final RequiredParamMetadata PARAM_METADATA =
 *     RequiredParamMetadata.create("fmod.rule.condition.foo.summary")
 *         .add(ParamKind.PLAYER, "fmod.rule.condition.foo.param.player.name",
 *              "fmod.rule.condition.foo.param.player.desc", "player", "var.player")
 *         .add(ParamKind.INT, "fmod.rule.condition.foo.param.min.name",
 *              "fmod.rule.condition.foo.param.min.desc", "min", "var.min");
 * }</pre>
 *
 * <p>Then in {@code buildCommand()}:
 * <pre>{@code
 * builder.add(PARAM_METADATA.get(0))
 *        .add(PARAM_METADATA.get(1));
 * // ...
 * builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey());
 * }</pre>
 *
 * @see ParamKind
 * @see com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder
 */
public class RequiredParamMetadata {

    private final List<Entry> argumentList;

    private String summaryI18nKey;

    private RequiredParamMetadata(String summaryI18nKey) {
        this.argumentList = new ArrayList<>();
        this.summaryI18nKey = summaryI18nKey;
    }

    /**
     * Creates a new metadata container with the given summary i18n key.
     *
     * <p>The summary key is shown as a one-line description of the component type in
     * command usage output.
     *
     * @param summaryI18nKey the i18n key for the component's summary description
     * @return a new, empty {@code RequiredParamMetadata}
     */
    public static RequiredParamMetadata create(String summaryI18nKey) {
        return new RequiredParamMetadata(summaryI18nKey);
    }

    /**
     * Returns the parameter entry at the given index, in {@code add} call order.
     *
     * <p>Used by {@code buildCommand()} to wire each parameter into a
     * {@link com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder}.
     *
     * @param index the 0-based position of the parameter
     * @return the {@link Entry} at that position
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Entry get(int index) {
        return argumentList.get(index);
    }

    /**
     * Replaces the summary i18n key.
     *
     * @param summaryI18nKey the new i18n key for the component's summary description
     * @return {@code this}, for method chaining
     */
    public RequiredParamMetadata summary(String summaryI18nKey) {
        this.summaryI18nKey = summaryI18nKey;
        return this;
    }

    /**
     * Adds a pre-built {@link Entry} to the parameter list.
     *
     * @param entry the parameter metadata to add
     * @return {@code this}, for method chaining
     */
    public RequiredParamMetadata add(Entry entry) {
        argumentList.add(entry);
        return this;
    }

    /**
     * Returns the number of registered parameters.
     *
     * @return the parameter count
     */
    public int size() {
        return argumentList.size();
    }

    /**
     * Registers a new parameter with an explicit {@code defaultValue}.
     *
     * <p>The {@code defaultValue} is an optional fallback value when the parameter is not specified. 
     * Its runtime type should be compatible with {@link ParamKind#valueType()}.
     *
     * @param kind           the parameter's declared shape and value type
     * @param nameI18nKey    i18n key for the parameter's short display name
     * @param descI18nKey    i18n key for the parameter's description
     * @param constArgName   argument name used for the constant value in the command context
     * @param varArgName     argument name used for the variable name in the command context
     * @param defaultValue   an optional default value, or {@code null} for none
     * @return {@code this}, for method chaining
     */
    public RequiredParamMetadata add(ParamKind<?> kind, String nameI18nKey, String descI18nKey, String constArgName, String varArgName, @Nullable Object defaultValue) {
        return add(new Entry(kind, nameI18nKey, descI18nKey, constArgName, varArgName, defaultValue));
    }

    /**
     * Registers a new parameter without a default value.
     *
     * <p>Equivalent to {@link #add(ParamKind, String, String, String, String, Object)
     * add(kind, nameI18nKey, descI18nKey, constArgName, varArgName, null)}.
     *
     * @param kind           the parameter's declared shape and value type
     * @param nameI18nKey    i18n key for the parameter's short display name
     * @param descI18nKey    i18n key for the parameter's description
     * @param constArgName   argument name used for the constant value in the command context
     * @param varArgName     argument name used for the variable name in the command context
     * @return {@code this}, for method chaining
     */
    public RequiredParamMetadata add(ParamKind<?> kind, String nameI18nKey, String descI18nKey, String constArgName, String varArgName) {
        return add(new Entry(kind, nameI18nKey, descI18nKey, constArgName, varArgName, null));
    }

    /**
     * Removes all registered parameters.
     *
     * @return {@code this}, for method chaining
     */
    public RequiredParamMetadata clear() {
        argumentList.clear();
        return this;
    }

    /**
     * Removes the parameter at the given index.
     *
     * @param index the 0-based position of the parameter to remove
     * @return {@code this}, for method chaining
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public RequiredParamMetadata remove(int index) {
        argumentList.remove(index);
        return this;
    }

    /**
     * Removes the given entry from the parameter list.
     *
     * @param entry the entry to remove
     * @return {@code this}, for method chaining
     */
    public RequiredParamMetadata remove(Entry entry) {
        argumentList.remove(entry);
        return this;
    }

    /**
     * Returns a defensive copy of all registered parameter entries.
     *
     * @return a new {@link List} containing the parameter entries in registration order
     */
    public List<Entry> getArgumentList() {
        return new ArrayList<>(argumentList);
    }

    /**
     * Returns the i18n key for this component type's summary description.
     *
     * @return the summary i18n key
     */
    public String getSummaryI18nKey() {
        return summaryI18nKey;
    }

    /**
     * A single parameter's metadata within a {@link RequiredParamMetadata} container.
     *
     * <p>Each entry declares the parameter's {@link ParamKind} (which determines its
     * Brigadier argument type, value extractor, and Java type), the argument names used
     * to read it from a {@link com.mojang.brigadier.context.CommandContext}, i18n keys
     * for display, and an optional default value.
     */
    public static final class Entry {

        /**
         * The parameter's declared shape - determines argument type, value extraction,
         * and the expected Java type.
         */
        public final ParamKind<?> kind;

        /**
         * I18n key for the parameter's short display name (e.g. {@code "Player"}).
         */
        public final String nameI18nKey;

        /**
         * I18n key for the parameter's description (e.g.
         * {@code "The player whose permission level to check."}).
         */
        public final String descI18nKey;

        /**
         * Argument name used to retrieve the constant value from a Brigadier command context.
         */
        public final String constArgName;

        /**
         * Argument name used to retrieve the variable name from a Brigadier command context.
         */
        public final String varArgName;

        /**
         * An optional default value for the parameter, or {@code null} for none.
         *
         * The runtime type should be compatible with {@code kind.valueType()}.
         */
        @Nullable
        public final Object defaultValue;

        /**
         * Constructs a parameter metadata entry.
         *
         * @param kind           the parameter's declared shape and value type
         * @param nameI18nKey    i18n key for the parameter's short display name
         * @param descI18nKey    i18n key for the parameter's description
         * @param constArgName   argument name for the constant value
         * @param varArgName     argument name for the variable name
         * @param defaultValue   an optional default value, or {@code null} for none
         */
        public Entry(ParamKind<?> kind, String nameI18nKey, String descI18nKey, String constArgName, String varArgName, @Nullable Object defaultValue) {
            this.kind = kind;
            this.nameI18nKey = nameI18nKey;
            this.descI18nKey = descI18nKey;
            this.constArgName = constArgName;
            this.varArgName = varArgName;
            this.defaultValue = defaultValue;
        }

        /**
         * Returns the boxed Java type this parameter resolves to, delegated from
         * {@link ParamKind#valueType()}.
         *
         * @return the value type declared by this parameter's {@link ParamKind}
         */
        public Class<?> getValueType() {
            return kind.valueType();
        }
    }

}
