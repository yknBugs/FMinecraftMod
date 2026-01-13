package com.ykn.fmod.server.base.schedule;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
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

    @Override
    public void onTrigger() {
        ServerData data = Util.getServerData(server);
        ExecutionContext ctx = new ExecutionContext(this.flowManager.flow, this.server);
        ctx.execute(Util.serverConfig.getMaxFlowLength(), this.eventNodeOutputs, this.contextVariables);
        data.executeHistory.add(ctx);
    }

    @Override
    public boolean shouldCancel() {
        if (flowManager == null || server == null || flowManager.isEnabled == false) {
            return true;
        }
        return false;
    }

}
