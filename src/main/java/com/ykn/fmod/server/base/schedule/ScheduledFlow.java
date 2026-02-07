/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.server.MinecraftServer;

public class ScheduledFlow extends ScheduledTask {

    private FlowManager flowManager;
    private List<Object> eventNodeOutputs;
    private Map<String, Object> contextVariables;
    private MinecraftServer server;

    public ScheduledFlow(@Nonnull FlowManager flowManager, @Nullable List<Object> eventNodeOutputs, @Nullable Map<String, Object> contextVariables, @Nonnull MinecraftServer server, int delay) {
        super(delay, 0);
        this.flowManager = flowManager;
        this.eventNodeOutputs = eventNodeOutputs;
        this.contextVariables = contextVariables;
        this.server = server;
    }

    public void addContextVariable(String name, Object value) {
        this.contextVariables.put(name, value);
    }

    public void addContextVariables(Map<String, Object> variables) {
        this.contextVariables.putAll(variables);
    }

    public Map<String, Object> getContextVariables() {
        return this.contextVariables;
    }

    @Override
    public void onTrigger() {
        ServerData data = Util.getServerData(server);
        this.flowManager.execute(data, this.eventNodeOutputs, this.contextVariables);
    }

    @Override
    public boolean shouldCancel() {
        if (flowManager == null || server == null || flowManager.isEnabled == false) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ScheduledFlow{flow='" + flowManager.flow.name + "'}";
    }
}
