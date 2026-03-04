/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.core.RuleEvent;

/**
 * Wraps a {@link CustomRule} with runtime state: enabled/disabled toggle, an optimised
 * evaluation snapshot, and integration with the server data history.
 *
 * <p>The lifecycle is:
 * <ol>
 *   <li>Create a {@code RuleManager} with the desired rule.</li>
 *   <li>Modify the rule via the mutation methods ({@link #setEvent}, {@link #addCondition}, etc.).
 *       Each mutation sets {@link #enabled} to {@code false} to signal that the optimised snapshot
 *       needs to be rebuilt.</li>
 *   <li>Call {@link #setEnabled(boolean) setEnabled(true)} to build the optimised snapshot and
 *       allow the rule to be dispatched.</li>
 *   <li>From an event callback, call {@link #trigger(com.ykn.fmod.server.base.data.ServerData, java.util.Map)}
 *       to evaluate the rule and run its actions.</li>
 * </ol>
 *
 * <p>The {@code optimizedRule} snapshot is rebuilt on each {@link #setEnabled(boolean)} call
 * so that {@link com.ykn.fmod.server.rule.core.ConditionReference} nodes are inlined once
 * rather than on every evaluation.
 *
 * @see CustomRule
 * @see RuleRegistry
 * @see RuleSerializer
 */
public class RuleManager {

    /** 
     * The mutable rule definition. Changes here require a call to {@link #setEnabled(boolean)} to take effect. 
     */
    private final CustomRule rule;

    /** 
     * The immutable, optimised copy of {@link #rule} used for evaluation, rebuilt on {@link #setEnabled(boolean)}. 
     */
    private volatile CustomRule optimizedRule;

    /** 
     * Whether this rule is currently active and will be evaluated when dispatched. 
     */
    private volatile boolean enabled;

    /**
     * Creates a manager wrapping an existing {@link CustomRule}.
     *
     * <p>The optimised snapshot is built immediately; the rule starts in the <em>disabled</em> state.
     *
     * @param rule the rule to manage
     */
    public RuleManager(CustomRule rule) {
        this.rule = rule;
        this.optimizedRule = rule.optimize();
        this.enabled = false;
    }
    
    /**
     * Creates a manager by building a new rule with the given name and event type string.
     *
     * @param name  the rule identifier
     * @param event the event type string (looked up via {@link RuleRegistry#createRuleEvent(String)})
     */
    public RuleManager(String name, String event) {
        this.rule = CustomRule.build(name, RuleRegistry.createRuleEvent(event));
        this.optimizedRule = rule.optimize();
        this.enabled = false;
    }

    /**
     * Replaces the rule's event by type string.
     *
     * <p>Marks the rule as disabled; call {@link #setEnabled(boolean) setEnabled(true)} to re-enable.
     *
     * @param event the event type string
     */
    public void setEvent(String event) {
        this.rule.setEvent(RuleRegistry.createRuleEvent(event));
        this.enabled = false;
    }

    /**
     * Replaces the rule's event with the given {@link RuleEvent} instance.
     *
     * <p>Marks the rule as disabled.
     *
     * @param event the new event
     */
    public void setEvent(RuleEvent event) {
        this.rule.setEvent(event);
        this.enabled = false;
    }

    /**
     * Adds a named source condition to the rule's {@code extra} list.
     *
     * <p>Marks the rule as disabled.
     *
     * @param condition the condition to add
     */
    public void addCondition(RuleCondition condition) {
        this.rule.addCondition(condition);
        this.enabled = false;
    }

    /**
     * Sets the main condition expression from a formula string.
     *
     * <p>The formula is parsed by
     * {@link ConditionFormulaParser#parse(String)}.
     * Marks the rule as disabled.
     *
     * @param name the boolean formula string (e.g. {@code "inZone && !isOp"})
     */
    public void setCondition(String name) {
        RuleCondition condition = ConditionFormulaParser.parse(name);
        this.rule.setCondition(condition);
        this.enabled = false;
    }

    /**
     * Removes an extra condition by name.
     *
     * <p>Marks the rule as disabled.
     *
     * @param name the name of the condition to remove
     */
    public void removeCondition(String name) {
        this.rule.removeCondition(name);
        this.enabled = false;
    }

    /**
     * Renames an existing extra condition.
     *
     * <p>The condition object is replaced with a copy bearing the new name.
     * Marks the rule as disabled.
     *
     * @param oldName the current name of the condition
     * @param newName the desired new name
     */
    public void renameCondition(String oldName, String newName) {
        RuleCondition newCondition = this.rule.getExtraCondition(oldName).setName(newName);
        this.rule.removeCondition(oldName);
        this.rule.addCondition(newCondition);
        this.enabled = false;
    }

