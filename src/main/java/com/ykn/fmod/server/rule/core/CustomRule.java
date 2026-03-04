/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/**
 * The central definition of an admin-configured server rule.
 *
 * <p>A {@code CustomRule} models the three-stage pipeline described in the rule JSON format:
 * <ol>
 *   <li><b>Event</b> ({@link RuleEvent}) – labels the game event that can trigger this rule
 *       ({@code "TickEvent"}, {@code "EntityDeathEvent"}, …).</li>
 *   <li><b>Condition</b> ({@link RuleCondition}) – a boolean expression evaluated when the
 *       rule fires.  The main expression ({@link #getCondition()}) typically uses
 *       {@link BinaryConditionExpression} nodes whose leaf operands are
 *       {@link ConditionReference}s pointing at named entries in the {@linkplain #getExtra() extra} list.</li>
 *   <li><b>Actions</b> ({@link RuleAction}) – side-effects executed after the condition:
 *       {@link #getActionIfSatisfied()} when the condition is {@code true};
 *       {@link #getActionIfViolated()} when it is {@code false}.</li>
 * </ol>
 *
 * <p><b>Lifecycle:</b>
 * <pre>{@code
 * CustomRule rule = CustomRule.build("warn", TickEvent.getInstance())
 *     .addCondition(new EntityPosition("inZone", ...))
 *     .setCondition(ConditionFormulaParser.parse("inZone"))
 *     .addAction(new BroadcastMessage("bc", ...))
 *     .optimize();          // inlines ConditionReference nodes for faster evaluation
 *
 * RuleContext ctx = new RuleContext(server, rule, variables);
 * ctx.trigger();            // evaluates condition + runs actions
 * }</pre>
 *
 * <p>Instances are mutable but intended to be used via {@link com.ykn.fmod.server.rule.tool.RuleManager},
 * which manages the enable/disable state and creates a new optimised snapshot before each dispatch.
 *
 * @see RuleContext
 * @see com.ykn.fmod.server.rule.tool.RuleManager
 * @see com.ykn.fmod.server.rule.tool.RuleSerializer
 */
public class CustomRule implements Cloneable {

    /** 
     * Human-readable identifier for this rule, used in commands and persistence. 
     */
    private String name;

    /** 
     * The event type that labels when this rule should be dispatched. 
     */
    private RuleEvent event;

    /**
     * The main boolean condition expression.  Typically a
     * {@link BinaryConditionExpression} / {@link UnaryConditionExpression} tree
     * whose leaves are {@link ConditionReference}s resolved against {@link #extra}.
     */
    private RuleCondition condition;

    /**
     * Named source conditions ({@link SourceCondition}) that are referenced by the main
     * {@link #condition} formula.  They correspond to the {@code "extra"} JSON array.
     */
    private final List<RuleCondition> extra;

    /** 
     * Actions executed when {@link #condition} evaluates to {@code true}. 
     */
    private final List<RuleAction> actionIfSatisfied;

    /** 
     * Actions executed when {@link #condition} evaluates to {@code false}. 
     */
    private final List<RuleAction> actionIfViolated;

    private CustomRule(String name, RuleEvent event, RuleCondition condition) {
        this.name = name;
        this.event = event;
        this.condition = condition;
        this.extra = new ArrayList<>();
        this.actionIfSatisfied = new ArrayList<>();
        this.actionIfViolated = new ArrayList<>();
    }

    /**
     * Creates a rule with a name only. The event defaults to {@link DummyEvent} and
     * the condition defaults to {@code ConstCondition.of(false)}.
     *
     * @param name the rule identifier
     * @return a new, empty {@code CustomRule}
     */
    public static CustomRule build(String name) {
        CustomRule rule = new CustomRule(name, DummyEvent.getInstance(), ConstCondition.of(false));
        return rule;
    }

    /**
     * Creates a rule with a name and event. The condition defaults to {@code ConstCondition.of(false)}.
     *
     * @param name  the rule identifier
     * @param event the triggering event type
     * @return a new {@code CustomRule}
     */
    public static CustomRule build(String name, RuleEvent event) {
        CustomRule rule = new CustomRule(name, event, ConstCondition.of(false));
        return rule;
    }

