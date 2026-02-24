/**
 * Copyright (c) Meteor Development, Meteor Client
 * This file is under the GPL-3.0 License
 */

package com.ykn.fmod.server.base.song;

import java.util.Objects;

import net.minecraft.block.enums.Instrument;

public class NoteBlockNote {

    public final Instrument instrument;
    public final int noteLevel;

    public NoteBlockNote(Instrument instrument, int noteLevel) {
        this.instrument = instrument;
        this.noteLevel = noteLevel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NoteBlockNote) {
            NoteBlockNote note = (NoteBlockNote) obj;
            return note.instrument == this.instrument && note.noteLevel == this.noteLevel;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(instrument, noteLevel);
    }

    @Override
    public String toString() {
        return "NoteBlockNote{" + "instrument=" + instrument + ", noteLevel=" + noteLevel + '}';
    }
}
