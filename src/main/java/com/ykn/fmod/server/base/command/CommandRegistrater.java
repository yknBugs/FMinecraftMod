/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.slf4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.logging.LogUtils;
import com.ykn.fmod.server.base.data.GptData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.schedule.PlaySong;
import com.ykn.fmod.server.base.song.NbsSongDecoder;
import com.ykn.fmod.server.base.song.NoteBlockSong;
import com.ykn.fmod.server.base.util.EnumI18n;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.GptHelper;
import com.ykn.fmod.server.base.util.MarkdownToTextConverter;
import com.ykn.fmod.server.base.util.MessageReceiver;
import com.ykn.fmod.server.base.util.TextPlaceholderFactory;
import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicFlow;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.flow.tool.FlowSerializer;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.loading.FMLPaths;

public class CommandRegistrater {

    private static Logger logger = LogUtils.getLogger();

    private static Object devFunction(CommandContext<CommandSourceStack> context) {
        // This function is used for development purposes. Execute command /f dev to run this function.
        // This function should be removed in the final release.

        // Markdown Highlight Test
        String markdownTest = "C++ Syntax Highlight Test\n" + 
        "```cpp\n" +
        "#include <string>\n" +
        "#include <vector>\n" +
        "#include <algorithm>\n\n" +
        "int main() {\n" +
        "    std::vector<std::string> s;\n" + 
        "    std::sort(s.begin(), s.end(), [&s](auto& a, auto& b) {\n" + 
        "        a = s;\n" +
        "    });\n" +
        "}\n" +
        "```\n" +
        "End of Test";

        context.getSource().sendSuccess(() -> MarkdownToTextConverter.parseMarkdownToText(markdownTest), false);

        ServerData serverData = Util.getServerData(context.getSource().getServer());
        if (serverData.logicFlows.size() == 0) {
            // Logic Flow Test
            FlowManager flowManager = new FlowManager("Test Flow", "SetVariableNode", "Save 10 to var x");
            flowManager.setConstInput("Save 10 to var x", 0, "x");
            flowManager.setConstInput("Save 10 to var x", 1, 10);
            
            flowManager.createNode("GetVariableNode", "Get var x");
            flowManager.setConstInput("Get var x", 0, "x");
            flowManager.setNextNode("Save 10 to var x", 0, "Get var x");

            flowManager.createNode("BinaryArithmeticNode", "Calculate x + 1");
            flowManager.setReferenceInput("Calculate x + 1", 0, "Get var x", 0);
            flowManager.setConstInput("Calculate x + 1", 1, 1);
            flowManager.setConstInput("Calculate x + 1", 2, "+");
            flowManager.setNextNode("Get var x", 0, "Calculate x + 1");

            flowManager.createNode("SetVariableNode", "Store calculate result to x");
            flowManager.setConstInput("Store calculate result to x", 0, "x");
            flowManager.setReferenceInput("Store calculate result to x", 1, "Calculate x + 1", 0);
            flowManager.setNextNode("Calculate x + 1", 0, "Store calculate result to x");

            flowManager.createNode("GetVariableNode", "Get var x again");
            flowManager.setNextNode("Store calculate result to x", 0, "Get var x again");

            flowManager.setConstInput("Get var x again", 0, "x");
            flowManager.createNode("BinaryArithmeticNode", "Calculate x + 1 again");
            flowManager.setReferenceInput("Calculate x + 1 again", 0, "Get var x again", 0);
            flowManager.setConstInput("Calculate x + 1 again", 1, 1);
            flowManager.setConstInput("Calculate x + 1 again", 2, "+");
            flowManager.setNextNode("Get var x again", 0, "Calculate x + 1 again");

            flowManager.createNode("BroadcastMessageNode", "Send calculated result to all players");
            flowManager.setConstInput("Send calculated result to all players", 0, "chat");
            flowManager.setReferenceInput("Send calculated result to all players", 1, "Calculate x + 1 again", 0);
            flowManager.setNextNode("Calculate x + 1 again", 0, "Send calculated result to all players");

            flowManager.createNode("BroadcastMessageNode", "Send get x result to all players");
            flowManager.setConstInput("Send get x result to all players", 0, "chat");
            flowManager.setReferenceInput("Send get x result to all players", 1, "Get var x again", 0);
            flowManager.setNextNode("Send calculated result to all players", 0, "Send get x result to all players");
            serverData.logicFlows.put(flowManager.flow.name, flowManager);
            context.getSource().sendSuccess(() -> Component.literal("Saved the example logic flow"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("Logic flow already exists, not saving"), false);
        }

        return null;
    }

    private static int runFModCommand(CommandContext<CommandSourceStack> context) {
        try {
            MutableComponent commandFeedback = Util.parseTranslateableText("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors());
            context.getSource().sendSuccess(() -> commandFeedback, false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.version.error"));
        }
    }

    private static int runDevCommand(CommandContext<CommandSourceStack> context) {
        try {
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.dev.start"), false);
            Object result = devFunction(context);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.dev.end", result == null ? "null" : result.toString()), false);
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
                logger.error("FMinecraftMod: Caught unexpected exception when executing command /f dev", exception);
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.dev.error"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptNewCommand(String text, CommandContext<CommandSourceStack> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            ServerData data = Util.getServerData(context.getSource().getServer());
            GptData gptData = data.getGptData(context.getSource().getTextName());
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.newConversation(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.globalRequestPool.submit(gptHelper);
            // if (context.getSource().getPlayer() != null) {
            //     // Other source would have already logged the message
            //     logger.info("<{}> {}", context.getSource().getDisplayName().getString(), text);
            // }
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f gpt new", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptReplyCommand(String text, CommandContext<CommandSourceStack> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            ServerData data = Util.getServerData(context.getSource().getServer());
            GptData gptData = data.getGptData(context.getSource().getTextName());
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.reply(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.globalRequestPool.submit(gptHelper);
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f gpt reply", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
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
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            String text = gptData.getPostMessages(gptDataLength - 1);
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.regenerate(url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.globalRequestPool.submit(gptHelper);

        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f gpt regenerate", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
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
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            // The Command argument index begins from 1, the source code index begins from 0
            if (index <= 0 || index > gptDataLength) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.editHistory(index - 1, text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(text)), true);
            data.globalRequestPool.submit(gptHelper);
        } catch (URISyntaxException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f gpt edit " + index, e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGptHistoryCommand(int index, CommandContext<CommandSourceStack> context) {
        try {
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getTextName());
            final int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            if (index == 0) {
                index = gptDataLength;
            }
            if (index < 0 || index > gptDataLength) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            final int finalIndex = index;
            final String postMessage = gptData.getPostMessages(index - 1);
            final String model = gptData.getGptModels(index - 1);
            final Component receivedMessage = gptData.getResponseTexts(index - 1);
            context.getSource().sendSuccess(() -> Component.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Component.literal(postMessage)), false);
            context.getSource().sendSuccess(() -> Component.literal("<").append(model.isBlank() ? "GPT" : model).append("> ").append(receivedMessage), false);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.gpt.history", finalIndex, gptDataLength), false);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f gpt history " + index, e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSongPlayCommand(Collection<ServerPlayer> players, String songName, CommandContext<CommandSourceStack> context) {
        try {
            // Refresh song suggestion list
            SongFileSuggestion.suggest();
            if (SongFileSuggestion.getAvailableSongs() == 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.hint"), false);
            }
            Path songFolder = FMLPaths.CONFIGDIR.get().resolve(Util.MODID);
            Path songPath = songFolder.resolve(songName).normalize();
            if (!songPath.startsWith(songFolder)) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.song.filenotfound", songName));
            }

            // Load song
            NoteBlockSong song = null;
            try (FileInputStream fileInputStream = new FileInputStream(songPath.toFile())) {
                song = NbsSongDecoder.parse(fileInputStream);
            }
            if (song == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.song.ioexception", songName));
            }
            // Check if a song is still playing, if so, cancel the task
            for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                if (scheduledTask instanceof PlaySong) {
                    PlaySong playSong = (PlaySong) scheduledTask;
                    // if (players.contains(playSong.getTarget())) {
                    //     // The Entity class overrides the equals method using network id instead of uuid, which will change after reloading
                    //     // So, this method is not reliable
                    //     playSong.cancel();
                    // }
                    for (ServerPlayer player : players) {
                        if (playSong.getTarget().getUUID() == player.getUUID()) {
                            playSong.cancel();
                        }
                    }
                }
            }
            // Submit song task
            for (ServerPlayer player : players) {
                PlaySong playSong = new PlaySong(song, songName, player, context);
                Util.getServerData(context.getSource().getServer()).submitScheduledTask(playSong);
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.start", player.getDisplayName(), songName), true);
            }
        } catch (FileNotFoundException fileNotFoundException) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.song.filenotfound", songName));
        } catch (EOFException eofException) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.song.eofexception", songName));
        } catch (IOException ioException) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.song.ioexception", songName));
        } catch (Exception exception) {
            if (exception instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) exception;
            }
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.song.error", songName));
        }
        return players.size();
    }

