/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.ServerMessageType;
import com.ykn.fmod.server.base.util.TextPlaceholderFactory;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.node.TriggerNode;
import com.ykn.fmod.server.flow.tool.FlowManager;

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public class CommandRegistrater {

    private static Object devFunction(CommandContext<CommandSourceStack> context) {
        // This function is used for development purposes. Execute command /f dev to run this function.
        // This function should be removed in the final release.
        return null;
    }

    private static int runFModCommand(CommandContext<CommandSourceStack> context) {
        try {
            MutableComponent commandFeedback = Util.parseTranslatableText("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors());
            context.getSource().sendSuccess(() -> commandFeedback, false);
            return Command.SINGLE_SUCCESS;
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.version.error"));
        }
    }

    private static int runDevCommand(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.dev.start"), false);
            Object result = devFunction(context);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.dev.end", result == null ? "null" : result.toString()), false);
        } catch (Exception e) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                // context.getSource().sendSuccess(() -> Component.literal(e.getMessage()), false);
                context.getSource().sendSuccess(() -> Component.literal(sw.toString()), false);
                pw.close();
                sw.close();
            } catch (Exception exception) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f dev", exception);
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.dev.error"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSayCommand(String message, CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            MutableComponent text = null;
            if (player == null) {
                text = Component.literal("[").append(context.getSource().getDisplayName()).append(Component.literal("] ")).append(
                    TextPlaceholderFactory.ofDefault().parse(message, player)
                );
            } else {
                text = Component.literal("<").append(player.getDisplayName()).append(Component.literal("> ")).append(
                    TextPlaceholderFactory.ofDefault().parse(message, player)
                );
            }
            ServerMessageType.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f say", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTriggerFlowCommand(String name, String param, CommandContext<CommandSourceStack> context) {
        try {
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            // Player without permission should not know the status of the trigger, always show not exists
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            if (targetFlow.isEnabled == false) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            if (targetFlow.flow.getFirstNode() == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            if (targetFlow.flow.getFirstNode() instanceof TriggerNode == false) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            List<Object> startNodeOutputs = new ArrayList<>();
            startNodeOutputs.add(context.getSource().getPlayer());
            startNodeOutputs.add(TypeAdaptor.parse(param).autoCast());
            LogicException exception = targetFlow.execute(data, startNodeOutputs, null);
            if (exception != null) {
                throw new CommandRuntimeException(exception.getMessageText());
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f trigger", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runReloadCommand(CommandContext<CommandSourceStack> context) {
        try {
            SongFileSuggestion.suggest();
            FlowFileSuggestion.suggest();
            Util.loadServerConfig();
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.reload.success"), true);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.reload.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static boolean registerCommand(RegisterCommandsEvent event) {
        try {
            final LiteralArgumentBuilder<CommandSourceStack> fModCommandNode = Commands.literal("fminecraftmod")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {return runFModCommand(context);})
                .then(Commands.literal("dev")
                    .requires(source -> source.hasPermission(4))
                    .executes(context -> {return runDevCommand(context);})
                )
                .then(GptCommand.buildCommand())
                .then(SongCommand.buildCommand())
                .then(GetAndShareCommand.buildGetCommand())
                .then(GetAndShareCommand.buildShareCommand())
                .then(Commands.literal("say")
                    .requires(source -> source.hasPermission(0))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .suggests(SayCommandSuggestion.suggestDefault())
                        .executes(context -> {return runSayCommand(StringArgumentType.getString(context, "message"), context);})
                    )
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(4))
                    .executes(context -> {return runReloadCommand(context);})
                )
                .then(Commands.literal("trigger")
                    .requires(source -> source.hasPermission(0))
                    .then(Commands.argument("function", StringArgumentType.string())
                        .suggests(LogicFlowSuggestion.suggestTrigger())
                        .executes(context -> {return runTriggerFlowCommand(StringArgumentType.getString(context, "function"), null, context);})
                        .then(Commands.argument("param", StringArgumentType.greedyString())
                            .executes(context -> {return runTriggerFlowCommand(StringArgumentType.getString(context, "function"), StringArgumentType.getString(context, "param"), context);})
                        )
                    )
                )
                .then(FlowCommand.buildCommand())
                .then(ServerConfigRegistry.buildCommand());
            
            final LiteralCommandNode<CommandSourceStack> commandNode = event.getDispatcher().register(fModCommandNode);
            event.getDispatcher().register(Commands.literal("f")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {return runFModCommand(context);})
                .redirect(commandNode)
            );
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Unable to register command.", e);
            return false;
        }
    }
}