    /**
     * Creates a rule with a name, event, and condition.
     *
     * @param name      the rule identifier
     * @param event     the triggering event type
     * @param condition the main boolean condition
     * @return a new {@code CustomRule}
     */
    public static CustomRule build(String name, RuleEvent event, RuleCondition condition) {
        CustomRule rule = new CustomRule(name, event, condition);
        return rule;
    }

    /**
     * Creates a rule with one satisfied-action.
     *
     * @param name               the rule identifier
     * @param event              the triggering event
     * @param condition          the main boolean condition
     * @param actionIfSatisfied  the single action to run when the condition is {@code true}
     * @return a new {@code CustomRule}
     */
    public static CustomRule build(String name, RuleEvent event, RuleCondition condition, RuleAction actionIfSatisfied) {
        CustomRule rule = new CustomRule(name, event, condition);
        rule.actionIfSatisfied.add(actionIfSatisfied);
        return rule;
    }

    /**
     * Creates a rule with one satisfied-action and one violated-action.
     *
     * @param name               the rule identifier
     * @param event              the triggering event
     * @param condition          the main boolean condition
     * @param actionIfSatisfied  run when condition is {@code true}
     * @param actionIfViolated   run when condition is {@code false}
     * @return a new {@code CustomRule}
     */
    public static CustomRule build(String name, RuleEvent event, RuleCondition condition, RuleAction actionIfSatisfied, RuleAction actionIfViolated) {
        CustomRule rule = new CustomRule(name, event, condition);
        rule.actionIfSatisfied.add(actionIfSatisfied);
        rule.actionIfViolated.add(actionIfViolated);
        return rule;
    }

    /**
     * Creates a rule with a list of satisfied-actions.
     *
     * @param name               the rule identifier
     * @param event              the triggering event
     * @param condition          the main boolean condition
     * @param actionIfSatisfied  actions run when condition is {@code true}
     * @return a new {@code CustomRule}
     */
    public static CustomRule build(String name, RuleEvent event, RuleCondition condition, List<RuleAction> actionIfSatisfied) {
        CustomRule rule = new CustomRule(name, event, condition);
        rule.actionIfSatisfied.addAll(actionIfSatisfied);
        return rule;
    }

    /**
     * Creates a fully specified rule.
     *
     * @param name               the rule identifier
     * @param event              the triggering event
     * @param condition          the main boolean condition
     * @param actionIfSatisfied  actions run when condition is {@code true}
     * @param actionIfViolated   actions run when condition is {@code false}
     * @return a new {@code CustomRule}
     */
    public static CustomRule build(String name, RuleEvent event, RuleCondition condition, List<RuleAction> actionIfSatisfied, List<RuleAction> actionIfViolated) {
        CustomRule rule = new CustomRule(name, event, condition);
        rule.actionIfSatisfied.addAll(actionIfSatisfied);
        rule.actionIfViolated.addAll(actionIfViolated);
        return rule;
    }

    public CustomRule setEvent(RuleEvent event) {
        this.event = event;
        return this;
    }

    public CustomRule setCondition(RuleCondition condition) {
        this.condition = condition;
        return this;
    }

    public CustomRule withCondition(RuleCondition extra) {
        this.extra.clear();
        this.extra.add(extra);
        return this;
    }

    public CustomRule addCondition(RuleCondition extra) {
        this.extra.add(extra);
        return this;
    }

    public CustomRule removeCondition(String name) {
        this.extra.removeIf(condition -> condition.getName().equals(name));
        return this;
    }

    public CustomRule clearConditions() {
        this.extra.clear();
        return this;
    }

    public CustomRule withAction(RuleAction actionIfSatisfied) {
        this.actionIfSatisfied.clear();
        this.actionIfSatisfied.add(actionIfSatisfied);
        return this;
    }

    public CustomRule addAction(RuleAction actionIfSatisfied) {
        this.actionIfSatisfied.add(actionIfSatisfied);
        return this;
    }

    public CustomRule removeAction(String name) {
        this.actionIfSatisfied.removeIf(action -> action.getName().equals(name));
        return this;
    }

    public CustomRule clearActions() {
        this.actionIfSatisfied.clear();
        return this;
    }

