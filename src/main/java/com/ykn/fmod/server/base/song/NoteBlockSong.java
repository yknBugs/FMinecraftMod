/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ykn.fmod.server.base.util.Util;

/**
 * Represents a NoteBlock song with support for variable playback speed,
 * tick-based note scheduling, and efficient note retrieval.
 * <p>
 * This class manages the mapping between virtual ticks (logical progression)
 * and real ticks (actual playback time), allowing for features such as
 * speed changes, seeking, and reverse playback.
 */
public class NoteBlockSong {

    /**
     * List of notes and their corresponding tick in the song.
     * Maps virtual tick values to lists of notes scheduled at those ticks.
     */
    private HashMap<Double, List<NoteBlockNote>> notesMap;

    /**
     * List of nearest tick for each note.
     * Maps real tick values to lists of notes scheduled at those ticks.
     */
    private HashMap<Integer, List<NoteBlockNote>> realTickIndex;

    /**
     * Last tick of the song (will not change even when the speed changes).
     * Represents the maximum virtual tick in the song.
     */
    private double maxVirtualTick;

    /**
     * Last tick of the song in real time (when speed == 1 it will be the same as maxVirtualTick).
     * Represents the maximum real tick considering the current speed.
     */
    private int maxRealTick;

    /**
     * Speed of the song.
     * A value greater than 1.0 increases speed, between 0.0 and 1.0 decreases it,
     * 0.0 pauses the song, and negative values enable reverse playback.
     */
    private double speed;

    /**
     * The title of the song.
     */
    private String title;
    
    /**
     * The author of the song.
     */
    private String author;

    /**
     * Set of unique notes with instruments required for this song.
     */
    private Set<NoteBlockNote> requirements;


    /**
     * Finds the nearest real tick based on the given virtual tick, maximum virtual tick, and speed.
     *
     * @param virtualTick   The current virtual tick value. Represents the logical progression of the song.
     * @param maxVirtualTick The maximum virtual tick value, used when speed is negative.
     * @param speed         The speed factor that determines the relationship between virtual and real ticks.
     *                      A positive speed indicates forward progression, while a negative speed indicates
     *                      reverse progression.
     * @return The nearest real tick as an integer. Returns:
     *         <ul>
     *         <li> 0 if speed is 0 and virtualTick is also 0. </li>
     *         <li> Integer.MAX_VALUE (2147483647) if speed is 0 and virtualTick is not 0, or if the calculated
     *           real tick is NaN or positive infinity. </li>
     *         <li> Integer.MIN_VALUE (-2147483648) if the calculated real tick is negative infinity. </li>
     *         <li> Integer.MAX_VALUE or Integer.MIN_VALUE if the calculated real tick exceeds the range of
     *           a 32-bit signed integer. </li>
     *         <li> The rounded value of the calculated real tick otherwise. </li>
     *         </ul>
     */
    public static int findNearestRealTick(double virtualTick, double maxVirtualTick, double speed) {
        if (speed == 0) {
            if (virtualTick == 0) {
                return 0;
            } else {
                return 2147483647;
            }
        }
        double realTick = 0.0;
        if (speed > 0) {
            realTick = virtualTick / speed;
            
        }
        if (speed < 0) {
            realTick = (maxVirtualTick - virtualTick) / Math.abs(speed);
        }
        int nearestTick = 0;
        if (Double.isNaN(realTick)) {
            nearestTick = 2147483647;
        } else if (Double.isInfinite(realTick) && realTick > 0) {
            nearestTick = 2147483647;
        } else if (Double.isInfinite(realTick) && realTick < 0) {
            nearestTick = -2147483648;
        } else if (realTick > 2147483647) {
            nearestTick = 2147483647;
        } else if (realTick < -2147483648) {
            nearestTick = -2147483648;
        } else {
            nearestTick = (int) Math.round(realTick);
        }
        return nearestTick;
    }

    /**
     * Constructs a new NoteBlock song with the specified notes map, title, and author.
     *
     * @param notesMap A map of virtual ticks to lists of notes scheduled at those ticks.
     * @param title The title of the song.
     * @param author The author of the song.
     */
    public NoteBlockSong(HashMap<Double, List<NoteBlockNote>> notesMap, String title, String author) {
        this.notesMap = notesMap;
        this.title = title;
        this.author = author;
        this.speed = 1.0;
        this.createIndex();
    }

