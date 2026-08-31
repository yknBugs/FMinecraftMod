/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/**
 * Execution context that is created for each rule evaluation with immutable input fields and mutable status fields.
 *
 * <p>A {@code RuleContext} bundles together everything needed to evaluate a rule:
 * the {@link MinecraftServer}, the optimised {@link CustomRule}, and the
 * {@code variables} map populated by the event dispatcher.
 * The context is passed through every call to {@link RuleCondition#evaluate(RuleContext)}
 * and {@link RuleAction#execute(RuleContext)}.
 *
 * <p>Status fields ({@link #isExecuted()}, {@link #isPassed()}, {@link #isSkipActions()},
 * {@link #getErrorMessage()}) are mutable and are set by {@link #test()} / {@link #trigger()}.
 * They are reset before each execution via {@link #resetStatus()}. The variable map is similarly
 * split into an immutable, event-supplied half and a mutable, per-evaluation overlay written via
 * {@link #setVariable(String, Object)} - see that method's docs for why.
 *
 * <p>The context stores a <em>copy</em> of the rule at construction time, so modifications
 * to the original rule during execution do not affect an in-flight evaluation.
 *
 * @see RuleManager
 * @see CustomRule
 */
public class RuleContext {

    /** 
     * The Minecraft server instance shared across the evaluation. 
     */
    private final MinecraftServer server;

    /** 
     * The rule being evaluated (a snapshot copy taken at construction time). 
     */
    private final CustomRule rule;

    /**
     * Immutable variable map supplied by the event dispatcher.
     * An empty map is used when no variables were provided.
     */
    private final Map<String, Object> variables;

    /**
     * Mutable overlay written by {@link #setVariable(String, Object)}, layered on top of
     * {@link #variables} by {@link #getVariable(String)}/{@link #getVariables()} so that a
     * side-effecting component (e.g. a "compute and store" condition or action) can hand a
     * derived value to every component evaluated after it, without the event dispatcher needing
     * to know about it.
     *
     * <p>Entries here take precedence over same-named entries in {@link #variables} - a computed
     * value is a more local, more recent write than whatever the event happened to supply.
     * Cleared by {@link #resetStatus()} before each new evaluation, same lifetime as the other
     * status fields.
     */
    private Map<String, Object> computedVariables;

    /** 
     * Whether this context has already run {@link #test()} or {@link #trigger()}. 
     */
    private boolean executed;

    /**
     * When {@code true}, the last {@link #test()} / {@link #trigger()} invocation
     * will only evaluate conditions and not execute any actions.
     */
    private boolean skipActions;

    /** 
     * Whether the condition evaluated to {@code true} in the last execution. 
     */
    private boolean passed;

    /**
     * Tracks the set of {@link RuleCondition} instances that are currently on the evaluation
     * call stack for this context.
     *
     * <p>This set is the foundation of the cyclic-reference detection mechanism: before
     * evaluating a condition, {@link RuleCondition#evaluate(RuleContext)} calls
     * {@link #addTestingConditions(RuleCondition)}; if the condition is already present
     * (i.e. it is an ancestor on the current call stack), a cycle is detected and evaluation
     * is short-circuited with an error.  The condition is removed by
     * {@link #removeTestingConditions(RuleCondition)} after evaluation completes.
     *
     * <p>The set is cleared by {@link #resetStatus()} before each new evaluation.
     */
    private HashSet<RuleCondition> testingConditions;

    /**
     * Optional error text set when an unexpected exception occurs during evaluation.
     */
    @Nullable
    private Component errorMessage;

    /**
     * Non-fatal warning messages logged by {@link RuleEvent}/{@link RuleCondition}/{@link RuleAction}
     * implementations during evaluation, for admin debugging.
     *
     * <p>Unlike {@link #errorMessage}, warnings do not abort evaluation or affect
     * {@link #isPassed()}; components simply append to this list via {@link #addWarning(Component)}
     * as they run. Cleared by {@link #resetStatus()} before each new evaluation.
     */
    private List<Component> warnings;

