/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.schedule;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.song.NoteBlockNote;
import com.ykn.fmod.server.base.song.NoteBlockSong;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class PlaySong extends ScheduledTask {

    private NoteBlockSong song;
    private String songName;
    private ServerPlayerEntity target;
    private CommandContext<ServerCommandSource> context;
    private int tick;
    private boolean showInfo;
    private int lastShowInfoSeconds;
    private int lastShowInfoTicks;
    private double lastShowInfoSpeed;

    public PlaySong(NoteBlockSong song, String songName, ServerPlayerEntity target, CommandContext<ServerCommandSource> context) {
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

    @Override
    public void onTick() {
        List<NoteBlockNote> notes = song.getNotes(this.tick);
        if (notes != null) {
            for (NoteBlockNote note : notes) {
                // target.playSound(note.instrument.getSound().value(), 2f, (float) Math.pow(2.0, (note.noteLevel - 12) / 12.0));
                target.networkHandler.sendPacket(new PlaySoundIdS2CPacket(note.instrument.getSound().getId(), target.getSoundCategory(), new Vec3d(target.getX(), target.getY(), target.getZ()), 2f, (float) Math.pow(2.0D, (note.noteLevel - 12) / 12.0D)));
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
                Util.sendActionBarMessage(target, Util.parseTranslateableText("fmod.command.song.info", songName, currentTimeStr, totalTimeStr, speedStr));
            }
        }
        if (this.getSong().getSpeed() != 0) {
            this.tick++;
        }
    }

    @Override
    public void onCancel() {
        this.tick = song.getMaxRealTick();
        context.getSource().sendFeedback(Util.parseTranslateableText("fmod.command.song.cancel", target.getDisplayName(), this.songName), true);
    }

    @Override
    public void onFinish() {
        context.getSource().sendFeedback(Util.parseTranslateableText("fmod.command.song.finish", target.getDisplayName(), this.songName), true);
    }

    @Override
    public boolean shouldCancel() {
        return target == null || target.isDisconnected() || target.isRemoved();
    }

    public NoteBlockSong getSong() {
        return song;
    }

    public String getSongName() {
        return songName;
    }

    public ServerPlayerEntity getTarget() {
        return target;
    }

    public int getTick() {
        return tick;
    }

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

    public void setShowInfo(boolean showInfo) {
        this.showInfo = showInfo;
    }
}
