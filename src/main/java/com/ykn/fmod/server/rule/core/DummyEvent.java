/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.core;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ykn.fmod.server.base.util.Util;

import net.minecraft.text.Text;

/**
 * A no-op placeholder {@link RuleEvent} used when no specific triggering event is configured.
 *
 * <p>{@code DummyEvent} declares no variables and provides no contract for the
 * context variable map. It is the default event assigned to a newly {@linkplain CustomRule#build built}
 * rule before the operator sets a real event.
 *
 * <p>This class is a singleton; obtain the instance via {@link #getInstance()}.
 *
 * @see CustomRule#build(String)
 */
public class DummyEvent implements RuleEvent {

    private static final DummyEvent INSTANCE = new DummyEvent();

    private DummyEvent() {
    }

    /**
     * Returns the singleton instance of {@code DummyEvent}.
     *
     * @return the shared {@code DummyEvent} instance
     */
    public static DummyEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "Dummy";
    }

    @Override
    public Set<String> variablesList() {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Class<? extends Object>> variablesType() {
        return Collections.emptyMap();
    }
    
    @Override
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.event.dummy").append("\n").append(Util.parseTranslatableText("fmod.rule.status.novar"));
    }
}
