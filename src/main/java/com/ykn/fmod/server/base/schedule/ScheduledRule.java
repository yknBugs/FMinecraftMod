/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.rule.tool.RuleManager;

/**
 * One deferred explicit rule trigger on the server thread. The delay is always at least one tick.
 *
 * <p>Variable bindings are shallow-copied at scheduling time; mutable value objects stay shared.
 * A disabled, deleted, or replaced target cancels the pending invocation. The rule definition used
 * is the manager's current enabled snapshot when the task fires. Event validation is skipped
 * because this is an explicit trigger; the rule history still records the result.
 */
public final class ScheduledRule extends ScheduledTask {

    private final ServerData data;

    private final RuleManager target;

    private final Map<String, Object> variables;

    /**
     * Schedules one explicit trigger.
     *
     * @param data      the server data owning the rule
     * @param target    the rule manager to trigger
     * @param variables the variable bindings to pass along, copied
     * @param delay     the delay in ticks, clamped to at least one
     */
    public ScheduledRule(ServerData data, RuleManager target, Map<String, Object> variables, int delay) {
        super(Math.max(1, delay), 0);
        this.data = Objects.requireNonNull(data);
        this.target = Objects.requireNonNull(target);
        this.variables = variables == null ? new HashMap<>() : new HashMap<>(variables);
    }

    @Override
    public boolean shouldCancel() {
        return !this.target.isEnabled()
                || this.data.getCustomRules().get(this.target.getRule().getName()) != this.target;
    }

    @Override
    public void onTrigger() {
        if (!shouldCancel()) {
            this.target.trigger(this.data, this.variables, true);
        }
    }

    /**
     * Reschedules one invocation, clamping the delay to one tick; the duration is ignored.
     *
     * @param delay    the new delay in ticks
     * @param duration ignored
     */
    @Override
    public void reschedule(int delay, int duration) {
        super.reschedule(Math.max(1, delay), 0);
    }

    @Override
    public String toString() {
        return "ScheduledRule{rule='" + this.target.getRule().getName() + "'}";
    }
}
