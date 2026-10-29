/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.ActionBody;
import com.ykn.fmod.server.rule.tool.PredicateBody;
import com.ykn.fmod.server.rule.tool.RuleRegistry;
import com.ykn.fmod.server.rule.tool.WriterBody;

/**
 * Registers parameter-backed rule components without a class per type.
 *
 * <p>A component type is fully described by three things next to each other:
 * <ol>
 *   <li>its type id – the string stored in rule JSON and typed in commands</li>
 *   <li>its {@link Params} – the declared parameters and their i18n keys</li>
 *   <li>its body – the actual logic</li>
 * </ol>
 *
 * <p>Conventions for a family of types:
 * <ul>
 *   <li>one {@code XxxConditions} / {@code XxxActions} class per domain, holding a
 *       {@code register()} method with one entry per type</li>
 *   <li>the type id is written once, in that entry; nothing else refers to it by name</li>
 *   <li>the i18n prefix is {@code fmod.rule.condition.<id lowercased>} or
 *       {@code fmod.rule.action.<id lowercased>}</li>
 * </ul>
 *
 * <p>Failure policy, applied uniformly to every registered body:
 * <ul>
 *   <li>a declared parameter that resolves to {@code null} – the first such parameter is
 *       reported through {@code fmod.rule.warning.nullinput} and the condition is
 *       {@code false}; this applies to strict components only</li>
 *   <li>an exception thrown by the body – not caught here. <em>Every</em> throw site is reviewed
 *       case by case; expected failures are handled at the site with a warning, and anything else
 *       reaches {@link RuleContext#test()} / {@link RuleContext#trigger()}, which log a stack trace
 *       and abort the current evaluation</li>
 * </ul>
 *
 * @see Params
 * @see PredicateBody
 */
public final class Components {

    private Components() {
    }

    /**
     * Registers a condition type that is a pure predicate.
     *
     * <p>Example:
     * <pre>{@code
     * Components.predicate("SmallerThan", Params.of("fmod.rule.condition.smallerthan")
     *         .param("left", ParamKind.DOUBLE)
     *         .param("right", ParamKind.DOUBLE),
     *     (call) -> call.number(0) < call.number(1));
     * }</pre>
     *
     * @param type   the type id stored in rule JSON and used by commands
     * @param params the declared parameters, including their i18n keys
     * @param body   the predicate logic
     */
    public static void predicate(String type, Params params, PredicateBody body) {
        RequiredParamMetadata metadata = params.metadata();
        boolean allowNulls = params.allowsNulls();
        RuleRegistry.registerCondition(type, metadata,
                (name, values) -> new ScriptedCondition(type, name, metadata, values, body, allowNulls));
    }

    /**
     * Registers a condition type that computes a value and stores it in a variable.
     *
     * <p>The component evaluates to {@code true} when the value was stored, and to {@code false}
     * when it was not: a missing parameter, an invalid variable name, or a body that returned
     * {@code null}. Every such failure records a warning first, except that a body which already
     * called {@link ComponentCall#fail} is not reported twice with a generic message.
     * Exceptions are not caught here; a body that can fail for an expected reason must handle it
     * and return {@code null}.
     *
     * <p>Because a failed writer is {@code false}, {@code &&} gives evaluation order for free:
     * {@code ComputeA && ComputeB && Check} skips everything after a failed compute, while {@code |}
     * still runs all of them. Nothing is lost by reporting the failure instead of hiding it.
     *
     * <p>A writer registered with {@link Params#allowNulls()} changes two things, both of them
     * deliberate: an unresolved parameter no longer aborts the write, and a body that returns
     * {@code null} stores an explicit null instead of failing. {@code SetVariable} is the type that
     * needs both, because publishing {@code null} is a legitimate operation (see
     * {@link RuleContext#setVariable(String, Object)}). Such a body must report its own failures
     * through {@link ComponentCall#fail}, since a plain {@code null} return is no longer an error.
     *
     * <p>Example:
     * <pre>{@code
     * Components.writer("ComputeAdd", Params.of("fmod.rule.condition.computeadd")
     *         .param("a", ParamKind.DOUBLE)
     *         .param("b", ParamKind.DOUBLE)
     *         .output("variable", ParamKind.VARIABLE_NAME),
     *     (call) -> call.number(0) + call.number(1));
     * }</pre>
     *
     * @param type   the type id stored in rule JSON and used by commands
     * @param params the declared parameters, one of them declared through {@link Params#output}
     * @param body   the computation
     * @throws IllegalArgumentException when no output parameter was declared
     */
    public static void writer(String type, Params params, WriterBody body) {
        RequiredParamMetadata metadata = params.metadata();
        int output = params.outputIndex();
        if (output < 0) {
            throw new IllegalArgumentException(type + " must declare an output parameter");
        }
        boolean allowNulls = params.allowsNulls();
        RuleRegistry.registerCondition(type, metadata,
                (name, values) -> new ScriptedWriter(type, name, metadata, values, body, output, allowNulls));
    }

    /**
     * Registers an action type.
     *
     * <p>A missing parameter is reported and the action is skipped, but the remaining actions
     * still run. A body that returns {@code false} has failed without being able to do its job:
     * the wrapper reports {@code fmod.rule.warning.operationfailed} (unless the body already
     * reported a specific reason through {@link ComponentCall#fail}) and continues with the next
     * action, so one bad action never cancels the rest. An exception is not caught here.
     *
     * @param type   the type id stored in rule JSON and used by commands
     * @param params the declared parameters, including their i18n keys
     * @param body   the side effect
     */
    public static void action(String type, Params params, ActionBody body) {
        RequiredParamMetadata metadata = params.metadata();
        RuleRegistry.registerAction(type, metadata,
                (name, values) -> new ScriptedAction(type, name, metadata, values, body));
    }

