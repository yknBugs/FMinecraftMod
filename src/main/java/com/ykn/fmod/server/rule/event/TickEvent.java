/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.RuleEvent;

import net.minecraft.text.Text;

/**
 * A {@link RuleEvent} that fires on every server world tick.
 *
 * <p>When a world tick occurs, the event dispatcher should build a variable map
 * containing the current tick count and pass it when calling
 * {@link com.ykn.fmod.server.rule.tool.RuleManager#trigger}:
 *
 * <pre>{@code
 * Map<String, Object> vars = new HashMap<>();
 * vars.put("tick", server.getTicks());
 * ruleManager.trigger(serverData, vars);
 * }</pre>
 *
 * <p>Variable contract:
 * <table border="1">
 *   <tr><th>Name</th><th>Type</th><th>Description</th></tr>
 *   <tr><td>{@code tick}</td><td>{@code int}</td><td>The server tick counter at the time of dispatch.</td></tr>
 * </table>
 *
 * <p>This class is a singleton; obtain the instance via {@link #getInstance()}.
 */
public class TickEvent implements RuleEvent {

    private static final TickEvent INSTANCE = new TickEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("tick", int.class);
        return Collections.unmodifiableMap(map);
    }

    private TickEvent() {
        this.variableTypes = createVariablesType();
    }

    public static TickEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "TickEvent";
    }

    @Override
    public Set<String> variablesList() {
        return variableTypes.keySet();
    }

    @Override
    public Map<String, Class<? extends Object>> variablesType() {
        return variableTypes;
    }

    @Override
    public Text render() {
        return Util.parseTranslatableText("fmod.rule.event.tick");
    }
}