    /**
     * Creates an index for the notes in the song by mapping each note's tick to its nearest
     * corresponding tick based on the song's speed. This method also calculates the last tick
     * of the song and identifies the unique requirements (notes with instruments) for the song.
     *
     * <p>Steps performed:
     * <ul>
     *   <li>Determines the last tick in the song by finding the maximum key in the notes map.</li>
     *   <li>Maps each note's tick to its nearest tick using the song's speed.</li>
     *   <li>Identifies and stores unique notes with instruments as requirements.</li>
     * </ul>
     *
     * <p>Note: The method assumes that the `notesMap` contains the mapping of ticks to notes
     * and that each note may have an associated instrument.
     */
    public void createIndex() {
        // Check if the notes map is empty
        Set<Double> keyTicks = this.notesMap.keySet();
        if (keyTicks.isEmpty()) {
            Util.LOGGER.warn("FMinecraftMod: Trying to create index for an empty song " + this.title + " by " + this.author);
            this.maxVirtualTick = 0;
            this.realTickIndex = new HashMap<>();
            this.maxRealTick = 0;
            this.requirements = new HashSet<>();
            return;
        }
        // Build the real tick index based on the virtual ticks and the current speed
        this.maxVirtualTick = Collections.max(keyTicks);
        this.realTickIndex = new HashMap<>();
        for (Double keyTick : keyTicks) {
            int nearestTick = findNearestRealTick(keyTick, this.maxVirtualTick, this.speed);    
            List<NoteBlockNote> notes = this.notesMap.get(keyTick);
            if (notes != null) {
                for (NoteBlockNote note : notes) {
                    this.realTickIndex.computeIfAbsent(nearestTick, k -> new ArrayList<>()).add(note);
                }
            }
        }
        // Determine all kinds of required instruments for this song
        if (this.realTickIndex.isEmpty()) {
            // Unlikely to happen
            Util.LOGGER.warn("FMinecraftMod: Building index for song " + this.title + " by " + this.author + " but no notes were found after indexing.");
            this.maxRealTick = 0;
            this.requirements = new HashSet<>();
            return;
        }
        this.maxRealTick = Collections.max(this.realTickIndex.keySet()); 
        this.requirements = new HashSet<>();
        this.notesMap.values().stream().distinct().forEach(notes -> {
            notes.forEach(note -> {
                if (note.instrument != null) {
                    requirements.add(note);
                }
            });
        });
    }

