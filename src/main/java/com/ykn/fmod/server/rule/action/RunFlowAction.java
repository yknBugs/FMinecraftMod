/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.command.LogicFlowSuggestion;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ScheduledFlow;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleParameter;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.RequiredParamMetadata;

/**
 * A {@link RuleAction} that schedules the execution of a named logic flow.
 *
 * <p>When executed, this action looks up the target flow by name, checks that it is
 * enabled, then submits a {@link ScheduledFlow} task with the requested delay. All
 * variables currently in the {@link RuleContext} are copied into a mutable map and
 * forwarded to the flow's execution context, allowing rules and flows to share data.
 *
 * <p>The {@code delay} is clamped to a minimum of {@code 1} tick to prevent
 * {@link StackOverflowError} and {@link java.util.ConcurrentModificationException}
 * that would occur if the flow were triggered synchronously within the same tick
 * (e.g., via a "trigger rule" flow node).
 *
 * <p>If the named flow does not exist or is not enabled, the action silently returns
 * {@code true} without executing the flow.
 *
 * <p>JSON value format:
 * <pre>{@code
 * "value": {
 *   "flow":  {"constant": "MyFlow"},
 *   "delay": {"constant": 1}
 * }
 * }</pre>
 */
public class RunFlowAction implements RuleAction {

    private final String name;

    private final RuleParameter<String> flowName;

    private final RuleParameter<Integer> delay;

    public static final String TYPE = "RunFlow";

    public static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.runflow.summary")
        .add(ParamKind.STRING, "fmod.rule.action.runflow.param.flow.name", "fmod.rule.action.runflow.param.flow.desc", "flow", "var.flow",
            LogicFlowSuggestion.suggest(true))
        .add(ParamKind.intAtLeast(1), "fmod.rule.action.runflow.param.delay.name", "fmod.rule.action.runflow.param.delay.desc", "delay", "var.delay");

    /**
     * Creates a {@code RunFlowAction}.
     *
     * @param name   the unique name of this action instance within the rule
     * @param values the parameter values, in {@link #getParameters()} order: {@code flowName},
     *               {@code delay} (clamped to ≥ 1 at execution time)
     */
    @SuppressWarnings("unchecked")
    public RunFlowAction(String name, List<RuleParameter<?>> values) {
        this.name = name;
        this.flowName = (RuleParameter<String>) values.get(0);
        this.delay = (RuleParameter<Integer>) values.get(1);
    }

    @Override
    public boolean execute(RuleContext context) {
        String targetFlowName = this.flowName.resolve(context, String.class);
        Integer delayValue = this.delay.resolve(context, Integer.class);

        if (targetFlowName == null || delayValue == null) {
            return true;
        }

        int ticks = delayValue < 1 ? 1 : delayValue;

        ServerData data = Util.getServerData(context.getServer());
        FlowManager targetFlow = data.getLogicFlows().get(targetFlowName);
        if (targetFlow == null || !targetFlow.isEnabled()) {
            return true;
        }

        Map<String, Object> variables = new HashMap<>(context.getVariables());
        ScheduledFlow scheduledFlow = new ScheduledFlow(targetFlow, null, variables, context.getServer(), ticks);
        data.submitScheduledTask(scheduledFlow);
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RuleAction setName(String name) {
        return new RunFlowAction(name, getParameterValues());
    }

    @Override
    public List<RuleParameter<?>> getParameterValues() {
        return List.of(this.flowName, this.delay);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequiredParamMetadata getParameters() {
        return PARAM_METADATA;
    }

}