    /**
     * Creates a new {@code RuleContext}.
     *
     * @param server    the Minecraft server; must not be {@code null}
     * @param rule      the rule to evaluate; a defensive copy is made
     * @param variables the event-supplied variable map, or {@code null} for an empty map
     */
    public RuleContext(@NotNull MinecraftServer server, @NotNull CustomRule rule, @Nullable Map<String, Object> variables) {
        this.server = server;
        this.rule = rule.copy();
        if (variables == null) {
            this.variables = Collections.emptyMap();
        } else {
            this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
        }
        this.executed = false;
        this.passed = false;
        this.skipActions = false;
        this.testingConditions = new HashSet<>();
        this.errorMessage = null;
        this.warnings = new ArrayList<>();
        this.computedVariables = new HashMap<>();
    }

    /**
     * Returns an unmodifiable view of every variable currently visible to components: the
     * event-supplied {@link #variables} with {@link #computedVariables} layered on top.
     *
     * @return a non-null, unmodifiable map
     */
    @NotNull
    public Map<String, Object> getVariables() {
        if (computedVariables.isEmpty()) {
            return variables;
        }
        Map<String, Object> merged = new HashMap<>(variables);
        merged.putAll(computedVariables);
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Returns the value of a single variable, or {@code null} if not present.
     *
     * <p>Checks {@link #computedVariables} first, falling back to the event-supplied
     * {@link #variables} - see {@link #setVariable(String, Object)} for why.
     *
     * @param name the variable name
     * @return the variable value, or {@code null}
     */
    @Nullable
    public Object getVariable(String name) {
        if (computedVariables.containsKey(name)) {
            return computedVariables.get(name);
        }
        return variables.get(name);
    }

    /**
     * Writes (or overwrites) a variable in the mutable overlay, making it visible to every
     * component evaluated after this call via {@link #getVariable(String)}/{@link #getVariables()} -
     * including a triggered flow, since {@code RunFlowAction} forwards {@link #getVariables()}.
     *
     * <p>Intended for components whose whole purpose is to derive and publish a value for later
     * use in the same evaluation (e.g. a "compute and store" condition/action), not for general
     * mutation of event data. The write only lives for the current {@link #test()}/{@link #trigger()}
     * call - it is cleared by {@link #resetStatus()} like every other status field, so nothing
     * persists across separate rule evaluations.
     *
     * @param name  the variable name to write
     * @param value the value to store; {@code null} is a valid value and shadows any
     *              same-named event-supplied variable with an explicit null
     */
    public void setVariable(String name, Object value) {
        this.computedVariables.put(name, value);
    }

    /**
     * Returns the rule snapshot that this context was created for.
     *
     * @return a non-null copy of the rule
     */
    @NotNull
    public CustomRule getRule() {
        return rule;
    }

    /**
     * Returns the Minecraft server instance.
     *
     * @return a non-null {@link MinecraftServer}
     */
    @NotNull
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Returns whether this context has already been executed.
     *
     * @return {@code true} after {@link #test()} or {@link #trigger()} has been called
     */
    public boolean isExecuted() {
        return executed;
    }

    /**
     * Returns whether the rule's condition was satisfied in the last execution.
     *
     * @return {@code true} if the condition evaluated to {@code true}
     */
    public boolean isPassed() {
        return passed;
    }

    /**
     * Returns whether actions were skipped in the last execution.
     *
     * <p>{@code true} after a call to {@link #test()} (condition-only evaluation);
     * {@code false} after a call to {@link #trigger()} (condition + actions).
     *
     * @return {@code true} if actions were not executed
     */
    public boolean isSkipActions() {
        return skipActions;
    }

    /**
     * Registers {@code condition} as currently being evaluated, for cyclic-reference detection.
     *
     * <p>Called by {@link RuleCondition#evaluate(RuleContext)} before delegating to
     * {@link RuleCondition#onEvaluate(RuleContext)}.  If {@code condition} is already
     * present in the set, the call stack contains a cycle and this method returns
     * {@code false} so that the caller can bail out immediately.
     *
     * @param condition the condition about to be evaluated
     * @return {@code true} if the condition was successfully added (no cycle);
     *         {@code false} if it was already present, indicating a cyclic reference
     */
    public boolean addTestingConditions(RuleCondition condition) {
        if (this.testingConditions.contains(condition)) {
            return false;
        }
        this.testingConditions.add(condition);
        return true;
    }

    /**
     * Removes {@code condition} from the currently-evaluating set after its evaluation is done.
     *
     * <p>Called by {@link RuleCondition#evaluate(RuleContext)} after
     * {@link RuleCondition#onEvaluate(RuleContext)} returns, to pop the condition off
     * the logical call-stack tracker.
     *
     * @param condition the condition that has finished evaluating
     * @return {@code true} if the condition was present and successfully removed
     */
    public boolean removeTestingConditions(RuleCondition condition) {
        return this.testingConditions.remove(condition);
    }

    /**
     * Returns the error message set during execution, or {@code null} if none occurred.
     *
     * @return the error {@link Component}, or {@code null}
     */
    @Nullable
    public Component getErrorMessage() {
        return errorMessage;
    }

    /**
     * Stores an error message to be displayed in the rule history.
     *
     * @param errorMessage the error text to store
     */
    public void setErrorMessage(Component errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns an unmodifiable view of the warning messages logged so far during this evaluation.
     *
     * @return a non-null, unmodifiable list, in the order the warnings were added
     */
    @NotNull
    public List<Component> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Logs a non-fatal warning message for admin debugging.
     *
     * <p>Intended to be called by {@link RuleEvent}, {@link RuleCondition}, and {@link RuleAction}
     * implementations while they run, e.g. to flag a recoverable misconfiguration. Does not affect
     * {@link #isPassed()} or abort evaluation.
     *
     * @param warning the warning text to record
     */
    public void addWarning(Component warning) {
        this.warnings.add(warning);
    }

    /**
     * Resets all execution status fields to their initial state
     * (not executed, not passed, actions not skipped, no error message, no warnings, no computed
     * variables, no testing conditions).
     *
     * <p>Also clears the {@code testingConditions} set so that cyclic-reference detection
     * state from a previous evaluation does not bleed into the next one.
     */
    public void resetStatus() {
        this.executed = false;
        this.passed = false;
        this.skipActions = false;
        this.testingConditions.clear();
        this.errorMessage = null;
        this.warnings.clear();
        this.computedVariables.clear();
    }

    /**
     * Evaluates the rule's condition without executing any actions.
     *
     * <p>Status is reset beforehand. After the call:
     * <ul>
     *   <li>{@link #isExecuted()} → {@code true}</li>
     *   <li>{@link #isPassed()} → the condition result</li>
     *   <li>{@link #isSkipActions()} → {@code true}</li>
     * </ul>
     *
     * @param skipVariableCheck if {@code true}, skips the call to {@link RuleEvent#validateVariables(RuleContext)}
     * @return {@code true} if the condition is satisfied
     */
    public boolean test(boolean skipVariableCheck) {
        try {
            resetStatus();
            boolean result = this.rule.test(this, skipVariableCheck);
            this.executed = true;
            this.passed = result;
            this.skipActions = true;
            return result;
        } catch (Exception e) {
            Util.LOGGER.warn("FMinecraftMod: Exception during rule condition evaluation", e);
            this.executed = true;
            this.passed = false;
            this.skipActions = true;
            this.errorMessage = Component.literal(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return false;
        }
    }

    /**
     * Evaluates the rule's condition and executes actions according to the result.
     *
     * <p>Status is reset beforehand. After the call:
     * <ul>
     *   <li>{@link #isExecuted()} → {@code true}</li>
     *   <li>{@link #isPassed()} → the condition result</li>
     *   <li>{@link #isSkipActions()} → {@code false}</li>
     * </ul>
     *
     * <p>If the condition is satisfied, each action in {@code actionIfSatisfied} is called
     * in order until one returns {@code false}. Otherwise, {@code actionIfViolated} is used.
     *
     * @return {@code true} if the condition is satisfied
     */
    public boolean test() {
        return test(false);
    }

    /**
     * Evaluates the rule's condition and executes actions according to the result.
     *
     * <p>Status is reset beforehand. After the call:
     * <ul>
     *   <li>{@link #isExecuted()} → {@code true}</li>
     *   <li>{@link #isPassed()} → the condition result</li>
     *   <li>{@link #isSkipActions()} → {@code false}</li>
     * </ul>
     *
     * <p>If the condition is satisfied, each action in {@code actionIfSatisfied} is called
     * in order until one returns {@code false}. Otherwise, {@code actionIfViolated} is used.
     *
     * @param skipVariableCheck if {@code true}, skips the call to {@link RuleEvent#validateVariables(RuleContext)}
     * @return {@code true} if the condition is satisfied
     */
    public boolean trigger(boolean skipVariableCheck) {
        try {
            resetStatus();
            boolean result = this.rule.trigger(this, skipVariableCheck);
            this.executed = true;
            this.passed = result;
            this.skipActions = false;
            return result;
        } catch (Exception e) {
            Util.LOGGER.warn("FMinecraftMod: Exception during rule evaluation", e);
            this.executed = true;
            this.passed = false;
            this.skipActions = false;
            this.errorMessage = Component.literal(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return false;
        }
    }

    /**
     * Evaluates the rule's condition and executes actions according to the result.
     *
     * <p>Status is reset beforehand. After the call:
     * <ul>
     *   <li>{@link #isExecuted()} → {@code true}</li>
     *   <li>{@link #isPassed()} → the condition result</li>
     *   <li>{@link #isSkipActions()} → {@code false}</li>
     * </ul>
     *
     * <p>If the condition is satisfied, each action in {@code actionIfSatisfied} is called
     * in order until one returns {@code false}. Otherwise, {@code actionIfViolated} is used.
     *
     * @return {@code true} if the condition is satisfied
     */
    public boolean trigger() {
        return trigger(false);
    }

    public Component render() {
        // RuleName: true/false/notrun/error (variable: value, variable: value, ...)
        MutableComponent title = Component.literal(rule.getName()).append(": ");
        List<Component> variableTexts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : getVariables().entrySet()) {
            String varName = entry.getKey();
            String varValue = TypeAdaptor.parse(entry.getValue()).asString();
            Component varText = Component.literal(varName + ": " + varValue);
            variableTexts.add(varText);
        }
        if (executed) {
            if (errorMessage != null) {
                title = title.append(errorMessage);
            } else if (skipActions) {
                if (passed) {
                    title = title.append(Util.parseTranslatableText("fmod.rule.status.true"));
                } else {
                    title = title.append(Util.parseTranslatableText("fmod.rule.status.false"));
                }
            } else {
                if (passed) {
                    title = title.append(Util.parseTranslatableText("fmod.rule.status.pass"));
                } else {
                    title = title.append(Util.parseTranslatableText("fmod.rule.status.violate"));
                }
            }
        } else {
            title = title.append(Util.parseTranslatableText("fmod.rule.status.notrun"));
        }
        if (!variableTexts.isEmpty()) {
            title = title.append(" (");
            int i = 0;
            for (Component varText : variableTexts) {
                title = title.append(varText);
                if (i < variableTexts.size() - 1) {
                    title = title.append(", ");
                }
                i++;
            }
            title = title.append(")");
        }
        for (Component warning : warnings) {
            title = title.append("\n").append(Util.parseTranslatableText("fmod.rule.status.warning")).append(warning);
        }
        return title;
    }
}
