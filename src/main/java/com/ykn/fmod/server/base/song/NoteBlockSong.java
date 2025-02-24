/**
 * Copyright (c) Meteor Development, Meteor Client
 * This file is under the GPL-3.0 License
 */

package com.ykn.fmod.server.base.song;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.util.Util;

public class NoteBlockSong {

    /**
     * List of notes and their corresponding tick in the song
     */
    private final HashMap<Integer, List<NoteBlockNote>> notesMap;

    private int lastTick;
    private final String title;
    private final String author;

    private final Set<NoteBlockNote> requirements;
    private boolean finishedLoading;

    public NoteBlockSong(HashMap<Integer, List<NoteBlockNote>> notesMap, String title, String author) {
        this.notesMap = notesMap;
        this.lastTick = 0;
        this.title = title;
        this.author = author;
        this.requirements = new HashSet<>();
        this.finishedLoading = false;
    }

    public void finishLoading() {
        if (this.finishedLoading) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Song is already finished loading");
            return;
        }

        this.lastTick = Collections.max(this.notesMap.keySet());
        this.notesMap.values().stream().distinct().forEach(notes -> {
            notes.forEach(note -> {
                if (note.instrument != null) {
                    requirements.add(note);
                }
            });
        });
        this.finishedLoading = true;
    }

    public HashMap<Integer, List<NoteBlockNote>> getNotesMap() {
        return notesMap;
    }

    public Set<NoteBlockNote> getRequirements() {
        if (this.finishedLoading == false) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Song is not finished loading");
            return null;
        }
        return requirements;
    }

    public int getLastTick() {
        if (this.finishedLoading == false) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Song is not finished loading");
            return -1;
        }
        return lastTick;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