    /**
     * Adds a satisfied-action to the rule.
     *
     * <p>Marks the rule as disabled.
     *
     * @param action the action to add
     */
    public void addActionIfSatisfied(RuleAction action) {
        this.rule.addAction(action);
        this.enabled = false;
    }

    /**
     * Removes a satisfied-action by name.
     *
     * <p>Marks the rule as disabled.
     *
     * @param name the name of the action to remove
     */
    public void removeActionIfSatisfied(String name) {
        this.rule.removeAction(name);
        this.enabled = false;
    }

    /**
     * Renames a satisfied-action.
     *
     * <p>Marks the rule as disabled.
     *
     * @param oldName the current name
     * @param newName the desired new name
     */
    public void renameActionIfSatisfied(String oldName, String newName) {
        RuleAction newAction = this.rule.getActionIfSatisfied(oldName).setName(newName);
        this.rule.removeAction(oldName);
        this.rule.addAction(newAction);
        this.enabled = false;
    }

    /**
     * Adds a violated-action to the rule.
     *
     * <p>Marks the rule as disabled.
     *
     * @param action the action to add
     */
    public void addActionIfViolated(RuleAction action) {
        this.rule.addPunishment(action);
        this.enabled = false;
    }

    /**
     * Removes a violated-action by name.
     *
     * <p>Marks the rule as disabled.
     *
     * @param name the name of the action to remove
     */
    public void removeActionIfViolated(String name) {
        this.rule.removePunishment(name);
        this.enabled = false;
    }

    /**
     * Renames a violated-action.
     *
     * <p>Marks the rule as disabled.
     *
     * @param oldName the current name
     * @param newName the desired new name
     */
    public void renameActionIfViolated(String oldName, String newName) {
        RuleAction newAction = this.rule.getActionIfViolated(oldName).setName(newName);
        this.rule.removePunishment(oldName);
        this.rule.addPunishment(newAction);
        this.enabled = false;
    }

    /**
     * Returns the underlying mutable rule definition.
     *
     * @return the {@link CustomRule} managed by this instance
     */
    public CustomRule getRule() {
        return rule;
    }

    /**
     * Returns whether this rule is currently enabled for evaluation.
     *
     * @return {@code true} if the rule will be evaluated when dispatched
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables this rule and rebuilds the optimised snapshot.
     *
     * <p>Calling {@code setEnabled(true)} rebuilds the optimised snapshot by applying
     * {@link CustomRule#optimize()}, inlining any {@link com.ykn.fmod.server.rule.core.ConditionReference}
     * nodes. This should be called after all mutations are complete.
     *
     * @param enabled {@code true} to activate the rule
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.optimizedRule = this.rule.optimize();
    }

    /**
     * Evaluates the rule's condition (without running actions) and records the result in
     * the server data history.
     *
     * @param data      the server data containing the history buffer
     * @param variables the event-supplied variable map; may be {@code null}
     * @return the populated {@link RuleContext} after condition evaluation
     */
    public RuleContext test(@NotNull ServerData data, @Nullable Map<String, Object> variables) {
        RuleContext context = new RuleContext(data.getServer(), this.optimizedRule, variables);
        data.addRuleHistory(context, Integer.MAX_VALUE);
        context.test();
        return context;
    }

    /**
     * Convenience overload of {@link #test(ServerData, Map)} with no variables.
     *
     * @param data the server data
     * @return the populated {@link RuleContext} after condition evaluation
     */
    public RuleContext test(@NotNull ServerData data) {
        return this.test(data, null);
    }

    /**
     * Evaluates the rule's condition and executes the appropriate action list, then records
     * the result in the server data history.
     *
     * @param data      the server data containing the history buffer
     * @param variables the event-supplied variable map; may be {@code null}
     * @return the populated {@link RuleContext} after full rule execution
     */
    public RuleContext trigger(@NotNull ServerData data, @Nullable Map<String, Object> variables) {
        RuleContext context = new RuleContext(data.getServer(), this.optimizedRule, variables);
        data.addRuleHistory(context, Integer.MAX_VALUE);
        context.trigger();
        return context;
    }

    /**
     * Convenience overload of {@link #trigger(ServerData, Map)} with no variables.
     *
     * @param data the server data
     * @return the populated {@link RuleContext} after full rule execution
     */
    public RuleContext trigger(@NotNull ServerData data) {
        return this.trigger(data, null);
    }
}
