/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ykn.fmod.server.base.data.GptData;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.schedule.playSong;
import com.ykn.fmod.server.base.song.NbsSongDecoder;
import com.ykn.fmod.server.base.song.NoteBlockSong;
import com.ykn.fmod.server.base.util.EnumI18n;
import com.ykn.fmod.server.base.util.GameMath;
import com.ykn.fmod.server.base.util.GptHelper;
import com.ykn.fmod.server.base.util.MarkdownToTextConverter;
import com.ykn.fmod.server.base.util.MessageReceiver;
import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class CommandRegistrater {

    private Logger logger;

    private Object devFunction(CommandContext<ServerCommandSource> context) {
        // This function is used for development purposes. Execute command /f dev to run this function.
        // This function should be removed in the final release.

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

        context.getSource().sendFeedback(() -> MarkdownToTextConverter.parseMarkdownToText(markdownTest), false);
        return null;
    }

    public CommandRegistrater(Logger logger) {
        this.logger = logger;
    }

    
    private int runFModCommand(CommandContext<ServerCommandSource> context) {
        try {
            MutableText commandFeedback = Util.parseTranslateableText("fmod.misc.version", Util.getMinecraftVersion(), Util.getModVersion(), Util.getModAuthors());
            context.getSource().sendFeedback(() -> commandFeedback, false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.version.error"));
        }
    }

    private int runDevCommand(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.dev.start"), false);
            Object result = devFunction(context);
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.dev.end", result == null ? "null" : result.toString()), false);
        } catch (Exception e) {
            try {
                context.getSource().sendFeedback(() -> Text.literal(e.getMessage()), false);
                context.getSource().sendFeedback(() -> Text.literal(e.getStackTrace().toString()), false);
            } catch (Exception exception) {
                logger.error("FMinectaftMod: Caught unexpected exception when executing command /f dev", exception);
                throw new CommandException(Util.parseTranslateableText("fmod.command.dev.error"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptNewCommand(String text, CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.newConversation(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
            // if (context.getSource().getPlayer() != null) {
            //     // Other source would have already logged the message
            //     logger.info("<{}> {}", context.getSource().getDisplayName().getString(), text);
            // }
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f gpt new", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptReplyCommand(String text, CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.reply(text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f gpt reply", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptRegenerateCommand(CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            String text = gptData.getPostMessages(gptDataLength - 1);
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.regenerate(url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f gpt regenerate", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptEditCommand(int index, String text, CommandContext<ServerCommandSource> context) {
        try {
            String urlString = Util.serverConfig.getGptUrl();
            URL url = new URI(urlString).toURL();
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            // The Command argument index begins from 1, the source code index begins from 0
            if (index <= 0 || index > gptDataLength) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            GptHelper gptHelper = new GptHelper(gptData, context);
            boolean postResult = gptData.editHistory(index - 1, text, url, Util.serverConfig.getGptModel(), Util.serverConfig.getGptTemperature());
            if (postResult == false) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.spam"));
            }
            Thread thread = new Thread(gptHelper);
            thread.setDaemon(true);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(text)), true);
            thread.start();
        } catch (URISyntaxException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (MalformedURLException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (IllegalArgumentException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.urlerror"));
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f gpt edit " + index, e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGptHistoryCommand(int index, CommandContext<ServerCommandSource> context) {
        try {
            GptData gptData = Util.getServerData(context.getSource().getServer()).getGptData(context.getSource().getName());
            final int gptDataLength = gptData.getHistorySize();
            if (gptDataLength == 0) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.nohistory"));
            }
            if (index == 0) {
                index = gptDataLength;
            }
            if (index < 0 || index > gptDataLength) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.historyindexerror", index, gptDataLength));
            }
            final int finalIndex = index;
            final String postMessage = gptData.getPostMessages(index - 1);
            final String model = gptData.getGptModels(index - 1);
            final Text receivedMessage = gptData.getResponseTexts(index - 1);
            context.getSource().sendFeedback(() -> Text.literal("<").append(context.getSource().getDisplayName()).append("> ").append(Text.literal(postMessage)), false);
            context.getSource().sendFeedback(() -> Text.literal("<").append(model.isBlank() ? "GPT" : model).append("> ").append(receivedMessage), false);
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.gpt.history", finalIndex, gptDataLength), false);
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f gpt history " + index, e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.gpt.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runSongPlayCommand(Collection<ServerPlayerEntity> players, String songName, CommandContext<ServerCommandSource> context) {
        try {
            // Refresh song suggestion list
            SongFileSuggestion.suggest();
            if (SongFileSuggestion.getAvailableSongs() == 0) {
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.hint"), false);
            }
            Path songFolder = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID).normalize();
            Path songPath = songFolder.resolve(songName).normalize();
            if (!songPath.startsWith(songFolder)) {
                throw new CommandException(Util.parseTranslateableText("fmod.command.song.filenotfound", songName));
            }

            // Load song
            NoteBlockSong song = null;
            try (FileInputStream fileInputStream = new FileInputStream(songPath.toFile())) {
                song = NbsSongDecoder.parse(fileInputStream);
            }
            // Check if a song is still playing, if so, cancel the task
            for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                if (scheduledTask instanceof playSong) {
                    playSong playSong = (playSong) scheduledTask;
                    // if (players.contains(playSong.getTarget())) {
                    //     // The Entity class overrides the equals method using network id instead of uuid, which will change after reloading
                    //     // So, this method is not reliable
                    //     playSong.cancel();
                    // }
                    for (ServerPlayerEntity player : players) {
                        if (playSong.getTarget().getUuid() == player.getUuid()) {
                            playSong.cancel();
                        }
                    }
                }
            }
            // Submit song task
            for (ServerPlayerEntity player : players) {
                playSong playSong = new playSong(song, songName, player, context);
                Util.getServerData(context.getSource().getServer()).submitScheduledTask(playSong);
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.start", player.getDisplayName(), songName), true);
            }
        } catch (FileNotFoundException fileNotFoundException) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.song.filenotfound", songName));
        } catch (EOFException eofException) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.song.eofexception", songName));
        } catch (IOException ioException) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.song.ioexception", songName));
        } catch (Exception exception) {
            if (exception instanceof CommandException) {
                throw (CommandException) exception;
            }
            throw new CommandException(Util.parseTranslateableText("fmod.command.song.error", songName));
        }
        return players.size();
    }

    private int runSongCancelCommand(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            for (ServerPlayerEntity player : players) {
                boolean isFound = false;
                for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                    if (scheduledTask instanceof playSong) {
                        playSong playSong = (playSong) scheduledTask;
                        if (playSong.getTarget().getUuid() == player.getUuid()) {
                            isFound = true;
                            playSong.cancel();
                            result++;
                        }
                    }
                }
                if (isFound == false) {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                }
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f song cancel", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private int runSongGetCommand(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            for (ServerPlayerEntity player : players) {
                boolean isFound = false;
                for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                    if (scheduledTask instanceof playSong) {
                        playSong playSong = (playSong) scheduledTask;
                        if (playSong.getTarget().getUuid() == player.getUuid()) {
                            isFound = true;
                            String currentTimeStr = String.format("%.1f", playSong.getSong().getVirtualTick(playSong.getTick()) / 20.0);
                            String totalTimeStr = String.format("%.1f", playSong.getSong().getMaxVirtualTick() / 20.0);
                            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.get", player.getDisplayName(), playSong.getSongName(), currentTimeStr, totalTimeStr), false);
                            result++;
                        }
                    }
                }
                if (isFound == false) {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                }
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f song get", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private int runSongSearchCommand(Collection<ServerPlayerEntity> players, double timepoint, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            for (ServerPlayerEntity player : players) {
                boolean isFound = false;
                for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                    if (scheduledTask instanceof playSong) {
                        playSong playSong = (playSong) scheduledTask;
                        if (playSong.getTarget().getUuid() == player.getUuid()) {
                            isFound = true;
                            String songName = playSong.getSongName();
                            double songLength = playSong.getSong().getMaxVirtualTick() / 20.0;
                            String songLengthStr = String.format("%.1f", songLength);
                            String timepointStr = String.format("%.1f", timepoint);
                            if (timepoint < 0 || timepoint > songLength) {
                                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.long", songName, songLengthStr, timepointStr), false);
                            } else {
                                playSong.search((int) (timepoint * 20));
                                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.search", player.getDisplayName(), songName, timepointStr, songLengthStr), true);
                                result++;
                            }
                        }
                    }
                }
                if (isFound == false) {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                }
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f song search", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private int runSongSpeedCommand(Collection<ServerPlayerEntity> players, double speed, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            for (ServerPlayerEntity player : players) {
                boolean isFound = false;
                for (ScheduledTask scheduledTask : Util.getServerData(context.getSource().getServer()).getScheduledTasks()) {
                    if (scheduledTask instanceof playSong) {
                        playSong playSong = (playSong) scheduledTask;
                        if (playSong.getTarget().getUuid() == player.getUuid()) {
                            isFound = true;
                            Text playerName = player.getDisplayName();
                            String songName = playSong.getSongName();
                            String speedStr = String.format("%.2f", speed);
                            playSong.changeSpeed(speed);
                            if (speed == 0) {
                                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.pause", playerName, songName), true);
                            } else {
                                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.speed", playerName, songName, speedStr), true);
                            }
                            result++;
                        }
                    }
                }
                if (isFound == false) {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.empty", player.getDisplayName()), false);
                }
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f song speed", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private ServerPlayerEntity getShareCommandExecutor(CommandContext<ServerCommandSource> context) {
        if (context == null) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.playeronly"));
        }
        ServerCommandSource source = context.getSource();
        if (source == null) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.playeronly"));
        }
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.playeronly"));
        }
        return player;
        // return Optional.ofNullable(context)
        //     .map(CommandContext::getSource)
        //     .map(ServerCommandSource::getPlayer)
        //     .orElseThrow(() -> new CommandException(Util.parseTranslateableText("fmod.command.share.playeronly")));
    }

    private int runGetCoordCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        try {
            for (Entity entity : entities) {
                Text name = entity.getDisplayName();
                MutableText biome = Util.getBiomeText(entity);
                String strX = String.format("%.2f", entity.getX());
                String strY = String.format("%.2f", entity.getY());
                String strZ = String.format("%.2f", entity.getZ());
                MutableText text = Util.parseTranslateableText("fmod.command.get.coord", name, biome, strX, strY, strZ).styled(style -> style.withClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + strX + " " + strY + " " + strZ)
                ).withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
                ));
                context.getSource().sendFeedback(() -> text, false);
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f get coord", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private int runShareCoordCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            Text name = player.getDisplayName();
            MutableText biome = Util.getBiomeText(player);
            String strX = String.format("%.2f", player.getX());
            String strY = String.format("%.2f", player.getY());
            String strZ = String.format("%.2f", player.getZ());
            MutableText text = Util.parseTranslateableText("fmod.command.share.coord", name, biome, strX, strY, strZ).styled(style -> style.withClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + strX + " " + strY + " " + strZ)
            ).withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
            ));
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f share coord", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private MutableText getDirectionText(Vec3d source, Vec3d target) {
        double pitch = GameMath.getPitch(source, target);
        double yaw = GameMath.getYaw(source, target);
        MutableText direction = Text.empty();
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

    private int runGetDistanceCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            Vec3d source = context.getSource().getPosition();
            for (Entity entity : entities) {
                if (context.getSource().getWorld() != entity.getWorld()) {
                    final Text name = entity.getDisplayName();
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.dimdistance", name), false);
                    continue;
                }
                Vec3d target = entity.getPos();
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
                final Text name = entity.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableText dirTxt = getDirectionText(source, target);
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.distance", name, dirTxt, degStr, distStr), false);
                result++;
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f get distance", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private int runShareDistanceCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            Vec3d target = player.getPos();
            List<ServerPlayerEntity> onlinePlayers = Util.getOnlinePlayers(context.getSource().getServer());
            for (ServerPlayerEntity onlinePlayer : onlinePlayers) {
                if (onlinePlayer.getUuid() == player.getUuid()) {
                    Util.sendTextMessage(onlinePlayer, Util.parseTranslateableText("fmod.command.share.selfdistance"));
                    continue;
                }
                if (player.getWorld() != onlinePlayer.getWorld()) {
                    final Text name = player.getDisplayName();
                    Util.sendTextMessage(onlinePlayer, Util.parseTranslateableText("fmod.command.share.dimdistance", name));
                    continue;
                }
                Vec3d source = onlinePlayer.getPos();
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
                final Text name = player.getDisplayName();
                final String degStr = String.format("%.2f°", degree);
                final String distStr = String.format("%.2f", distance);
                final MutableText dirTxt = getDirectionText(source, target);
                final MutableText text = Util.parseTranslateableText("fmod.command.share.distance", name, dirTxt, degStr, distStr);
                Util.sendTextMessage(onlinePlayer, text);
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f share distance", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGetHealthCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        try {
            for (Entity entity : entities) {
                final Text name = entity.getDisplayName();
                double hp = Util.getHealth(entity);
                double maxhp = Util.getMaxHealth(entity);
                final String hpStr = String.format("%.2f", hp);
                final String maxhpStr = String.format("%.2f", maxhp);
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.health", name, hpStr, maxhpStr), false);
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f get health", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return entities.size();
    }

    private int runShareHealthCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            double hp = Util.getHealth(player);
            double maxhp = Util.getMaxHealth(player);
            final Text name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String maxhpStr = String.format("%.2f", maxhp);
            MutableText text = Util.parseTranslateableText("fmod.command.share.health", name, hpStr, maxhpStr);
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f share health", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGetStatusCommand(Collection<ServerPlayerEntity> players, CommandContext<ServerCommandSource> context) {
        try {
            for (ServerPlayerEntity player : players) {
                double hp = player.getHealth();
                int hunger = player.getHungerManager().getFoodLevel();
                double saturation = player.getHungerManager().getSaturationLevel();
                int level = player.experienceLevel;
                final Text name = player.getDisplayName();
                final String hpStr = String.format("%.2f", hp);
                final String hungerStr = String.valueOf(hunger);
                final String saturationStr = String.format("%.2f", saturation);
                final String levelStr = String.valueOf(level);
                context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.status", name, hpStr, hungerStr, saturationStr, levelStr), false);
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f get status", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return players.size();
    }

    private int runShareStatusCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            double hp = player.getHealth();
            int hunger = player.getHungerManager().getFoodLevel();
            double saturation = player.getHungerManager().getSaturationLevel();
            int level = player.experienceLevel;
            final Text name = player.getDisplayName();
            final String hpStr = String.format("%.2f", hp);
            final String hungerStr = String.valueOf(hunger);
            final String saturationStr = String.format("%.2f", saturation);
            final String levelStr = String.valueOf(level);
            MutableText text = Util.parseTranslateableText("fmod.command.share.status", name, hpStr, hungerStr, saturationStr, levelStr);
            Util.broadcastTextMessage(context.getSource().getServer(), text);
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f share status", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private MutableText formatInventoryItemStack(ItemStack item) {
        MutableText itemText = Text.empty();
        try {
            if (item == null || item.isEmpty()) {
                itemText = Text.literal("00").formatted(Formatting.GRAY).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.command.get.emptyslot")))
                );
            } else if (item.getCount() < 100) {
                String itemCount = String.format("%02d", item.getCount());
                itemText = Text.literal(itemCount).formatted(Formatting.AQUA).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(item)))
                );
            } else {
                itemText = Text.literal("9+").formatted(Formatting.AQUA).styled(s -> s
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(item)))
                );
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when formatting item stack", e);
            itemText = Text.literal("??").formatted(Formatting.RED);
        }
        return itemText;
    }

    private List<MutableText> getInventoryTexts(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        // Text Structure:
        // [x] [x] [x] [x] [-] [-] [S] [+] [1]   (x: Armor, -: Placeholder, +: Offhand, 1: Current Chosen Slot Index)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, S: Survival Gamemode [S: Survival, C: Creative, A: Adventure, V: Spectator])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '+' symbol formatting: [Has Item: Formatting.AQUA, Empty Slot: Formatting.GRAY])
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Inventory, '[]' bracket formmating: Formatting.GREEN)
        // [+] [+] [+] [+] [+] [+] [+] [+] [+]   (+: Hotbar, '[]' bracket formmating: [Selected: Formatting.GOLD, Other: Formatting.LIGHT_PURPLE])
        MutableText armorText = Text.empty();
        for (int i = 0; i < 4; i++) {
            ItemStack item = inventory.getArmorStack(i);
            armorText.append(Text.literal("[").formatted(Formatting.LIGHT_PURPLE));
            armorText.append(formatInventoryItemStack(item));
            armorText.append(Text.literal("]").formatted(Formatting.LIGHT_PURPLE));
            armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        }
        // Placeholder
        for (int i = 0; i < 2; i++) {
            armorText.append(Text.literal("[--]").formatted(Formatting.GRAY));
            armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        }
        // Gamemode
        armorText.append(Text.literal("[").formatted(Formatting.GOLD));
        GameMode gamemode = player.interactionManager.getGameMode();
        MutableText gamemodeText = Text.literal("+S");
        if (gamemode == GameMode.CREATIVE) {
            gamemodeText = Text.literal("+C");
        } else if (gamemode == GameMode.ADVENTURE) {
            gamemodeText = Text.literal("+A");
        } else if (gamemode == GameMode.SPECTATOR) {
            gamemodeText = Text.literal("+V");
        }
        gamemodeText = gamemodeText.formatted(Formatting.RED).styled(s -> s
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("gameMode." + gamemode.getName())))
        );
        armorText.append(gamemodeText);
        armorText.append(Text.literal("]").formatted(Formatting.GOLD));
        armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        // Offhand
        ItemStack offhandItem = inventory.getStack(PlayerInventory.OFF_HAND_SLOT);
        armorText.append(Text.literal("[").formatted(Formatting.LIGHT_PURPLE));
        armorText.append(formatInventoryItemStack(offhandItem));
        armorText.append(Text.literal("]").formatted(Formatting.LIGHT_PURPLE));
        armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        // Selected Slot
        armorText.append(Text.literal("[").formatted(Formatting.GOLD));
        armorText.append(Text.literal("0" + String.valueOf(inventory.selectedSlot + 1)).formatted(Formatting.RED));
        armorText.append(Text.literal("]").formatted(Formatting.GOLD));
        armorText.append(Text.literal(" ").formatted(Formatting.RESET));
        // Inventory
        MutableText[] inventoryText = {Text.empty(), Text.empty(), Text.empty()};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                // Index 0 ~ 8 belongs to Hotbar, Index 9 ~ 35 belongs to Inventory
                int index = (i + 1) * 9 + j;
                ItemStack item = inventory.getStack(index);
                inventoryText[i].append(Text.literal("[").formatted(Formatting.GREEN));
                inventoryText[i].append(formatInventoryItemStack(item));
                inventoryText[i].append(Text.literal("]").formatted(Formatting.GREEN));
                inventoryText[i].append(Text.literal(" ").formatted(Formatting.RESET));
            }
        }
        // Hotbar
        MutableText hotbarText = Text.empty();
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getStack(i);
            if (i == inventory.selectedSlot) {
                hotbarText.append(Text.literal("[").formatted(Formatting.GOLD));
            } else {
                hotbarText.append(Text.literal("[").formatted(Formatting.LIGHT_PURPLE));
            }
            hotbarText.append(formatInventoryItemStack(item));
            if (i == inventory.selectedSlot) {
                hotbarText.append(Text.literal("]").formatted(Formatting.GOLD));
            } else {
                hotbarText.append(Text.literal("]").formatted(Formatting.LIGHT_PURPLE));
            }
            hotbarText.append(Text.literal(" ").formatted(Formatting.RESET));
        }
        // Feedback
        final MutableText linea = armorText;
        final MutableText lineb = inventoryText[0];
        final MutableText linec = inventoryText[1];
        final MutableText lined = inventoryText[2];
        final MutableText linee = hotbarText;
        return Arrays.asList(linea, lineb, linec, lined, linee);
    }

    private int runGetInventoryCommand(ServerPlayerEntity player, CommandContext<ServerCommandSource> context) {
        try {
            List<MutableText> inventoryText = getInventoryTexts(player);
            final Text name = player.getDisplayName();
            final Text title = Util.parseTranslateableText("fmod.command.get.inventory", name);
            context.getSource().sendFeedback(() -> title, false);
            for (MutableText text : inventoryText) {
                context.getSource().sendFeedback(() -> text, false);
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f get inventory", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runShareInventoryCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            List<MutableText> inventoryText = getInventoryTexts(player);
            final Text name = player.getDisplayName();
            final Text title = Util.parseTranslateableText("fmod.command.share.inventory", name);
            Util.broadcastTextMessage(context.getSource().getServer(), title);
            for (MutableText text : inventoryText) {
                Util.broadcastTextMessage(context.getSource().getServer(), text);
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f share inventory", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runGetItemCommand(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
        int result = 0;
        try {
            for (Entity entity : entities) {
                Iterable<ItemStack> items = entity.getHandItems();
                if (items == null || items.iterator().hasNext() == false) {
                    final Text name = entity.getDisplayName();
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.noitem", name), false);
                    continue;
                }
                MutableText itemList = Text.empty();
                int itemCountSum = 0;
                for (ItemStack item : items) {
                    if (item.isEmpty()) {
                        continue;
                    }
                    Text itemText = item.toHoverableText();
                    int itemCount = item.getCount();
                    result += itemCount;
                    itemCountSum += itemCount;
                    itemList.append(itemText);
                    if (itemCount > 1) {
                        itemList.append(Text.literal("x" + itemCount + " "));
                    } else {
                        itemList.append(Text.literal(" "));
                    }
                }
                final Text name = entity.getDisplayName();
                final MutableText itemTxt = itemList;
                if (itemCountSum <= 0) {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.noitem", name), false);
                } else {
                    context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.get.item", name).append(itemTxt), false);
                }
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f get item", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private int runShareItemCommand(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = getShareCommandExecutor(context);
            Iterable<ItemStack> items = player.getHandItems();
            if (items == null || items.iterator().hasNext() == false) {
                final Text name = player.getDisplayName();
                Util.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslateableText("fmod.command.share.noitem", name));
                return Command.SINGLE_SUCCESS;
            }
            MutableText itemList = Text.empty();
            int itemCountSum = 0;
            for (ItemStack item : items) {
                if (item.isEmpty()) {
                    continue;
                }
                Text itemText = item.toHoverableText();
                int itemCount = item.getCount();
                itemCountSum += itemCount;
                itemList.append(itemText);
                if (itemCount > 1) {
                    itemList.append(Text.literal("x" + itemCount + " "));
                } else {
                    itemList.append(Text.literal(" "));
                }
            }
            final Text name = player.getDisplayName();
            final MutableText itemTxt = itemList;
            if (itemCountSum <= 0) {
                Util.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslateableText("fmod.command.share.noitem", name));
            } else {
                Util.broadcastTextMessage(context.getSource().getServer(), Util.parseTranslateableText("fmod.command.share.item", name).append(itemTxt));
            }
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            logger.error("FMinectaftMod: Caught unexpected exception when executing command /f share item", e);
            throw new CommandException(Util.parseTranslateableText("fmod.command.share.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int runReloadCommand(CommandContext<ServerCommandSource> context) {
        try {
            Util.loadServerConfig();
            context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.reload.success"), true);
        } catch (Exception e) {
            if (e instanceof CommandException) {
                throw (CommandException) e;
            }
            throw new CommandException(Util.parseTranslateableText("fmod.command.reload.error"));
        }
        return Command.SINGLE_SUCCESS;
    }

    public boolean registerCommand() {
        try {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                final LiteralCommandNode<ServerCommandSource> fModCommandNode = dispatcher.register(CommandManager.literal("fminecraftmod")
                    .requires(source -> source.hasPermissionLevel(0))
                    .executes(context -> {return runFModCommand(context);})
                    .then(CommandManager.literal("dev")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {return runDevCommand(context);})
                    )
                    .then(CommandManager.literal("gpt")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(CommandManager.literal("new")
                            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> {return runGptNewCommand(StringArgumentType.getString(context, "message"), context);})
                            ))
                        .then(CommandManager.literal("reply")
                            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> {return runGptReplyCommand(StringArgumentType.getString(context, "message"), context);})
                            ))
                        .then(CommandManager.literal("regenerate").executes(context -> {return runGptRegenerateCommand(context);}))
                        .then(CommandManager.literal("edit")
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                    .executes(context -> {return runGptEditCommand(IntegerArgumentType.getInteger(context, "index"), StringArgumentType.getString(context, "message"), context);})
                                )
                            )
                        )
                        .then(CommandManager.literal("history")
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> {return runGptHistoryCommand(IntegerArgumentType.getInteger(context, "index"), context);})
                            )
                            .executes(context -> {return runGptHistoryCommand(0, context);})
                        )
                    )
                    .then(CommandManager.literal("song")
                        .requires(source -> source.hasPermissionLevel(3))
                        .then(CommandManager.literal("play")
                            .then(CommandManager.argument("player", EntityArgumentType.players())
                                .then(CommandManager.argument("song", StringArgumentType.greedyString())
                                    .suggests(SongFileSuggestion.suggest())
                                    .executes(context -> {return runSongPlayCommand(EntityArgumentType.getPlayers(context, "player"), StringArgumentType.getString(context, "song"), context);})
                                )
                            )
                        )
                        .then(CommandManager.literal("cancel")
                            .then(CommandManager.argument("player", EntityArgumentType.players())
                                .executes(context -> {return runSongCancelCommand(EntityArgumentType.getPlayers(context, "player"), context);})
                            )
                        )
                        .then(CommandManager.literal("get")
                            .then(CommandManager.argument("player", EntityArgumentType.players())
                                .executes(context -> {return runSongGetCommand(EntityArgumentType.getPlayers(context, "player"), context);})
                            )
                        )
                        .then(CommandManager.literal("search")
                            .then(CommandManager.argument("player", EntityArgumentType.players())
                                .then(CommandManager.argument("timepoint", DoubleArgumentType.doubleArg(0.0))
                                    .executes(context -> {return runSongSearchCommand(EntityArgumentType.getPlayers(context, "player"), DoubleArgumentType.getDouble(context, "timepoint"), context);})
                                )
                            )
                        )
                        .then(CommandManager.literal("speed")
                            .then(CommandManager.argument("player", EntityArgumentType.players())
                                .then(CommandManager.argument("speed", DoubleArgumentType.doubleArg())
                                    .executes(context -> {return runSongSpeedCommand(EntityArgumentType.getPlayers(context, "player"), DoubleArgumentType.getDouble(context, "speed"), context);})
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("get")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("coord")
                            .then(CommandManager.argument("entity", EntityArgumentType.entities())
                                .executes(context -> {return runGetCoordCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                            )
                        )
                        .then(CommandManager.literal("distance")
                            .then(CommandManager.argument("entity", EntityArgumentType.entities())
                                .executes(context -> {return runGetDistanceCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                            )
                        )
                        .then(CommandManager.literal("health")
                            .then(CommandManager.argument("entity", EntityArgumentType.entities())
                                .executes(context -> {return runGetHealthCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                            )
                        )
                        .then(CommandManager.literal("status")
                            .then(CommandManager.argument("player", EntityArgumentType.players())
                                .executes(context -> {return runGetStatusCommand(EntityArgumentType.getPlayers(context, "player"), context);})
                            )
                        )
                        .then(CommandManager.literal("inventory")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> {return runGetInventoryCommand(EntityArgumentType.getPlayer(context, "player"), context);})
                            )
                        )
                        .then(CommandManager.literal("item")
                            .then(CommandManager.argument("entity", EntityArgumentType.entities())
                                .executes(context -> {return runGetItemCommand(EntityArgumentType.getEntities(context, "entity"), context);})
                            )
                        )
                    )
                    .then(CommandManager.literal("share")
                        .requires(source -> source.hasPermissionLevel(0))
                        .then(CommandManager.literal("coord").executes(context -> {return runShareCoordCommand(context);}))
                        .then(CommandManager.literal("distance").executes(context -> {return runShareDistanceCommand(context);}))
                        .then(CommandManager.literal("health").executes(context -> {return runShareHealthCommand(context);}))
                        .then(CommandManager.literal("status").executes(context -> {return runShareStatusCommand(context);}))
                        .then(CommandManager.literal("inventory").executes(context -> {return runShareInventoryCommand(context);}))
                        .then(CommandManager.literal("item").executes(context -> {return runShareItemCommand(context);}))
                    )
                    .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {return runReloadCommand(context);})
                    )
                    .then(CommandManager.literal("options")
                        .requires(source -> source.hasPermissionLevel(4))
                        .then(CommandManager.literal("serverTranslation")
                            .then(CommandManager.argument("enable", BoolArgumentType.bool())
                                .executes(context -> {return runOptionsCommand("serverTranslation", BoolArgumentType.getBool(context, "enable"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("serverTranslation", null, context);})
                        )
                        .then(CommandManager.literal("entityDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("entityDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("entityDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("bossDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("bossDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("namedMobDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("namedMobDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("namedMobDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("killerDeathMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("killerDeathMessage", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("killerDeathMessage", null, context);})
                        )
                        .then(CommandManager.literal("bossMaxHealthThreshold")
                            .then(CommandManager.argument("HP", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", DoubleArgumentType.getDouble(context, "HP"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossMaxHealthThreshold", null, context);})
                        )
                        .then(CommandManager.literal("playerDeathCoord")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("playerDeathCoord", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("playerDeathCoord", null, context);})
                        )
                        .then(CommandManager.literal("projectileHitsEntity")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("projectileHitsEntity", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("projectileHitsEntity", null, context);})
                        )
                        .then(CommandManager.literal("projectileBeingHit")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("projectileBeingHit", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("projectileBeingHit", null, context);})
                        )
                        .then(CommandManager.literal("informAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("informAFK", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("informAFK", null, context);})
                        )
                        .then(CommandManager.literal("informAFKThreshold")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("informAFKThreshold", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("informAFKThreshold", null, context);})
                        )
                        .then(CommandManager.literal("broadcastAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("broadcastAFK", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("broadcastAFK", null, context);})
                        )
                        .then(CommandManager.literal("broadcastAFKThreshold")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("broadcastAFKThreshold", null, context);})
                        )
                        .then(CommandManager.literal("backFromAFK")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("backFromAFK", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("backFromAFK", null, context);})
                        )
                        .then(CommandManager.literal("biomeChangeMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("biomeChangeMessage", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("biomeChangeMessage", null, context);})
                        )
                        .then(CommandManager.literal("biomeChangeDelay")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("biomeChangeDelay", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("biomeChangeDelay", null, context);})
                        )
                        .then(CommandManager.literal("bossFightMessageLocation")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("bossFightMessageLocation", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("bossFightMessageLocation", null, context);})
                        )
                        .then(CommandManager.literal("bossFightMessageReceiver")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("bossFightMessageReceiver", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("bossFightMessageReceiver", null, context);})
                        )
                        .then(CommandManager.literal("bossFightMessageInterval")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("bossFightMessageInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("bossFightMessageInterval", null, context);})
                        )
                        .then(CommandManager.literal("monsterSurroundMessageLocation")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageLocation", null, context);})
                        )
                        .then(CommandManager.literal("monsterSurroundMessageReceiver")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageReceiver", null, context);})
                        )
                        .then(CommandManager.literal("monsterSurroundMessageInterval")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("monsterSurroundMessageInterval", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("monsterSurroundMessageInterval", null, context);})
                        )
                        .then(CommandManager.literal("monsterNumberThreshold")
                            .then(CommandManager.argument("num", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("monsterNumberThreshold", IntegerArgumentType.getInteger(context, "num"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("monsterNumberThreshold", null, context);})
                        )
                        .then(CommandManager.literal("monsterDistanceThreshold")
                            .then(CommandManager.argument("meters", DoubleArgumentType.doubleArg(0))
                                .executes(context -> {return runOptionsCommand("monsterDistanceThreshold", DoubleArgumentType.getDouble(context, "meters"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("monsterDistanceThreshold", null, context);})
                        )
                        .then(CommandManager.literal("entityNumberWarning")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.NONE, context);}))
                            .then(CommandManager.literal("chat").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.CHAT, context);}))
                            .then(CommandManager.literal("actionbar").executes(context -> {return runOptionsCommand("entityNumberWarning", MessageLocation.ACTIONBAR, context);}))
                            .executes(context -> {return runOptionsCommand("entityNumberWarning", null, context);})
                        )
                        .then(CommandManager.literal("entityNumberThreshold")
                            .then(CommandManager.argument("num", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("entityNumberThreshold", IntegerArgumentType.getInteger(context, "num"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("entityNumberThreshold", null, context);})
                        )
                        .then(CommandManager.literal("entityNumberCheckInterval")
                            .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1))
                                .executes(context -> {return runOptionsCommand("entityNumberCheckInterval", IntegerArgumentType.getInteger(context, "ticks"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("entityNumberCheckInterval", null, context);})
                        )
                        .then(CommandManager.literal("playerHurtMessage")
                            .then(CommandManager.literal("off").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.NONE, context);}))
                            .then(CommandManager.literal("all").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.ALL, context);}))
                            .then(CommandManager.literal("ops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.OP, context);}))
                            .then(CommandManager.literal("selfops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.SELFOP, context);}))
                            .then(CommandManager.literal("teamops").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.TEAMOP, context);}))
                            .then(CommandManager.literal("team").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.TEAM, context);}))
                            .then(CommandManager.literal("self").executes(context -> {return runOptionsCommand("playerHurtMessage", MessageReceiver.SELF, context);}))
                            .executes(context -> {return runOptionsCommand("playerHurtMessage", null, context);})
                        )
                        .then(CommandManager.literal("hugeDamageThreshold")
                            .then(CommandManager.argument("percentage", DoubleArgumentType.doubleArg(0, 100))
                                .executes(context -> {return runOptionsCommand("hugeDamageThreshold", DoubleArgumentType.getDouble(context, "percentage"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("hugeDamageThreshold", null, context);})
                        )
                        .then(CommandManager.literal("gptUrl")
                            .then(CommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptUrl", StringArgumentType.getString(context, "url"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptUrl", null, context);})
                        )
                        .then(CommandManager.literal("gptAccessTokens")
                            .then(CommandManager.argument("tokens", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptAccessTokens", StringArgumentType.getString(context, "tokens"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptAccessTokens", null, context);})
                        )
                        .then(CommandManager.literal("gptModel")
                            .then(CommandManager.argument("model", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptModel", StringArgumentType.getString(context, "model"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptModel", null, context);})
                        )
                        .then(CommandManager.literal("gptSystemPrompts")
                            .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                                .executes(context -> {return runOptionsCommand("gptSystemPrompts", StringArgumentType.getString(context, "prompt"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptSystemPrompts", null, context);})
                        )
                        .then(CommandManager.literal("gptTemperature")
                            .then(CommandManager.argument("temperature", DoubleArgumentType.doubleArg(0, 1))
                                .executes(context -> {return runOptionsCommand("gptTemperature", DoubleArgumentType.getDouble(context, "temperature"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptTemperature", null, context);})
                        )
                        .then(CommandManager.literal("gptTimeout")
                            .then(CommandManager.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> {return runOptionsCommand("gptTimeout", IntegerArgumentType.getInteger(context, "seconds"), context);})
                            )
                            .executes(context -> {return runOptionsCommand("gptTimeout", null, context);})
                        )
                    )
                );
                dispatcher.register(CommandManager.literal("f")
                    .requires(source -> source.hasPermissionLevel(0))
                    .executes(context -> {return runFModCommand(context);})
                    .redirect(fModCommandNode)
                );
            });

            return true;
        } catch (Exception e) {
            logger.error("FMinectaftMod: Unable to register command.", e);
            return false;
        }
    }

    private int runOptionsCommand(String options, Object value, CommandContext<ServerCommandSource> context) {
        try {
            switch (options) {
                case "serverTranslation":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.translate", Util.serverConfig.isEnableServerTranslation()), false);
                    } else {
                        Util.serverConfig.setEnableServerTranslation((boolean) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.translate", value), true);
                    }
                    break;
                case "entityDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entdeathmsg", text), false);
                    } else {
                        Util.serverConfig.setEntityDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entdeathmsg", text), true);
                    }
                    break;
                case "bossDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcbossdeath", text), false);
                    } else {
                        Util.serverConfig.setBossDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcbossdeath", text), true);
                    }
                    break;
                case "namedMobDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getNamedEntityDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.nameddeath", text), false);
                    } else {
                        Util.serverConfig.setNamedEntityDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.nameddeath", text), true);
                    }
                    break;
                case "killerDeathMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getKillerEntityDeathMessage());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bckillerdeath", text), false);
                    } else {
                        Util.serverConfig.setKillerEntityDeathMessage((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bckillerdeath", text), true);
                    }
                    break;
                case "bossMaxHealthThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossmaxhp", Util.serverConfig.getBossMaxHpThreshold()), false);
                    } else {
                        Util.serverConfig.setBossMaxHpThreshold((double) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossmaxhp", value), true);
                    }
                    break;
                case "playerDeathCoord":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerDeathCoord());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcdeathcoord", text), false);
                    } else {
                        Util.serverConfig.setPlayerDeathCoord((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcdeathcoord", text), true);
                    }
                    break;
                case "projectileHitsEntity":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileHitOthers());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.projhitting", text), false);
                    } else {
                        Util.serverConfig.setProjectileHitOthers((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.projhitting", text), true);
                    }
                    break;
                case "projectileBeingHit":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getProjectileBeingHit());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.projbeinghit", text), false);
                    } else {
                        Util.serverConfig.setProjectileBeingHit((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.projbeinghit", text), true);
                    }
                    break;
                case "informAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getInformAfking());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.informafk", text), false);
                    } else {
                        Util.serverConfig.setInformAfking((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.informafk", text), true);
                    }
                    break;
                case "informAFKThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.informafkthres", String.format("%.2f", Util.serverConfig.getInformAfkingThreshold() / 20.0)), false);
                    } else {
                        Util.serverConfig.setInformAfkingThreshold((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.informafkthres", value), true);
                    }
                    break;
                case "broadcastAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBroadcastAfking());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcafk", text), false);
                    } else {
                        Util.serverConfig.setBroadcastAfking((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcafk", text), true);
                    }
                    break;
                case "broadcastAFKThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bcafkthres", String.format("%.2f", Util.serverConfig.getBroadcastAfkingThreshold() / 20.0)), false);
                    } else {
                        Util.serverConfig.setBroadcastAfkingThreshold((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bcafkthres", value), true);
                    }
                    break;
                case "backFromAFK":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getStopAfking());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.stopafk", text), false);
                    } else {
                        Util.serverConfig.setStopAfking((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.stopafk", text), true);
                    }
                    break;
                case "biomeChangeMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getChangeBiome());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.changebiome", text), false);
                    } else {
                        Util.serverConfig.setChangeBiome((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.changebiome", text), true);
                    }
                    break;
                case "biomeChangeDelay":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.biomedelay", String.format("%.2f", Util.serverConfig.getChangeBiomeDelay() / 20.0)), false);
                    } else {
                        Util.serverConfig.setChangeBiomeDelay((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.biomedelay", value), true);
                    }
                    break;
                case "bossFightMessageLocation":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getBossFightMessageLocation());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightloc", text), false);
                    } else {
                        Util.serverConfig.setBossFightMessageLocation((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossfightloc", text), true);
                    }
                    break;
                case "bossFightMessageReceiver":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getBossFightMessageReceiver());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightreceiver", text), false);
                    } else {
                        Util.serverConfig.setBossFightMessageReceiver((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossfightreceiver", text), true);
                    }
                    break;
                case "bossFightMessageInterval":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.bossfightinterval", String.format("%.2f", Util.serverConfig.getBossFightInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setBossFightInterval((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.bossfightinterval", value), true);
                    }
                    break;
                case "monsterSurroundMessageLocation":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getMonsterSurroundMessageLocation());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterloc", text), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundMessageLocation((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterloc", text), true);
                    }
                    break;
                case "monsterSurroundMessageReceiver":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getMonsterSurroundMessageReceiver());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterreceiver", text), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundMessageReceiver((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterreceiver", text), true);
                    }
                    break;
                case "monsterSurroundMessageInterval":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterinterval", String.format("%.2f", Util.serverConfig.getMonsterSurroundInterval() / 20.0)), false);
                    } else {
                        Util.serverConfig.setMonsterSurroundInterval((int) value * 20);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterinterval", value), true);
                    }
                    break;
                case "monsterNumberThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsternumber", Util.serverConfig.getMonsterNumberThreshold()), false);
                    } else {
                        Util.serverConfig.setMonsterNumberThreshold((int) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsternumber", value), true);
                    }
                    break;
                case "monsterDistanceThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.monsterdistance", Util.serverConfig.getMonsterDistanceThreshold()), false);
                    } else {
                        Util.serverConfig.setMonsterDistanceThreshold((double) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.monsterdistance", value), true);
                    }
                    break;
                case "entityNumberWarning":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageLocationI18n(Util.serverConfig.getEntityNumberWarning());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entitywarning", text), false);
                    } else {
                        Util.serverConfig.setEntityNumberWarning((MessageLocation) value);
                        final MutableText text = EnumI18n.getMessageLocationI18n((MessageLocation) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entitywarning", text), true);
                    }
                    break;
                case "entityNumberThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entitynumber", Util.serverConfig.getEntityNumberThreshold()), false);
                    } else {
                        Util.serverConfig.setEntityNumberThreshold((int) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entitynumber", value), true);
                    }
                    break;
                case "entityNumberCheckInterval":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.entityinterval", Util.serverConfig.getEntityNumberInterval()), false);
                    } else {
                        Util.serverConfig.setEntityNumberInterval((int) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.entityinterval", value), true);
                    }
                    break;
                case "playerHurtMessage":
                    if (value == null) {
                        final MutableText text = EnumI18n.getMessageReceiverI18n(Util.serverConfig.getPlayerSeriousHurt());
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.playerhurt", text), false);
                    } else {
                        Util.serverConfig.setPlayerSeriousHurt((MessageReceiver) value);
                        final MutableText text = EnumI18n.getMessageReceiverI18n((MessageReceiver) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.playerhurt", text), true);
                    }
                    break;
                case "hugeDamageThreshold":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.damagethres", Util.serverConfig.getPlayerHurtThreshold() * 100.0), false);
                    } else {
                        Util.serverConfig.setPlayerHurtThreshold((double) value / 100.0);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.damagethres", value), true);
                    }
                    break;
                case "gptUrl":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpturl", Util.serverConfig.getGptUrl()), false);
                    } else {
                        try {
                            new URI((String) value).toURL();
                        } catch (Exception e) {
                            throw new CommandException(Util.parseTranslateableText("fmod.command.options.invalidurl", value));
                        }
                        Util.serverConfig.setGptUrl((String) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpturl", value), true);
                    }
                    break;
                case "gptAccessTokens":
                    if (value == null) {
                        final String secureTokens = Util.serverConfig.getSecureGptAccessTokens();
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gptkey", secureTokens), false);
                    } else {
                        String token = (String) value;
                        Util.serverConfig.setGptAccessTokens(token);
                        // For security reasons, we don't want to show the full token in the log, only show the first 5 and the last 5 characters
                        final String secureTokens = Util.serverConfig.getSecureGptAccessTokens();
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gptkey", secureTokens), true);
                    }
                    break;
                case "gptModel":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gptmodel", Util.serverConfig.getGptModel()), false);
                    } else {
                        Util.serverConfig.setGptModel((String) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gptmodel", value), true);
                    }
                    break;
                case "gptSystemPrompts":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gptsysprompt", Util.serverConfig.getGptSystemPrompt()), false);
                    } else {
                        Util.serverConfig.setGptSystemPrompt((String) value);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gptsysprompt", value), true);
                    }
                    break;
                case "gptTemperature":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpttemperature", Util.serverConfig.getGptTemperature()), false);
                    } else {
                        double temperature = (double) value;
                        Util.serverConfig.setGptTemperature(temperature);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpttemperature", value), true);
                    }
                    break;
                case "gptTimeout":
                    if (value == null) {
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.get.gpttimeout", (int) (Util.serverConfig.getGptServerTimeout() / 1000)), false);
                    } else {
                        int timeout = (int) value;
                        Util.serverConfig.setGptServerTimeout(timeout * 1000);
                        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.options.gpttimeout", value), true);
                    }
                    break;
                default:
                    throw new CommandException(Util.parseTranslateableText("fmod.command.options.unknownoption", options));
            }
            if (value != null) {
                Util.saveServerConfig();
            }
        } catch (ClassCastException e) {
            throw new CommandException(Util.parseTranslateableText("fmod.command.options.classcast", value, options, e.getMessage()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
