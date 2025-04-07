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
import java.util.Set;

public class NoteBlockSong {

    /**
     * List of notes and their corresponding tick in the song
     */
    private HashMap<Double, List<NoteBlockNote>> notesMap;

    /**
     * List of nearest tick for each note
     */
    private HashMap<Integer, List<NoteBlockNote>> realTickIndex;

    /**
     * Last tick of the song (Will not change even the speed changes)
     */
    private double maxVirtualTick;

    /**
     * Last tick of the song in real time (When speed == 1 it will be the same as maxVirtualTick)
     */
    private int maxRealTick;

    /**
     * Speed of the song
     */
    private double speed;

    private String title;
    private String author;

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
     *         - 0 if speed is 0 and virtualTick is also 0.
     *         - Integer.MAX_VALUE (2147483647) if speed is 0 and virtualTick is not 0, or if the calculated
     *           real tick is NaN or positive infinity.
     *         - Integer.MIN_VALUE (-2147483648) if the calculated real tick is negative infinity.
     *         - Integer.MAX_VALUE or Integer.MIN_VALUE if the calculated real tick exceeds the range of
     *           a 32-bit signed integer.
     *         - The rounded value of the calculated real tick otherwise.
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
     * Adds a value to a list associated with a specific key in the given map. 
     * If the key does not exist in the map, a new list is created and added to the map.
     *
     * @param <K>  The type of keys in the map.
     * @param <T>  The type of elements in the list.
     * @param map  The map where the key-value pair will be added.
     * @param key  The key to which the value should be associated.
     * @param value The value to be added to the list associated with the key.
     * @return The updated map with the new key-value association.
     */
    public static <K, T> HashMap<K, List<T>> put(HashMap<K, List<T>> map, K key, T value) {
        List<T> list = map.get(key);
        if (list == null) {
            list = new ArrayList<>();
            map.put(key, list);
        }
        list.add(value);
        return map;
    }

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
        Set<Double> keyTicks = this.notesMap.keySet();
        this.maxVirtualTick = Collections.max(keyTicks);
        this.realTickIndex = new HashMap<>();
        for (Double keyTick : keyTicks) {
            int nearestTick = findNearestRealTick(keyTick, this.maxVirtualTick, this.speed);    
            List<NoteBlockNote> notes = this.notesMap.get(keyTick);
            if (notes != null) {
                for (NoteBlockNote note : notes) {
                    this.realTickIndex = put(this.realTickIndex, nearestTick, note);
                }
            }
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
     * Sets the playback speed of the song and updates the internal index.
     *
     * @param speed The new playback speed to set. A higher value increases the speed,
     *              while a lower value decreases it.
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

    public int getRemainingRealTicks(int currentRealTick) {
        return this.maxRealTick - currentRealTick;
    }

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

    public int getMaxRealTick() {
        return this.maxRealTick;
    }

    public double getMaxVirtualTick() {
        return this.maxVirtualTick;
    }

    public HashMap<Double, List<NoteBlockNote>> getNotesMap() {
        return this.notesMap;
    }

    public HashMap<Integer, List<NoteBlockNote>> getRealTicksMap() {
        return this.realTickIndex;
    }

    public Set<NoteBlockNote> getRequirements() {
        return this.requirements;
    }

    public double getSpeed() {
        return this.speed;
    }

    public String getTitle() {
        return this.title;
    }

    public String getAuthor() {
        return this.author;
    }
}
