/**
 * Copyright (c) Meteor Development, Meteor Client
 * This file is under the GPL-3.0 License
 */

package com.ykn.fmod.server.base.song;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public record NoteBlockNote(NoteBlockInstrument instrument, int noteLevel) {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NoteBlockNote note) {
            return note.instrument == this.instrument && note.noteLevel == this.noteLevel;
        }
        return false;
    }

    @Override
    public String toString() {
        return "NoteBlockNote{" + "instrument=" + instrument + ", noteLevel=" + noteLevel + '}';
    }
}
