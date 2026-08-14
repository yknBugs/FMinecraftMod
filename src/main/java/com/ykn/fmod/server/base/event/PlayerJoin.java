/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.MessageType;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.event.PlayerJoinEvent;
import com.ykn.fmod.server.rule.tool.RuleManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class PlayerJoin {

    private final ServerPlayer player;

    public PlayerJoin(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * This method is called when a player joins the world.
     * <p>
     * Delivers any startup messages (e.g. auto-loaded rule/flow counts, see {@link NewLevel})
     * that were queued before this player joined, which always happens on a singleplayer
     * integrated server, since the world is loaded before the local player joins it.
     */
    public void onPlayerJoin() {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ServerData data = Util.getServerData(server);
        for (Component message : data.drainPendingStartupMessages()) {
            MessageType.sendTextMessage(player, message);
        }

        runCustomRule(data);
        runLogicFlow(data);
    }

    private void runCustomRule(ServerData data) {
        List<RuleManager> joinEventRules = data.gatherRuleByEventType(PlayerJoinEvent.class, true);
        for (RuleManager rule : joinEventRules) {
            Map<String, Object> eventVariables = new HashMap<>();
            eventVariables.put("player", this.player.getUUID());
            eventVariables.put("x", this.player.getX());
            eventVariables.put("y", this.player.getY());
            eventVariables.put("z", this.player.getZ());
            eventVariables.put("pitch", Double.valueOf(this.player.getXRot()));
            eventVariables.put("yaw", Double.valueOf(this.player.getYRot()));
            eventVariables.put("position", this.player.position());
            eventVariables.put("rotation", this.player.getRotationVector());
            eventVariables.put("health", Double.valueOf(this.player.getHealth()));
            eventVariables.put("dimension", this.player.level().dimension().location());
            eventVariables.put("biome", this.player.level().getBiome(this.player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null));
            eventVariables.put("name", this.player.getDisplayName().getString());
            eventVariables.put("cansleep", this.player.level().dimensionType().natural() && !this.player.level().isDay() && this.player.level().dimensionType().bedWorks());
            eventVariables.put("__player__", this.player);
            eventVariables.put("__world__", this.player.level());
            eventVariables.put("__name__", this.player.getDisplayName());
            rule.trigger(data, eventVariables);
        }
    }

    private void runLogicFlow(ServerData data) {
        List<FlowManager> joinEventFlow = data.gatherFlowByFirstNodeType("PlayerJoinEventNode", true);
        for (FlowManager flow : joinEventFlow) {
            List<Object> eventOutput = new ArrayList<>();
            eventOutput.add(this.player);
            eventOutput.add(this.player.position());
            flow.execute(data, eventOutput, null);
        }
    }

}
