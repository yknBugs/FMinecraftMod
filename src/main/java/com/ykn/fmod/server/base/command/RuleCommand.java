/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.CustomRule;
import com.ykn.fmod.server.rule.core.RuleAction;
import com.ykn.fmod.server.rule.core.RuleCondition;
import com.ykn.fmod.server.rule.core.RuleContext;
import com.ykn.fmod.server.rule.tool.ConditionFormulaParser;
import com.ykn.fmod.server.rule.tool.RuleManager;
import com.ykn.fmod.server.rule.tool.RuleRegistry;
import com.ykn.fmod.server.rule.tool.RuleSerializer;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.loading.FMLPaths;

public class RuleCommand {

    private static RuleManager getRequiredRule(CommandContext<CommandSourceStack> context, String name) throws CommandRuntimeException {
        RuleFileSuggestion.suggest();
        ServerData data = Util.getServerData(Util.requireNotNullServer(context));
        RuleManager ruleManager = data.getCustomRules().get(name);
        if (ruleManager == null) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
        }
        return ruleManager;
    }

    private static int runCreateRuleCommand(String name, String event, CommandContext<CommandSourceStack> context) {
        try {
            if (name == null || name.isEmpty()) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.empty"));
            }
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if (data.getCustomRules().get(name) != null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.exists", name));
            }
            Collection<String> validEvents = RuleRegistry.getRegisteredEventNames();
            if (!validEvents.contains(event)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.event.unknown", event));
            }
            RuleManager ruleManager = new RuleManager(name, event);
            data.getCustomRules().put(name, ruleManager);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.create.success", event, name), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule create", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runCopyRuleCommand(String sourceName, String targetName, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager sourceRule = data.getCustomRules().get(sourceName);
            if (sourceRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", sourceName));
            }
            RuleManager targetRule = data.getCustomRules().get(targetName);
            if (targetRule != null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.exists", targetName));
            }
            RuleManager copiedRule = new RuleManager(sourceRule.getRule().copy());
            copiedRule.getRule().setName(targetName);
            data.getCustomRules().put(targetName, copiedRule);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.copy.success", sourceName, targetName), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule copy", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLoadRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            if (RuleFileSuggestion.getAvailableRules() == 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.hint"), false);
            }
            Path ruleFolder = FMLPaths.CONFIGDIR.get().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if ("*".equals(name)) {
                // Load all rules
                int loadedCount = 0;
                for (String ruleFileName : RuleFileSuggestion.getCachedRuleList()) {
                    Path rulePath = ruleFolder.resolve(ruleFileName).normalize();
                    if (!rulePath.startsWith(ruleFolder)) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.load.filenotfound", ruleFileName), false);
                        continue;
                    }
                    CustomRule rule = RuleSerializer.loadFile(rulePath);
                    if (rule == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.load.ioexception", ruleFileName), false);
                        continue;
                    }
                    if (data.getCustomRules().get(rule.getName()) != null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.exists", rule.getName()), false);
                        continue;
                    }
                    RuleManager ruleManager = new RuleManager(rule);
                    data.getCustomRules().put(rule.getName(), ruleManager);
                    ruleManager.setEnabled(true);
                    loadedCount++;
                }
                int loadedCountFinal = loadedCount;
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.load.all", String.valueOf(loadedCountFinal)), true);
                return loadedCountFinal;
            }
            Path rulePath = ruleFolder.resolve(name).normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.load.filenotfound", name));
            }
            CustomRule rule = RuleSerializer.loadFile(rulePath);
            if (rule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.load.ioexception", name));
            }
            if (data.getCustomRules().get(rule.getName()) != null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.exists", rule.getName()));
            }
            RuleManager ruleManager = new RuleManager(rule);
            data.getCustomRules().put(rule.getName(), ruleManager);
            ruleManager.setEnabled(true);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.load.success", rule.getName()), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule load", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSaveRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            Path ruleFolder = FMLPaths.CONFIGDIR.get().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if ("*".equals(name)) {
                // Save all rules
                int savedCount = 0;
                for (RuleManager ruleManager : data.getCustomRules().values()) {
                    Path rulePath = ruleFolder.resolve(ruleManager.getRule().getName() + ".rule").normalize();
                    if (!rulePath.startsWith(ruleFolder)) {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.save.notavailable", ruleManager.getRule().getName()), false);
                        continue;
                    }
                    boolean success = RuleSerializer.saveFile(ruleManager.getRule(), rulePath, true);
                    if (success) {
                        savedCount++;
                    } else {
                        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.save.ioexception", ruleManager.getRule().getName()), false);
                    }
                }
                int savedCountFinal = savedCount;
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.save.all", String.valueOf(savedCountFinal)), true);
                RuleFileSuggestion.suggest();
                return savedCountFinal;
            }
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            Path rulePath = ruleFolder.resolve(targetRule.getRule().getName() + ".rule").normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.save.notavailable", targetRule.getRule().getName()));
            }
            boolean success = RuleSerializer.saveFile(targetRule.getRule(), rulePath, true);
            if (!success) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.save.ioexception", targetRule.getRule().getName()));
            }
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.save.success", targetRule.getRule().getName()), true);
            RuleFileSuggestion.suggest();
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule save", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListRulesCommand(CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if (data.getCustomRules().isEmpty()) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.list.empty"), false);
                return Command.SINGLE_SUCCESS;
            }
            List<MutableComponent> ruleLines = new ArrayList<>();
            int enabledCount = 0;
            int totalCount = 0;
            for (RuleManager ruleManager : data.getCustomRules().values()) {
                MutableComponent line = null;
                String ruleName = ruleManager.getRule().getName();
                String ruleEvent = ruleManager.getRule().getEvent().getType();
                RuleCondition condition = ruleManager.getRule().getCondition();
                Component ruleCondition = condition.getName().isEmpty() ? condition.render() : Component.literal(condition.getName());
                if (ruleManager.isEnabled()) {
                    line = Util.parseTranslatableText("fmod.command.rule.list.enabled", ruleName, ruleEvent, ruleCondition).withStyle(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").withStyle(ChatFormatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule view \"" + ruleName + "\""))
                    );
                    enabledCount++;
                } else {
                    line = Util.parseTranslatableText("fmod.command.rule.list.disabled", ruleName, ruleEvent, ruleCondition).withStyle(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").withStyle(ChatFormatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule view \"" + ruleName + "\""))
                    );
                }
                totalCount++;
                ruleLines.add(line);
            }
            String enabledCountStr = String.valueOf(enabledCount);
            String totalCountStr = String.valueOf(totalCount);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.list.title", totalCountStr, enabledCountStr), false);
            for (MutableComponent line : ruleLines) {
                context.getSource().sendSuccess(() -> line, false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule list", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRenameRuleCommand(String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(oldName);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", oldName));
            }
            if (newName == null || newName.isEmpty()) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.empty"));
            }
            if (data.getCustomRules().get(newName) != null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.exists", newName));
            }
            data.getCustomRules().remove(oldName);
            targetRule.getRule().setName(newName);
            data.getCustomRules().put(newName, targetRule);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.rename.success", oldName, newName), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule rename", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetEnableRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            if (targetRule.isEnabled()) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.enable.get.true", name), false);
            } else {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.enable.get.false", name), false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule enable", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSetEnableRuleCommand(String name, boolean enable, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            targetRule.setEnabled(enable);
            if (enable) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.enable.set.true", name), true);
            } else {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.enable.set.false", name), true);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule enable", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runDeleteRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            data.getCustomRules().remove(name);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.delete.success", name), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule delete", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRuleHistoryCommand(int pageIndex, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            List<RuleContext> history = data.getRuleExecutionHistory();
            // 5 entries per page
            int maxPage = (history.size() + 4) / 5;
            if (maxPage <= 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.history.null"), false);
                return Command.SINGLE_SUCCESS;
            }
            int index = pageIndex;
            if (index <= 0) {
                index = maxPage;
            }
            if (index > maxPage) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.history.indexerror", pageIndex, maxPage));
            }
            int start = (index - 1) * 5;
            int end = Math.min(start + 5, history.size());
            String indexStr = String.valueOf(index);
            String maxPageStr = String.valueOf(maxPage);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.history.title", indexStr, maxPageStr), false);
            for (int i = start; i < end; i++) {
                RuleContext entry = history.get(i);
                String iStr = String.valueOf(i + 1);
                MutableComponent entryText =  Util.parseTranslatableText("fmod.command.rule.history.entry", iStr, entry.getRule().getName()).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").withStyle(ChatFormatting.GREEN)))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule log " + iStr))
                );
                context.getSource().sendSuccess(() -> entryText, false);
            }
            MutableComponent navigateText = Component.empty();
            if (index > 1) {
                String prevIndexStr = String.valueOf(index - 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.rule.history.prev").withStyle(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule history " + prevIndexStr))
                ));
            }
            if (index < maxPage) {
                String nextIndexStr = String.valueOf(index + 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.rule.history.next").withStyle(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule history " + nextIndexStr))
                ));
            }
            if (maxPage > 1) {
                context.getSource().sendSuccess(() -> navigateText, false);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule history", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runViewRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            Component text = targetRule.getRule().render();
            context.getSource().sendSuccess(() -> text, false);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule view", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLogRuleCommand(int index, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            List<RuleContext> history = data.getRuleExecutionHistory();
            if (index <= 0 || index > history.size()) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.log.indexerror", String.valueOf(index)));
            }
            RuleContext entry = history.get(index - 1);
            Component text = entry.render();
            context.getSource().sendSuccess(() -> text, false);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule log", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTriggerRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            RuleContext ctx = targetRule.trigger(data);
            if (ctx.getErrorMessage() != null) {
                throw new CommandRuntimeException(ctx.getErrorMessage());
            }
            context.getSource().sendSuccess(() -> ctx.render(), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule trigger", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTestRuleCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            RuleContext ctx = targetRule.test(data);
            if (ctx.getErrorMessage() != null) {
                throw new CommandRuntimeException(ctx.getErrorMessage());
            }
            context.getSource().sendSuccess(() -> ctx.render(), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule test", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleEventCommand(String name, String event, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            Collection<String> validEvents = RuleRegistry.getRegisteredEventNames();
            if (!validEvents.contains(event)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.event.unknown", event));
            }
            targetRule.setEvent(event);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.event.success", name, event), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit event", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleConditionCommand(String name, String formula, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            targetRule.setCondition(formula);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.success", name, formula), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalid", formula));
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleConditionRemoveCommand(String name, String condition, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (!targetRule.getRule().hasExtraCondition(condition)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.notexists", name, condition));
            }
            targetRule.removeCondition(condition);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.remove.success", name, condition), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition remove", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleConditionRenameCommand(String rule, String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, rule);
            if (!targetRule.getRule().hasExtraCondition(oldName)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.notexists", rule, oldName));
            }
            if (targetRule.getRule().hasExtraCondition(newName)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.exists", rule, newName));
            }
            if (!RuleCondition.NAME_PATTERN.matcher(newName).matches()) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalidname", newName));
            }
            targetRule.renameCondition(oldName, newName);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.rename.success", rule, oldName, newName), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition rename", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleActionRemoveCommand(String name, String action, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (!targetRule.getRule().hasActionIfSatisfied(action)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.action.notexists", name, action));
            }
            targetRule.removeActionIfSatisfied(action);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.action.remove.success", name, action), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit action remove", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleActionRenameCommand(String rule, String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, rule);
            if (!targetRule.getRule().hasActionIfSatisfied(oldName)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.action.notexists", rule, oldName));
            }
            if (targetRule.getRule().hasActionIfSatisfied(newName)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.action.exists", rule, newName));
            }
            targetRule.renameActionIfSatisfied(oldName, newName);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.action.rename.success", rule, oldName, newName), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit action rename", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRulePunishRemoveCommand(String name, String action, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (!targetRule.getRule().hasActionIfViolated(action)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.punish.notexists", name, action));
            }
            targetRule.removeActionIfViolated(action);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.punish.remove.success", name, action), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit punish remove", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRulePunishRenameCommand(String rule, String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, rule);
            if (!targetRule.getRule().hasActionIfViolated(oldName)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.punish.notexists", rule, oldName));
            }
            if (targetRule.getRule().hasActionIfViolated(newName)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.punish.exists", rule, newName));
            }
            targetRule.renameActionIfViolated(oldName, newName);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.punish.rename.success", rule, oldName, newName), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit punish rename", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleAddFormulaConditionCommand(String name, String condition, String formula, CommandContext<CommandSourceStack> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (targetRule.getRule().hasExtraCondition(condition)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.exists", name, condition));
            }
            if (!RuleCondition.NAME_PATTERN.matcher(condition).matches()) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalidname", condition));
            }
            RuleCondition ruleCondition = ConditionFormulaParser.parse(formula).setName(condition);
            targetRule.addCondition(ruleCondition);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.add.success", name, formula), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalid", formula));
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition add formula", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void runEditRuleAddConditionCommand(String name, RuleCondition condition, CommandContext<CommandSourceStack> context) {
        RuleManager targetRule = getRequiredRule(context, name);
        if (targetRule.getRule().hasExtraCondition(condition.getName())) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.exists", name, condition.getName()));
        }
        if (!RuleCondition.NAME_PATTERN.matcher(condition.getName()).matches()) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalidname", condition.getName()));
        }
        targetRule.addCondition(condition);
        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.add.success", name, condition.getName()), true);
    }

    private static void runEditRuleAddActionCommand(String name, RuleAction action, CommandContext<CommandSourceStack> context) {
        RuleManager targetRule = getRequiredRule(context, name);
        if (targetRule.getRule().hasActionIfSatisfied(action.getName())) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.action.exists", name, action.getName()));
        }
        targetRule.addActionIfSatisfied(action);
        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.action.add.success", name, action.getName()), true);
    }

    private static void runEditRuleAddPunishCommand(String name, RuleAction action, CommandContext<CommandSourceStack> context) {
        RuleManager targetRule = getRequiredRule(context, name);
        if (targetRule.getRule().hasActionIfViolated(action.getName())) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.rule.edit.punish.exists", name, action.getName()));
        }
        targetRule.addActionIfViolated(action);
        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.rule.edit.punish.add.success", name, action.getName()), true);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildAddConditionCommand() {
        LiteralArgumentBuilder<CommandSourceStack> sourceNode = Commands.literal("add");
        sourceNode = sourceNode.then(Commands.literal("ConditionExpression")
            .then(Commands.argument("name", StringArgumentType.string())
                .then(Commands.argument("formula", StringArgumentType.greedyString())
                    .executes(context -> {return runEditRuleAddFormulaConditionCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "formula"), context);})
                )
            )
        );
        BiConsumer<CommandContext<CommandSourceStack>, RuleCondition> consumer = (ctx, condition) -> runEditRuleAddConditionCommand(StringArgumentType.getString(ctx, "rule"), condition, ctx);
        for (String conditionType : RuleRegistry.getRegisteredConditionCommandNames()) {
            LiteralArgumentBuilder<CommandSourceStack> conditionNode = RuleRegistry.getConditionCommandFactory(conditionType).buildCommand(Commands.literal(conditionType), consumer);
            sourceNode = sourceNode.then(conditionNode);
        }
        return sourceNode;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildAddActionCommand(boolean isPunish) {
        BiConsumer<CommandContext<CommandSourceStack>, RuleAction> consumer = null;
        if (isPunish) {
            consumer = (ctx, action) -> runEditRuleAddPunishCommand(StringArgumentType.getString(ctx, "rule"), action, ctx);
        } else {
            consumer = (ctx, action) -> runEditRuleAddActionCommand(StringArgumentType.getString(ctx, "rule"), action, ctx);
        }
        LiteralArgumentBuilder<CommandSourceStack> sourceNode = Commands.literal("add");
        for (String actionType : RuleRegistry.getRegisteredActionCommandNames()) {
            LiteralArgumentBuilder<CommandSourceStack> actionNode = RuleRegistry.getActionCommandFactory(actionType).buildCommand(Commands.literal(actionType), consumer);
            sourceNode = sourceNode.then(actionNode);
        }
        return sourceNode;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("rule")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("event", StringArgumentType.string())
                        .suggests(StringSuggestion.suggest(RuleRegistry.getRegisteredEventNames(), true))
                        .executes(context -> {return runCreateRuleCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "event"), context);})
                    )
                )
            )
            .then(Commands.literal("copy")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> {return runCopyRuleCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "name"), context);})
                    )
                )
            )
            .then(Commands.literal("load")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .suggests(RuleFileSuggestion.suggest())
                    .executes(context -> {return runLoadRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("save")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                    .suggests(CustomRuleSuggestion.suggestSave())
                    .executes(context -> {return runSaveRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("list")
                .executes(context -> {return runListRulesCommand(context);})
            )
            .then(Commands.literal("rename")
                .then(Commands.argument("old", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(Commands.argument("new", StringArgumentType.string())
                        .executes(context -> {return runRenameRuleCommand(StringArgumentType.getString(context, "old"), StringArgumentType.getString(context, "new"), context);})
                    )
                )
            )
            .then(Commands.literal("enable")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {return runSetEnableRuleCommand(StringArgumentType.getString(context, "name"), BoolArgumentType.getBool(context, "enabled"), context);})
                    )
                    .executes(context -> {return runGetEnableRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runDeleteRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("history")
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> {return runRuleHistoryCommand(IntegerArgumentType.getInteger(context, "page"), context);})
                )
                .executes(context -> {return runRuleHistoryCommand(0, context);})
            )
            .then(Commands.literal("view")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runViewRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("log")
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                    .executes(context -> {return runLogRuleCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                )
            )
            .then(Commands.literal("trigger")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runTriggerRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("test")
                .then(Commands.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runTestRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(Commands.literal("edit")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(Commands.literal("event")
                        .then(Commands.argument("event", StringArgumentType.string())
                            .suggests(StringSuggestion.suggest(RuleRegistry.getRegisteredEventNames(), true))
                            .executes(context -> {return runEditRuleEventCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "event"), context);})
                        )
                    )
                    .then(Commands.literal("condition")
                        .then(buildAddConditionCommand())
                        .then(Commands.literal("set")
                            .then(Commands.argument("formula", StringArgumentType.greedyString())
                                .executes(context -> {return runEditRuleConditionCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "formula"), context);})
                            )
                        )
                        .then(Commands.literal("remove")
                            .then(Commands.argument("condition", StringArgumentType.string())
                                .suggests(RuleComponentSuggestion.suggestCondition(true, 3))
                                .executes(context -> {return runEditRuleConditionRemoveCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "condition"), context);})
                            )
                        )
                        .then(Commands.literal("rename")
                            .then(Commands.argument("oldName", StringArgumentType.string())
                                .suggests(RuleComponentSuggestion.suggestCondition(true, 3))
                                .then(Commands.argument("newName", StringArgumentType.string())
                                    .executes(context -> {return runEditRuleConditionRenameCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "oldName"), StringArgumentType.getString(context, "newName"), context);})
                                )
                            )
                        )
                    )
                    .then(Commands.literal("action")
                        .then(buildAddActionCommand(false))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("action", StringArgumentType.string())
                                .suggests(RuleComponentSuggestion.suggestAction(true, 3))
                                .executes(context -> {return runEditRuleActionRemoveCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "action"), context);})
                            )
                        )
                        .then(Commands.literal("rename")
                            .then(Commands.argument("oldName", StringArgumentType.string())
                                .suggests(RuleComponentSuggestion.suggestAction(true, 3))
                                .then(Commands.argument("newName", StringArgumentType.string())
                                    .executes(context -> {return runEditRuleActionRenameCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "oldName"), StringArgumentType.getString(context, "newName"), context);})
                                )
                            )
                        )
                    )
                    .then(Commands.literal("punish")
                        .then(buildAddActionCommand(true))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("action", StringArgumentType.string())
                                .suggests(RuleComponentSuggestion.suggestPunish(true, 3))
                                .executes(context -> {return runEditRulePunishRemoveCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "action"), context);})
                            )
                        )
                        .then(Commands.literal("rename")
                            .then(Commands.argument("oldName", StringArgumentType.string())
                                .suggests(RuleComponentSuggestion.suggestPunish(true, 3))
                                .then(Commands.argument("newName", StringArgumentType.string())
                                    .executes(context -> {return runEditRulePunishRenameCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "oldName"), StringArgumentType.getString(context, "newName"), context);})
                                )
                            )
                        )
                    )
                )
            );
    }
    
}
