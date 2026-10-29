/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.data;

/**
 * The AFK episode state machine: decides when an idle player enters AFK and when a moving player
 * leaves it.
 *
 * <p>The episode is remembered as state instead of being re-derived from the idle tick counter and
 * the currently configured threshold, because the threshold can change while a player is already
 * idle. An equality test against the current threshold would then never report the entry, while the
 * return - which only has to compare the counter against that same threshold - would still be
 * reported afterwards, leaving an unmatched return event.
 *
 * <p>Deliberately free of Minecraft types: one instance per player, owned by {@link PlayerData}, so
 * the transition rules can be exercised without a running server.
 */
public final class AfkState {

    /** No transition this tick. */
    public static final int NONE = 0;

    /** The player just reached the threshold and entered an AFK episode. */
    public static final int ENTERED = 1;

    /** The player just moved after being AFK, ending the episode. */
    public static final int RETURNED = -1;

    /**
     * Whether the player is currently inside an AFK episode.
     */
    private boolean afk;

    /**
     * Advances the state machine by one tick.
     *
     * <p>A moving player always ends the episode, whatever the threshold currently is: the event
     * that started it has already been delivered, so the matching return must be delivered too.
     * An idle player enters as soon as the counter reaches the threshold, which also covers a
     * threshold lowered mid-episode - the entry then happens on the next tick instead of never.
     *
     * @param ticks     the number of idle ticks accumulated before this tick
     * @param threshold the currently configured AFK threshold
     * @param moved     whether the player moved during this tick
     * @return {@link #ENTERED}, {@link #RETURNED}, or {@link #NONE}
     */
    public int update(int ticks, int threshold, boolean moved) {
        if (moved) {
            if (!this.afk) {
                return NONE;
            }
            this.afk = false;
            return RETURNED;
        }
        if (this.afk) {
            return NONE;
        }
        if (ticks < threshold) {
            return NONE;
        }
        this.afk = true;
        return ENTERED;
    }

    /**
     * Whether the player is currently inside an AFK episode.
     *
     * @return {@code true} between the entry and the return
     */
    public boolean isAfk() {
        return this.afk;
    }

    /**
     * Forgets the current episode without reporting a transition.
     *
     * <p>For state that outlives the player entity, such as a reconnect, where no return event can
     * meaningfully be delivered.
     */
    public void reset() {
        this.afk = false;
    }
}
