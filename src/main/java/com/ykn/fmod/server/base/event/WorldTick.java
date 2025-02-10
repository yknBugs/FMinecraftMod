package com.ykn.fmod.server.base.event;

import java.util.List;

import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.server.MinecraftServer;

public class WorldTick {

    private MinecraftServer server;

    public WorldTick(MinecraftServer server) {
        this.server = server;
    }

    /**
     * This method is called every tick.
     */
    public void onWorldTick() {
        List<ScheduledTask> scheduledTasks = Util.getServerData(server).getScheduledTasks();
        scheduledTasks.forEach(ScheduledTask::tick);
        scheduledTasks.removeIf(ScheduledTask::isFinished);
    }

}
