/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.flow.tool.FlowSerializer;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.fabricmc.loader.api.FabricLoader;

public class FlowCommand {

    private static int runCreateFlowCommand(String name, String eventNode, String eventNodeName, CommandContext<CommandSourceStack> context) {
        try {
            if (name == null || name.isEmpty()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.empty"));
                return 0;
            }
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            if (data.getLogicFlows().get(name) != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.exists", name));
                return 0;
            }
            Collection<String> validEventNodes = NodeRegistry.getEventNodeList();
            if (!validEventNodes.contains(eventNode)) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.event.unknown", eventNode));
                return 0;
            }
            FlowManager flowManager = new FlowManager(name, eventNode, eventNodeName);
            if (eventNodeName == null || eventNodeName.isEmpty()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.empty"));
                return 0;
            }
            data.getLogicFlows().put(name, flowManager);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.create.success", eventNode, name), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow create", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runCopyFlowCommand(String sourceName, String targetName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager sourceFlow = data.getLogicFlows().get(sourceName);
            if (sourceFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", sourceName));
                return 0;
            }
            FlowManager targetFlow = data.getLogicFlows().get(targetName);
            if (targetFlow != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.exists", targetName));
                return 0;
            }
            FlowManager copiedFlow = new FlowManager(sourceFlow.getFlow().copy());
            copiedFlow.getFlow().setName(targetName);
            data.getLogicFlows().put(targetName, copiedFlow);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.copy.success", sourceName, targetName), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow copy", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLoadFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            if (FlowFileSuggestion.getAvailableFlows() == 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.hint"), false);
            }
            Path flowFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            if ("*".equals(name)) {
                // Load all flow files
                int loadedCount = 0;
                for (String flowFileName : FlowFileSuggestion.getCachedFlowList()) {
                    Path flowPath = flowFolder.resolve(flowFileName).normalize();
                    if (!flowPath.startsWith(flowFolder)) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.load.filenotfound", flowFileName), false);
                        continue;
                    }
                    LogicFlow flow = FlowSerializer.loadFile(flowPath);
                    if (flow == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.load.ioexception", flowFileName), false);
                        continue;
                    }
                    if (data.getLogicFlows().get(flow.getName()) != null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.exists", flow.getName()), false);
                        continue;
                    }
                    FlowManager flowManager = new FlowManager(flow);
                    data.getLogicFlows().put(flow.getName(), flowManager);
                    flowManager.setEnabled(true);
                    loadedCount++;
                }
                int loadedCountFinal = loadedCount;
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.load.all", String.valueOf(loadedCountFinal)), true);
                return loadedCountFinal;
            }
            Path flowPath = flowFolder.resolve(name).normalize();
            if (!flowPath.startsWith(flowFolder)) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.load.filenotfound", name));
                return 0;
            }
            LogicFlow flow = FlowSerializer.loadFile(flowPath);
            if (flow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.load.ioexception", name));
                return 0;
            }
            if (data.getLogicFlows().get(flow.getName()) != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.exists", flow.getName()));
                return 0;
            }
            FlowManager flowManager = new FlowManager(flow);
            data.getLogicFlows().put(flow.getName(), flowManager);
            flowManager.setEnabled(true);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.load.success", flow.getName()), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow load", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSaveFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            Path flowFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            if ("*".equals(name)) {
                // Save all flows
                int savedCount = 0;
                for (FlowManager flowManager : data.getLogicFlows().values()) {
                    Path flowPath = flowFolder.resolve(flowManager.getFlow().getName() + ".flow").normalize();
                    if (!flowPath.startsWith(flowFolder)) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.save.notavailable", flowManager.getFlow().getName()), false);
                        continue;
                    }
                    boolean success = FlowSerializer.saveFile(flowManager.getFlow(), flowPath, true);
                    if (success) {
                        savedCount++;
                    } else {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.save.ioexception", flowManager.getFlow().getName()), false);
                    }
                }
                int savedCountFinal = savedCount;
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.save.all", String.valueOf(savedCountFinal)), true);
                FlowFileSuggestion.suggest();
                return savedCountFinal;
            }
            FlowManager targetFlow = data.getLogicFlows().get(name);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
                return 0;
            }
            Path flowPath = flowFolder.resolve(targetFlow.getFlow().getName() + ".flow").normalize();
            if (!flowPath.startsWith(flowFolder)) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.save.notavailable", targetFlow.getFlow().getName()));
                return 0;
            }
            boolean success = FlowSerializer.saveFile(targetFlow.getFlow(), flowPath, true);
            if (!success) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.save.ioexception", targetFlow.getFlow().getName()));
                return 0;
            }
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.save.success", targetFlow.getFlow().getName()), true);
            FlowFileSuggestion.suggest();
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow save", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListFlowCommand(CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            if (data.getLogicFlows().isEmpty()) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.list.empty"), false);
                return Command.SINGLE_SUCCESS;
            }
            List<MutableComponent> flowLines = new ArrayList<>();
            int enabledCount = 0;
            int totalCount = 0;
            for (FlowManager flowManager : data.getLogicFlows().values()) {
                MutableComponent line = null;
                String numNodesStr = String.valueOf(flowManager.getFlow().getNodes().size());
                FlowNode startNode = flowManager.getFlow().getFirstNode();
                if (startNode == null) {
                    Util.LOGGER.warn("FMinecraftMod: Skipped broken flow " + flowManager.getFlow().getName() + " with no start node");
                    continue;
                }
                String startNodeStr = startNode.getName();
                if (flowManager.isEnabled()) {
                    line = Util.parseTranslatableText("fmod.command.flow.list.enabled", flowManager.getFlow().getName(), numNodesStr, startNodeStr).withStyle(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").withStyle(ChatFormatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow view \"" + flowManager.getFlow().getName() + "\""))
                    );
                    enabledCount++;
                } else {
                    line = Util.parseTranslatableText("fmod.command.flow.list.disabled", flowManager.getFlow().getName(), numNodesStr, startNodeStr).withStyle(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").withStyle(ChatFormatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow view \"" + flowManager.getFlow().getName() + "\""))
                    );
                }
                totalCount++;
                flowLines.add(line);
            }
            String enabledCountStr = String.valueOf(enabledCount);
            String totalCountStr = String.valueOf(totalCount);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.list.title", totalCountStr, enabledCountStr), false);
            for (MutableComponent line : flowLines) {
                context.getSource().sendSuccess(() -> line, false);
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow list", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRenameFlowCommand(String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager targetFlow = data.getLogicFlows().get(oldName);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", oldName));
                return 0;
            }
            if (newName == null || newName.isEmpty()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.empty"));
                return 0;
            }
            if (data.getLogicFlows().get(newName) != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.exists", newName));
                return 0;
            }
            data.getLogicFlows().remove(oldName);
            targetFlow.getFlow().setName(newName);
            data.getLogicFlows().put(newName, targetFlow);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.rename.success", oldName, newName), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow rename", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetEnableFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager targetFlow = data.getLogicFlows().get(name);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
                return 0;
            }
            if (targetFlow.isEnabled()) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.enable.get.true", name), false);
            } else {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.enable.get.false", name), false);
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow enable", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSetEnableFlowCommand(String name, boolean enable, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager targetFlow = data.getLogicFlows().get(name);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
                return 0;
            }
            targetFlow.setEnabled(enable);
            if (enable) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.enable.set.true", name), true);
            } else {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.enable.set.false", name), true);
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow enable", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runExecuteFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager targetFlow = data.getLogicFlows().get(name);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
                return 0;
            }
            LogicException exception = targetFlow.execute(data, null, null);
            if (exception != null) {
                context.getSource().sendFailure(exception.getMessageText());
                return 0;
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow execute", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runDeleteFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager targetFlow = data.getLogicFlows().get(name);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
                return 0;
            }
            data.getLogicFlows().remove(name);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.delete.success", name), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow delete", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runFlowHistoryCommand(int pageIndex, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            List<ExecutionContext> history = data.getExecuteHistory();
            // 5 entries per page
            int maxPage = (history.size() + 4) / 5;
            if (maxPage <= 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.history.null"), false);
                return Command.SINGLE_SUCCESS;
            }
            int index = pageIndex;
            if (index <= 0) {
                index = maxPage;
            }
            if (index > maxPage) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.history.indexerror", pageIndex, maxPage));
                return 0;
            }
            int start = (index - 1) * 5;
            int end = Math.min(start + 5, history.size());
            String indexStr = String.valueOf(index);
            String maxPageStr = String.valueOf(maxPage);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.history.title", indexStr, maxPageStr), false);
            for (int i = start; i < end; i++) {
                ExecutionContext entry = history.get(i);
                String iStr = String.valueOf(i + 1);
                MutableComponent entryText =  Util.parseTranslatableText("fmod.command.flow.history.entry", iStr, entry.getFlow().getName()).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").withStyle(ChatFormatting.GREEN)))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow log " + iStr))
                );
                context.getSource().sendSuccess(() -> entryText, false);
            }
            MutableComponent navigateText = Component.empty();
            if (index > 1) {
                String prevIndexStr = String.valueOf(index - 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.flow.history.prev").withStyle(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow history " + prevIndexStr))
                ));
            }
            if (index < maxPage) {
                String nextIndexStr = String.valueOf(index + 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.flow.history.next").withStyle(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow history " + nextIndexStr))
                ));
            }
            if (maxPage > 1) {
                context.getSource().sendSuccess(() -> navigateText, false);
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow history", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runViewFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            FlowManager targetFlow = data.getLogicFlows().get(name);
            if (targetFlow == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
                return 0;
            }
            Component text = targetFlow.getFlow().render();
            context.getSource().sendSuccess(() -> text, false);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow view", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLogFlowCommand(int index, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            MinecraftServer server = Util.requireNotNullServer(context);
            if (server == null) {
                return 0;
            }
            ServerData data = Util.getServerData(server);
            List<ExecutionContext> history = data.getExecuteHistory();
            if (index <= 0 || index > history.size()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.log.indexerror", String.valueOf(index)));
                return 0;
            }
            ExecutionContext entry = history.get(index - 1);
            Component text = entry.render();
            context.getSource().sendSuccess(() -> text, false);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow log", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    @Nullable
    private static FlowManager getRequiredFlow(CommandContext<CommandSourceStack> context, String name) {
        FlowFileSuggestion.suggest();
        MinecraftServer server = Util.requireNotNullServer(context);
        if (server == null) {
            return null;
        }
        ServerData data = Util.getServerData(server);
        FlowManager targetFlow = data.getLogicFlows().get(name);
        if (targetFlow == null) {
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            return null;
        }
        return targetFlow;
    }

    private static int runEditFlowNewNodeCommand(String flowName, String type, String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            Collection<String> validNodeTypes = NodeRegistry.getNodeList();
            if (!validNodeTypes.contains(type)) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.unknown", type));
                return 0;
            }
            if (name == null || name.isEmpty()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.empty"));
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.exists", name, flowName));
                return 0;
            }
            targetFlow.createNode(type, name);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.newnode.success", name, flowName), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRemoveNodeCommand(String flowName, String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
                return 0;
            }
            if (existingNode.isEventNode()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.delete.event", flowName, name));
                return 0;
            }
            targetFlow.removeNode(name);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.removenode.success", name, flowName), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowReplaceEventCommand(String flowName, String type, String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            Collection<String> validEventNodes = NodeRegistry.getEventNodeList();
            if (!validEventNodes.contains(type)) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.event.unknown", type));
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.exists", name, flowName));
                return 0;
            }
            targetFlow.replaceEventNode(type, name);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.replaceevent.success", type, flowName), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRenameNodeCommand(String flowName, String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(oldName);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", oldName, flowName));
                return 0;
            }
            if (newName == null || newName.isEmpty()) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.empty"));
                return 0;
            }
            FlowNode newNode = targetFlow.getFlow().getNodeByName(newName);
            if (newNode != null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.exists", newName, flowName));
                return 0;
            }
            targetFlow.renameNode(oldName, newName);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.renamenode.success", oldName, flowName, newName), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowConstInputCommand(String flowName, String name, int index, String value, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
                return 0;
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
                return 0;
            }
            // parse const value
            DataReference ref = FlowSerializer.parseConstDataReference(value);
            Object parsedValue = ref.getValue();
            String parsedValueStr = String.valueOf(parsedValue);
            targetFlow.setConstInput(name, index - 1, parsedValue);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.const.success", name, existingNode.getMetadata().inputNames.get(index - 1), parsedValueStr), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRefInputCommand(String flowName, String name, int index, String refNode, int refIndex, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
                return 0;
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
                return 0;
            }
            FlowNode refExistingNode = targetFlow.getFlow().getNodeByName(refNode);
            if (refExistingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", refNode, flowName));
                return 0;
            }
            if (refIndex <= 0 || refIndex > refExistingNode.getMetadata().outputNumber) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.edit.output.indexerror", refNode, String.valueOf(refIndex)));
                return 0;
            }
            targetFlow.setReferenceInput(name, index - 1, refNode, refIndex - 1);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.ref.success", name, existingNode.getMetadata().inputNames.get(index - 1), refNode, refExistingNode.getMetadata().outputNames.get(refIndex - 1)), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowDisconnectInputCommand(String flowName, String name, int index, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
                return 0;
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
                return 0;
            }
            targetFlow.disconnectInput(name, index - 1);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.disconnect.success", name, existingNode.getMetadata().inputNames.get(index - 1)), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowNextNodeCommand(String flowName, String name, int index, String next, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
                return 0;
            }
            if (index <= 0 || index > existingNode.getMetadata().branchNumber) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.edit.branch.indexerror", name, String.valueOf(index)));
                return 0;
            }
            FlowNode nextNode = targetFlow.getFlow().getNodeByName(next);
            if (nextNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", next, flowName));
                return 0;
            }
            targetFlow.setNextNode(name, index - 1, next);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.next.success", name, existingNode.getMetadata().branchNames.get(index - 1), next), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowFinalBranchCommand(String flowName, String name, int index, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            FlowNode existingNode = targetFlow.getFlow().getNodeByName(name);
            if (existingNode == null) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
                return 0;
            }
            if (index <= 0 || index > existingNode.getMetadata().branchNumber) {
                context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.flow.edit.branch.indexerror", name, String.valueOf(index)));
                return 0;
            }
            targetFlow.disconnectNextNode(name, index - 1);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.final.success", name, existingNode.getMetadata().branchNames.get(index - 1)), true);
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowUndoCommand(String flowName, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            if (!targetFlow.canUndo()) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.undo.nothing", flowName), false);
                return Command.SINGLE_SUCCESS;
            } else {
                targetFlow.undo();
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.undo.success", flowName), true);
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRedoCommand(String flowName, CommandContext<CommandSourceStack> context) {
        try {
            FlowManager targetFlow = getRequiredFlow(context, flowName);
            if (targetFlow == null) {
                return 0;
            }
            if (!targetFlow.canRedo()) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.redo.nothing", flowName), false);
                return Command.SINGLE_SUCCESS;
            } else {
                targetFlow.redo();
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.flow.edit.redo.success", flowName), true);
            }
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            context.getSource().sendFailure(Util.parseTranslatableText("fmod.command.unknownerror"));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("flow")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("event", StringArgumentType.string())
                        .suggests(StringSuggestion.suggest(NodeRegistry.getEventNodeList(), true))
                        .then(Commands.argument("node", StringArgumentType.string())
                            .executes(context -> {return runCreateFlowCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "event"), StringArgumentType.getString(context, "node"), context);})
                        )
                    )
                )
            )
            .then(Commands.literal("list")
                .executes(context -> {return runListFlowCommand(context);})
            )
            .then(Commands.literal("edit")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(Commands.literal("new")
                        .then(Commands.argument("type", StringArgumentType.string())
                            .suggests(StringSuggestion.suggest(NodeRegistry.getNodeList(), true))
                            .then(Commands.argument("node", StringArgumentType.string())
                                .executes(context -> {return runEditFlowNewNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "type"), StringArgumentType.getString(context, "node"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("remove")
                        .then(Commands.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .executes(context -> {return runEditFlowRemoveNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), context);})
                        )
                    )
                    .then(Commands.literal("event")
                        .then(Commands.argument("type", StringArgumentType.string())
                            .suggests(StringSuggestion.suggest(NodeRegistry.getEventNodeList(), true))
                            .then(Commands.argument("node", StringArgumentType.string())
                                .executes(context -> {return runEditFlowReplaceEventCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "type"), StringArgumentType.getString(context, "node"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("rename")
                        .then(Commands.argument("old", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(Commands.argument("new", StringArgumentType.string())
                                .executes(context -> {return runEditFlowRenameNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "old"), StringArgumentType.getString(context, "new"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("const")
                        .then(Commands.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .then(Commands.argument("value", StringArgumentType.string())
                                    .executes(context -> {return runEditFlowConstInputCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "value"), context);})
                                )
                            )
                        )
                    )
                    .then(Commands.literal("reference")
                        .then(Commands.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .then(Commands.argument("refNode", StringArgumentType.string())
                                    .suggests(FlowNodeSuggestion.suggest(true, 3))
                                    .then(Commands.argument("refIndex", IntegerArgumentType.integer(1))
                                        .executes(context -> {return runEditFlowRefInputCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "refNode"), IntegerArgumentType.getInteger(context, "refIndex"), context);})
                                    )
                                )
                            )
                        )
                    )
                    .then(Commands.literal("disconnect")
                        .then(Commands.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {return runEditFlowDisconnectInputCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("next")
                        .then(Commands.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .then(Commands.argument("next", StringArgumentType.string())
                                    .suggests(FlowNodeSuggestion.suggest(true, 3))
                                    .executes(context -> {return runEditFlowNextNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "next"), context);})
                                )
                            )
                        )
                    )
                    .then(Commands.literal("final")
                        .then(Commands.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {return runEditFlowFinalBranchCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("redo")
                        .executes(context -> {return runEditFlowRedoCommand(StringArgumentType.getString(context, "name"), context);})
                    )
                    .then(Commands.literal("undo")
                        .executes(context -> {return runEditFlowUndoCommand(StringArgumentType.getString(context, "name"), context);})
                    )
                )
            )
            .then(Commands.literal("rename")
                .then(Commands.argument("old", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(Commands.argument("new", StringArgumentType.string())
                        .executes(context -> {return runRenameFlowCommand(StringArgumentType.getString(context, "old"), StringArgumentType.getString(context, "new"), context);})
                    )
                )
            )
            .then(Commands.literal("copy")
                .then(Commands.argument("flow", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> {return runCopyFlowCommand(StringArgumentType.getString(context, "flow"), StringArgumentType.getString(context, "name"), context);})
                    )
                )
            )
            .then(Commands.literal("save")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .suggests(LogicFlowSuggestion.suggestSave())
                    .executes(context -> {return runSaveFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("load")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .suggests(FlowFileSuggestion.suggest())
                    .executes(context -> {return runLoadFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("enable")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {return runSetEnableFlowCommand(StringArgumentType.getString(context, "name"), BoolArgumentType.getBool(context, "enabled"), context);})
                    )
                    .executes(context -> {return runGetEnableFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("execute")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .executes(context -> {return runExecuteFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("view")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .executes(context -> {return runViewFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("log")
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                    .executes(context -> {return runLogFlowCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                )
            )
            .then(Commands.literal("history")
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> {return runFlowHistoryCommand(IntegerArgumentType.getInteger(context, "page"), context);})
                )
                .executes(context -> {return runFlowHistoryCommand(0, context);})
            )
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .executes(context -> {return runDeleteFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            );
    }
    
}
