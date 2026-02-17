/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.song.NoteBlockNote;
import com.ykn.fmod.server.base.song.NoteBlockSong;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Represents a scheduled task for playing a NoteBlock song to a player.
 * This class manages the playback of songs, including tick-based note triggering,
 * speed control, seeking, and displaying song information to the player.
 */
public class PlaySong extends ScheduledTask {

    /**
     * The NoteBlock song to be played.
     */
    private NoteBlockSong song;
    
    /**
     * The name of the song being played.
     */
    private String songName;
    
    /**
     * The player who is receiving the song playback.
     */
    private ServerPlayer target;
    
    /**
     * The command context from which this song playback was initiated.
     */
    private CommandContext<CommandSourceStack> context;
    
    /**
     * The current tick position in the song.
     */
    private int tick;
    
    /**
     * Whether to display song information to the player.
     */
    private boolean showInfo;
    
    /**
     * The last second value when song information was displayed.
     */
    private int lastShowInfoSeconds;
    
    /**
     * The last tick value when song information was displayed.
     */
    private int lastShowInfoTicks;
    
    /**
     * The last speed value when song information was displayed.
     */
    private double lastShowInfoSpeed;

    /**
     * Constructs a new PlaySong task for playing a song to a player.
     *
     * @param song The NoteBlock song to be played.
     * @param songName The name of the song.
     * @param target The player who will receive the song playback.
     * @param context The command context from which this playback was initiated.
     */
    public PlaySong(NoteBlockSong song, String songName, ServerPlayer target, CommandContext<CommandSourceStack> context) {
        super(1, song.getMaxRealTick() == 2147483647 ? 2147483647 : song.getMaxRealTick() + 1);
        this.song = song;
        this.songName = songName;
        this.target = target;
        this.context = context;
        this.tick = 0;
        this.showInfo = false;
        this.lastShowInfoSeconds = 0;
        this.lastShowInfoTicks = 0;
        this.lastShowInfoSpeed = 1.0;
    }

    /**
     * Called on each tick to play the notes scheduled for the current tick.
     * Also handles displaying song information to the player if enabled.
     */
    @Override
    public void onTick() {
        List<NoteBlockNote> notes = song.getNotes(this.tick);
        if (notes != null) {
            for (NoteBlockNote note : notes) {
                // target.playSound(note.instrument.getSound().value(), 2f, (float) Math.pow(2.0, (note.noteLevel - 12) / 12.0));
                target.connection.send(new ClientboundSoundPacket(note.instrument.getSoundEvent(), target.getSoundSource(), target.getX(), target.getY(), target.getZ(), 2f, (float) Math.pow(2.0, (note.noteLevel - 12) / 12.0), 0));
            }
        }
        int currentSeconds = (int) (this.song.getVirtualTick(this.tick) / 20.0);
        if (this.showInfo) {
            if (currentSeconds != this.lastShowInfoSeconds || Math.abs(this.tick - this.lastShowInfoTicks) > 40 || this.song.getSpeed() != this.lastShowInfoSpeed) {
                this.lastShowInfoSeconds = currentSeconds;
                this.lastShowInfoTicks = this.tick;
                this.lastShowInfoSpeed = this.song.getSpeed();
                String currentTimeStr = Integer.toString(currentSeconds);
                String totalTimeStr = Integer.toString((int) (this.song.getMaxVirtualTick() / 20.0));
                String speedStr = String.format("%.2f", this.song.getSpeed());
                Util.sendActionBarMessage(target, Util.parseTranslatableText("fmod.command.song.info", songName, currentTimeStr, totalTimeStr, speedStr));
            }
        }
        if (this.getSong().getSpeed() != 0) {
            this.tick++;
        }
    }

