/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;

/**
 * A {@link SourceCondition} that computes {@code a + b} and stores the result in a variable,
 * returning {@code true} from {@link #onEvaluate(RuleContext)}, even when invalid inputs skip the write.
 *
 * <p>This is a data-flow component, not a logical test: its only purpose is the side effect of
 * {@link RuleContext#setVariable(String, Object)}. Being a condition (not a {@link
 * com.ykn.fmod.server.rule.core.RuleAction RuleAction}) lets it be threaded into the condition
 * tree itself via the non-short-circuit {@code &} relationship, ensuring this operand is evaluated before
 * the tree's overall result is used, provided the enclosing expression is reached, e.g.:
 * <pre>{@code
 * (ComputeAdd & ComputeAdd) & (RealFormulaA && RealFormulaB)
 * }</pre>
 * Java guarantees left-to-right evaluation of both operands for {@code &}, so every {@code
 * ComputeAdd} in such a chain runs, in written order, before the real formula is evaluated -
 * unlike {@code &&}, which would skip the right-hand side once the left is {@code false}.
 *
 * <p>The {@code variable} parameter uses {@link ParamKind#VARIABLE_NAME}, which accepts a
 * single-word literal ({@code const}) name without validating {@link RuleCondition#NAME_PATTERN}.
 * A {@code var}-bound name is resolved at evaluation time instead - the variable's current value
 * becomes the target name (pointer-style indirection: if {@code player} currently holds
 * {@code "steve"}, {@code var player} writes into a variable literally named {@code steve}), so
 * it cannot be validated until then. {@link #onEvaluate(RuleContext)} validates the resolved name
 * against {@link RuleCondition#NAME_PATTERN} and skips the write (with a warning) if it fails.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "a":        {"constant": 1.0},
 *   "b":        {"variable": "someDouble"},
 *   "variable": {"constant": "v"}
 * }
 * }</pre>
 *
 * @see RuleContext#setVariable(String, Object)
 * @see ParamKind#VARIABLE_NAME
 */
public class ComputeAdd implements SourceCondition {

    private final String name;

    private final RuleParameter<Double> a;

    private final RuleParameter<Double> b;

    private final RuleParameter<String> variableName;

    public static final String TYPE = "ComputeAdd";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.computeadd.summary")
        .add(ParamKind.DOUBLE, "fmod.rule.condition.computeadd.param.a.name", "fmod.rule.condition.computeadd.param.a.desc", "a", "var.a")
        .add(ParamKind.DOUBLE, "fmod.rule.condition.computeadd.param.b.name", "fmod.rule.condition.computeadd.param.b.desc", "b", "var.b")
        .add(ParamKind.VARIABLE_NAME, "fmod.rule.condition.computeadd.param.variable.name", "fmod.rule.condition.computeadd.param.variable.desc", "variable", "var.variable");

    /**
     * Creates a {@code ComputeAdd} condition.
     *
     * @param name   the unique name of this condition instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code a},
     *               {@code b}, {@code variableName}
     */
    @SuppressWarnings("unchecked")
    public ComputeAdd(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.a = (RuleParameter<Double>) values.get(0);
        this.b = (RuleParameter<Double>) values.get(1);
        this.variableName = (RuleParameter<String>) values.get(2);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Double a = this.a.resolve(context, Double.class);
        Double b = this.b.resolve(context, Double.class);
        String variableName = this.variableName.resolve(context, String.class);

        if (a == null) {
            warnNullInput(context, PARAM_METADATA.get(0));
            return true;
        }
        if (b == null) {
            warnNullInput(context, PARAM_METADATA.get(1));
            return true;
        }
        if (variableName == null) {
            warnNullInput(context, PARAM_METADATA.get(2));
            return true;
        }
        if (!RuleCondition.NAME_PATTERN.matcher(variableName).matches()) {
            addWarning(context, Util.parseTranslatableText("fmod.rule.param.variablename.invalid", variableName));
            return true;
        }

        context.setVariable(variableName, a + b);
        return true;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public RuleCondition setName(String name) {
        return new ComputeAdd(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.a, this.b, this.variableName);
    }

}
