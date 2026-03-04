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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RuleCommand {

    private static RuleManager getRequiredRule(CommandContext<ServerCommandSource> context, String name) throws CommandException {
        RuleFileSuggestion.suggest();
        ServerData data = Util.getServerData(Util.requireNotNullServer(context));
        RuleManager ruleManager = data.getCustomRules().get(name);
        if (ruleManager == null) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
        }
        return ruleManager;
    }

    private static int runCreateRuleCommand(String name, String event, CommandContext<ServerCommandSource> context) {
        try {
            if (name == null || name.isEmpty()) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.empty"));
            }
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if (data.getCustomRules().get(name) != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.exists", name));
            }
            Collection<String> validEvents = RuleRegistry.getRegisteredEventNames();
            if (!validEvents.contains(event)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.event.unknown", event));
            }
            RuleManager ruleManager = new RuleManager(name, event);
            data.getCustomRules().put(name, ruleManager);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.create.success", event, name), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule create", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runCopyRuleCommand(String sourceName, String targetName, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager sourceRule = data.getCustomRules().get(sourceName);
            if (sourceRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", sourceName));
            }
            RuleManager targetRule = data.getCustomRules().get(targetName);
            if (targetRule != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.exists", targetName));
            }
            RuleManager copiedRule = new RuleManager(sourceRule.getRule().copy());
            copiedRule.getRule().setName(targetName);
            data.getCustomRules().put(targetName, copiedRule);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.copy.success", sourceName, targetName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule copy", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLoadRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            if (RuleFileSuggestion.getAvailableRules() == 0) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.hint"), false);
            }
            Path ruleFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if ("*".equals(name)) {
                // Load all rules
                int loadedCount = 0;
                for (String ruleFileName : RuleFileSuggestion.getCachedRuleList()) {
                    Path rulePath = ruleFolder.resolve(ruleFileName).normalize();
                    if (!rulePath.startsWith(ruleFolder)) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.load.filenotfound", ruleFileName), false);
                        continue;
                    }
                    CustomRule rule = RuleSerializer.loadFile(rulePath);
                    if (rule == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.load.ioexception", ruleFileName), false);
                        continue;
                    }
                    if (data.getCustomRules().get(rule.getName()) != null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.exists", rule.getName()), false);
                        continue;
                    }
                    RuleManager ruleManager = new RuleManager(rule);
                    data.getCustomRules().put(rule.getName(), ruleManager);
                    ruleManager.setEnabled(true);
                    loadedCount++;
                }
                int loadedCountFinal = loadedCount;
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.load.all", String.valueOf(loadedCountFinal)), true);
                return loadedCountFinal;
            }
            Path rulePath = ruleFolder.resolve(name).normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.load.filenotfound", name));
            }
            CustomRule rule = RuleSerializer.loadFile(rulePath);
            if (rule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.load.ioexception", name));
            }
            if (data.getCustomRules().get(rule.getName()) != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.exists", rule.getName()));
            }
            RuleManager ruleManager = new RuleManager(rule);
            data.getCustomRules().put(rule.getName(), ruleManager);
            ruleManager.setEnabled(true);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.load.success", rule.getName()), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule load", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSaveRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            Path ruleFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if ("*".equals(name)) {
                // Save all rules
                int savedCount = 0;
                for (RuleManager ruleManager : data.getCustomRules().values()) {
                    Path rulePath = ruleFolder.resolve(ruleManager.getRule().getName() + ".rule").normalize();
                    if (!rulePath.startsWith(ruleFolder)) {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.save.notavailable", ruleManager.getRule().getName()), false);
                        continue;
                    }
                    boolean success = RuleSerializer.saveFile(ruleManager.getRule(), rulePath, true);
                    if (success) {
                        savedCount++;
                    } else {
                        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.save.ioexception", ruleManager.getRule().getName()), false);
                    }
                }
                int savedCountFinal = savedCount;
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.save.all", String.valueOf(savedCountFinal)), true);
                RuleFileSuggestion.suggest();
                return savedCountFinal;
            }
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            Path rulePath = ruleFolder.resolve(targetRule.getRule().getName() + ".rule").normalize();
            if (!rulePath.startsWith(ruleFolder)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.save.notavailable", targetRule.getRule().getName()));
            }
            boolean success = RuleSerializer.saveFile(targetRule.getRule(), rulePath, true);
            if (!success) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.save.ioexception", targetRule.getRule().getName()));
            }
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.save.success", targetRule.getRule().getName()), true);
            RuleFileSuggestion.suggest();
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule save", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListRulesCommand(CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            if (data.getCustomRules().isEmpty()) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.list.empty"), false);
                return Command.SINGLE_SUCCESS;
            }
            List<MutableText> ruleLines = new ArrayList<>();
            int enabledCount = 0;
            int totalCount = 0;
            for (RuleManager ruleManager : data.getCustomRules().values()) {
                MutableText line = null;
                String ruleName = ruleManager.getRule().getName();
                String ruleEvent = ruleManager.getRule().getEvent().getType();
                RuleCondition condition = ruleManager.getRule().getCondition();
                Text ruleCondition = condition.getName().isEmpty() ? condition.render() : Text.literal(condition.getName());
                if (ruleManager.isEnabled()) {
                    line = Util.parseTranslatableText("fmod.command.rule.list.enabled", ruleName, ruleEvent, ruleCondition).styled(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").formatted(Formatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule view \"" + ruleName + "\""))
                    );
                    enabledCount++;
                } else {
                    line = Util.parseTranslatableText("fmod.command.rule.list.disabled", ruleName, ruleEvent, ruleCondition).styled(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").formatted(Formatting.GREEN)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule view \"" + ruleName + "\""))
                    );
                }
                totalCount++;
                ruleLines.add(line);
            }
            String enabledCountStr = String.valueOf(enabledCount);
            String totalCountStr = String.valueOf(totalCount);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.list.title", totalCountStr, enabledCountStr), false);
            for (MutableText line : ruleLines) {
                context.getSource().sendFeedback(() -> line, false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule list", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRenameRuleCommand(String oldName, String newName, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(oldName);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", oldName));
            }
            if (newName == null || newName.isEmpty()) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.empty"));
            }
            if (data.getCustomRules().get(newName) != null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.exists", newName));
            }
            data.getCustomRules().remove(oldName);
            targetRule.getRule().setName(newName);
            data.getCustomRules().put(newName, targetRule);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.rename.success", oldName, newName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule rename", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetEnableRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            if (targetRule.isEnabled()) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.enable.get.true", name), false);
            } else {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.enable.get.false", name), false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule enable", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSetEnableRuleCommand(String name, boolean enable, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            targetRule.setEnabled(enable);
            if (enable) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.enable.set.true", name), true);
            } else {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.enable.set.false", name), true);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule enable", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runDeleteRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            data.getCustomRules().remove(name);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.delete.success", name), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule delete", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRuleHistoryCommand(int pageIndex, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            List<RuleContext> history = data.getRuleExecutionHistory();
            // 5 entries per page
            int maxPage = (history.size() + 4) / 5;
            if (maxPage <= 0) {
                context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.history.null"), false);
                return Command.SINGLE_SUCCESS;
            }
            int index = pageIndex;
            if (index <= 0) {
                index = maxPage;
            }
            if (index > maxPage) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.history.indexerror", pageIndex, maxPage));
            }
            int start = (index - 1) * 5;
            int end = Math.min(start + 5, history.size());
            String indexStr = String.valueOf(index);
            String maxPageStr = String.valueOf(maxPage);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.history.title", indexStr, maxPageStr), false);
            for (int i = start; i < end; i++) {
                RuleContext entry = history.get(i);
                String iStr = String.valueOf(i + 1);
                MutableText entryText =  Util.parseTranslatableText("fmod.command.rule.history.entry", iStr, entry.getRule().getName()).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslatableText("fmod.misc.clickview").formatted(Formatting.GREEN)))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule log " + iStr))
                );
                context.getSource().sendFeedback(() -> entryText, false);
            }
            MutableText navigateText = Text.empty();
            if (index > 1) {
                String prevIndexStr = String.valueOf(index - 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.rule.history.prev").styled(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule history " + prevIndexStr))
                ));
            }
            if (index < maxPage) {
                String nextIndexStr = String.valueOf(index + 1);
                navigateText.append(Util.parseTranslatableText("fmod.command.rule.history.next").styled(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f rule history " + nextIndexStr))
                ));
            }
            if (maxPage > 1) {
                context.getSource().sendFeedback(() -> navigateText, false);
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule history", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runViewRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            Text text = targetRule.getRule().render();
            context.getSource().sendFeedback(() -> text, false);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule view", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLogRuleCommand(int index, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            List<RuleContext> history = data.getRuleExecutionHistory();
            if (index <= 0 || index > history.size()) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.log.indexerror", String.valueOf(index)));
            }
            RuleContext entry = history.get(index - 1);
            Text text = entry.render();
            context.getSource().sendFeedback(() -> text, false);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule log", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTriggerRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            RuleContext ctx = targetRule.trigger(data);
            if (ctx.getErrorMessage() != null) {
                throw new CommandException(ctx.getErrorMessage());
            }
            context.getSource().sendFeedback(() -> ctx.render(), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule trigger", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTestRuleCommand(String name, CommandContext<ServerCommandSource> context) {
        try {
            RuleFileSuggestion.suggest();
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            RuleManager targetRule = data.getCustomRules().get(name);
            if (targetRule == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.notexists", name));
            }
            RuleContext ctx = targetRule.test(data);
            if (ctx.getErrorMessage() != null) {
                throw new CommandException(ctx.getErrorMessage());
            }
            context.getSource().sendFeedback(() -> ctx.render(), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule test", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleEventCommand(String name, String event, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            Collection<String> validEvents = RuleRegistry.getRegisteredEventNames();
            if (!validEvents.contains(event)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.event.unknown", event));
            }
            targetRule.setEvent(event);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.event.success", name, event), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit event", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleConditionCommand(String name, String formula, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            targetRule.setCondition(formula);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.success", name, formula), true);
        } catch (CommandException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalid", formula));
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleConditionRemoveCommand(String name, String condition, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (!targetRule.getRule().hasExtraCondition(condition)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.notexists", name, condition));
            }
            targetRule.removeCondition(condition);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.remove.success", name, condition), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition remove", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleConditionRenameCommand(String rule, String oldName, String newName, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, rule);
            if (!targetRule.getRule().hasExtraCondition(oldName)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.notexists", rule, oldName));
            }
            if (targetRule.getRule().hasExtraCondition(newName)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.exists", rule, newName));
            }
            targetRule.renameCondition(oldName, newName);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.rename.success", rule, oldName, newName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition rename", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleActionRemoveCommand(String name, String action, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (!targetRule.getRule().hasActionIfSatisfied(action)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.action.notexists", name, action));
            }
            targetRule.removeActionIfSatisfied(action);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.action.remove.success", name, action), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit action remove", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleActionRenameCommand(String rule, String oldName, String newName, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, rule);
            if (!targetRule.getRule().hasActionIfSatisfied(oldName)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.action.notexists", rule, oldName));
            }
            if (targetRule.getRule().hasActionIfSatisfied(newName)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.action.exists", rule, newName));
            }
            targetRule.renameActionIfSatisfied(oldName, newName);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.action.rename.success", rule, oldName, newName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit action rename", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRulePunishRemoveCommand(String name, String action, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (!targetRule.getRule().hasActionIfViolated(action)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.punish.notexists", name, action));
            }
            targetRule.removeActionIfViolated(action);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.punish.remove.success", name, action), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit punish remove", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRulePunishRenameCommand(String rule, String oldName, String newName, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, rule);
            if (!targetRule.getRule().hasActionIfViolated(oldName)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.punish.notexists", rule, oldName));
            }
            if (targetRule.getRule().hasActionIfViolated(newName)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.punish.exists", rule, newName));
            }
            targetRule.renameActionIfViolated(oldName, newName);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.punish.rename.success", rule, oldName, newName), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit punish rename", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditRuleAddFormulaConditionCommand(String name, String condition, String formula, CommandContext<ServerCommandSource> context) {
        try {
            RuleManager targetRule = getRequiredRule(context, name);
            if (targetRule.getRule().hasExtraCondition(condition)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.exists", name, condition));
            }
            RuleCondition ruleCondition = ConditionFormulaParser.parse(formula).setName(condition);
            targetRule.addCondition(ruleCondition);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.add.success", name, formula), true);
        } catch (CommandException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.invalid", formula));
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f rule edit condition add formula", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void runEditRuleAddConditionCommand(String name, RuleCondition condition, CommandContext<ServerCommandSource> context) {
        RuleManager targetRule = getRequiredRule(context, name);
        if (targetRule.getRule().hasExtraCondition(condition.getName())) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.condition.exists", name, condition.getName()));
        }
        targetRule.addCondition(condition);
        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.condition.add.success", name, condition.getName()), true);
    }

    private static void runEditRuleAddActionCommand(String name, RuleAction action, CommandContext<ServerCommandSource> context) {
        RuleManager targetRule = getRequiredRule(context, name);
        if (targetRule.getRule().hasActionIfSatisfied(action.getName())) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.action.exists", name, action.getName()));
        }
        targetRule.addActionIfSatisfied(action);
        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.action.add.success", name, action.getName()), true);
    }

    private static void runEditRuleAddPunishCommand(String name, RuleAction action, CommandContext<ServerCommandSource> context) {
        RuleManager targetRule = getRequiredRule(context, name);
        if (targetRule.getRule().hasActionIfViolated(action.getName())) {
            throw new CommandException(Util.parseTranslatableText("fmod.command.rule.edit.punish.exists", name, action.getName()));
        }
        targetRule.addActionIfViolated(action);
        context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.rule.edit.punish.add.success", name, action.getName()), true);
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAddConditionCommand() {
        LiteralArgumentBuilder<ServerCommandSource> sourceNode = CommandManager.literal("add");
        sourceNode = sourceNode.then(CommandManager.literal("ConditionExpression")
            .then(CommandManager.argument("name", StringArgumentType.string())
                .then(CommandManager.argument("formula", StringArgumentType.greedyString())
                    .executes(context -> {return runEditRuleAddFormulaConditionCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "formula"), context);})
                )
            )
        );
        BiConsumer<CommandContext<ServerCommandSource>, RuleCondition> consumer = (ctx, condition) -> runEditRuleAddConditionCommand(StringArgumentType.getString(ctx, "rule"), condition, ctx);
        for (String conditionType : RuleRegistry.getRegisteredConditionCommandNames()) {
            LiteralArgumentBuilder<ServerCommandSource> conditionNode = RuleRegistry.getConditionCommandFactory(conditionType).buildCommand(CommandManager.literal(conditionType), consumer);
            sourceNode = sourceNode.then(conditionNode);
        }
        return sourceNode;
    }

    private static LiteralArgumentBuilder<ServerCommandSource> buildAddActionCommand(boolean isPunish) {
        BiConsumer<CommandContext<ServerCommandSource>, RuleAction> consumer = null;
        if (isPunish) {
            consumer = (ctx, action) -> runEditRuleAddPunishCommand(StringArgumentType.getString(ctx, "rule"), action, ctx);
        } else {
            consumer = (ctx, action) -> runEditRuleAddActionCommand(StringArgumentType.getString(ctx, "rule"), action, ctx);
        }
        LiteralArgumentBuilder<ServerCommandSource> sourceNode = CommandManager.literal("add");
        for (String actionType : RuleRegistry.getRegisteredActionCommandNames()) {
            LiteralArgumentBuilder<ServerCommandSource> actionNode = RuleRegistry.getActionCommandFactory(actionType).buildCommand(CommandManager.literal(actionType), consumer);
            sourceNode = sourceNode.then(actionNode);
        }
        return sourceNode;
    }

    public static LiteralArgumentBuilder<ServerCommandSource> buildCommand() {
        return CommandManager.literal("rule")
            .requires(source -> source.hasPermissionLevel(3))
            .then(CommandManager.literal("create")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .then(CommandManager.argument("event", StringArgumentType.string())
                        .suggests(StringSuggestion.suggest(RuleRegistry.getRegisteredEventNames(), true))
                        .executes(context -> {return runCreateRuleCommand(StringArgumentType.getString(context, "name"), StringArgumentType.getString(context, "event"), context);})
                    )
                )
            )
            .then(CommandManager.literal("copy")
                .then(CommandManager.argument("rule", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .executes(context -> {return runCopyRuleCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "name"), context);})
                    )
                )
            )
            .then(CommandManager.literal("load")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .suggests(RuleFileSuggestion.suggest())
                    .executes(context -> {return runLoadRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("save")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .suggests(CustomRuleSuggestion.suggestSave())
                    .executes(context -> {return runSaveRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("list")
                .executes(context -> {return runListRulesCommand(context);})
            )
            .then(CommandManager.literal("rename")
                .then(CommandManager.argument("old", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(CommandManager.argument("new", StringArgumentType.string())
                        .executes(context -> {return runRenameRuleCommand(StringArgumentType.getString(context, "old"), StringArgumentType.getString(context, "new"), context);})
                    )
                )
            )
            .then(CommandManager.literal("enable")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {return runSetEnableRuleCommand(StringArgumentType.getString(context, "name"), BoolArgumentType.getBool(context, "enabled"), context);})
                    )
                    .executes(context -> {return runGetEnableRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("delete")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runDeleteRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("history")
                .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                    .executes(context -> {return runRuleHistoryCommand(IntegerArgumentType.getInteger(context, "page"), context);})
                )
                .executes(context -> {return runRuleHistoryCommand(0, context);})
            )
            .then(CommandManager.literal("view")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runViewRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("log")
                .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                    .executes(context -> {return runLogRuleCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                )
            )
            .then(CommandManager.literal("trigger")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runTriggerRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("test")
                .then(CommandManager.argument("name", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .executes(context -> {return runTestRuleCommand(StringArgumentType.getString(context, "name"), context);})
                )
            )
            .then(CommandManager.literal("edit")
                .then(CommandManager.argument("rule", StringArgumentType.string())
                    .suggests(CustomRuleSuggestion.suggest(true))
                    .then(CommandManager.literal("event")
                        .then(CommandManager.argument("event", StringArgumentType.string())
                            .suggests(StringSuggestion.suggest(RuleRegistry.getRegisteredEventNames(), true))
                            .executes(context -> {return runEditRuleEventCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "event"), context);})
                        )
                    )
                    .then(CommandManager.literal("condition")
                        .then(buildAddConditionCommand())
                        .then(CommandManager.literal("set")
                            .then(CommandManager.argument("formula", StringArgumentType.greedyString())
                                .executes(context -> {return runEditRuleConditionCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "formula"), context);})
                            )
                        )
                        .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("condition", StringArgumentType.string())
                                .executes(context -> {return runEditRuleConditionRemoveCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "condition"), context);})
                            )
                        )
                        .then(CommandManager.literal("rename")
                            .then(CommandManager.argument("oldName", StringArgumentType.string())
                                .then(CommandManager.argument("newName", StringArgumentType.string())
                                    .executes(context -> {return runEditRuleConditionRenameCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "oldName"), StringArgumentType.getString(context, "newName"), context);})
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("action")
                        .then(buildAddActionCommand(false))
                        .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("action", StringArgumentType.string())
                                .executes(context -> {return runEditRuleActionRemoveCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "action"), context);})
                            )
                        )
                        .then(CommandManager.literal("rename")
                            .then(CommandManager.argument("oldName", StringArgumentType.string())
                                .then(CommandManager.argument("newName", StringArgumentType.string())
                                    .executes(context -> {return runEditRuleActionRenameCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "oldName"), StringArgumentType.getString(context, "newName"), context);})
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("punish")
                        .then(buildAddActionCommand(true))
                        .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("action", StringArgumentType.string())
                                .executes(context -> {return runEditRulePunishRemoveCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "action"), context);})
                            )
                        )
                        .then(CommandManager.literal("rename")
                            .then(CommandManager.argument("oldName", StringArgumentType.string())
                                .then(CommandManager.argument("newName", StringArgumentType.string())
                                    .executes(context -> {return runEditRulePunishRenameCommand(StringArgumentType.getString(context, "rule"), StringArgumentType.getString(context, "oldName"), StringArgumentType.getString(context, "newName"), context);})
                                )
                            )
                        )
                    )
                )
            );
    }
    
}
