/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import java.util.List;

import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.SourceCondition;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;

/**
 * A {@link SourceCondition} that tests whether {@code left.equals(right)}.
 *
 * <p>Parameters are resolved as {@link Object} and compared with
 * {@link Object#equals(Object)}.
 *
 * <p>Because constants are stored as strings in JSON, both the {@code left} and
 * {@code right} parameters are serialised as plain string values. Variable
 * bindings may carry any runtime type.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "left":  {"variable": "someVar"},
 *   "right": {"constant": "expectedValue"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class EqualsTo implements SourceCondition {

    private final String name;

    private final RuleParameter<Object> left;

    private final RuleParameter<Object> right;

    public static final String TYPE = "EqualsTo";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.equalsto.summary")
        .add(ParamKind.AUTO, "fmod.rule.condition.equalsto.param.left.name", "fmod.rule.condition.equalsto.param.left.desc", "left", "var.left")
        .add(ParamKind.AUTO, "fmod.rule.condition.equalsto.param.right.name", "fmod.rule.condition.equalsto.param.right.desc", "right", "var.right");

    @SuppressWarnings("unchecked")
    public EqualsTo(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.left = (RuleParameter<Object>) values.get(0);
        this.right = (RuleParameter<Object>) values.get(1);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Object left = this.left.resolve(context, Object.class);
        Object right = this.right.resolve(context, Object.class);

        if (left == null && right == null) {
            return true;
        }

        if (left == null || right == null) {
            return false;
        }

        return left.equals(right);
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
        return new EqualsTo(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.left, this.right);
    }

}
