/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import com.ykn.fmod.server.base.util.SplitMixRandom;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import java.util.Locale;

/**
 * A flow node that samples from its own random source, independent of Minecraft randomness.
 * Inputs:
 * 1. Number - Minimum for uniform/triangular; mean for normal. Default 0.
 * 2. Number - Maximum for uniform/triangular; nonnegative standard deviation for normal. Default 1.
 * 3. String - uniform (flat), normal (gaussian/gauss), triangular (triangle). Case-insensitive; default uniform. Triangular mode is the midpoint.
 * Outputs:
 * 1. Number - Double sample. Uniform/triangular use [min,max); equal bounds return the bound. Zero standard deviation returns the mean.
 * Branches: 1 (Next node)
 */
public class RandomNode extends FlowNode {

    public RandomNode(long id, String name) {
        super(id, name, 3, 1, 1, "RandomNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        return NodeMetadata.builder("fmod.node.random.title.name", "fmod.node.random.title.feat")
                .input("fmod.node.random.input.first.name", "fmod.node.random.input.first.feat", "fmod.node.random.input.first.type")
                .input("fmod.node.random.input.second.name", "fmod.node.random.input.second.feat", "fmod.node.random.input.second.type")
                .input("fmod.node.random.input.distribution.name", "fmod.node.random.input.distribution.feat", "fmod.node.random.input.distribution.type")
                .output("fmod.node.random.output.value.name", "fmod.node.random.output.value.feat", "fmod.node.random.output.value.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    // Owned exclusively by flows; never uses a world/entity random source.
    private static final SplitMixRandom RANDOM = new SplitMixRandom(System.nanoTime() ^ System.currentTimeMillis());

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Double first = TypeAdaptor.parse(inputs.get(0)).asDouble();
        Double second = TypeAdaptor.parse(inputs.get(1)).asDouble();
        if ((inputs.get(0) != null && first == null) || (inputs.get(1) != null && second == null)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.random.error.parameters"), null);
        }
        String distribution = inputs.get(2) == null ? null : TypeAdaptor.parse(inputs.get(2)).asString();
        String kind = distribution == null ? "uniform" : distribution.trim().toLowerCase(Locale.ROOT);
        double a = first == null ? 0 : first;
        double b = second == null ? 1 : second;
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.random.error.finite"), null);
        }
        double result;
        switch (kind) {
            case "normal", "gaussian", "gauss" -> {
                if (b < 0) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.random.error.deviation"),
                            null);
                }
                result = b == 0 ? a : a + b * RANDOM.nextGaussian();
            }
            case "uniform", "flat", "triangular", "triangle" -> {
                if (a > b) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.random.error.bounds"), null);
                }
                double unit = RANDOM.nextDouble();
                if (kind.equals("triangular") || kind.equals("triangle")) {
                    unit = unit < 0.5 ? Math.sqrt(unit / 2) : 1 - Math.sqrt((1 - unit) / 2);
                }
                double span = b - a;
                result = (a == b) ? 
                        a : Math.max(
                                a, Math.min(Math.nextDown(b),
                                Double.isFinite(span) ? a + span * unit : a * (1 - unit) + b * unit)
                            );
            }
            default -> throw new LogicException(null, Util.parseTranslatableText("fmod.node.random.error.distribution", kind), null);
        }
        if (!Double.isFinite(result)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.random.error.overflow"), null);
        }
        status.setOutput(0, result);
    }

}
