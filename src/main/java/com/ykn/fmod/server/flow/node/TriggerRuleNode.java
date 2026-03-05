/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.tool.RuleManager;

/**
 * A flow node that triggers a custom rule from the rule system.
 * Inputs:
 * 1. String - The name of the rule to trigger.
 * 2. Boolean - Whether to run rule actions if the condition passes. Default is true.
 * 3. Boolean - Whether to pass current flow variables to the rule context. Default is true.
 * Outputs:
 * 1. Boolean - Whether the rule condition passed.
 * 2. Text - The error message set by the rule, or null if none.
 * Branches: 1 (Next node)
 */
public class TriggerRuleNode extends FlowNode {

    public TriggerRuleNode(long id, String name) {
        super(id, name, 3, 2, 1, "TriggerRuleNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.triggerrule.title.name", "fmod.node.triggerrule.title.feat")
            .input("fmod.node.triggerrule.input.rule.name", "fmod.node.triggerrule.input.rule.feat", "fmod.node.triggerrule.input.rule.type")
            .input("fmod.node.triggerrule.input.runactions.name", "fmod.node.triggerrule.input.runactions.feat", "fmod.node.triggerrule.input.runactions.type")
            .input("fmod.node.triggerrule.input.keepvars.name", "fmod.node.triggerrule.input.keepvars.feat", "fmod.node.triggerrule.input.keepvars.type")
            .output("fmod.node.triggerrule.output.passed.name", "fmod.node.triggerrule.output.passed.feat", "fmod.node.triggerrule.output.passed.type")
            .output("fmod.node.triggerrule.output.error.name", "fmod.node.triggerrule.output.error.feat", "fmod.node.triggerrule.output.error.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        String ruleName = TypeAdaptor.parse(resolvedInputs.get(0)).asString();
        Boolean runActionsValue = TypeAdaptor.parse(resolvedInputs.get(1)).asBoolean();
        Boolean keepVariablesValue = TypeAdaptor.parse(resolvedInputs.get(2)).asBoolean();
        boolean runActions = runActionsValue == null ? true : runActionsValue.booleanValue();
        boolean keepVariables = keepVariablesValue == null ? true : keepVariablesValue.booleanValue();

        ServerData data = Util.getServerData(context.getServer());
        RuleManager ruleManager = data.getCustomRules().get(ruleName);
        if (ruleManager == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.triggerrule.error.norule", ruleName), null);
        }
        if (!ruleManager.isEnabled()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.triggerrule.error.disabled", ruleName), null);
        }

        Map<String, Object> variables = keepVariables ? new HashMap<>(context.getVariables()) : null;

        RuleContext ruleContext = null;
        if (runActions) {
            ruleContext = ruleManager.trigger(data, variables, true);
        } else {
            ruleContext = ruleManager.test(data, variables, true);
        }

        status.setOutput(0, ruleContext.isPassed());
        status.setOutput(1, ruleContext.getErrorMessage());
    }
}