    /**
     * Executes a task for a collection of players based on their scheduled tasks or a default task.
     *
     * @param players    The collection of players to process.
     * @param context    The command context providing the server command source.
     * @param taskToDo   A BiPredicate representing the task to perform if a matching scheduled task is found.
     *                   The first parameter is the player, and the second parameter is the matching PlaySong task.
     *                   If null, no task will be performed for matching scheduled tasks.
     * @param defaultTask A Predicate representing the default task to perform if no matching scheduled task is found.
     *                    The parameter is the player. If null, no default task will be performed.
     * @return The number of successful task executions.
     */
    private static int doSongTaskOrDefault(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context, BiPredicate<ServerPlayer, PlaySong> taskToDo, Predicate<ServerPlayer> defaultTask) {
        SongFileSuggestion.suggest();
        int result = 0;
        for (ServerPlayer player : players) {
            boolean isFound = false;
            for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                if (scheduledTask instanceof PlaySong) {
                    PlaySong playSong = (PlaySong) scheduledTask;
                    if (playSong.getTarget().getUUID() == player.getUUID()) {
                        isFound = true;
                        boolean isSuccess = true;
                        if (taskToDo != null) {
                            isSuccess = taskToDo.test(player, playSong);
                        }
                        if (isSuccess) {
                            result++;
                        }
                    }
                }
            }
            if (isFound == false) {
                boolean isSuccess = false;
                if (defaultTask != null) {
                    isSuccess = defaultTask.test(player);
                }
                if (isSuccess) {
                    result++;
                }
            }
        }
        return result;
    }

    private static int runSongCancelCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                playSong.cancel();
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f song cancel", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongGetCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                String currentTimeStr = String.format("%.1f", playSong.getSong().getVirtualTick(playSong.getTick()) / 20.0);
                String totalTimeStr = String.format("%.1f", playSong.getSong().getMaxVirtualTick() / 20.0);
                String speedStr = String.format("%.2f", playSong.getSong().getSpeed());
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.get", player.getDisplayName(), playSong.getSongName(), currentTimeStr, totalTimeStr, speedStr), false);
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f song get", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongShowInfoCommand(Collection<ServerPlayer> players, boolean showInfo, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                playSong.setShowInfo(showInfo);
                if (showInfo) {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.show", player.getDisplayName(), playSong.getSongName()), true);
                } else {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.hide", player.getDisplayName(), playSong.getSongName()), true);
                }
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f song showinfo", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongShowInfoCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                MutableComponent isShowInfo = EnumI18n.getBooleanValueI18n(playSong.isShowInfo());
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.status", player.getDisplayName(), playSong.getSongName(), isShowInfo), false);
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f song showinfo", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongSeekCommand(Collection<ServerPlayer> players, double timepoint, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                String songName = playSong.getSongName();
                double songLength = playSong.getSong().getMaxVirtualTick() / 20.0;
                String songLengthStr = String.format("%.1f", songLength);
                String timepointStr = String.format("%.1f", timepoint);
                if (timepoint < 0 || timepoint > songLength) {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.long", player.getDisplayName(), songName, songLengthStr, timepointStr), false);
                } else {
                    playSong.seek((int) (timepoint * 20));
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.search", player.getDisplayName(), songName, timepointStr, songLengthStr), true);
                    return true;
                }
                return false;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f song search", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongSpeedCommand(Collection<ServerPlayer> players, double speed, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                Component playerName = player.getDisplayName();
                String songName = playSong.getSongName();
                String speedStr = String.format("%.2f", speed);
                playSong.changeSpeed(speed);
                if (speed == 0) {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.pause", playerName, songName), true);
                } else {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.speed", playerName, songName, speedStr), true);
                }
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f song speed", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static ServerPlayer getShareCommandExecutor(CommandContext<CommandSourceStack> context) {
        if (context == null) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.playeronly"));
        }
        CommandSourceStack source = context.getSource();
        if (source == null) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.playeronly"));
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.playeronly"));
        }
        return player;
        // return Optional.ofNullable(context)
        //     .map(CommandContext::getSource)
        //     .map(CommandSourceStack::getPlayer)
        //     .orElseThrow(() -> new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.playeronly")));
    }

    private static int runGetCoordCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        try {
            for (Entity entity : entities) {
                Component name = entity.getDisplayName();
                MutableComponent biome = Util.getBiomeText(entity);
                String strDim = entity.level().dimension().location().toString();
                String strX = String.format("%.2f", entity.getX());
                String strY = String.format("%.2f", entity.getY());
                String strZ = String.format("%.2f", entity.getZ());
                String strPitch = String.format("%.2f", entity.getXRot());
                String strYaw = String.format("%.2f", entity.getYRot());
                MutableComponent text = Util.parseTranslateableText("fmod.command.get.coord", name, biome, strX, strY, strZ).withStyle(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + strDim + " run tp @s " + strX + " " + strY + " " + strZ + " " + strYaw + " " + strPitch)
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
                ));
                context.getSource().sendSuccess(() -> text, false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f get coord", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private static int runShareCoordCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            Component name = player.getDisplayName();
            MutableComponent biome = Util.getBiomeText(player);
            String strDim = player.level().dimension().location().toString();
            String strX = String.format("%.2f", player.getX());
            String strY = String.format("%.2f", player.getY());
            String strZ = String.format("%.2f", player.getZ());
            String strPitch = String.format("%.2f", player.getXRot());
            String strYaw = String.format("%.2f", player.getYRot());
            Component text = Util.parseTranslateableText("fmod.command.share.coord", name, biome, strX, strY, strZ).withStyle(style -> style.withClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + strDim + " run tp @s " + strX + " " + strY + " " + strZ + " " + strYaw + " " + strPitch)
            ).withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
            ));
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f share coord", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent getDirectionText(Vec3 source, Vec3 target) {
        double pitch = GameMath.getPitch(source, target);
        double yaw = GameMath.getYaw(source, target);
        MutableComponent direction = Component.empty();
        if (pitch > 60.0) {
            direction = Util.parseTranslateableText("fmod.misc.diru");
        } else if (pitch < -60.0) {
            direction = Util.parseTranslateableText("fmod.misc.dird");
        } else if (yaw > 22.5 && yaw < 67.5) {
            direction = Util.parseTranslateableText("fmod.misc.dirnw");
        } else if (yaw >= 67.5 && yaw <= 112.5) {
            direction = Util.parseTranslateableText("fmod.misc.dirw");
        } else if (yaw > 112.5 && yaw < 157.5) {
            direction = Util.parseTranslateableText("fmod.misc.dirsw");
        } else if (yaw >= 157.5 && yaw <= 202.5) {
            direction = Util.parseTranslateableText("fmod.misc.dirs");
        } else if (yaw > 202.5 && yaw < 247.5) {
            direction = Util.parseTranslateableText("fmod.misc.dirse");
        } else if (yaw >= 247.5 && yaw <= 292.5) {
            direction = Util.parseTranslateableText("fmod.misc.dire");
        } else if (yaw > 292.5 && yaw < 337.5) {
            direction = Util.parseTranslateableText("fmod.misc.dirne");
        } else {
            direction = Util.parseTranslateableText("fmod.misc.dirn");
        }
        return direction;
    }

    private static int runGetDistanceCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            Vec3 source = context.getSource().getPosition();
            for (Entity entity : entities) {
                if (context.getSource().getLevel() != entity.level()) {
                    final Component name = entity.getDisplayName();
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.dimdistance", name), false);
                    continue;
                }
                Vec3 target = entity.position();
                double distance = GameMath.getEuclideanDistance(source, target);
                double pitch = GameMath.getPitch(source, target);
                double yaw = GameMath.getYaw(source, target);
                double degree = yaw;
                if (pitch > 60.0) {
                    degree = pitch;
                } else if (pitch < -60.0) {
                    degree = -pitch;
                }
                if (degree > 180.0) {
                    degree -= 360.0;
                }
                final Component name = entity.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableComponent dirTxt = getDirectionText(source, target);
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.distance", name, dirTxt, degStr, distStr), false);
                result++;
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f get distance", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runShareDistanceCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            Vec3 target = player.position();
            List<ServerPlayer> onlinePlayers = Util.getOnlinePlayers(context.getSource().getServer());
            for (ServerPlayer onlinePlayer : onlinePlayers) {
                if (onlinePlayer.getUUID() == player.getUUID()) {
                    Util.sendTextMessage(onlinePlayer, Util.parseTranslateableText("fmod.command.share.selfdistance"));
                    continue;
                }
                if (player.level() != onlinePlayer.level()) {
                    final Component name = player.getDisplayName();
                    Util.sendTextMessage(onlinePlayer, Util.parseTranslateableText("fmod.command.share.dimdistance", name));
                    continue;
                }
                Vec3 source = onlinePlayer.position();
                double distance = GameMath.getEuclideanDistance(source, target);
                double pitch = GameMath.getPitch(source, target);
                double yaw = GameMath.getYaw(source, target);
                double degree = yaw;
                if (pitch > 60.0) {
                    degree = pitch;
                } else if (pitch < -60.0) {
                    degree = -pitch;
                }
                if (degree > 180.0) {
                    degree -= 360.0;
                }
                final Component name = player.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableComponent dirTxt = getDirectionText(source, target);
                final MutableComponent text = Util.parseTranslateableText("fmod.command.share.distance", name, dirTxt, degStr, distStr);
                Util.sendTextMessage(onlinePlayer, text);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f share distance", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetHealthCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        try {
            for (Entity entity : entities) {
                final Component name = entity.getDisplayName();
                double hp = Util.getHealth(entity);
                double maxhp = Util.getMaxHealth(entity);
                final String hpStr = String.format("%.2f", hp);
                final String maxhpStr = String.format("%.2f", maxhp);
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.health", name, hpStr, maxhpStr), false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f get health", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private static int runShareHealthCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            double hp = Util.getHealth(player);
            double maxhp = Util.getMaxHealth(player);
            final Component name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String maxhpStr = String.format("%.2f", maxhp);
            MutableComponent text = Util.parseTranslateableText("fmod.command.share.health", name, hpStr, maxhpStr);
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f share health", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetStatusCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        try {
            for (ServerPlayer player : players) {
                double hp = player.getHealth();
                int hunger = player.getFoodData().getFoodLevel();
                double saturation = player.getFoodData().getSaturationLevel();
                int level = player.experienceLevel;
                final Component name = player.getDisplayName();
                final String hpStr = String.format("%.2f", hp);
                final String hungerStr = String.valueOf(hunger);
                final String saturationStr = String.format("%.2f", saturation);
                final String levelStr = String.valueOf(level);
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.status", name, hpStr, hungerStr, saturationStr, levelStr), false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f get status", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private static int runShareStatusCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            double hp = player.getHealth();
            int hunger = player.getFoodData().getFoodLevel();
            double saturation = player.getFoodData().getSaturationLevel();
            int level = player.experienceLevel;
            final Component name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String hungerStr = String.valueOf(hunger);
            final String saturationStr = String.format("%.2f", saturation);
            final String levelStr = String.valueOf(level);
            MutableComponent text = Util.parseTranslateableText("fmod.command.share.status", name, hpStr, hungerStr, saturationStr, levelStr);
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f share status", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent formatInventoryItemStack(ItemStack item) {
        MutableComponent itemText = Component.empty();
        try {
            if (item == null || item.isEmpty()) {
                itemText = Component.literal("00").withStyle(ChatFormatting.GRAY).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.command.get.emptyslot")))
                );
            } else if (item.getCount() < 100) {
                String itemCount = String.format("%02d", item.getCount());
                itemText = Component.literal(itemCount).withStyle(ChatFormatting.AQUA).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item)))
                );
            } else {
                itemText = Component.literal("9+").withStyle(ChatFormatting.AQUA).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(item)))
                );
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when formatting item stack", e);
            itemText = Component.literal("??").withStyle(ChatFormatting.RED);
        }
        return itemText;
    }

    private static List<MutableComponent> getInventoryTexts(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        // Text Structure:
        // [x] [x] [x] [x] [-] [-] [S] [+] [1]   (x: Armor, -: Placeholder, +: Offhand, 1: Current Chosen Slot Index)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, S: Survival Gamemode [S: Survival, C: Creative, A: Adventure, V: Spectator])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '+' symbol formatting: [Has Item: Formatting.AQUA, Empty Slot: Formatting.GRAY])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '[]' bracket formmating: Formatting.GREEN)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Hotbar, '[]' bracket formmating: [Selected: Formatting.GOLD, Other: Formatting.LIGHT_PURPLE])
        MutableComponent armorText = Component.empty();
        for (int i = 0; i < 4; i++) {
            ItemStack item = inventory.getArmor(i);
            armorText.append(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE));
            armorText.append(formatInventoryItemStack(item));
            armorText.append(Component.literal("]").withStyle(ChatFormatting.LIGHT_PURPLE));
            armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        }
        // Placeholder
        for (int i = 0; i < 2; i++) {
            armorText.append(Component.literal("[--]").withStyle(ChatFormatting.GRAY));
            armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        }
        // Gamemode
        armorText.append(Component.literal("[").withStyle(ChatFormatting.GOLD));
        GameType gamemode = player.gameMode.getGameModeForPlayer();
        MutableComponent gamemodeText = Component.literal("+S");
        if (gamemode == GameType.CREATIVE) {
            gamemodeText = Component.literal("+C");
        } else if (gamemode == GameType.ADVENTURE) {
            gamemodeText = Component.literal("+A");
        } else if (gamemode == GameType.SPECTATOR) {
            gamemodeText = Component.literal("+V");
        }
        gamemodeText = gamemodeText.withStyle(ChatFormatting.RED).withStyle(s -> s
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("gameMode." + gamemode.getName())))
        );
        armorText.append(gamemodeText);
        armorText.append(Component.literal("]").withStyle(ChatFormatting.GOLD));
        armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        // Offhand
        ItemStack offhandItem = inventory.getItem(Inventory.SLOT_OFFHAND);
        armorText.append(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE));
        armorText.append(formatInventoryItemStack(offhandItem));
        armorText.append(Component.literal("]").withStyle(ChatFormatting.LIGHT_PURPLE));
        armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        // Selected Slot
        armorText.append(Component.literal("[").withStyle(ChatFormatting.GOLD));
        armorText.append(Component.literal("0" + String.valueOf(inventory.selected + 1)).withStyle(ChatFormatting.RED));
        armorText.append(Component.literal("]").withStyle(ChatFormatting.GOLD));
        armorText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        // Inventory
        MutableComponent[] inventoryText = {Component.empty(), Component.empty(), Component.empty()};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                // Index 0 ~ 8 belongs to Hotbar, Index 9 ~ 35 belongs to Inventory
                int index = (i + 1) * 9 + j;
                ItemStack item = inventory.getItem(index);
                inventoryText[i].append(Component.literal("[").withStyle(ChatFormatting.GREEN));
                inventoryText[i].append(formatInventoryItemStack(item));
                inventoryText[i].append(Component.literal("]").withStyle(ChatFormatting.GREEN));
                inventoryText[i].append(Component.literal(" ").withStyle(ChatFormatting.RESET));
            }
        }
        // Hotbar
        MutableComponent hotbarText = Component.empty();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (i == inventory.selected) {
                hotbarText.append(Component.literal("[").withStyle(ChatFormatting.GOLD));
            } else {
                hotbarText.append(Component.literal("[").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            hotbarText.append(formatInventoryItemStack(item));
            if (i == inventory.selected) {
                hotbarText.append(Component.literal("]").withStyle(ChatFormatting.GOLD));
            } else {
                hotbarText.append(Component.literal("]").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            hotbarText.append(Component.literal(" ").withStyle(ChatFormatting.RESET));
        }
        // Feedback
        final MutableComponent linea = armorText;
        final MutableComponent lineb = inventoryText[0];
        final MutableComponent linec = inventoryText[1];
        final MutableComponent lined = inventoryText[2];
        final MutableComponent linee = hotbarText;
        return Arrays.asList(linea, lineb, linec, lined, linee);
    }

    private static int runGetInventoryCommand(ServerPlayer player, CommandContext<CommandSourceStack> context) {
        try {
            List<MutableComponent> inventoryText = getInventoryTexts(player);
            final Component name = player.getDisplayName();
            final Component title = Util.parseTranslateableText("fmod.command.get.inventory", name);
            context.getSource().sendSuccess(() -> title, false);
            for (MutableComponent text : inventoryText) {
                context.getSource().sendSuccess(() -> text, false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f get inventory", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runShareInventoryCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            List<MutableComponent> inventoryText = getInventoryTexts(player);
            final Component name = player.getDisplayName();
            final Component title = Util.parseTranslateableText("fmod.command.share.inventory", name);
            Util.broadcastTextMessage(context.getSource().getServer(), title);
            for (MutableComponent text : inventoryText) {
                Util.broadcastTextMessage(context.getSource().getServer(), text);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f share inventory", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetItemCommand(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            for (Entity entity : entities) {
                Iterable<ItemStack> items = entity.getHandSlots();
                if (items == null || items.iterator().hasNext() == false) {
                    final Component name = entity.getDisplayName();
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.noitem", name), false);
                    continue;
                }
                MutableComponent itemList = Component.empty();
                int itemCountSum = 0;
                for (ItemStack item : items) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    Component itemText = item.getDisplayName();
                    int itemCount = item.getCount();
                    result += itemCount;
                    itemCountSum += itemCount;
                    itemList.append(itemText);
                    if (itemCount > 1) {
                        itemList.append(Component.literal("x" + itemCount + " "));
                    } else {
                        itemList.append(Component.literal(" "));
                    }
                }
                final Component name = entity.getDisplayName();
                final MutableComponent itemTxt = itemList;
                if (itemCountSum <= 0) {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.noitem", name), false);
                } else {
                    context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.get.item", name).append(itemTxt), false);
                }
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f get item", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runShareItemCommand(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = getShareCommandExecutor(context);
            Iterable<ItemStack> items = player.getHandSlots();
            if (items == null || items.iterator().hasNext() == false) {
                final Component name = player.getDisplayName();
                Util.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslateableText("fmod.command.share.noitem", name));
                return Command.SINGLE_SUCCESS;
            }
            MutableComponent itemList = Component.empty();
            int itemCountSum = 0;
            for (ItemStack item : items) {
                if (item.isEmpty()) {
                    continue;
                }
                Component itemText = item.getDisplayName();
                int itemCount = item.getCount();
                itemCountSum += itemCount;
                itemList.append(itemText);
                if (itemCount > 1) {
                    itemList.append(Component.literal("x" + itemCount + " "));
                } else {
                    itemList.append(Component.literal(" "));
                }
            }
            final Component name = player.getDisplayName();
            final MutableComponent itemTxt = itemList;
            if (itemCountSum <= 0) {
                Util.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslateableText("fmod.command.share.noitem", name));
            } else {
                Util.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslateableText("fmod.command.share.item", name).append(itemTxt));
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f share item", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.share.error"));
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
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f say", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runCreateFlowCommand(String name, String eventNode, String eventNodeName, CommandContext<CommandSourceStack> context) {
        try {
            ServerData data = Util.getServerData(context.getSource().getServer());
            if (data.logicFlows.get(name) != null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.exists", name));
            }
            Collection<String> validEventNodes = NodeRegistry.getEventNodeList();
            if (!validEventNodes.contains(eventNode)) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.event.unknown", eventNode));
            }
            FlowManager flowManager = new FlowManager(name, eventNode, eventNodeName);
            data.logicFlows.put(name, flowManager);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.create.success", eventNode, name), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow create", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runCopyFlowCommand(String sourceName, String targetName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager sourceFlow = data.logicFlows.get(sourceName);
            if (sourceFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", sourceName));
            }
            FlowManager targetFlow = data.logicFlows.get(targetName);
            if (targetFlow != null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.exists", targetName));
            }
            FlowManager copiedFlow = new FlowManager(sourceFlow.flow.copy());
            copiedFlow.flow.name = targetName;
            data.logicFlows.put(targetName, copiedFlow);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.copy.success", sourceName, targetName), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow copy", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLoadFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            if (FlowFileSuggestion.getAvailableFlows() == 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.hint"), false);
            }
            Path flowFolder = FMLPaths.CONFIGDIR.get().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(context.getSource().getServer());
            if ("*".equals(name)) {
                // Load all flow files
                int loadedCount = 0;
                for (String flowFileName : FlowFileSuggestion.cachedFlowList) {
                    Path flowPath = flowFolder.resolve(flowFileName).normalize();
                    if (!flowPath.startsWith(flowFolder)) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.load.filenotfound", flowFileName), false);
                        continue;
                    }
                    LogicFlow flow = FlowSerializer.loadFile(flowPath);
                    if (flow == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.load.ioexception", flowFileName), false);
                        continue;
                    }
                    if (data.logicFlows.get(flow.name) != null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.exists", flow.name), false);
                        continue;
                    }
                    FlowManager flowManager = new FlowManager(flow);
                    data.logicFlows.put(flow.name, flowManager);
                    loadedCount++;
                }
                int loadedCountFinal = loadedCount;
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.load.all", String.valueOf(loadedCountFinal)), true);
                return loadedCountFinal;
            }
            Path flowPath = flowFolder.resolve(name).normalize();
            if (!flowPath.startsWith(flowFolder)) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.load.filenotfound", name));
            }
            LogicFlow flow = FlowSerializer.loadFile(flowPath);
            if (flow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.load.ioexception", name));
            }
            if (data.logicFlows.get(flow.name) != null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.exists", flow.name));
            }
            FlowManager flowManager = new FlowManager(flow);
            data.logicFlows.put(flow.name, flowManager);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.load.success", flow.name), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow load", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSaveFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            Path flowFolder = FMLPaths.CONFIGDIR.get().resolve(Util.MODID).normalize();
            ServerData data = Util.getServerData(context.getSource().getServer());
            if ("*".equals(name)) {
                // Save all flows
                int savedCount = 0;
                for (FlowManager flowManager : data.logicFlows.values()) {
                    Path flowPath = flowFolder.resolve(flowManager.flow.name + ".flow").normalize();
                    if (!flowPath.startsWith(flowFolder)) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.save.notavailable", flowManager.flow.name), false);
                        continue;
                    }
                    boolean success = FlowSerializer.saveFile(flowManager.flow, flowPath, true);
                    if (success) {
                        savedCount++;
                    } else {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.save.ioexception", flowManager.flow.name), false);
                    }
                }
                int savedCountFinal = savedCount;
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.save.all", String.valueOf(savedCountFinal)), true);
                FlowFileSuggestion.suggest();
                return savedCountFinal;
            }
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", name));
            }
            Path flowPath = flowFolder.resolve(targetFlow.flow.name + ".flow").normalize();
            if (!flowPath.startsWith(flowFolder)) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.save.notavailable", targetFlow.flow.name));
            }
            boolean success = FlowSerializer.saveFile(targetFlow.flow, flowPath, true);
            if (!success) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.save.ioexception", targetFlow.flow.name));
            }
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.save.success", targetFlow.flow.name), true);
            FlowFileSuggestion.suggest();
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow save", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runListFlowCommand(CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            if (data.logicFlows.isEmpty()) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.list.empty"), false);
                return Command.SINGLE_SUCCESS;
            }
            List<MutableComponent> flowLines = new ArrayList<>();
            int enabledCount = 0;
            int totalCount = 0;
            for (FlowManager flowManager : data.logicFlows.values()) {
                MutableComponent line = null;
                String numNodesStr = String.valueOf(flowManager.flow.getNodes().size());
                FlowNode startNode = flowManager.flow.getFirstNode();
                if (startNode == null) {
                    continue;
                }
                String startNodeStr = startNode.name;
                if (flowManager.isEnabled) {
                    line = Util.parseTranslateableText("fmod.command.flow.list.enabled", flowManager.flow.name, numNodesStr, startNodeStr).withStyle(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clickview")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow view \"" + flowManager.flow.name + "\""))
                    );
                    enabledCount++;
                } else {
                    line = Util.parseTranslateableText("fmod.command.flow.list.disabled", flowManager.flow.name, numNodesStr, startNodeStr).withStyle(s -> s
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clickview")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow view \"" + flowManager.flow.name + "\""))
                    );
                }
                totalCount++;
                flowLines.add(line);
            }
            String enabledCountStr = String.valueOf(enabledCount);
            String totalCountStr = String.valueOf(totalCount);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.list.title", totalCountStr, enabledCountStr), false);
            for (MutableComponent line : flowLines) {
                context.getSource().sendSuccess(() -> line, false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow list", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runRenameFlowCommand(String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(oldName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", oldName));
            }
            if (data.logicFlows.get(newName) != null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.exists", newName));
            }
            data.logicFlows.remove(oldName);
            targetFlow.flow.name = newName;
            data.logicFlows.put(newName, targetFlow);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.rename.success", oldName, newName), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow rename", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runGetEnableFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", name));
            }
            if (targetFlow.isEnabled) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.enable.get.true", name), false);
            } else {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.enable.get.false", name), false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow enable", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runSetEnableFlowCommand(String name, boolean enable, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", name));
            }
            targetFlow.isEnabled = enable;
            if (enable) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.enable.set.true", name), true);
            } else {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.enable.set.false", name), true);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow enable", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runExecuteFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", name));
            }
            if (targetFlow.isEnabled == false) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.disabled", name));
            }
            ExecutionContext ctx = new ExecutionContext(targetFlow.flow, context.getSource().getServer());
            ctx.execute(Util.serverConfig.getMaxFlowLength(), null, null);
            data.executeHistory.add(ctx);
            if (ctx.getException() != null) {
                throw new CommandRuntimeException(ctx.getException().getMessageText());
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow execute", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runDeleteFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", name));
            }
            data.logicFlows.remove(name);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.delete.success", name), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow delete", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runFlowHistoryCommand(int pageIndex, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            List<ExecutionContext> history = data.executeHistory;
            // 5 entries per page
            int maxPage = (history.size() + 4) / 5;
            if (maxPage <= 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.history.null"), false);
                return Command.SINGLE_SUCCESS;
            }
            int index = pageIndex;
            if (index <= 0) {
                index = maxPage;
            }
            if (index > maxPage) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.history.indexerror", pageIndex, maxPage));
            }
            int start = (index - 1) * 5;
            int end = Math.min(start + 5, history.size());
            String indexStr = String.valueOf(index);
            String maxPageStr = String.valueOf(maxPage);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.history.title", indexStr, maxPageStr), false);
            for (int i = start; i < end; i++) {
                ExecutionContext entry = history.get(i);
                String iStr = String.valueOf(i + 1);
                MutableComponent entryText =  Util.parseTranslateableText("fmod.command.flow.history.entry", iStr, entry.getFlow().name).withStyle(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clickview")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow log " + iStr))
                );
                context.getSource().sendSuccess(() -> entryText, false);
            }
            MutableComponent navigateText = Component.empty();
            if (index > 1) {
                String prevIndexStr = String.valueOf(index - 1);
                navigateText.append(Util.parseTranslateableText("fmod.command.flow.history.prev").withStyle(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow history " + prevIndexStr))
                ));
            }
            if (index < maxPage) {
                String nextIndexStr = String.valueOf(index + 1);
                navigateText.append(Util.parseTranslateableText("fmod.command.flow.history.next").withStyle(s -> s
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/f flow history " + nextIndexStr))
                ));
            }
            if (maxPage > 1) {
                context.getSource().sendSuccess(() -> navigateText, false);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow history", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runViewFlowCommand(String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(name);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", name));
            }
            Component text = targetFlow.flow.render();
            context.getSource().sendSuccess(() -> text, false);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow view", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runLogFlowCommand(int index, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            List<ExecutionContext> history = data.executeHistory;
            if (index <= 0 || index > history.size()) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.log.indexerror", String.valueOf(index)));
            }
            ExecutionContext entry = history.get(index - 1);
            Component text = entry.render();
            context.getSource().sendSuccess(() -> text, false);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow log", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowNewNodeCommand(String flowName, String type, String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            Collection<String> validNodeTypes = NodeRegistry.getNodeList();
            if (!validNodeTypes.contains(type)) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.unknown", type));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode != null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.exists", name, flowName));
            }
            targetFlow.createNode(type, name);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.newnode.success", name, flowName), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRemoveNodeCommand(String flowName, String name, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (existingNode.isEventNode()) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.delete.event", flowName, name));
            }
            targetFlow.removeNode(name);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.removenode.success", name, flowName), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRenameNodeCommand(String flowName, String oldName, String newName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(oldName);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", oldName, flowName));
            }
            FlowNode newNode = targetFlow.flow.getNodeByName(newName);
            if (newNode != null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.exists", newName, flowName));
            }
            targetFlow.renameNode(oldName, newName);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.renamenode.success", oldName, flowName, newName), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowConstInputCommand(String flowName, String name, int index, String value, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
            }
            // parse const value
            DataReference ref = FlowSerializer.parseConstDataReference(value);
            Object parsedValue = ref.value;
            String parsedValueStr = String.valueOf(parsedValue);
            targetFlow.setConstInput(name, index - 1, parsedValue);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.const.success", name, existingNode.getMetadata().inputNames.get(index - 1), parsedValueStr), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRefInputCommand(String flowName, String name, int index, String refNode, int refIndex, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
            }
            FlowNode refExistingNode = targetFlow.flow.getNodeByName(refNode);
            if (refExistingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", refNode, flowName));
            }
            if (refIndex <= 0 || refIndex > refExistingNode.getMetadata().outputNumber) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.edit.output.indexerror", refNode, String.valueOf(refIndex)));
            }
            targetFlow.setReferenceInput(name, index - 1, refNode, refIndex - 1);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.ref.success", name, existingNode.getMetadata().inputNames.get(index - 1), refNode, refExistingNode.getMetadata().outputNames.get(refIndex - 1)), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowDisconnectInputCommand(String flowName, String name, int index, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().inputNumber) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.edit.input.indexerror", name, String.valueOf(index)));
            }
            targetFlow.disconnectInput(name, index - 1);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.disconnect.success", name, existingNode.getMetadata().inputNames.get(index - 1)), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowNextNodeCommand(String flowName, String name, int index, String next, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().branchNumber) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.edit.branch.indexerror", name, String.valueOf(index)));
            }
            FlowNode nextNode = targetFlow.flow.getNodeByName(next);
            if (nextNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", next, flowName));
            }
            targetFlow.setNextNode(name, index - 1, next);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.next.success", name, existingNode.getMetadata().branchNames.get(index - 1), next), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowFinalBranchCommand(String flowName, String name, int index, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            FlowNode existingNode = targetFlow.flow.getNodeByName(name);
            if (existingNode == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.node.notexists", name, flowName));
            }
            if (index <= 0 || index > existingNode.getMetadata().branchNumber) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.edit.branch.indexerror", name, String.valueOf(index)));
            }
            targetFlow.disconnectNextNode(name, index - 1);
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.final.success", name, existingNode.getMetadata().branchNames.get(index - 1)), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowUndoCommand(String flowName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            if (targetFlow.undoPath.isEmpty()) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.undo.nothing", flowName), false);
                return Command.SINGLE_SUCCESS;
            } else {
                targetFlow.undo();
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.undo.success", flowName), true);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runEditFlowRedoCommand(String flowName, CommandContext<CommandSourceStack> context) {
        try {
            FlowFileSuggestion.suggest();
            ServerData data = Util.getServerData(context.getSource().getServer());
            FlowManager targetFlow = data.logicFlows.get(flowName);
            if (targetFlow == null) {
                throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.flow.notexists", flowName));
            }
            if (targetFlow.redoPath.isEmpty()) {
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.redo.nothing", flowName), false);
                return Command.SINGLE_SUCCESS;
            } else {
                targetFlow.redo();
                context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.flow.edit.redo.success", flowName), true);
            }
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            logger.error("FMinecraftMod: Caught unexpected exception when executing command /f flow edit", e);
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int runReloadCommand(CommandContext<CommandSourceStack> context) {
        try {
            SongFileSuggestion.suggest();
            FlowFileSuggestion.suggest();
            Util.loadServerConfig();
            context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.reload.success"), true);
        } catch (Exception e) {
            if (e instanceof CommandRuntimeException) {
                throw (CommandRuntimeException) e;
            }
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.reload.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static void registerCommand(RegisterCommandsEvent event) {
        try {
            final LiteralCommandNode<CommandSourceStack> fModCommandNode = event.getDispatcher().register(Commands.literal("fminecraftmod")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {return runFModCommand(context);})
                .then(Commands.literal("dev")
                    .requires(source -> source.hasPermission(4))
                    .executes(context -> {return runDevCommand(context);})
                )
                .then(Commands.literal("gpt")
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
                    )
                )
                .then(Commands.literal("song")
                    .requires(source -> source.hasPermission(3))
                    .then(Commands.literal("play")
                        .then(Commands.argument("player", EntityArgument.players())
                            .then(Commands.argument("song", StringArgumentType.greedyString())
                                .suggests(SongFileSuggestion.suggest())
                                .executes(context -> {return runSongPlayCommand(EntityArgument.getPlayers(context, "player"), StringArgumentType.getString(context, "song"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("cancel")
                        .then(Commands.argument("player", EntityArgument.players())
                            .executes(context -> {return runSongCancelCommand(EntityArgument.getPlayers(context, "player"), context);})
                        )
                    )
                    .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.players())
                            .executes(context -> {return runSongGetCommand(EntityArgument.getPlayers(context, "player"), context);})
                        )
                    )
                    .then(Commands.literal("show")
                        .then(Commands.argument("player", EntityArgument.players())
                            .then(Commands.argument("enable", BoolArgumentType.bool())
                                .executes(context -> {return runSongShowInfoCommand(EntityArgument.getPlayers(context, "player"), BoolArgumentType.getBool(context, "enable"), context);})
                            )
                            .executes(context -> {return runSongShowInfoCommand(EntityArgument.getPlayers(context, "player"), context);})
                        )
                    )
                    .then(Commands.literal("seek")
                        .then(Commands.argument("player", EntityArgument.players())
                            .then(Commands.argument("timepoint", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> {return runSongSeekCommand(EntityArgument.getPlayers(context, "player"), DoubleArgumentType.getDouble(context, "timepoint"), context);})
                            )
                        )
                    )
                    .then(Commands.literal("speed")
                        .then(Commands.argument("player", EntityArgument.players())
                            .then(Commands.argument("speed", DoubleArgumentType.doubleArg())
                                .executes(context -> {return runSongSpeedCommand(EntityArgument.getPlayers(context, "player"), DoubleArgumentType.getDouble(context, "speed"), context);})
                            )
                        )
                    )
                )
                .then(Commands.literal("get")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("coord")
                        .then(Commands.argument("entity", EntityArgument.entities())
                            .executes(context -> {return runGetCoordCommand(EntityArgument.getEntities(context, "entity"), context);})
                        )
                    )
                    .then(Commands.literal("distance")
                        .then(Commands.argument("entity", EntityArgument.entities())
                            .executes(context -> {return runGetDistanceCommand(EntityArgument.getEntities(context, "entity"), context);})
                        )
                    )
                    .then(Commands.literal("health")
                        .then(Commands.argument("entity", EntityArgument.entities())
                            .executes(context -> {return runGetHealthCommand(EntityArgument.getEntities(context, "entity"), context);})
                        )
                    )
                    .then(Commands.literal("status")
                        .then(Commands.argument("player", EntityArgument.players())
                            .executes(context -> {return runGetStatusCommand(EntityArgument.getPlayers(context, "player"), context);})
                        )
                    )
                    .then(Commands.literal("inventory")
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(context -> {return runGetInventoryCommand(EntityArgument.getPlayer(context, "player"), context);})
                        )
                    )
                    .then(Commands.literal("item")
                        .then(Commands.argument("entity", EntityArgument.entities())
                            .executes(context -> {return runGetItemCommand(EntityArgument.getEntities(context, "entity"), context);})
                        )
                    )
                )
                .then(Commands.literal("share")
                    .requires(source -> source.hasPermission(0))
                    .then(Commands.literal("coord").executes(context -> {return runShareCoordCommand(context);}))
                    .then(Commands.literal("distance").executes(context -> {return runShareDistanceCommand(context);}))
                    .then(Commands.literal("health").executes(context -> {return runShareHealthCommand(context);}))
                    .then(Commands.literal("status").executes(context -> {return runShareStatusCommand(context);}))
                    .then(Commands.literal("inventory").executes(context -> {return runShareInventoryCommand(context);}))
                    .then(Commands.literal("item").executes(context -> {return runShareItemCommand(context);}))
                )
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
                .then(Commands.literal("flow")
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
                                        .then(Commands.argument("value", StringArgumentType.greedyString())
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
                    )
                )
                .then(Commands.literal("options")
                    .requires(source -> source.hasPermission(4))
                    .then(Commands.literal("serverTranslation")
                        .then(Commands.argument("enable", BoolArgumentType.bool())
                            .executes(context -> {return runOptionsCommand("serverTranslation", BoolArgumentType.getBool(context, "enable"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("serverTranslation", null, context);})
                    )
                    .then(Commands.literal("maxFlowLength")
                        .then(Commands.argument("length", IntegerArgumentType.integer(1))
                            .executes(context -> {return runOptionsCommand("maxFlowLength", IntegerArgumentType.getInteger(context, "length"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("maxFlowLength", null, context);})
                    )
                    .then(Commands.literal("entityDeathMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("entityDeathMessage", null, context);})
                    )
                    .then(Commands.literal("bossDeathMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("bossDeathMessage", null, context);})
                    )
                    .then(Commands.literal("namedMobDeathMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("namedMobDeathMessage", null, context);})
                    )
                    .then(Commands.literal("killerDeathMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("killerDeathMessage", null, context);})
                    )
                    .then(Commands.literal("canSleepMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("canSleepMessage", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("canSleepMessage", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("canSleepMessage", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("canSleepMessage", null, context);})
                    )
                    .then(Commands.literal("bossMaxHealthThreshold")
                        .then(Commands.argument("HP", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", DoubleArgumentType.getDouble(context, "HP"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", null, context);})
                    )
                    .then(Commands.literal("playerDeathCoord")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("playerDeathCoord", null, context);})
                    )
                    .then(Commands.literal("projectileHitsEntity")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("projectileHitsEntity", null, context);})
                    )
                    .then(Commands.literal("projectileBeingHit")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("projectileBeingHit", null, context);})
                    )
                    .then(Commands.literal("informAFK")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("informAFK", null, context);})
                    )
                    .then(Commands.literal("informAFKThreshold")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("informAFKThreshold", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("informAFKThreshold", null, context);})
                    )
                    .then(Commands.literal("broadcastAFK")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("broadcastAFK", null, context);})
                    )
                    .then(Commands.literal("broadcastAFKThreshold")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", null, context);})
                    )
                    .then(Commands.literal("backFromAFK")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("backFromAFK", null, context);})
                    )
                    .then(Commands.literal("biomeChangeMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("biomeChangeMessage", null, context);})
                    )
                    .then(Commands.literal("biomeChangeDelay")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("biomeChangeDelay", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("biomeChangeDelay", null, context);})
                    )
                    .then(Commands.literal("bossFightMessageLocation")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("bossFightMessageLocation", null, context);})
                    )
                    .then(Commands.literal("bossFightMessageReceiver")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("bossFightMessageReceiver", null, context);})
                    )
                    .then(Commands.literal("bossFightMessageInterval")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("bossFightMessageInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("bossFightMessageInterval", null, context);})
                    )
                    .then(Commands.literal("monsterSurroundMessageLocation")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", null, context);})
                    )
                    .then(Commands.literal("monsterSurroundMessageReceiver")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", null, context);})
                    )
                    .then(Commands.literal("monsterSurroundMessageInterval")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("monsterSurroundMessageInterval", null, context);})
                    )
                    .then(Commands.literal("monsterNumberThreshold")
                        .then(Commands.argument("num", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("monsterNumberThreshold", IntegerArgumentType.getInteger(context, "num"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("monsterNumberThreshold", null, context);})
                    )
                    .then(Commands.literal("monsterDistanceThreshold")
                        .then(Commands.argument("meters", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {return runOptionsCommand("monsterDistanceThreshold", DoubleArgumentType.getDouble(context, "meters"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("monsterDistanceThreshold", null, context);})
                    )
                    .then(Commands.literal("entityNumberWarning")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("entityNumberWarning", null, context);})
                    )
                    .then(Commands.literal("entityNumberThreshold")
                        .then(Commands.argument("num", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("entityNumberThreshold", IntegerArgumentType.getInteger(context, "num"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("entityNumberThreshold", null, context);})
                    )
                    .then(Commands.literal("entityNumberCheckInterval")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                            .executes(context -> {return runOptionsCommand("entityNumberCheckInterval", IntegerArgumentType.getInteger(context, "ticks"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("entityNumberCheckInterval", null, context);})
                    )
                    .then(Commands.literal("playerHurtMessage")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("playerHurtMessage", null, context);})
                    )
                    .then(Commands.literal("hugeDamageThreshold")
                        .then(Commands.argument("percentage", DoubleArgumentType.doubleArg(0, 100))
                            .executes(context -> {return runOptionsCommand("hugeDamageThreshold", DoubleArgumentType.getDouble(context, "percentage"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("hugeDamageThreshold", null, context);})
                    )
                    .then(Commands.literal("travelMessageLocation")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("travelMessageLocation", MessageLocation.NONE, context);}))
                        .then(Commands.literal("chat").executes(context -> {return runOptionsCommand("travelMessageLocation", MessageLocation.CHAT, context);}))
                        .then(Commands.literal("actionbar").executes(context -> {return runOptionsCommand("travelMessageLocation", MessageLocation.ACTIONBAR, context);}))
                        .executes(context -> {return runOptionsCommand("travelMessageLocation", null, context);})
                    )
                    .then(Commands.literal("travelMessageReceiver")
                        .then(Commands.literal("off").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.NONE, context);}))
                        .then(Commands.literal("all").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.ALL, context);}))
                        .then(Commands.literal("ops").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.OP, context);}))
                        .then(Commands.literal("selfops").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.SELFOP, context);}))
                        .then(Commands.literal("teamops").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.TEAMOP, context);}))
                        .then(Commands.literal("team").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.TEAM, context);}))
                        .then(Commands.literal("self").executes(context -> {return runOptionsCommand("travelMessageReceiver", MessageReceiver.SELF, context);}))
                        .executes(context -> {return runOptionsCommand("travelMessageReceiver", null, context);})
                    )
                    .then(Commands.literal("travelWindow")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                            .executes(context -> {return runOptionsCommand("travelWindow", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("travelWindow", null, context);})
                    )
                    .then(Commands.literal("travelDistanceThreshold")
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {return runOptionsCommand("travelDistanceThreshold", DoubleArgumentType.getDouble(context, "distance"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("travelDistanceThreshold", null, context);})
                    )
                    .then(Commands.literal("travelTeleportThreshold")
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {return runOptionsCommand("travelTeleportThreshold", DoubleArgumentType.getDouble(context, "distance"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("travelTeleportThreshold", null, context);})
                    )
                    .then(Commands.literal("travelPartialInterval")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                            .executes(context -> {return runOptionsCommand("travelPartialInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("travelPartialInterval", null, context);})
                    )
                    .then(Commands.literal("travelPartialDistance")
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(0))
                            .executes(context -> {return runOptionsCommand("travelPartialDistance", DoubleArgumentType.getDouble(context, "distance"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("travelPartialDistance", null, context);})
                    )
                    .then(Commands.literal("gptUrl")
                        .then(Commands.argument("url", StringArgumentType.greedyString())
                            .executes(context -> {return runOptionsCommand("gptUrl", StringArgumentType.getString(context, "url"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("gptUrl", null, context);})
                    )
                    .then(Commands.literal("gptAccessTokens")
                        .then(Commands.argument("tokens", StringArgumentType.greedyString())
                            .executes(context -> {return runOptionsCommand("gptAccessTokens", StringArgumentType.getString(context, "tokens"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("gptAccessTokens", null, context);})
                    )
                    .then(Commands.literal("gptModel")
                        .then(Commands.argument("model", StringArgumentType.greedyString())
                            .executes(context -> {return runOptionsCommand("gptModel", StringArgumentType.getString(context, "model"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("gptModel", null, context);})
                    )
                    .then(Commands.literal("gptSystemPrompts")
                        .then(Commands.argument("prompt", StringArgumentType.greedyString())
                            .executes(context -> {return runOptionsCommand("gptSystemPrompts", StringArgumentType.getString(context, "prompt"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("gptSystemPrompts", null, context);})
                    )
                    .then(Commands.literal("gptTemperature")
                        .then(Commands.argument("temperature", DoubleArgumentType.doubleArg(0, 1))
                            .executes(context -> {return runOptionsCommand("gptTemperature", DoubleArgumentType.getDouble(context, "temperature"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("gptTemperature", null, context);})
                    )
                    .then(Commands.literal("gptTimeout")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                            .executes(context -> {return runOptionsCommand("gptTimeout", IntegerArgumentType.getInteger(context, "seconds"), context);})
                        )
                        .executes(context -> {return runOptionsCommand("gptTimeout", null, context);})
                    )
                )
            );
            event.getDispatcher().register(Commands.literal("f")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {return runFModCommand(context);})
                .redirect(fModCommandNode)
            );
        } catch (Exception e) {
            logger.error("FMinecraftMod: Unable to register command.", e);
        }
    }

    private static int runOptionsCommand(String options, Object value, CommandContext<CommandSourceStack> context) {
        try {
            switch (options) {
                case "serverTranslation":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.translate", EnumI18n.getBooleanValueI18n(Util.serverConfig.isEnableServerTranslation())), false);
                    } else {
                        Util.serverConfig.setEnableServerTranslation((boolean) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.translate", value), true);
                    }
                    break;
                case "maxFlowLength":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.flowlength", Util.serverConfig.getMaxFlowLength()), false);
                    } else {
                        Util.serverConfig.setMaxFlowLength((int) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.flowlength", value), true);
                    }
                    break;
                case "entityDeathMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.entdeathmsg", text), false);
                    } else {
                        Util.serverConfig.setEntityDeathMessage((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.entdeathmsg", text), true);
                    }
                    break;
                case "bossDeathMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bcbossdeath", text), false);
                    } else {
                        Util.serverConfig.setBossDeathMessage((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bcbossdeath", text), true);
                    }
                    break;
                case "namedMobDeathMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.nameddeath", text), false);
                    } else {
                        Util.serverConfig.setNamedEntityDeathMessage((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.nameddeath", text), true);
                    }
                    break;
                case "killerDeathMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bckillerdeath", text), false);
                    } else {
                        Util.serverConfig.setKillerEntityDeathMessage((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bckillerdeath", text), true);
                    }
                    break;
                case "bossMaxHealthThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bossmaxhp", Util.serverConfig.getBossMaxHpThreshold()), false);
                    } else {
                        Util.serverConfig.setBossMaxHpThreshold((double) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bossmaxhp", value), true);
                    }
                    break;
                case "canSleepMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getPlayerCanSleepMessage());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.cansleepmsg", text), false);
                    } else {
                        Util.serverConfig.setPlayerCanSleepMessage((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.cansleepmsg", text), true);
                    }
                    break;
                case "playerDeathCoord":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bcdeathcoord", text), false);
                    } else {
                        Util.serverConfig.setPlayerDeathCoord((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bcdeathcoord", text), true);
                    }
                    break;
                case "projectileHitsEntity":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.projhitting", text), false);
                    } else {
                        Util.serverConfig.setProjectileHitOthers((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.projhitting", text), true);
                    }
                    break;
                case "projectileBeingHit":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.projbeinghit", text), false);
                    } else {
                        Util.serverConfig.setProjectileBeingHit((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.projbeinghit", text), true);
                    }
                    break;
                case "informAFK":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.informafk", text), false);
                    } else {
                        Util.serverConfig.setInformAfking((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.informafk", text), true);
                    }
                    break;
                case "informAFKThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.informafkthres", String.format("%.2f", Util.serverConfig.getInformAfkingThreshold() / 20.0)), false);
                    } else {
                        Util.serverConfig.setInformAfkingThreshold((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.informafkthres", value), true);
                    }
                    break;
                case "broadcastAFK":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bcafk", text), false);
                    } else {
                        Util.serverConfig.setBroadcastAfking((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bcafk", text), true);
                    }
                    break;
                case "broadcastAFKThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bcafkthres", String.format("%.2f", Util.serverConfig.getBroadcastAfkingThreshold() / 20.0)), false);
                    } else {
                        Util.serverConfig.setBroadcastAfkingThreshold((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bcafkthres", value), true);
                    }
                    break;
                case "backFromAFK":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.stopafk", text), false);
                    } else {
                        Util.serverConfig.setStopAfking((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.stopafk", text), true);
                    }
                    break;
                case "biomeChangeMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.changebiome", text), false);
                    } else {
                        Util.serverConfig.setChangeBiome((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.changebiome", text), true);
                    }
                    break;
                case "biomeChangeDelay":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.biomedelay", String.format("%.2f", Util.serverConfig.getChangeBiomeDelay() / 20.0)), false);
                    } else {
                        Util.serverConfig.setChangeBiomeDelay((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.biomedelay", value), true);
                    }
                    break;
                case "bossFightMessageLocation":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightloc", text), false);
                    } else {
                        Util.serverConfig.setBossFightMessageLocation((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bossfightloc", text), true);
                    }
                    break;
                case "bossFightMessageReceiver":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightreceiver", text), false);
                    } else {
                        Util.serverConfig.setBossFightMessageReceiver((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bossfightreceiver", text), true);
                    }
                    break;
                case "bossFightMessageInterval":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightinterval", String.format("%.2f", Util.serverConfig.getBossFightInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setBossFightInterval((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.bossfightinterval", value), true);
                    }
                    break;
                case "monsterSurroundMessageLocation":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.monsterloc", text), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundMessageLocation((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.monsterloc", text), true);
                    }
                    break;
                case "monsterSurroundMessageReceiver":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.monsterreceiver", text), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundMessageReceiver((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.monsterreceiver", text), true);
                    }
                    break;
                case "monsterSurroundMessageInterval":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.monsterinterval", String.format("%.2f", Util.serverConfig.getMonsterSurroundInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundInterval((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.monsterinterval", value), true);
                    }
                    break;
                case "monsterNumberThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.monsternumber", Util.serverConfig.getMonsterNumberThreshold()), false);
                    } else {
                        Util.serverConfig.setMonsterNumberThreshold((int) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.monsternumber", value), true);
                    }
                    break;
                case "monsterDistanceThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.monsterdistance", Util.serverConfig.getMonsterDistanceThreshold()), false);
                    } else {
                        Util.serverConfig.setMonsterDistanceThreshold((double) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.monsterdistance", value), true);
                    }
                    break;
                case "entityNumberWarning":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.entitywarning", text), false);
                    } else {
                        Util.serverConfig.setEntityNumberWarning((MessageLocation) value);
                        final MutableComponent text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.entitywarning", text), true);
                    }
                    break;
                case "entityNumberThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.entitynumber", Util.serverConfig.getEntityNumberThreshold()), false);
                    } else {
                        Util.serverConfig.setEntityNumberThreshold((int) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.entitynumber", value), true);
                    }
                    break;
                case "entityNumberCheckInterval":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.entityinterval", Util.serverConfig.getEntityNumberInterval()), false);
                    } else {
                        Util.serverConfig.setEntityNumberInterval((int) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.entityinterval", value), true);
                    }
                    break;
                case "playerHurtMessage":
                    if (value == null) {
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt());
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.playerhurt", text), false);
                    } else {
                        Util.serverConfig.setPlayerSeriousHurt((MessageReceiver) value);
                        final MutableComponent text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.playerhurt", text), true);
                    }
                    break;
                case "hugeDamageThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.damagethres", Util.serverConfig.getPlayerHurtThreshold() * 100.0), false);
                    } else {
                        Util.serverConfig.setPlayerHurtThreshold((double) value / 100.0);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.damagethres", value), true);
                    }
                    break;
                case "travelMessageLocation":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.loc", EnumI18n.getMessageLocationI18n(Util.serverConfig.getTravelMessageLocation())), false);
                    } else {
                        Util.serverConfig.setTravelMessageLocation((MessageLocation) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.loc", EnumI18n.getMessageLocationI18n((MessageLocation) value)), true);
                    }
                    break;
                case "travelMessageReceiver":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.receiver", EnumI18n.getMessageReceiverI18n(Util.serverConfig.getTravelMessageReceiver())), false);
                    } else {
                        Util.serverConfig.setTravelMessageReceiver((MessageReceiver) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.receiver", EnumI18n.getMessageReceiverI18n((MessageReceiver) value)), true);
                    }
                    break;
                case "travelWindow":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.window", String.format("%.2f", Util.serverConfig.getTravelWindowTicks() / 20.0)), false);
                    } else {
                        Util.serverConfig.setTravelWindowTicks((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.window", value), true);
                    }
                    break;
                case "travelDistanceThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.total", Util.serverConfig.getTravelTotalDistanceThreshold()), false);
                    } else {
                        Util.serverConfig.setTravelTotalDistanceThreshold((double) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.total", value), true);
                    }
                    break;
                case "travelTeleportThreshold":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.teleport", Util.serverConfig.getTravelTeleportThreshold()), false);
                    } else {
                        Util.serverConfig.setTravelTeleportThreshold((double) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.teleport", value), true);
                    }
                    break;
                case "travelPartialInterval":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.interval", String.format("%.2f", Util.serverConfig.getTravelPartialInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setTravelPartialInterval((int) value * 20);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.interval", value), true);
                    }
                    break;
                case "travelPartialDistance":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.travelmsg.partial", Util.serverConfig.getTravelPartialDistanceThreshold()), false);
                    } else {
                        Util.serverConfig.setTravelPartialDistanceThreshold((double) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.travelmsg.partial", value), true);
                    }
                    break;
                case "gptUrl":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.gpturl", Util.serverConfig.getGptUrl()), false);
                    } else {
                        try {
                            new URI((String) value).toURL();
                        } catch (Exception e) {
                            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.options.invalidurl", value));
                        }
                        Util.serverConfig.setGptUrl((String) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.gpturl", value), true);
                    }
                    break;
                case "gptAccessTokens":
                    if (value == null) {
                        final String secureTokens = Util.serverConfig.getSecureGptAccessTokens();
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.gptkey", secureTokens), false);
                    } else {
                        String token = (String) value;
                        Util.serverConfig.setGptAccessTokens(token);
                        // For security reasons, we don't want to show the full token in the log, only show the first 5 and the last 5 characters
                        final String secureTokens = Util.serverConfig.getSecureGptAccessTokens();
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.gptkey", secureTokens), true);
                    }
                    break;
                case "gptModel":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.gptmodel", Util.serverConfig.getGptModel()), false);
                    } else {
                        Util.serverConfig.setGptModel((String) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.gptmodel", value), true);
                    }
                    break;
                case "gptSystemPrompts":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.gptsysprompt", Util.serverConfig.getGptSystemPrompt()), false);
                    } else {
                        Util.serverConfig.setGptSystemPrompt((String) value);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.gptsysprompt", value), true);
                    }
                    break;
                case "gptTemperature":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.gpttemperature", Util.serverConfig.getGptTemperature()), false);
                    } else {
                        double temperature = (double) value;
                        Util.serverConfig.setGptTemperature(temperature);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.gpttemperature", value), true);
                    }
                    break;
                case "gptTimeout":
                    if (value == null) {
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.get.gpttimeout", (int) (Util.serverConfig.getGptServerTimeout() / 1000)), false);
                    } else {
                        int timeout = (int) value;
                        Util.serverConfig.setGptServerTimeout(timeout * 1000);
                        context.getSource().sendSuccess(() -> Util.parseTranslateableText("fmod.command.options.gpttimeout", value), true);
                    }
                    break;
                default:
                    throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.options.unknownoption", options));
            }
            if (value != null) {
                Util.saveServerConfig();
            }
        } catch (ClassCastException e) {
            throw new CommandRuntimeException(Util.parseTranslateableText("fmod.command.options.classcast", value, options, e.getMessage()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
