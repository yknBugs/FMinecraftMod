package com.ykn.fmod.server.base.util;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class GameMath {

    /**
     * Calculates the Euclidean distance between two entities in the same world.
     *
     * @param a the first entity
     * @param b the second entity
     * @return the Euclidean distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getEuclideanDistance(Entity a, Entity b) {
        double xa = a.getX();
        double ya = a.getY();
        double za = a.getZ();
        World worlda = a.getWorld();
        double xb = b.getX();
        double yb = b.getY();
        double zb = b.getZ();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates the horizontal Euclidean distance between two entities in the same world.
     * The distance is computed using only the X and Z coordinates, ignoring the Y coordinate.
     *
     * @param a the first entity
     * @param b the second entity
     * @return the horizontal Euclidean distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getHorizonalEuclideanDistance(Entity a, Entity b) {
        double xa = a.getX();
        double za = a.getZ();
        World worlda = a.getWorld();
        double xb = b.getX();
        double zb = b.getZ();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        double dx = xa - xb;
        double dz = za - zb;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calculates the Manhattan distance between two entities in the same world.
     * The Manhattan distance is the sum of the absolute differences of their Cartesian coordinates.
     *
     * @param a the first entity
     * @param b the second entity
     * @return the Manhattan distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getManhattanDistance(Entity a, Entity b) {
        double xa = a.getX();
        double ya = a.getY();
        double za = a.getZ();
        World worlda = a.getWorld();
        double xb = b.getX();
        double yb = b.getY();
        double zb = b.getZ();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        return Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
    }

    /**
     * Calculates the horizontal Manhattan distance between two entities.
     * The Manhattan distance is the sum of the absolute differences of their Cartesian coordinates.
     * 
     * @param a the first entity
     * @param b the second entity
     * @return the horizontal Manhattan distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getHorizonalManhattanDistance(Entity a, Entity b) {
        double xa = a.getX();
        double za = a.getZ();
        World worlda = a.getWorld();
        double xb = b.getX();
        double zb = b.getZ();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        double dx = xa - xb;
        double dz = za - zb;
        return Math.abs(dx) + Math.abs(dz);
    }

    /**
     * Calculates the Chebyshev distance between two entities in the same world.
     * The Chebyshev distance is the maximum of the absolute differences of their coordinates.
     *
     * @param a the first entity
     * @param b the second entity
     * @return the Chebyshev distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getChebyshevDistance(Entity a, Entity b) {
        double xa = a.getX();
        double ya = a.getY();
        double za = a.getZ();
        World worlda = a.getWorld();
        double xb = b.getX();
        double yb = b.getY();
        double zb = b.getZ();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        return Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
    }

    /**
     * Calculates the horizontal Chebyshev distance between two entities in the same world.
     * The Chebyshev distance is the maximum of the absolute differences of their coordinates.
     *
     * @param a the first entity
     * @param b the second entity
     * @return the horizontal Chebyshev distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getHorizonalChebyshevDistance(Entity a, Entity b) {
        double xa = a.getX();
        double za = a.getZ();
        World worlda = a.getWorld();
        double xb = b.getX();
        double zb = b.getZ();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        double dx = xa - xb;
        double dz = za - zb;
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    /**
     * Calculates the vertical distance between two entities.
     * The vertical distance is the absolute difference of their Y coordinates.
     *
     * @param a the first entity
     * @param b the second entity
     * @return the vertical distance between the two entities, or Double.NaN if they are in different worlds
     */
    public static double getVerticalDistance(Entity a, Entity b) {
        double ya = a.getY();
        World worlda = a.getWorld();
        double yb = b.getY();
        World worldb = b.getWorld();
        if (worlda != worldb) {
            return Double.NaN;
        }
        return Math.abs(ya - yb);
    }
}
