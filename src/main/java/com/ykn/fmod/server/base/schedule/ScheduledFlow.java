/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.MinecraftServer;

/**
 * Runs an enabled flow after a positive delay in server ticks.
 * <p>The task owns a mutable snapshot of the initial variable map, so subsequent
 * additions do not mutate the caller's map and work with unmodifiable input maps.
 * Variable value objects, event outputs, and the target manager are shared references.
 * Disabling the target before execution cancels the task.
 */
public class ScheduledFlow extends ScheduledTask {

    private FlowManager flowManager;
    private List<Object> eventNodeOutputs;
    private Map<String, Object> contextVariables;
    private MinecraftServer server;

    /**
     * Creates a pending flow invocation.
     *
     * @param flowManager The target flow manager
     * @param eventNodeOutputs Optional start-node outputs, retained by reference
     * @param contextVariables Initial variables, shallow-copied into task-owned mutable storage;
     *                         null starts with no variables
     * @param server The server on which the flow will run
     * @param delay Positive delay in server ticks; zero does not invoke onTrigger automatically
     */
    public ScheduledFlow(@NotNull FlowManager flowManager, @Nullable List<Object> eventNodeOutputs, @Nullable Map<String, Object> contextVariables, @NotNull MinecraftServer server, int delay) {
        super(delay, 0);
        this.flowManager = flowManager;
        this.eventNodeOutputs = eventNodeOutputs;
        this.contextVariables = contextVariables == null ? new HashMap<>() : new HashMap<>(contextVariables);
        this.server = server;
    }

    public void addContextVariable(String name, Object value) {
        if (this.contextVariables == null) {
            this.contextVariables = new HashMap<>();
        }
        this.contextVariables.put(name, value);
    }

    public void addContextVariables(Map<String, Object> variables) {
        if (this.contextVariables == null) {
            this.contextVariables = new HashMap<>();
        }
        this.contextVariables.putAll(variables);
    }

    /**
     * Returns this task's mutable variable map. Changes affect its pending invocation.
     *
     * @return The task-owned map, including variables added after scheduling
     */
    @NotNull
    public Map<String, Object> getContextVariables() {
        if (this.contextVariables == null) {
            this.contextVariables = new HashMap<>();
            return this.contextVariables;
        }
        return this.contextVariables;
    }

    @NotNull
    public FlowManager getFlowManager() {
        return this.flowManager;
    }

    @Override
    public void onTrigger() {
        ServerData data = Util.getServerData(server);
        this.flowManager.execute(data, this.eventNodeOutputs, this.contextVariables);
    }

    @Override
    public boolean shouldCancel() {
        return flowManager == null || server == null || !flowManager.isEnabled();
    }

    @Override
    public String toString() {
        return "ScheduledFlow{flow='" + flowManager.getFlow().getName() + "'}";
    }
}
