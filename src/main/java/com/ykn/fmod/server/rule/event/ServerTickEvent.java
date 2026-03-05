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
 *   <tr><td>{@code tick}</td><td>{@code Integer}</td><td>The server tick counter at the time of dispatch.</td></tr>
 * </table>
 *
 * <p>This class is a singleton; obtain the instance via {@link #getInstance()}.
 */
public class ServerTickEvent implements RuleEvent {

    private static final ServerTickEvent INSTANCE = new ServerTickEvent();

    private final Map<String, Class<? extends Object>> variableTypes;

    private static final Map<String, Class<? extends Object>> createVariablesType() {
        Map<String, Class<? extends Object>> map = new HashMap<>();
        map.put("tick", Integer.class);
        return Collections.unmodifiableMap(map);
    }

    private ServerTickEvent() {
        this.variableTypes = createVariablesType();
    }

    public static ServerTickEvent getInstance() {
        return INSTANCE;
    }

    @Override
    public String getType() {
        return "ServerTickEvent";
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
        return Util.parseTranslatableText("fmod.rule.event.servertick");
    }
}