    public CustomRule withPunishment(RuleAction actionIfViolated) {
        this.actionIfViolated.clear();
        this.actionIfViolated.add(actionIfViolated);
        return this;
    }

    public CustomRule addPunishment(RuleAction actionIfViolated) {
        this.actionIfViolated.add(actionIfViolated);
        return this;
    }

    public CustomRule removePunishment(String name) {
        this.actionIfViolated.removeIf(action -> action.getName().equals(name));
        return this;
    }

    public CustomRule clearPunishments() {
        this.actionIfViolated.clear();
        return this;
    }

    /**
     * Resolves all {@link ConditionReference} instances in the condition tree and replaces them
     * with the actual condition objects from the extra list. This avoids repeated lookups during
     * each call to {@link #test} or {@link #trigger}.
     *
     * <p>Unresolvable references (whose name has no match in the extra list) are left unchanged.</p>
     *
     * @return {@code this}, for method chaining
     */
    public CustomRule optimize() {
        this.condition = this.condition.optimize(this);
        return this;
    }

    /**
     * Evaluates the main condition without executing any actions.
     *
     * <p>If {@link RuleEvent#validateVariables(RuleContext)} returns {@code false}, a
     * warning is logged but evaluation continues.
     *
     * @param context the execution context
     * @return {@code true} if the condition is satisfied
     */
    public boolean test(RuleContext context) {
        if (!event.validateVariables(context)) {
            Util.LOGGER.warn("FMinecraftMod: Rule " + name + " failed to validate variables for event " + event.getType() + ". Check your event dispatcher implementation.");
        }
        return this.condition.evaluate(context);
    }

