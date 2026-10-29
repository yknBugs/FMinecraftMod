/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * One component's resolved parameter values, handed to a component body.
 *
 * <p>Every declared parameter is resolved exactly once, eagerly, before the body runs, so a
 * body cannot observe different values for the same slot during one evaluation. Resolution
 * follows {@link RuleParameter#resolve(RuleContext, Class)}: a matching event variable wins,
 * otherwise the declared constant is used, otherwise the value is {@code null}.
 *
 * <p>The shared wrapper in {@link Components} rejects unresolved
 * parameters for strict components, so <em>inside a strict body every accessor returns a
 * non-null value</em>. Components registered with {@link Params#allowNulls()} must check
 * {@link #isNull(int)} themselves.
 *
 * <p>That guarantee is the only thing standing between a numeric body and a
 * {@link NullPointerException}: {@link #number(int)} and {@link #integer(int)} return the boxed
 * type, and a body that writes {@code call.number(0) < call.number(1)} unboxes it. Such a body
 * must therefore never be registered with {@link Params#allowNulls()}; see that method for the
 * full explanation and for what to do instead when a numeric bound is optional.
 *
 * @see RuleParameter
 * @see Params
 */
public final class ComponentCall {

    private final RuleContext context;

    private final String name;

    private final RequiredParamMetadata parameters;

    private final List<RuleParameter<?>> values;

    private final Object[] resolved;

    private boolean failureReported;

    /**
     * Resolves every parameter of one component evaluation.
     *
     * @param context    the current rule execution context
     * @param name       the component instance's own name, used in warnings
     * @param parameters the component type's declared parameters
     * @param values     the instance's parameter values, in {@code parameters} order
     */
    public ComponentCall(RuleContext context, String name, RequiredParamMetadata parameters,
            List<RuleParameter<?>> values) {
        this.context = context;
        this.name = name;
        this.parameters = parameters;
        this.values = List.copyOf(values);
        this.resolved = new Object[this.values.size()];
        for (int index = 0; index < this.resolved.length; index++) {
            this.resolved[index] = this.resolve(index);
        }
        this.reportNaN();
    }

    /**
     * Reports every {@code double} parameter that resolved to {@code NaN}.
     *
     * <p>{@code NaN} compares false against everything, so a condition built on it silently
     * answers "no" no matter what the bounds are. Reporting it turns that silence into a warning;
     * the value is still passed through unchanged, because a computed {@code NaN} is a legitimate
     * result that the rule author may want to test for.
     */
    private void reportNaN() {
        for (int index = 0; index < this.resolved.length; index++) {
            if (this.resolved[index] instanceof Double value && value.isNaN()) {
                this.warn(Util.parseTranslatableText("fmod.rule.warning.nan",
                        Util.parseTranslatableText(this.parameters.get(index).nameI18nKey)));
            }
        }
    }

    /**
     * Returns the execution context this call belongs to.
     *
     * @return the current {@link RuleContext}
     */
    public RuleContext context() {
        return this.context;
    }

    /**
     * Returns the component instance's own name.
     *
     * @return the name shown in warnings and command output
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the number of declared parameters.
     *
     * @return the parameter count
     */
    public int size() {
        return this.resolved.length;
    }

    /**
     * Returns a parameter's resolved value without any type filtering.
     *
     * @param index the parameter index
     * @return the resolved value, or {@code null} when unresolved
     */
    @Nullable
    public Object raw(int index) {
        return this.resolved[index];
    }

    /**
     * Returns a parameter's resolved value cast to {@code type}.
     *
     * @param index the parameter index
     * @param type  the expected type
     * @param <T>   the expected type
     * @return the value, or {@code null} when it is absent or not of {@code type}
     */
    @Nullable
    public <T> T get(int index, Class<T> type) {
        Object value = this.resolved[index];
        return type.isInstance(value) ? type.cast(value) : null;
    }

    @Nullable
    public String text(int index) {
        return this.get(index, String.class);
    }

    /**
     * Returns a numeric parameter as a {@code double}.
     *
     * <p><b>Do not unbox the result in a component registered with {@link Params#allowNulls()}.</b>
     * The accessor then returns {@code null} for an unresolved parameter, and an expression such as
     * {@code call.number(0) < call.number(1)} throws {@link NullPointerException}, which the shared
     * wrapper does not catch. In a body that opts out of the null check, read the value as
     * {@code Double} and branch on {@code null} instead.
     *
     * @param index the parameter index
     * @return the value, or {@code null} when it is absent or not a {@code Double}
     */
    @Nullable
    public Double number(int index) {
        return this.get(index, Double.class);
    }

    /**
     * Returns a numeric parameter as an {@code int}.
     *
     * <p>Same unboxing caveat as {@link #number(int)}: safe only inside a strict body.
     *
     * @param index the parameter index
     * @return the value, or {@code null} when it is absent or not an {@code Integer}
     */
    @Nullable
    public Integer integer(int index) {
        return this.get(index, Integer.class);
    }

    @Nullable
    public Boolean bool(int index) {
        return this.get(index, Boolean.class);
    }

    @Nullable
    public UUID uuid(int index) {
        return this.get(index, UUID.class);
    }

    @Nullable
    public Vec3 vector(int index) {
        return this.get(index, Vec3.class);
    }

    @Nullable
    public ResourceLocation resourceId(int index) {
        return this.get(index, ResourceLocation.class);
    }

    @Nullable
    public Tag tag(int index) {
        return this.get(index, Tag.class);
    }

    @Nullable
    public CompoundTag compound(int index) {
        return this.get(index, CompoundTag.class);
    }

    /**
     * Returns a parameter's raw declaration, for the rare component that needs resolution rules
     * the shared accessors do not express (e.g. a variable holding either one UUID or a list).
     *
     * @param index the parameter index
     * @return the declared {@link RuleParameter}
     */
    public RuleParameter<?> parameter(int index) {
        return this.values.get(index);
    }

    /**
     * Whether a parameter resolved to nothing.
     *
     * @param index the parameter index
     * @return {@code true} when the variable was absent and no constant was declared
     */
    public boolean isNull(int index) {
        return this.resolved[index] == null;
    }

    /**
     * Returns the index of the first unresolved parameter.
     *
     * @return the first unresolved index, or {@code -1} when every parameter resolved
     */
    public int firstNull() {
        for (int index = 0; index < this.resolved.length; index++) {
            if (this.resolved[index] == null) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Records the standard "required parameter resolved to null" warning for one parameter,
     * prefixed with this component instance's name.
     *
     * @param index the unresolved parameter's index
     */
    public void warnNull(int index) {
        this.warn(Util.parseTranslatableText("fmod.rule.warning.nullinput",
                Util.parseTranslatableText(this.parameters.get(index).nameI18nKey)));
    }

    /**
     * Records a component-authored warning, prefixed with this component instance's name.
     *
     * @param message the warning body
     */
    public void warn(@Nullable Component message) {
        if (message == null) {
            return;
        }
        this.context.addWarning(Util.parseTranslatableText("fmod.rule.warning.prefix", this.name, message));
    }

    /**
     * Records the specific reason this component failed and marks it as already reported.
     *
     * <p>Use this instead of {@link #warn} when the component then fails: a writer that returns
     * {@code null} after calling this is not additionally reported by the shared wrapper, so the
     * admin sees the concrete reason once rather than a specific message followed by a generic one.
     *
     * @param message the failure description
     */
    public void fail(@Nullable Component message) {
        this.warn(message);
        this.failureReported = true;
    }

    /**
     * Whether the body already reported why the component failed.
     *
     * @return {@code true} when {@link #fail} was called on this evaluation
     */
    public boolean hasFailureReported() {
        return this.failureReported;
    }

    /**
     * Resolves one parameter to the value its body sees.
     *
     * <p>The order is the one {@link RuleParameter} documents - a matching event variable wins,
     * otherwise the declared constant, otherwise {@code null} - with one widening applied on top:
     * a {@link ParamKind#DOUBLE} parameter accepts any {@link Number} and converts it to
     * {@code double}. Without that widening, storing an integer (a tick count, an item count, a
     * score) and then feeding it to {@code ComputeAdd} reports "null input" instead of doing the
     * arithmetic.
     *
     * @param index the parameter index
     * @return the resolved value, or {@code null} when neither source produced a usable value
     */
    private Object resolve(int index) {
        RuleParameter<?> parameter = this.values.get(index);
        Class<?> type = this.parameters.get(index).kind.valueType();
        Object fromVariable = parameter.getVariableName() == null
                ? null
                : this.context.getVariable(parameter.getVariableName());
        Object resolved = coerce(fromVariable, type);
        return resolved != null ? resolved : coerce(parameter.getConstantValue(), type);
    }

    /**
     * Returns {@code value} when it can feed a parameter of {@code type}, or {@code null} when it
     * cannot.
     *
     * @param value the candidate value, possibly {@code null}
     * @param type  the parameter's declared value type
     * @return the value itself, or the {@code double} widening of a {@link Number} for a
     *         {@link ParamKind#DOUBLE} parameter
     */
    private static Object coerce(Object value, Class<?> type) {
        if (type.isInstance(value)) {
            return value;
        }
        if (type == Double.class && value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
