/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;

/**
 * Fluent declaration of a rule component's parameter list, incl. its i18n keys.
 *
 * <p>A component type's translation keys are derived from a single prefix, matching the
 * convention every existing type already follows:
 * <ul>
 *   <li>{@code <prefix>.summary} – one-line type description</li>
 *   <li>{@code <prefix>} – the {@code render()} line, derived by
 *       {@link RequiredParamMetadata#getRenderI18nKey()}</li>
 *   <li>{@code <prefix>.param.<name>.name} / {@code .desc} – per-parameter labels</li>
 * </ul>
 *
 * <p>For a condition named {@code EqualsTo}, the prefix is
 * {@code fmod.rule.condition.equalsto}; actions use {@code fmod.rule.action.<type>}.
 *
 * <p>Example:
 * <pre>{@code
 * Params.of("fmod.rule.condition.equalsto")
 *     .param("left", ParamKind.AUTO)
 *     .param("right", ParamKind.AUTO)
 *     .allowNulls()
 * }</pre>
 *
 * <p>Build the metadata with {@link #metadata()} after the last parameter is declared.
 *
 * @see RequiredParamMetadata
 * @see ParamKind
 */
public final class Params {

    private final String prefix;

    private final RequiredParamMetadata metadata;

    private boolean allowNulls;

    private int outputIndex = -1;

    private Params(String prefix) {
        this.prefix = prefix;
        this.metadata = RequiredParamMetadata.create(prefix + ".summary");
    }

    /**
     * Starts a parameter list whose i18n keys all live under {@code prefix}.
     *
     * @param prefix the component type's translation key prefix, e.g.
     *               {@code "fmod.rule.condition.equalsto"}
     * @return an empty {@code Params} builder
     */
    public static Params of(String prefix) {
        return new Params(prefix);
    }

    /**
     * Declares a required parameter.
     *
     * @param name the parameter name; used as the JSON member name, the command argument
     *             name, the {@code var.} prefix, and the i18n sub-key
     * @param kind the parameter's declared shape
     * @return {@code this}, for chaining
     */
    public Params param(String name, ParamKind<?> kind) {
        return this.add(name, kind, null, null);
    }

    /**
     * Declares a parameter with a fallback value used when the parameter is absent or
     * resolves to an incompatible type.
     *
     * @param name     the parameter name
     * @param kind     the parameter's declared shape
     * @param fallback the fallback value, or {@code null} for none
     * @return {@code this}, for chaining
     */
    public Params defaulted(String name, ParamKind<?> kind, @Nullable Object fallback) {
        return this.add(name, kind, fallback, null);
    }

    /**
     * Declares a parameter with both a fallback value and a custom command-line suggester.
     *
     * @param name      the parameter name
     * @param kind      the parameter's declared shape
     * @param fallback  the fallback value, or {@code null} for none
     * @param suggester the suggester attached to this parameter's constant node
     * @return {@code this}, for chaining
     */
    public Params defaulted(String name, ParamKind<?> kind, @Nullable Object fallback,
            SuggestionProvider<CommandSourceStack> suggester) {
        return this.add(name, kind, fallback, suggester);
    }

    /**
     * Declares the parameter that receives a writer's result and marks it as the output.
     *
     * <p>Only {@link com.ykn.fmod.server.rule.core.Components#writer} uses this: the named parameter
     * must resolve to a variable name, and the computed value is stored under it.
     *
     * @param name the output parameter name, conventionally {@code variable}
     * @param kind the parameter's declared shape, conventionally {@link ParamKind#VARIABLE_NAME}
     * @return {@code this}, for chaining
     */
    public Params output(String name, ParamKind<?> kind) {
        this.outputIndex = this.metadata.size();
        return this.param(name, kind);
    }

    /**
     * Returns the position of the parameter declared by {@link #output}.
     *
     * @return the output parameter's index, or {@code -1} when none was declared
     */
    public int outputIndex() {
        return this.outputIndex;
    }

    /**
     * Declares a parameter with a custom command-line suggester.
     *
     * @param name      the parameter name
     * @param kind      the parameter's declared shape
     * @param suggester the suggester attached to this parameter's constant node
     * @return {@code this}, for chaining
     */
    public Params suggested(String name, ParamKind<?> kind, SuggestionProvider<CommandSourceStack> suggester) {
        return this.add(name, kind, null, suggester);
    }

    /**
     * Marks this component as accepting unresolved parameters: the body then receives
     * {@code null} and decides what that means, instead of the shared wrapper failing fast.
     *
     * <p>Only use this when a missing value is meaningful, e.g. {@code EqualsTo} treating
     * two missing values as equal.
     *
     * <p><b>Never combine this with a numeric parameter whose body unboxes a typed accessor.</b>
     * An expression such as {@code call.number(0) < call.number(1)} - or any other arithmetic on
     * the result of {@link ComponentCall#number(int)} / {@link ComponentCall#integer(int)} -
     * is safe only because the strict null check rejects the call before the body runs. With
     * {@code allowNulls()} switched on, an unresolved parameter reaches the body as {@code null},
     * the unboxing throws a {@link NullPointerException}, and the shared wrapper does not catch
     * it: the exception propagates to {@link RuleContext#test()} / {@link RuleContext#trigger()},
     * which log a stack trace and abort the whole evaluation. The rule then fails for a reason
     * that has nothing to do with its own logic.
     *
     * <p>A component that wants optional numeric bounds must instead read them as
     * {@code Double}/{@code Integer} and branch on {@code null} - see
     * {@link com.ykn.fmod.server.rule.tool.Ranges}, which every range condition uses - or declare
     * the parameter with {@link #defaulted(String, ParamKind, Object)} and keep the strict check.
     *
     * <p>On a writer - a component registered through
     * {@link Components#writer(String, Params, com.ykn.fmod.server.rule.tool.WriterBody)} - this
     * flag additionally means that a body returning {@code null} stores an explicit null instead
     * of failing, because publishing {@code null} is a legitimate result there (see
     * {@link RuleContext#setVariable(String, Object)}). The body then has to report its own
     * failures through {@link ComponentCall#fail}; a plain {@code null} is no longer one.
     *
     * @return {@code this}, for chaining
     */
    public Params allowNulls() {
        this.allowNulls = true;
        return this;
    }

    /**
     * Whether this component's body handles unresolved parameters itself.
     *
     * @return {@code true} when {@link #allowNulls()} was called
     */
    public boolean allowsNulls() {
        return this.allowNulls;
    }

    /**
     * Returns the declared parameter metadata. Call this after the last {@code param} call.
     *
     * @return the accumulated {@link RequiredParamMetadata}
     */
    public RequiredParamMetadata metadata() {
        return this.metadata;
    }

    private Params add(String name, ParamKind<?> kind, @Nullable Object fallback,
            @Nullable SuggestionProvider<CommandSourceStack> suggester) {
        String key = this.prefix + ".param." + name.toLowerCase(Locale.ROOT);
        this.metadata.add(new RequiredParamMetadata.Entry(kind, key + ".name", key + ".desc", name, "var." + name,
                fallback, suggester));
        return this;
    }
}
