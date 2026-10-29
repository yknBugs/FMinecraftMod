/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

/**
 * Mod-owned SplitMix64 stream shared by rule and flow components.
 *
 * <p>It never reads or advances a Minecraft random source and never derives from the world seed, so
 * a component that draws a random number cannot change monster spawns, loot, or any other game
 * outcome that depends on the world's random sequence.
 *
 * <p>The stream is {@code synchronized} because several components may evaluate on different
 * threads; each owning component family creates one instance.
 */
public final class SplitMixRandom {

    private long state;

    /**
     * Creates an independent, reproducible stream.
     *
     * @param seed the caller-owned seed
     */
    public SplitMixRandom(long seed) {
        this.state = seed;
    }

    /**
     * Draws a uniform double in {@code [0, 1)}.
     *
     * @return a value in {@code [0, 1)}
     */
    public synchronized double nextDouble() {
        return (nextBits() >>> 11) * 0x1.0p-53;
    }

    /**
     * Draws a normally distributed double with mean 0 and standard deviation 1.
     *
     * <p>Uses the Marsaglia polar method, which needs a rejection loop but no trigonometric call.
     *
     * @return a value from the standard normal distribution
     */
    public synchronized double nextGaussian() {
        double first;
        double second;
        double squared;
        do {
            first = nextDouble() * 2 - 1;
            second = nextDouble() * 2 - 1;
            squared = first * first + second * second;
        } while (squared >= 1 || squared == 0);
        return first * Math.sqrt(-2 * Math.log(squared) / squared);
    }

    /**
     * Draws a uniform double in {@code [min, max)}.
     *
     * @param min the inclusive lower bound
     * @param max the exclusive upper bound
     * @return a value in range; equal bounds return that value without advancing the stream
     * @throws IllegalArgumentException when a bound is not finite or {@code min > max}
     */
    public synchronized double between(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Invalid random bounds");
        }
        if (min == max) {
            return min;
        }
        double unit = (nextBits() >>> 11) * 0x1.0p-53;
        double span = max - min;
        double value = Double.isFinite(span)
                ? min + span * unit
                : min * (1 - unit) + max * unit;
        return Math.max(min, Math.min(Math.nextDown(max), value));
    }

    private long nextBits() {
        long bits = (this.state += 0x9e3779b97f4a7c15L);
        bits = (bits ^ (bits >>> 30)) * 0xbf58476d1ce4e5b9L;
        bits = (bits ^ (bits >>> 27)) * 0x94d049bb133111ebL;
        return bits ^ (bits >>> 31);
    }
}
