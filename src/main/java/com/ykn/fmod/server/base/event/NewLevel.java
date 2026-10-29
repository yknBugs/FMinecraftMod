/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.async.FlowBulkLoadExecutor;
import com.ykn.fmod.server.base.async.RuleBulkLoadExecutor;
import com.ykn.fmod.server.base.command.FlowFileSuggestion;
import com.ykn.fmod.server.base.command.RuleFileSuggestion;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.StartupMessageTask;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * The world load feature: auto-loading the rule and flow files once per server start.
 *
 * <p>This class is the existing feature only; it is invoked by {@link GameEventHandler}, which is
 * the single entry point the loader layer registers.
 */
public final class NewLevel {

    private NewLevel() {
    }

    /**
     * Runs the once-per-startup auto load.
     *
     * @param server the server that just started
     * @param world  the world that finished loading
     * @see com.ykn.fmod.server.FMod
     */
    public static void onNewLevel(MinecraftServer server, ServerLevel world) {
        if (server == null || world == null) {
            return;
        }
        ServerData data = Util.getServerData(server);
        // ServerWorldEvents.LOAD fires once per dimension, so make sure the auto load
        // below only ever runs once per server startup / save load.
        if (data.tryMarkAutoLoadPerformed()) {
            autoLoadRuleFiles(server, data);
            autoLoadFlowFiles(server, data);
        }
    }

    /**
     * Automatically loads every {@code .rule} file in the config directory into {@code data},
     * if the {@code autoLoadRuleFiles} config option is enabled.
     * Reports how many rules were loaded via a console log, or a chat message if the server
     * is a singleplayer integrated server.
     *
     * @param server the server that just started
     * @param data   the {@link ServerData} of that server
     */
    private static void autoLoadRuleFiles(MinecraftServer server, ServerData data) {
        if (!Util.getServerConfig().getAutoLoadRuleFiles()) {
            return;
        }
        Path ruleFolder = Util.getConfigDir();
        RuleFileSuggestion.suggest();
        // Reading and JSON-parsing every .rule file synchronously here would stall server
        // startup / new-save loading, so the actual file loading runs off the server thread.
        List<String> fileNames = new ArrayList<>(RuleFileSuggestion.getCachedRuleList());
        data.submitAsyncTask(new RuleBulkLoadExecutor(fileNames, ruleFolder, data, new RuleBulkLoadExecutor.ResultHandler() {
            @Override
            public void onFileNotFound(String fileName) {
                Util.LOGGER.error("FMinecraftMod: Error auto loading rule file " + fileName);
            }

            @Override
            public void onLoadFailed(String fileName) {
                Util.LOGGER.error("FMinecraftMod: Error auto loading rule file " + fileName);
            }

            @Override
            public void onAlreadyExists(String ruleName) {
                // A name collision with an already-loaded rule is silently skipped, no message.
            }

            @Override
            public void onCompleted(int loadedCount) {
                Component message = Util.parseTranslatableText("fmod.command.rule.load.all", String.valueOf(loadedCount));
                if (server.isSingleplayer()) {
                    // The integrated server loads the world before the local player actually joins
                    // it, so deliver the message via a task that waits for them (see StartupMessageTask).
                    data.submitScheduledTask(new StartupMessageTask(server, message));
                } else {
                    Util.LOGGER.info(message.getString());
                }
            }
        }));
    }

    /**
     * Automatically loads every {@code .flow} file in the config directory into {@code data},
     * if the {@code autoLoadFlowFiles} config option is enabled.
     * Reports how many flows were loaded via a console log, or a chat message if the server
     * is a singleplayer integrated server.
     *
     * @param server the server that just started
     * @param data   the {@link ServerData} of that server
     */
    private static void autoLoadFlowFiles(MinecraftServer server, ServerData data) {
        if (!Util.getServerConfig().getAutoLoadFlowFiles()) {
            return;
        }
        Path flowFolder = Util.getConfigDir();
        FlowFileSuggestion.suggest();
        // Reading and JSON-parsing every .flow file synchronously here would stall server
        // startup / new-save loading, so the actual file loading runs off the server thread.
        List<String> fileNames = new ArrayList<>(FlowFileSuggestion.getCachedFlowList());
        data.submitAsyncTask(new FlowBulkLoadExecutor(fileNames, flowFolder, data, new FlowBulkLoadExecutor.ResultHandler() {
            @Override
            public void onFileNotFound(String fileName) {
                Util.LOGGER.error("FMinecraftMod: Error auto loading flow file " + fileName);
            }

            @Override
            public void onLoadFailed(String fileName) {
                Util.LOGGER.error("FMinecraftMod: Error auto loading flow file " + fileName);
            }

            @Override
            public void onAlreadyExists(String flowName) {
                // A name collision with an already-loaded flow is silently skipped, no message.
            }

            @Override
            public void onCompleted(int loadedCount) {
                Component message = Util.parseTranslatableText("fmod.command.flow.load.all", String.valueOf(loadedCount));
                if (server.isSingleplayer()) {
                    // The integrated server loads the world before the local player actually joins
                    // it, so deliver the message via a task that waits for them (see StartupMessageTask).
                    data.submitScheduledTask(new StartupMessageTask(server, message));
                } else {
                    Util.LOGGER.info(message.getString());
                }
            }
        }));
    }
}
