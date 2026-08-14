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
 * A {@link SourceCondition} that tests whether {@code left} is strictly less than {@code right}.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "left":  {"constant": 1.0},
 *   "right": {"variable": "someDouble"}
 * }
 * }</pre>
 *
 * @see RuleParameter
 */
public class SmallerThan implements SourceCondition {

    private final String name;

    private final RuleParameter<Double> left;

    private final RuleParameter<Double> right;

    public static final String TYPE = "SmallerThan";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.condition.smallerthan.summary")
        .add(ParamKind.DOUBLE, "fmod.rule.condition.smallerthan.param.left.name", "fmod.rule.condition.smallerthan.param.left.desc", "left", "var.left")
        .add(ParamKind.DOUBLE, "fmod.rule.condition.smallerthan.param.right.name", "fmod.rule.condition.smallerthan.param.right.desc", "right", "var.right");

    @SuppressWarnings("unchecked")
    public SmallerThan(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.left = (RuleParameter<Double>) values.get(0);
        this.right = (RuleParameter<Double>) values.get(1);
    }

    @Override
    public boolean onEvaluate(RuleContext context) {
        Double left = this.left.resolve(context, Double.class);
        Double right = this.right.resolve(context, Double.class);

        if (left == null || right == null) {
            return false;
        }

        return left < right;
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
        return new SmallerThan(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.left, this.right);
    }

}
