/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.command;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.schedule.PlaySong;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.song.NbsSongDecoder;
import com.ykn.fmod.server.base.song.NoteBlockSong;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;

public class SongCommand {

    private static int runSongPlayCommand(Collection<ServerPlayer> players, String songName, CommandContext<CommandSourceStack> context) {
        try {
            // Refresh song suggestion list
            SongFileSuggestion.suggest();
            if (SongFileSuggestion.getAvailableSongs() == 0) {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.hint"), false);
            }
            Path songFolder = FMLPaths.CONFIGDIR.get().resolve(Util.MODID).normalize();
            Path songPath = songFolder.resolve(songName).normalize();
            if (!songPath.startsWith(songFolder)) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.song.filenotfound", songName));
            }

            // Load song
            NoteBlockSong song = null;
            try (FileInputStream fileInputStream = new FileInputStream(songPath.toFile())) {
                song = NbsSongDecoder.parse(fileInputStream);
            }
            if (song == null) {
                throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.song.ioexception", songName));
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
                        if (playSong.getTarget().getUUID().equals(player.getUUID())) {
                            playSong.cancel();
                        }
                    }
                }
            }
            // Submit song task
            for (ServerPlayer player : players) {
                PlaySong playSong = new PlaySong(song, songName, player, context);
                Util.getServerData(context.getSource().getServer()).submitScheduledTask(playSong);
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.start", player.getDisplayName(), songName), true);
            }
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (FileNotFoundException fileNotFoundException) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.song.filenotfound", songName));
        } catch (EOFException eofException) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.song.eofexception", songName));
        } catch (IOException ioException) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.song.ioexception", songName));
        } catch (Exception exception) {
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.song.error", songName));
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
                    if (playSong.getTarget().getUUID().equals(player.getUUID())) {
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
                playSong.setContext(context);
                playSong.cancel();
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f song cancel", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
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
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.get", player.getDisplayName(), playSong.getSongName(), currentTimeStr, totalTimeStr, speedStr), false);
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f song get", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongShowInfoCommand(Collection<ServerPlayer> players, boolean showInfo, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                playSong.setShowInfo(showInfo);
                if (showInfo) {
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.show", player.getDisplayName(), playSong.getSongName()), true);
                } else {
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.hide", player.getDisplayName(), playSong.getSongName()), true);
                }
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f song showinfo", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    private static int runSongShowInfoCommand(Collection<ServerPlayer> players, CommandContext<CommandSourceStack> context) {
        int result = 0;
        try {
            result = doSongTaskOrDefault(players, context, (player, playSong) -> {
                MutableComponent isShowInfo = Util.getBooleanText(playSong.isShowInfo());
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.status", player.getDisplayName(), playSong.getSongName(), isShowInfo), false);
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f song showinfo", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
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
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.long", player.getDisplayName(), songName, songLengthStr, timepointStr), false);
                } else {
                    playSong.seek((int) (timepoint * 20));
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.search", player.getDisplayName(), songName, timepointStr, songLengthStr), true);
                    return true;
                }
                return false;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f song search", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
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
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.pause", playerName, songName), true);
                } else {
                    context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.speed", playerName, songName, speedStr), true);
                }
                return true;
            }, player -> {
                context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.empty", player.getDisplayName()), false);
                return false;
            });
        } catch (CommandRuntimeException e) {
            throw e;
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Caught unexpected exception when executing command /f song speed", e);
            throw new CommandRuntimeException(Util.parseTranslatableText("fmod.command.unknownerror"));
        }
        return result;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("song")
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
            );
    }
    
}
