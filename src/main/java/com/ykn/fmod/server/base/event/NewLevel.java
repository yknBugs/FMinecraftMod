/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.nio.file.Path;

import com.ykn.fmod.server.base.command.FlowFileSuggestion;
import com.ykn.fmod.server.base.command.RuleFileSuggestion;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.flow.tool.FlowSerializer;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.tool.RuleManager;
import com.ykn.fmod.server.rule.tool.RuleSerializer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class NewLevel {

    private final MinecraftServer server;
    private final ServerLevel world;

    public NewLevel(MinecraftServer server, ServerLevel world) {
        this.server = server;
        this.world = world;
    }

    /**
     * This method is called when a world is loaded.
     */
    public void onNewLevel() {
        if (server != null && world != null) {
            ServerData data = Util.getServerData(server);
            // ServerWorldEvents.LOAD fires once per dimension, so make sure the auto load
            // below only ever runs once per server startup / save load.
            if (data.tryMarkAutoLoadPerformed()) {
                autoLoadRuleFiles(data);
                autoLoadFlowFiles(data);
            }
        }
    }

    /**
     * Automatically loads every {@code .rule} file in the config directory into {@code data},
     * if the {@code autoLoadRuleFiles} config option is enabled.
     * Reports how many rules were loaded via a console log, or a chat message if the server
     * is a singleplayer integrated server.
     *
     * @param data the {@link ServerData} of the server that just started
     */
    private void autoLoadRuleFiles(ServerData data) {
        if (!Util.getServerConfig().getAutoLoadRuleFiles()) {
            return;
        }
        Path ruleFolder = Util.getConfigDir();
        RuleFileSuggestion.suggest();
        int loadedCount = 0;
        for (String ruleFileName : RuleFileSuggestion.getCachedRuleList()) {
            Path rulePath = ruleFolder.resolve(ruleFileName).normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                continue;
            }
            CustomRule rule = RuleSerializer.loadFile(rulePath);
            if (rule == null) {
                Util.LOGGER.error("FMinecraftMod: Error auto loading rule file " + ruleFileName);
                continue;
            }
            if (data.getCustomRules().get(rule.getName()) != null) {
                continue;
            }
            RuleManager ruleManager = new RuleManager(rule);
            data.getCustomRules().put(rule.getName(), ruleManager);
            ruleManager.setEnabled(true);
            loadedCount++;
        }
        Component message = Util.parseTranslatableText("fmod.command.rule.load.all", String.valueOf(loadedCount));
        if (server.isSingleplayer()) {
            // The integrated server loads the world before the local player actually joins it,
            // so queue the message and deliver it once they do (see ServerPlayConnectionEvents.JOIN).
            data.queueStartupMessage(message);
        } else {
            Util.LOGGER.info(message.getString());
        }
    }

    /**
     * Automatically loads every {@code .flow} file in the config directory into {@code data},
     * if the {@code autoLoadFlowFiles} config option is enabled.
     * Reports how many flows were loaded via a console log, or a chat message if the server
     * is a singleplayer integrated server.
     *
     * @param data the {@link ServerData} of the server that just started
     */
    private void autoLoadFlowFiles(ServerData data) {
        if (!Util.getServerConfig().getAutoLoadFlowFiles()) {
            return;
        }
        Path flowFolder = Util.getConfigDir();
        FlowFileSuggestion.suggest();
        int loadedCount = 0;
        for (String flowFileName : FlowFileSuggestion.getCachedFlowList()) {
            Path flowPath = flowFolder.resolve(flowFileName).normalize();
            if (!flowPath.startsWith(flowFolder)) {
                continue;
            }
            LogicFlow flow = FlowSerializer.loadFile(flowPath);
            if (flow == null) {
                Util.LOGGER.error("FMinecraftMod: Error auto loading flow file " + flowFileName);
                continue;
            }
            if (data.getLogicFlows().get(flow.getName()) != null) {
                continue;
            }
            FlowManager flowManager = new FlowManager(flow);
            data.getLogicFlows().put(flow.getName(), flowManager);
            flowManager.setEnabled(true);
            loadedCount++;
        }
        Component message = Util.parseTranslatableText("fmod.command.flow.load.all", String.valueOf(loadedCount));
        if (server.isSingleplayer()) {
            // The integrated server loads the world before the local player actually joins it,
            // so queue the message and deliver it once they do (see ServerPlayConnectionEvents.JOIN).
            data.queueStartupMessage(message);
        } else {
            Util.LOGGER.info(message.getString());
        }
    }

}