    /**
     * The single {@link SourceCondition} implementation behind every predicate type.
     *
     * <p>Everything type-specific lives in a component's {@link Params} and body, so this
     * class is written once instead of once per type.
     */
    private static final class ScriptedCondition implements SourceCondition {

        private final String type;

        private final String name;

        private final RequiredParamMetadata parameters;

        private final List<RuleParameter<?>> values;

        private final PredicateBody body;

        private final boolean allowNulls;

        private ScriptedCondition(String type, String name, RequiredParamMetadata parameters,
                List<RuleParameter<?>> values, PredicateBody body, boolean allowNulls) {
            this.type = type;
            this.name = name;
            this.parameters = parameters;
            this.values = List.copyOf(values);
            this.body = body;
            this.allowNulls = allowNulls;
        }

        @Override
        public boolean onEvaluate(RuleContext context) {
            ComponentCall call = new ComponentCall(context, this.name, this.parameters, this.values);
            if (!this.allowNulls) {
                int missing = call.firstNull();
                if (missing >= 0) {
                    call.warnNull(missing);
                    return false;
                }
            }
            return this.body.test(call);
        }

        @Override
        public String getType() {
            return this.type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public RequiredParamMetadata getParameters() {
            return this.parameters;
        }

        @Override
        public List<RuleParameter<?>> getParameterValues() {
            return this.values;
        }

        @Override
        public RuleCondition setName(String name) {
            return new ScriptedCondition(this.type, name, this.parameters, this.values, this.body, this.allowNulls);
        }
    }

    /**
     * The single {@link SourceCondition} implementation behind every writer type. See
     * {@link #writer}.
     */
    private static final class ScriptedWriter implements SourceCondition {

        private final String type;

        private final String name;

        private final RequiredParamMetadata parameters;

        private final List<RuleParameter<?>> values;

        private final WriterBody body;

        private final int output;

        private final boolean allowNulls;

        private ScriptedWriter(String type, String name, RequiredParamMetadata parameters,
                List<RuleParameter<?>> values, WriterBody body, int output, boolean allowNulls) {
            this.type = type;
            this.name = name;
            this.parameters = parameters;
            this.values = List.copyOf(values);
            this.body = body;
            this.output = output;
            this.allowNulls = allowNulls;
        }

        @Override
        public boolean onEvaluate(RuleContext context) {
            ComponentCall call = new ComponentCall(context, this.name, this.parameters, this.values);
            if (!this.allowNulls) {
                int missing = call.firstNull();
                if (missing >= 0) {
                    call.warnNull(missing);
                    return false;
                }
            }
            String target = call.text(this.output);
            if (target == null || !RuleCondition.NAME_PATTERN.matcher(target).matches()) {
                call.warn(Util.parseTranslatableText("fmod.rule.param.variablename.invalid", String.valueOf(target)));
                return false;
            }
            Object value = this.body.compute(call);
            if (value == null) {
                if (!this.allowNulls) {
                    if (!call.hasFailureReported()) {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
                    }
                    return false;
                }
                if (call.hasFailureReported()) {
                    return false;
                }
                // An allowNulls writer stores an explicit null instead of failing; the target name
                // was validated above, so the write itself cannot fail.
                context.setVariable(target, null);
                return true;
            }
            if (value instanceof Double number && number.isNaN()) {
                // Stored anyway: a computed NaN is a result, not a failure. The warning keeps it visible.
                call.warn(Util.parseTranslatableText("fmod.rule.warning.nanresult"));
            }
            context.setVariable(target, value);
            return true;
        }

        @Override
        public String getType() {
            return this.type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public RequiredParamMetadata getParameters() {
            return this.parameters;
        }

        @Override
        public List<RuleParameter<?>> getParameterValues() {
            return this.values;
        }

        @Override
        public RuleCondition setName(String name) {
            return new ScriptedWriter(this.type, name, this.parameters, this.values, this.body, this.output, this.allowNulls);
        }
    }

    /**
     * The single {@link RuleAction} implementation behind every action type. See
     * {@link #action}.
     */
    private static final class ScriptedAction implements RuleAction {

        private final String type;

        private final String name;

        private final RequiredParamMetadata parameters;

        private final List<RuleParameter<?>> values;

        private final ActionBody body;

        private ScriptedAction(String type, String name, RequiredParamMetadata parameters,
                List<RuleParameter<?>> values, ActionBody body) {
            this.type = type;
            this.name = name;
            this.parameters = parameters;
            this.values = List.copyOf(values);
            this.body = body;
        }

        @Override
        public boolean execute(RuleContext context) {
            ComponentCall call = new ComponentCall(context, this.name, this.parameters, this.values);
            int missing = call.firstNull();
            if (missing >= 0) {
                call.warnNull(missing);
                return true;
            }
            if (this.body.run(call)) {
                return true;
            }
            if (!call.hasFailureReported()) {
                call.warn(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
            }
            return true;
        }

        @Override
        public String getType() {
            return this.type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public RequiredParamMetadata getParameters() {
            return this.parameters;
        }

        @Override
        public List<RuleParameter<?>> getParameterValues() {
            return this.values;
        }

        @Override
        public RuleAction setName(String name) {
            return new ScriptedAction(this.type, name, this.parameters, this.values, this.body);
        }
    }
}
