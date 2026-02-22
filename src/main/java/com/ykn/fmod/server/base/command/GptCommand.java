/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.LoggerFactory;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.async.GptCommandExecutor;
import com.ykn.fmod.server.base.data.GptData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class GptCommand {

    private static int runGptNewCommand(String text, CommandContext<CommandSourceStack> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            ServerData data = Util.getServerData(context.getSource().getServer());
            GptData gptData = data.getGptData(context.getSource().getTextName());
            GptCommandExecutor gptHelper = new GptCommandExecutor(gptData, context);
            boolean postResult = gptData.newConversation(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.submitAsyncTask(gptHelper);
            // if (context.getSource().getPlayer() != null) {
            //     // Other source would have already logged the message
            //     logger.info("<{}> {}", context.getSource().getDisplayName().getString(), text);
            // }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f gpt new", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptReplyCommand(String text, CommandContext<CommandSourceStack> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            ServerData data = Util.getServerData(context.getSource().getServer());
            GptData gptData = data.getGptData(context.getSource().getTextName());
            GptCommandExecutor gptHelper = new GptCommandExecutor(gptData, context);
            boolean postResult = gptData.reply(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.submitAsyncTask(gptHelper);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f gpt reply", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptRegenerateCommand(CommandContext<CommandSourceStack> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            ServerData data = Util.getServerData(context.getSource().getServer());
            GptData gptData = data.getGptData(context.getSource().getTextName());
            int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.nohistory"));
            }
            String text = gptData.getPostMessages(gptDataLength - 1);
            GptCommandExecutor gptHelper = new GptCommandExecutor(gptData, context);
            boolean postResult = gptData.regenerate(url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.submitAsyncTask(gptHelper);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f gpt regenerate", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptEditCommand(int index, String text, CommandContext<CommandSourceStack> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            ServerData data = Util.getServerData(context.getSource().getServer());
            GptData gptData = data.getGptData(context.getSource().getTextName());
            int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.nohistory"));
            }
            // The Command argument index begins from 1, the source code index begins from 0
            if (index <= 0 || index > gptDataLength) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            GptCommandExecutor gptHelper = new GptCommandExecutor(gptData, context);
            boolean postResult = gptData.editHistory(index - 1, text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.submitAsyncTask(gptHelper);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f gpt edit " + index, e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptHistoryCommand(int index, CommandContext<CommandSourceStack> context) {
        try {
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getTextName());
            final int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.nohistory"));
            }
            if (index == 0) {
                index = gptDataLength;
            }
            if (index < 0 || index > gptDataLength) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            final int finalIndex = index;
            final String postMessage = gptData.getPostMessages(index - 1);
            final String model = gptData.getGptModels(index - 1);
            final Component receivedMessage = gptData.getResponseTexts(index - 1);
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(postMessage)), false);
            context.getSource().sendSuccess(() -> Component.literal("<").append(model.isBlank() ? "GPT" : model).append("> ").append(receivedMessage), false);
            context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.gpt.history", finalIndex, gptDataLength), false);
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f gpt history " + index, e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("gpt")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("new")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {return runGptNewCommand(StringArgumentType.getString(context, "message"), context);})
                ))
            .then(Commands.literal("reply")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> {return runGptReplyCommand(StringArgumentType.getString(context, "message"), context);})
                ))
            .then(Commands.literal("regenerate").executes(context -> {return runGptRegenerateCommand(context);}))
            .then(Commands.literal("edit")
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {return runGptEditCommand(IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "message"), context);})
                    )
                )
            )
            .then(Commands.literal("history")
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                    .executes(context -> {return runGptHistoryCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                )
                .executes(context -> {return runGptHistoryCommand(0, context);})
            );
    }
}