    /**
     * Evaluates the main condition and executes the appropriate action list.
     *
     * <p>If the condition is {@code true}, each action in {@link #actionIfSatisfied} is
     * executed in order; if any action returns {@code false} the remaining actions are
     * skipped. The same short-circuit logic applies to {@link #actionIfViolated}.
     *
     * @param context the execution context
     * @return {@code true} if the condition is satisfied
     */
    public boolean trigger(RuleContext context) {
        boolean result = this.test(context);
        if (result) {
            for (RuleAction action : this.actionIfSatisfied) {
                boolean shouldContinue = action.execute(context);
                if (!shouldContinue) {
                    break;
                }
            }
        } else {
            for (RuleAction action : this.actionIfViolated) {
                boolean shouldContinue = action.execute(context);
                if (!shouldContinue) {
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Creates a shallow copy of this rule with independent, mutable action and condition lists.
     *
     * @return a new {@code CustomRule} with the same configuration
     */
    public CustomRule copy() {
        CustomRule copy = new CustomRule(this.name, this.event, this.condition);
        copy.extra.addAll(this.extra);
        copy.actionIfSatisfied.addAll(this.actionIfSatisfied);
        copy.actionIfViolated.addAll(this.actionIfViolated);
        return copy;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this.copy();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuleEvent getEvent() {
        return event;
    }

    public RuleCondition getCondition() {
        return condition;
    }

    /**
     * Returns an unmodifiable view of the extra (named source) conditions.
     *
     * <p>These are the conditions declared in the {@code "extra"} JSON array and
     * referenced by name in the main condition formula.
     *
     * @return an unmodifiable list of extra conditions
     */
    public List<RuleCondition> getExtra() {
        return Collections.unmodifiableList(extra);
    }

    /**
     * Looks up a named extra condition by name.
     *
     * @param name the condition name to search for
     * @return the matching {@link RuleCondition}, or {@code null} if not found
     */
    @Nullable
    public RuleCondition getExtraCondition(String name) {
        for (RuleCondition extraCondition : extra) {
            if (extraCondition.getName().equals(name)) {
                return extraCondition;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if an extra condition with the given name exists.
     *
     * @param name the name to check
     * @return {@code true} if found
     */
    public boolean hasExtraCondition(String name) {
        for (RuleCondition extraCondition : extra) {
            if (extraCondition.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the actions executed when the condition is {@code true}.
     *
     * @return an unmodifiable list of satisfied-actions
     */
    public List<RuleAction> getActionIfSatisfied() {
        return Collections.unmodifiableList(actionIfSatisfied);
    }

    /**
     * Looks up a satisfied-action by name.
     *
     * @param name the action name
     * @return the matching {@link RuleAction}, or {@code null} if not found
     */
    @Nullable
    public RuleAction getActionIfSatisfied(String name) {
        for (RuleAction action : actionIfSatisfied) {
            if (action.getName().equals(name)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if a satisfied-action with the given name exists.
     *
     * @param name the name to check
     * @return {@code true} if found
     */
    public boolean hasActionIfSatisfied(String name) {
        for (RuleAction action : actionIfSatisfied) {
            if (action.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the actions executed when the condition is {@code false}.
     *
     * @return an unmodifiable list of violated-actions
     */
    public List<RuleAction> getActionIfViolated() {
        return Collections.unmodifiableList(actionIfViolated);
    }

    /**
     * Looks up a violated-action by name.
     *
     * @param name the action name
     * @return the matching {@link RuleAction}, or {@code null} if not found
     */
    @Nullable
    public RuleAction getActionIfViolated(String name) {
        for (RuleAction action : actionIfViolated) {
            if (action.getName().equals(name)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if a violated-action with the given name exists.
     *
     * @param name the name to check
     * @return {@code true} if found
     */
    public boolean hasActionIfViolated(String name) {
        for (RuleAction action : actionIfViolated) {
            if (action.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public Component render() {
        // RuleName: EventType -> ConditionName -> [Action1, Action2, ...]/[violationAction1, violationAction2, ...], (extraCondition1, extraCondition2, ...)
        MutableComponent title = Component.literal(name);
        Component eventDetail = event.render();
        MutableComponent eventText = Component.literal(event.getType()).withStyle(s -> s
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, eventDetail))
        );
        Component conditionDetail = condition.render();
        Component conditionName = condition.getName().isEmpty() ? condition.render() : Component.literal(condition.getName());
        MutableComponent conditionText = Component.empty().append(conditionName).withStyle(s -> s
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, conditionDetail))
        );
        List<MutableComponent> extraConditionTexts = new ArrayList<>();
        for (RuleCondition extraCondition : extra) {
            Component extraConditionDetail = extraCondition.render();
            String extraConditionName = extraCondition.getName().isEmpty() ? extraCondition.getType() : extraCondition.getName();
            MutableComponent extraConditionText = Component.literal(extraConditionName).withStyle(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, extraConditionDetail))
            );
            extraConditionTexts.add(extraConditionText);
        }
        List<MutableComponent> actionIfSatisfiedTexts = new ArrayList<>();
        for (RuleAction action : actionIfSatisfied) {
            Component actionDetail = action.render();
            String actionName = action.getName().isEmpty() ? action.getType() : action.getName();
            MutableComponent actionText = Component.literal(actionName).withStyle(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, actionDetail))
            );
            actionIfSatisfiedTexts.add(actionText);
        }
        List<MutableComponent> actionIfViolatedTexts = new ArrayList<>();
        for (RuleAction action : actionIfViolated) {
            Component actionDetail = action.render();
            String actionName = action.getName().isEmpty() ? action.getType() : action.getName();
            MutableComponent actionText = Component.literal(actionName).withStyle(s -> s
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, actionDetail))
            );
            actionIfViolatedTexts.add(actionText);
        }
        title = title.append(": ").append(eventText).append(" -> ").append(conditionText).append(" -> [");
        int i = 0;
        for (MutableComponent actionText : actionIfSatisfiedTexts) {
            if (i > 0) {
                title = title.append(", ");
            }
            title = title.append(actionText);
            i++;
        }
        title = title.append("]/[");
        i = 0;
        for (MutableComponent actionText : actionIfViolatedTexts) {
            if (i > 0) {
                title = title.append(", ");
            }
            title = title.append(actionText);
            i++;
        }
        title = title.append("]");
        if (!extraConditionTexts.isEmpty()) {
            title = title.append(", (");
            i = 0;
            for (MutableComponent extraConditionText : extraConditionTexts) {
                if (i > 0) {
                    title = title.append(", ");
                }
                title = title.append(extraConditionText);
                i++;
            }
            title = title.append(")");
        }
        return title;
    }
}
