/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class FlowCommand {

    private static int runCreateFlowCommand(String name, String eventNode, String eventNodeName, CommandContext<ServerCommandSource> context) {
        try {
            ServerData data = Util.getServerData(context.getSource().getServer());
            if (data.logicFlows.get(name) != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.exists", name));
            }
            Collection<String> validEventNodes = NodeRegistry.getEventNodeList();
            if (!validEventNodes.contains(eventNode)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.event.unknown", eventNode));
            }
            FlowManager flowManager = new FlowManager(name, eventNode, eventNodeName);
            data.logicFlows.put(name, flowManager);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.create.success", eventNode, name), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow create", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runCopyFlowCommand(String sourceName, String targetName, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager sourceFlow = data.logicFlows.get(sourceName);
            if (sourceFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", sourceName));
            }
            FlowManager targetFlow = data.logicFlows.get(targetName);
            if (targetFlow != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.exists", targetName));
            }
            FlowManager copiedFlow = new FlowManager(sourceFlow.flow.copy());
            copiedFlow.flow.name = targetName;
            data.logicFlows.put(targetName, copiedFlow);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.copy.success", sourceName, targetName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow copy", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLoadFlowCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            if (FlowFileSuggestion.getAvailableFlows() == 0) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.hint"), false);
            }
            Path flowFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(context.getSource().getServer());
            if ("*".equals(name)) {
                // Load all flow files
                int loadedCount = 0;
                for (String flowFileName : FlowFileSuggestion.cachedFlowList) {
                    Path flowPath = flowFolder.resolve(flowFileName).normalize();
                    if (!flowPath.startsWith(flowFolder)) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.load.filenotfound", flowFileName), false);
                        continue;
                    }
                    LogicFlow flow = FlowSerializer.loadFile(flowPath);
                    if (flow == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.load.ioexception", flowFileName), false);
                        continue;
                    }
                    if (data.logicFlows.get(flow.name) != null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.exists", flow.name), false);
                        continue;
                    }
                    FlowManager flowManager = new FlowManager(flow);
                    data.logicFlows.put(flow.name, flowManager);
                    flowManager.isEnabled = true;
                    loadedCount++;
                }
                int loadedCountFinal = loadedCount;
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.load.all", String.valueOf(loadedCountFinal)), true);
                return loadedCountFinal;
            }
            Path flowPath = flowFolder.resolve(name).normalize();
            if (!flowPath.startsWith(flowFolder)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.load.filenotfound", name));
            }
            LogicFlow flow = FlowSerializer.loadFile(flowPath);
            if (flow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.load.ioexception", name));
            }
            if (data.logicFlows.get(flow.name) != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.exists", flow.name));
            }
            FlowManager flowManager = new FlowManager(flow);
            data.logicFlows.put(flow.name, flowManager);
            flowManager.isEnabled = true;
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.load.success", flow.name), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow load", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSaveFlowCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            Path flowFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(context.getSource().getServer());
            if ("*".equals(name)) {
                // Save all flows
                int savedCount = 0;
                for (FlowManager flowManager : data.logicFlows.values()) {
                    Path flowPath = flowFolder.resolve(flowManager.flow.name + ".flow").normalize();
                    if (!flowPath.startsWith(flowFolder)) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.save.notavailable", flowManager.flow.name), false);
                        continue;
                    }
                    boolean success = FlowSerializer.saveFile(flowManager.flow, flowPath, true);
                    if (success) {
                        savedCount++;
                    } else {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.save.ioexception", flowManager.flow.name), false);
                    }
                }
                int savedCountFinal = savedCount;
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.save.all", String.valueOf(savedCountFinal)), true);
                FlowFileSuggestion.suggest();
                return savedCountFinal;
            }
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            }
            Path flowPath = flowFolder.resolve(targetFlow.flow.name + ".flow").normalize();
            if (!flowPath.startsWith(flowFolder)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.save.notavailable", targetFlow.flow.name));
            }
            boolean success = FlowSerializer.saveFile(targetFlow.flow, flowPath, true);
            if (!success) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.save.ioexception", targetFlow.flow.name));
            }
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.save.success", targetFlow.flow.name), true);
            FlowFileSuggestion.suggest();
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow save", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListFlowCommand(CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            if (data.logicFlows.isEmpty()) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.list.empty"), false);
                return Command.SINGLE_SUCCESS;
            }
            List<MutableText> flowLines = new ArrayList<>();
            int enabledCount = 0;
            int totalCount = 0;
            for (FlowManager flowManager : data.logicFlows.values()) {
                MutableText line = null;
                String numNodesStr = String.valueOf(flowManager.flow.getNodes().size());
                FlowNode startNode = flowManager.flow.getFirstNode();
                if (startNode == null) {
                    continue;
                }
                String startNodeStr = startNode.name;
                if (flowManager.isEnabled) {
                    line = Util.parseTranslatableText("fmod.command.flow.list.enabled", flowManager.flow.name, numNodesStr, startNodeStr).styled(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").formatted(Formatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow view \"" + flowManager.flow.name + "\""))
                    );
                    enabledCount++;
                } else {
                    line = Util.parseTranslatableText("fmod.command.flow.list.disabled", flowManager.flow.name, numNodesStr, startNodeStr).styled(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").formatted(Formatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow view \"" + flowManager.flow.name + "\""))
                    );
                }
                totalCount++;
                flowLines.add(line);
            }
            String enabledCountStr = String.valueOf(enabledCount);
            String totalCountStr = String.valueOf(totalCount);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.list.title", totalCountStr, enabledCountStr), false);
            for (MutableText line : flowLines) {
                context.getSource().sendFeedback(() -> line, false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow list", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRenameFlowCommand(String oldName, String newName, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(oldName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", oldName));
            }
            if (data.logicFlows.get(newName) != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.exists", newName));
            }
            data.logicFlows.remove(oldName);
            targetFlow.flow.name = newName;
            data.logicFlows.put(newName, targetFlow);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.rename.success", oldName, newName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow rename", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetEnableFlowCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            }
            if (targetFlow.isEnabled) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.enable.get.true", name), false);
            } else {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.enable.get.false", name), false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow enable", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSetEnableFlowCommand(String name, boolean enable, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            }
            targetFlow.isEnabled = enable;
            if (enable) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.enable.set.true", name), true);
            } else {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.enable.set.false", name), true);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow enable", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runExecuteFlowCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            }
            LogicException exception = targetFlow.execute(data, null, null);
            if (exception != null) {
                throw new CommandException(exception.getMessageText());
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow execute", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runDeleteFlowCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            }
            data.logicFlows.remove(name);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.delete.success", name), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow delete", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runFlowHistoryCommand(int pageIndex, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            List<ExecutionContext> history = data.executeHistory;
            // 5 entries per page
            int maxPage = (history.size() + 4) / 5;
            if (maxPage <= 0) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.history.null"), false);
                return Command.SINGLE_SUCCESS;
            }
            int index = pageIndex;
            if (index <= 0) {
                index = maxPage;
            }
            if (index > maxPage) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.history.indexerror", pageIndex, maxPage));
            }
            int start = (index - 1) * 5;
            int end = Math.min(start + 5, history.size());
            String indexStr = String.valueOf(index);
            String maxPageStr = String.valueOf(maxPage);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.history.title", indexStr, maxPageStr), false);
            for (int i = start; i < end; i++) {
                ExecutionContext entry = history.get(i);
                String iStr = String.valueOf(i + 1);
                MutableText entryText =  Util.parseTranslatableText("fmod.command.flow.history.entry", iStr, entry.getFlow().name).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").formatted(Formatting.GREEN)))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow log " + iStr))
                );
                context.getSource().sendFeedback(() -> entryText, false);
            }
            MutableText navigateText = Text.empty();
            if (index > 1) {
                String prevIndexStr = String.valueOf(index - 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.flow.history.prev").styled(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow history " + prevIndexStr))
                ));
            }
            if (index < maxPage) {
                String nextIndexStr = String.valueOf(index + 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.flow.history.next").styled(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow history " + nextIndexStr))
                ));
            }
            if (maxPage > 1) {
                context.getSource().sendFeedback(() -> navigateText, false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow history", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runViewFlowCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", name));
            }
            Text text = targetFlow.flow.render();
            context.getSource().sendFeedback(() -> text, false);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow view", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLogFlowCommand(int index, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            List<ExecutionContext> history = data.executeHistory;
            if (index <= 0 || index > history.size()) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.log.indexerror", String.valueOf(index)));
            }
            ExecutionContext entry = history.get(index - 1);
            Text text = entry.render();
            context.getSource().sendFeedback(() -> text, false);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow log", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowNewNodeCommand(String flowName, String type, String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            Collection<String> validNodeTypes = NodeRegistry.getNodeList();
            if (!validNodeTypes.contains(type)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.unknown", type));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.exists", name, flowName));
            }
            targetFlow.createNode(type, name);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.newnode.success", name, flowName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRemoveNodeCommand(String flowName, String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (existingNode.isEventNode()) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.delete.event", flowName, name));
            }
            targetFlow.removeNode(name);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.removenode.success", name, flowName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowReplaceEventCommand(String flowName, String type, String name, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            Collection<String> validEventNodes = NodeRegistry.getEventNodeList();
            if (!validEventNodes.contains(type)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.event.unknown", type));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.exists", name, flowName));
            }
            targetFlow.replaceEventNode(type, name);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.replaceevent.success", type, flowName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRenameNodeCommand(String flowName, String oldName, String newName, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(oldName);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", oldName, flowName));
            }
            FlowNode newNode = targetFlow.flow.getNodeByName(newName);
            if (newNode != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.exists", newName, flowName));
            }
            targetFlow.renameNode(oldName, newName);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.renamenode.success", oldName, flowName, newName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowConstInputCommand(String flowName, String name, int index, String value, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
            }
            // parse const value
            DataReference ref = FlowSerializer.parseConstDataReference(value);
            Object parsedValue = ref.value;
            String parsedValueStr = String.valueOf(parsedValue);
            targetFlow.setConstInput(name, index - 1, parsedValue);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.const.success", name, existingNode.getMetadata().inputNames.get(index - 1), parsedValueStr), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRefInputCommand(String flowName, String name, int index, String refNode, int refIndex, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
            }
            FlowNode refExistingNode = targetFlow.flow.getNodeByName(refNode);
            if (refExistingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", refNode, flowName));
            }
            if (refIndex <= 0 || refIndex > refExistingNode.getMetadata().outputNumber) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.edit.output.indexerror", refNode, String.valueOf(refIndex)));
            }
            targetFlow.setReferenceInput(name, index - 1, refNode, refIndex - 1);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.ref.success", name, existingNode.getMetadata().inputNames.get(index - 1), refNode, refExistingNode.getMetadata().outputNames.get(refIndex - 1)), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowDisconnectInputCommand(String flowName, String name, int index, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
            }
            targetFlow.disconnectInput(name, index - 1);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.disconnect.success", name, existingNode.getMetadata().inputNames.get(index - 1)), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowNextNodeCommand(String flowName, String name, int index, String next, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().branchNumber) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.edit.branch.indexerror", name, String.valueOf(index)));
            }
            FlowNode nextNode = targetFlow.flow.getNodeByName(next);
            if (nextNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", next, flowName));
            }
            targetFlow.setNextNode(name, index - 1, next);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.next.success", name, existingNode.getMetadata().branchNames.get(index - 1), next), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowFinalBranchCommand(String flowName, String name, int index, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().branchNumber) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.edit.branch.indexerror", name, String.valueOf(index)));
            }
            targetFlow.disconnectNextNode(name, index - 1);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.final.success", name, existingNode.getMetadata().branchNames.get(index - 1)), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowUndoCommand(String flowName, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            if (targetFlow.undoPath.isEmpty()) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.undo.nothing", flowName), false);
                return Command.SINGLE_SUCCESS;
            } else {
                targetFlow.undo();
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.undo.success", flowName), true);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRedoCommand(String flowName, CommandContext<ServerCommandSource> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.flow.notexists", flowName));
            }
            if (targetFlow.redoPath.isEmpty()) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.redo.nothing", flowName), false);
                return Command.SINGLE_SUCCESS;
            } else {
                targetFlow.redo();
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.flow.edit.redo.success", flowName), true);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand() {
        return CommandManager.literal("flow")
            .requires(source -> source.hasPermissionLevel(3))
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .then(CommandManager.argument("event", StringArgumentType.string())
                        .suggests(StringSuggestion.suggest(NodeRegistry.getEventNodeList(), true))
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .executes(context -> {return runCreateFlowCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "event"), StringArgumentType.getString(context, "node"), context);})
                        )
                    )
                )
            )
            .then(CommandManager.literal("list")
                .executes(context -> {return runListFlowCommand(context);})
            )
            .then(CommandManager.literal("edit")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(CommandManager.literal("new")
                        .then(CommandManager.argument("type", StringArgumentType.string())
                            .suggests(StringSuggestion.suggest(NodeRegistry.getNodeList(), true))
                            .then(CommandManager.argument("node", StringArgumentType.string())
                                .executes(context -> {return runEditFlowNewNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "type"), StringArgumentType.getString(context, "node"), context);})
                            )
                        )
                    )
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .executes(context -> {return runEditFlowRemoveNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), context);})
                        )
                    )
                    .then(CommandManager.literal("event")
                        .then(CommandManager.argument("type", StringArgumentType.string())
                            .suggests(StringSuggestion.suggest(NodeRegistry.getEventNodeList(), true))
                            .then(CommandManager.argument("node", StringArgumentType.string())
                                .executes(context -> {return runEditFlowReplaceEventCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "type"), StringArgumentType.getString(context, "node"), context);})
                            )
                        )
                    )
                    .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("old", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(CommandManager.argument("new", StringArgumentType.string())
                                .executes(context -> {return runEditFlowRenameNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "old"), StringArgumentType.getString(context, "new"), context);})
                            )
                        )
                    )
                    .then(CommandManager.literal("const")
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("value", StringArgumentType.string())
                                    .executes(context -> {return runEditFlowConstInputCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "value"), context);})
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("reference")
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("refNode", StringArgumentType.string())
                                    .suggests(FlowNodeSuggestion.suggest(true, 3))
                                    .then(CommandManager.argument("refIndex", IntegerArgumentType.integer(1))
                                        .executes(context -> {return runEditFlowRefInputCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "refNode"), IntegerArgumentType.getInteger(context, "refIndex"), context);})
                                    )
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("disconnect")
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {return runEditFlowDisconnectInputCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), context);})
                            )
                        )
                    )
                    .then(CommandManager.literal("next")
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("next", StringArgumentType.string())
                                    .suggests(FlowNodeSuggestion.suggest(true, 3))
                                    .executes(context -> {return runEditFlowNextNodeCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "next"), context);})
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("final")
                        .then(CommandManager.argument("node", StringArgumentType.string())
                            .suggests(FlowNodeSuggestion.suggest(true, 3))
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {return runEditFlowFinalBranchCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "node"), IntegerArgumentType.getInteger(context, "index"), context);})
                            )
                        )
                    )
                    .then(CommandManager.literal("redo")
                        .executes(context -> {return runEditFlowRedoCommand(StringArgumentType.getString(context, "name"), context);})
                    )
                    .then(CommandManager.literal("undo")
                        .executes(context -> {return runEditFlowUndoCommand(StringArgumentType.getString(context, "name"), context);})
                    )
                )
            )
            .then(CommandManager.literal("rename")
                .then(CommandManager.argument("old", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(CommandManager.argument("new", StringArgumentType.string())
                        .executes(context -> {return runRenameFlowCommand(StringArgumentType.getString(context, "old"), StringArgumentType.getString(context, "new"), context);})
                    )
                )
            )
            .then(CommandManager.literal("copy")
                .then(CommandManager.argument("flow", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .executes(context -> {return runCopyFlowCommand(StringArgumentType.getString(context, "flow"), StringArgumentType.getString(context, "name"), context);})
                    )
                )
            )
            .then(CommandManager.literal("save")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .suggests(LogicFlowSuggestion.suggestSave())
                    .executes(context -> {return runSaveFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("load")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .suggests(FlowFileSuggestion.suggest())
                    .executes(context -> {return runLoadFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("enable")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {return runSetEnableFlowCommand(StringArgumentType.getString(context, "name"), BoolArgumentType.getBool(context, "enabled"), context);})
                    )
                    .executes(context -> {return runGetEnableFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("execute")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .executes(context -> {return runExecuteFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("view")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .executes(context -> {return runViewFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("log")
                .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                    .executes(context -> {return runLogFlowCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                )
            )
            .then(CommandManager.literal("history")
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> {return runFlowHistoryCommand(IntegerArgumentType.getInteger(context, "page"), context);})
                )
                .executes(context -> {return runFlowHistoryCommand(0, context);})
            )
            .then(CommandManager.literal("delete")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(LogicFlowSuggestion.suggest(true))
                    .executes(context -> {return runDeleteFlowCommand(StringArgumentType.getString(context, "name"), context);})
                )
            );
    }
    
}
