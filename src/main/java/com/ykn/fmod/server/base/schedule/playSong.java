package com.ykn.fmod.server.base.schedule;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.song.NoteBlockNote;
import com.ykn.fmod.server.base.song.NoteBlockSong;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class playSong extends ScheduledTask {

    private NoteBlockSong song;
    private ServerPlayerEntity target;
    private CommandContext<ServerCommandSource> context;
    private int tick;

    public playSong(NoteBlockSong song, ServerPlayerEntity target, CommandContext<ServerCommandSource> context) {
        super(1, song.getLastTick() + 1);
        this.song = song;
        this.target = target;
        this.context = context;
        this.tick = 0;
    }

    @Override
    public void onTick() {
        List<NoteBlockNote> notes = song.getNotesMap().get(this.tick);
        if (notes != null) {
            for (NoteBlockNote note : notes) {
                if (target == null || target.isDisconnected() || target.isRemoved()) {
                    // Do not cancel the task here, so that if the player connects back, the song will continue
                    break;
                }
                // target.playSound(note.instrument.getSound().value(), 2f, (float) Math.pow(2.0D, (note.noteLevel - 12) / 12.0D));
                target.networkHandler.sendPacket(new PlaySoundS2CPacket(note.instrument.getSound(), target.getSoundCategory(), target.getX(), target.getY(), target.getZ(), 2f, (float) Math.pow(2.0D, (note.noteLevel - 12) / 12.0D), 0));
            }
        }
        this.tick++;
    }

    @Override
    public void onCancel() {
        this.tick = song.getLastTick();
        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.cancel", target.getDisplayName(), song.getTitle()), true);
    }

    @Override
    public void onFinish() {
        context.getSource().sendFeedback(() -> Util.parseTranslateableText("fmod.command.song.finish", target.getDisplayName(), song.getTitle()), true);
    }

    public NoteBlockSong getSong() {
        return song;
    }

    public ServerPlayerEntity getTarget() {
        return target;
    }

    public int getTick() {
        return tick;
    }
}
