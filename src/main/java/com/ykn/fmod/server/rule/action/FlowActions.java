/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.HashMap;
import java.util.Map;

import com.ykn.fmod.server.base.command.CustomRuleSuggestion;
import com.ykn.fmod.server.base.command.LogicFlowSuggestion;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ScheduledFlow;
import com.ykn.fmod.server.base.schedule.ScheduledRule;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.RuleManager;

/**
 * Actions that bridge the rule system into the flow system.
 *
 * <p>These are the only rule components allowed to import flow code: running or triggering a flow
 * is their entire purpose.
 */
public final class FlowActions {

    private FlowActions() {
    }

    /**
     * Registers every flow-bridge action type.
     */
    public static void register() {
        Components.action("RunFlow", Params.of("fmod.rule.action.runflow")
                .suggested("flow", ParamKind.STRING, LogicFlowSuggestion.suggest(true))
                .param("delay", ParamKind.intAtLeast(1)),
                (call) -> {
                    String flowName = call.text(0);
                    int ticks = Math.max(1, call.integer(1));
                    ServerData data = Util.getServerData(call.context().getServer());
                    FlowManager targetFlow = data.getLogicFlows().get(flowName);
                    if (targetFlow == null) {
                        call.warnNull(0);
                        return true;
                    }
                    if (!targetFlow.isEnabled()) {
                        call.warn(Util.parseTranslatableText("fmod.rule.action.runflow.disabled", flowName));
                        return true;
                    }
                    Map<String, Object> variables = new HashMap<>(call.context().getVariables());
                    ScheduledFlow scheduled = new ScheduledFlow(targetFlow, null, variables,
                            call.context().getServer(), ticks);
                    try {
                        data.submitScheduledTask(scheduled);
                    } catch (RuntimeException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.action.runflow.schedulefailed"));
                    }
                    return true;
                });

        Components.action("TriggerRule", Params.of("fmod.rule.action.triggerrule")
                .suggested("rule", ParamKind.STRING, CustomRuleSuggestion.suggest(true))
                .param("delay", ParamKind.intAtLeast(1)),
                (call) -> {
                    String ruleName = call.text(0);
                    int delay = call.integer(1);
                    ServerData data = Util.getServerData(call.context().getServer());
                    RuleManager target = data.getCustomRules().get(ruleName);
                    if (target == null || !target.isEnabled()) {
                        call.warn(Util.parseTranslatableText(
                                "fmod.rule.action.triggerrule.unavailable", ruleName));
                        return true;
                    }
                    data.submitScheduledTask(new ScheduledRule(data, target,
                            call.context().getVariables(), delay));
                    return true;
                });
    }
}
