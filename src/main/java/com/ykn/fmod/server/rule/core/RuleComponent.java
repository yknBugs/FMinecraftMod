/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import net.minecraft.text.Text;

/**
 * Base marker interface for all rule components.
 *
 * <p>Every element of a {@link CustomRule} — events, conditions, and actions —
 * implements {@code RuleComponent}, which provides a common type identifier and
 * a human-readable representation for display in chat or command feedback.
 *
 * <p>To create a new rule component type, implement one of the three sub-interfaces:
 * <ul>
 *   <li>{@link RuleEvent}       – triggered when a game event occurs</li>
 *   <li>{@link RuleCondition}   – evaluates whether a boolean predicate holds</li>
 *   <li>{@link RuleAction}      – executes a side-effect when the rule fires</li>
 * </ul>
 *
 * @see RuleEvent
 * @see RuleCondition
 * @see RuleAction
 */
public interface RuleComponent {
    
    /**
     * Returns the type identifier string used for registration and JSON serialization.
     *
     * <p>By default this returns the simple class name (e.g. {@code "BroadcastMessage"}).
     * Overriding this method is optional but recommended when the class name may change
     * or when the desired type key differs from the class name.
     *
     * <p>The returned value must exactly match the key used in {@link com.ykn.fmod.server.rule.tool.RuleRegistry}
     * so that the factory can reconstruct the component from its JSON representation.
     *
     * @return the non-null type string for this component
     */
    default public String getType() {
        return getClass().getSimpleName();
    }

    /**
     * Returns a human-readable {@link Text} describing this component.
     *
     * <p>The text is shown when an admin inspects a rule with the {@code /f rule} command,
     * typically as hover text attached to the component name.
     *
     * @return a non-null {@link Text} suitable for display in Minecraft chat
     */
    public Text render();

}