    /**
     * Creates a deep copy of this NoteBlockSong instance.
     * <p>
     * The method creates a new NoteBlockSong object with a copied notes map, while keeping the same title and author.
     * The copied notes map is constructed by creating new lists for each tick, ensuring that modifications to the
     * notes in the copied song do not affect the original song.
     *
     * @return A new NoteBlockSong instance that is a deep copy of the original.
     */
    public NoteBlockSong copy() {
        HashMap<Double, List<NoteBlockNote>> copiedNotesMap = new HashMap<>();
        for (Map.Entry<Double, List<NoteBlockNote>> entry : this.notesMap.entrySet()) {
            copiedNotesMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        NoteBlockSong copied = new NoteBlockSong(copiedNotesMap, this.title, this.author);
        return copied;
    }

    /**
     * Sets the playback speed of the song and updates the internal index.
     * This method recalculates all tick mappings based on the new speed.
     *
     * @param speed The new playback speed to set. A higher value increases the speed,
     *              while a lower value decreases it. A value of 0.0 pauses the song,
     *              and negative values enable reverse playback.
     */
    public void setSpeed(double speed) {
        this.speed = speed;
        this.createIndex();
    }

    /**
     * Sets the playback speed of the song and recalculates the tick indices.
     *
     * @param speed The new speed to set for the song playback. A higher value increases the speed, 
     *              while a lower value decreases it.
     * @param currentRealTick The current real tick of the song, used to calculate the corresponding 
     *                        virtual tick before updating the speed.
     * @return The recalculated real tick corresponding to the same virtual tick after the speed change.
     */
    public int setSpeed(double speed, int currentRealTick) {
        double currentVirtualTick = this.getVirtualTick(currentRealTick);
        this.speed = speed;
        this.createIndex();
        return this.getRealTick(currentVirtualTick);
    }

    /**
     * Retrieves the list of notes scheduled to play at the specified tick.
     *
     * @param realTick The tick for which to retrieve the notes.
     * @return A list of {@link NoteBlockNote} objects scheduled for the given tick.
     *         If no notes are scheduled for the tick, an empty list is returned.
     */
    public List<NoteBlockNote> getNotes(int realTick) {
        return this.realTickIndex.getOrDefault(realTick, new ArrayList<>());
    }

    /**
     * Calculates the number of real ticks remaining from the current position to the end of the song.
     *
     * @param currentRealTick The current real tick position in the song.
     * @return The number of real ticks remaining.
     */
    public int getRemainingRealTicks(int currentRealTick) {
        return this.maxRealTick - currentRealTick;
    }

    /**
     * Calculates the number of virtual ticks remaining from the current position to the end of the song.
     *
     * @param currentVirtualTick The current virtual tick position in the song.
     * @return The number of virtual ticks remaining.
     */
    public double getRemainingVirtualTicks(double currentVirtualTick) {
        return this.maxVirtualTick - currentVirtualTick;
    }

    /**
     * Calculates the virtual tick based on the real tick and the speed of the song.
     * <p>
     * The method determines the virtual tick value depending on the speed:
     * <ul>
     *   <li>If the speed is 0, the virtual tick is the same as the real tick.</li>
     *   <li>If the speed is positive, the virtual tick is calculated as the product
     *       of the real tick and the speed.</li>
     *   <li>If the speed is negative, the virtual tick is calculated as the product
     *       of the remaining ticks (difference between maxRealTick and realTick)
     *       and the absolute value of the speed.</li>
     * </ul>
     *
     * @param realTick The current real tick of the song.
     * @return The calculated virtual tick as a double.
     */
    public double getVirtualTick(int realTick) {
        if (this.speed == 0) {
            return realTick;
        } else if (this.speed > 0) {
            return realTick * this.speed;
        } else {
            return (this.maxRealTick - realTick) * Math.abs(this.speed);
        }
    }

    /**
     * Converts a virtual tick value to a real tick value based on the song's speed.
     * If the speed is zero, the virtual tick is rounded to the nearest integer.
     * Otherwise, it calculates the nearest real tick using the song's maximum virtual tick
     * and speed parameters.
     *
     * @param virtualTick The virtual tick value to be converted.
     * @return The corresponding real tick value.
     */
    public int getRealTick(double virtualTick) {
        if (speed == 0) {
            return (int) Math.round(virtualTick);
        }
        return findNearestRealTick(virtualTick, this.maxVirtualTick, this.speed);
    }

    /**
     * Gets the maximum real tick value of the song based on the current speed.
     *
     * @return The maximum real tick.
     */
    public int getMaxRealTick() {
        return this.maxRealTick;
    }

    /**
     * Gets the maximum virtual tick value of the song.
     * This value remains constant regardless of speed changes.
     *
     * @return The maximum virtual tick.
     */
    public double getMaxVirtualTick() {
        return this.maxVirtualTick;
    }

    /**
     * Gets the map of virtual ticks to lists of notes.
     *
     * @return The notes map with virtual tick keys.
     */
    public Map<Double, List<NoteBlockNote>> getNotesMap() {
        return Collections.unmodifiableMap(this.notesMap);
    }

    /**
     * Gets the map of real ticks to lists of notes.
     * This map is recalculated whenever the speed changes.
     *
     * @return The notes map with real tick keys.
     */
    public Map<Integer, List<NoteBlockNote>> getRealTicksMap() {
        return Collections.unmodifiableMap(this.realTickIndex);
    }

    /**
     * Gets the set of unique notes with instruments required for this song.
     *
     * @return The set of required notes.
     */
    public Set<NoteBlockNote> getRequirements() {
        return Collections.unmodifiableSet(this.requirements);
    }

    /**
     * Gets the current playback speed of the song.
     *
     * @return The current speed multiplier.
     */
    public double getSpeed() {
        return this.speed;
    }

    /**
     * Gets the title of the song.
     *
     * @return The song title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Gets the author of the song.
     *
     * @return The song author.
     */
    public String getAuthor() {
        return this.author;
    }
}
