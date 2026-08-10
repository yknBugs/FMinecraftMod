/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
import com.ykn.fmod.server.rule.tool.RecursiveCommandBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
 *   "flowName": {"constant": "MyFlow"},
 *   "delay":    {"constant": 1}
 * }
 * }</pre>
 */
public class RunFlowAction implements RuleAction {

    private final String name;

    private final RuleParameter<String> flowName;

    private final RuleParameter<Integer> delay;

    private static final String TYPE = "RunFlow";

    private static final RequiredParamMetadata PARAM_METADATA = RequiredParamMetadata.create("fmod.rule.action.runflow.summary")
        .add(ParamKind.STRING, "fmod.rule.action.runflow.param.flow.name", "fmod.rule.action.runflow.param.flow.desc", "flow", "var.flow")
        .add(ParamKind.intAtLeast(1), "fmod.rule.action.runflow.param.delay.name", "fmod.rule.action.runflow.param.delay.desc", "delay", "var.delay");

    /**
     * Creates a {@code RunFlowAction}.
     *
     * @param name     the unique name of this action instance within the rule
     * @param flowName the name of the logic flow to trigger
     * @param delay    the number of ticks to wait before running the flow (clamped to ≥ 1)
     */
    public RunFlowAction(String name, RuleParameter<String> flowName, RuleParameter<Integer> delay) {
        this.name = name;
        this.flowName = flowName;
        this.delay = delay;
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
        return new RunFlowAction(name, this.flowName, this.delay);
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
    public Component render() {
        return Util.parseTranslatableText("fmod.rule.action.runflow", this.getName(), this.getType(),
            this.flowName.render(), this.delay.render());
    }

    @Override
    public JsonObject getValueJson() {
        JsonObject json = new JsonObject();
        json.add("flowName", RuleParameter.toJson(flowName, JsonPrimitive::new));
        json.add("delay", RuleParameter.toJson(delay, JsonPrimitive::new));
        return json;
    }

    public static JsonObject toJson(RunFlowAction action) {
        return action.toJson();
    }

    public static RunFlowAction fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        RuleParameter<String> flowName = RuleParameter.fromJson(json, "flowName", JsonElement::getAsString);
        RuleParameter<Integer> delay = RuleParameter.fromJson(json, "delay", JsonElement::getAsInt);
        return new RunFlowAction(name, flowName, delay);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(LiteralArgumentBuilder<CommandSourceStack> commandNode, BiConsumer<CommandContext<CommandSourceStack>, RuleAction> actionConsumer) {
        LogicFlowSuggestion flowSuggestion = LogicFlowSuggestion.suggest(true);
        RecursiveCommandBuilder builder = RecursiveCommandBuilder.builder();
        builder.executes((arguments, ctx) -> {
                String name = StringArgumentType.getString(ctx, "name");
                RuleParameter<String> flowNameParameter = builder.resolveParameter(0, arguments, ctx);
                RuleParameter<Integer> delayParameter = builder.resolveParameter(1, arguments, ctx);
                RunFlowAction action = new RunFlowAction(name, flowNameParameter, delayParameter);
                actionConsumer.accept(ctx, action);
            })
            .add(PARAM_METADATA.get(0), flowSuggestion)
            .add(PARAM_METADATA.get(1));
        RequiredArgumentBuilder<CommandSourceStack, ?> commandTree = builder.build(Commands.argument("name", StringArgumentType.string()));
        return commandNode.executes(builder.usageExecutor(TYPE, PARAM_METADATA.getSummaryI18nKey())).then(commandTree);
    }
}