    @Override
    public void onCancel() {
        this.tick = song.getMaxRealTick();
        if (context.getSource().isPlayer()) {
            if (context.getSource().getPlayer() == null || context.getSource().getPlayer().hasDisconnected()) {
                LoggerFactory.getLogger(Util.LOGGERNAME).info(Util.parseTranslatableText("fmod.command.song.cancel", target.getDisplayName(), this.songName).getString());
                return;
            }
        }
        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.cancel", target.getDisplayName(), this.songName), true);
    }

    @Override
    public void onFinish() {
        if (context.getSource().isPlayer()) {
            if (context.getSource().getPlayer() == null || context.getSource().getPlayer().hasDisconnected()) {
                LoggerFactory.getLogger(Util.LOGGERNAME).info(Util.parseTranslatableText("fmod.command.song.finish", target.getDisplayName(), this.songName).getString());
                return;
            }
        }
        context.getSource().sendSuccess(() -> Util.parseTranslatableText("fmod.command.song.finish", target.getDisplayName(), this.songName), true);
    }

    @Override
    public boolean shouldCancel() {
        return target == null || target.hasDisconnected() || target.isRemoved() || target.getHealth() <= 0;
    }

    /**
     * Gets the NoteBlock song being played.
     *
     * @return The NoteBlock song.
     */
    public NoteBlockSong getSong() {
        return song;
    }

    /**
     * Gets the name of the song being played.
     *
     * @return The song name.
     */
    public String getSongName() {
        return songName;
    }

    /**
     * Gets the player receiving the song playback.
     *
     * @return The target player.
     */
    public ServerPlayer getTarget() {
        return target;
    }

    /**
     * Gets the current tick position in the song.
     *
     * @return The current tick.
     */
    public int getTick() {
        return tick;
    }

    /**
     * Checks whether song information is being displayed to the player.
     *
     * @return {@code true} if song information is being shown; {@code false} otherwise.
     */
    public boolean isShowInfo() {
        return showInfo;
    }

    /**
     * Jump to a specific position of the song based on the given virtual tick.
     * 
     * @param virtualTick The current virtual tick to evaluate the song's state.
     *                     This represents the logical progression of the song.
     */
    public void seek(double virtualTick) {
        double remainingVirtualTicks = song.getRemainingVirtualTicks(virtualTick);
        if (remainingVirtualTicks < 0) {
            this.cancel();
            return;
        }
        if (virtualTick > song.getMaxVirtualTick()) {
            this.cancel();
            return;
        }
        this.tick = song.getRealTick(virtualTick);
        int remainingRealTicks = song.getRemainingRealTicks(this.tick);
        this.reschedule(1, remainingRealTicks == 2147483647 ? 2147483647 : remainingRealTicks + 1);
    }

    /**
     * Changes the playback speed of the song and reschedules the task accordingly.
     *
     * @param speed The new speed multiplier for the song playback. A value greater than 1.0
     *              increases the speed, while a value between 0.0 and 1.0 decreases it.
     *              A value of 0.0 means the song is paused, and a value below 0 means reverse playback.
     */
    public void changeSpeed(double speed) {
        this.tick = song.setSpeed(speed, this.tick);
        int remainingRealTicks = song.getRemainingRealTicks(this.tick);
        this.reschedule(1, remainingRealTicks == 2147483647 ? 2147483647 : remainingRealTicks + 1);
    }

    /**
     * Sets whether to display song information to the player during playback.
     *
     * @param showInfo {@code true} to enable song information display; {@code false} to disable.
     */
    public void setShowInfo(boolean showInfo) {
        this.showInfo = showInfo;
    }

    /**
     * Gets the command context from which this song playback was initiated.
     *
     * @return The command context.
     */
    public CommandContext<CommandSourceStack> getContext() {
        return context;
    }

    /**
     * Sets the command context for this song playback, which is used for sending feedback messages to the player.
     * 
     * @param context The command context to be associated with this song playback.
     */
    public void setContext(CommandContext<CommandSourceStack> context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "PlaySong{songName='" + songName + "'}";
    }
}
