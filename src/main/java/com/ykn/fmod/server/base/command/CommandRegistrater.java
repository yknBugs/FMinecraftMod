/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

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

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class CommandRegistrater {

    private static Object devFunction(CommandContext<ServerCommandSource> context) {
        // This function is used for development purposes. Execute command /f dev to run this function.
        // This function should be removed in the final release.
        // Add any code you want to test here, and you can return any kinds of value.
        // Exception will be gracefully handled and printed in the command feedback.
        return null;
    }

    private static int runFModCommand(CommandContext<ServerCommandSource> context) {
        try {
            MutableText commandFeedback = Util.parseTranslatableText("fmod.misc.version", Util.getMinecraftVersion(), Util.MOD_VERSION.toString(), Util.getModAuthors());
            context.getSource().sendFeedback(() -> commandFeedback, false);
            return Command.SINGLE_SUCCESS;
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.version.error"));
        }
    }

    private static int runDevCommand(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.dev.start"), false);
            Object result = devFunction(context);
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.dev.end", result == null ? "null" : result.toString()), false);
        } catch (Exception e) {
            try {
                try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                    e.printStackTrace(pw);
                    // context.getSource().sendFeedback(() -> Text.literal(e.getMessage()), false);
                    context.getSource().sendFeedback(() -> Text.literal(sw.toString()), false);
                }
            } catch (Exception exception) {
                Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f dev", exception);
                throw new CommandException(Util.parseTranslatableText("fmod.command.dev.error"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSayCommand(String message, CommandContext<ServerCommandSource> context) {
        try {
            MinecraftServer server = Util.requireNotNullServer(context);
            ServerPlayerEntity player = context.getSource().getPlayer();
            MutableText text = null;
            if (player == null) {
                text = Text.literal("[").append(context.getSource().getName()).append(Text.literal("] ")).append(
                    TextPlaceholderFactory.ofDefault().parse(message, player)
                );
            } else {
                text = Text.literal("<").append(player.getDisplayName()).append(Text.literal("> ")).append(
                    TextPlaceholderFactory.ofDefault().parse(message, player)
                );
            }
            ServerMessageType.broadcastTextMessage(server, text);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f say", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runTriggerFlowCommand(String name, String param, CommandContext<ServerCommandSource> context) {
        try {
            ServerData data = Util.getServerData(Util.requireNotNullServer(context));
            FlowManager targetFlow = data.getLogicFlows().get(name);
            // Player without permission should not know the status of the trigger, always show not exists
            if (targetFlow == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            if (!targetFlow.isEnabled()) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            if (targetFlow.getFlow().getFirstNode() == null) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            if (!(targetFlow.getFlow().getFirstNode() instanceof TriggerNode)) {
                throw new CommandException(Util.parseTranslatableText("fmod.command.trigger.notexists", name));
            }
            List<Object> startNodeOutputs = new ArrayList<>();
            startNodeOutputs.add(context.getSource().getPlayer());
            startNodeOutputs.add(TypeAdaptor.parse(param).autoCast());
            LogicException exception = targetFlow.execute(data, startNodeOutputs, null);
            if (exception != null) {
                throw new CommandException(exception.getMessageText());
            }
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f trigger", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runReloadCommand(CommandContext<ServerCommandSource> context) {
        try {
            SongFileSuggestion.suggest();
            FlowFileSuggestion.suggest();
            Util.loadServerConfig();
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.command.reload.success"), true);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Caught unexpected exception when executing command /f reload", e);
            throw new CommandException(Util.parseTranslatableText("fmod.command.reload.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static boolean registerCommand() {
        try {
            final LiteralArgumentBuilder<ServerCommandSource> fModCommandNode = CommandManager.literal("fminecraftmod")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(context -> {return runFModCommand(context);})
                .then(CommandManager.literal("dev")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {return runDevCommand(context);})
                )
                .then(GptCommand.buildCommand())
                .then(SongCommand.buildCommand())
                .then(GetAndShareCommand.buildGetCommand())
                .then(GetAndShareCommand.buildShareCommand())
                .then(CommandManager.literal("say")
                    .requires(source -> source.hasPermissionLevel(0))
                    .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .suggests(SayCommandSuggestion.suggestDefault())
                        .executes(context -> {return runSayCommand(StringArgumentType.getString(context, "message"), context);})
                    )
                )
                .then(CommandManager.literal("reload")
                    .requires(source -> source.hasPermissionLevel(4))
                    .executes(context -> {return runReloadCommand(context);})
                )
                .then(CommandManager.literal("trigger")
                    .requires(source -> source.hasPermissionLevel(0))
                    .then(CommandManager.argument("function", StringArgumentType.string())
                        .suggests(LogicFlowSuggestion.suggestTrigger())
                        .executes(context -> {return runTriggerFlowCommand(StringArgumentType.getString(context, "function"), null, context);})
                        .then(CommandManager.argument("param", StringArgumentType.greedyString())
                            .executes(context -> {return runTriggerFlowCommand(StringArgumentType.getString(context, "function"), StringArgumentType.getString(context, "param"), context);})
                        )
                    )
                )
                .then(FlowCommand.buildCommand())
                .then(ServerConfigRegistry.buildCommand());

            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                final LiteralCommandNode<ServerCommandSource> commandNode = dispatcher.register(fModCommandNode);
                dispatcher.register(CommandManager.literal("f")
                    .requires(source -> source.hasPermissionLevel(0))
                    .executes(context -> {return runFModCommand(context);})
                    .redirect(commandNode)
                );
            });
            return true;
        } catch (Exception e) {
            Util.LOGGER.error("FMinecraftMod: Unable to register command.", e);
            return false;
        }
    }
}
