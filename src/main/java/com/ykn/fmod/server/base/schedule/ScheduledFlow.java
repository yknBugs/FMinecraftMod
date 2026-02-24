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

public class ScheduledFlow extends ScheduledTask {

    private FlowManager flowManager;
    private List<Object> eventNodeOutputs;
    private Map<String, Object> contextVariables;
    private MinecraftServer server;

    public ScheduledFlow(@NotNull FlowManager flowManager, @Nullable List<Object> eventNodeOutputs, @Nullable Map<String, Object> contextVariables, @NotNull MinecraftServer server, int delay) {
        super(delay, 0);
        this.flowManager = flowManager;
        this.eventNodeOutputs = eventNodeOutputs;
        this.contextVariables = contextVariables;
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
        if (flowManager == null || server == null || !flowManager.isEnabled()) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScheduledFlow{flow='" + flowManager.getFlow().getName() + "'}";
    }
}
