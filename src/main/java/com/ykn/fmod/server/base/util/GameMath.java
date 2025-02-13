package com.ykn.fmod.server.base.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
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

    /**
     * Calculates the minimum distance from a point to an axis-aligned bounding box.
     *
     * @param xa The x-coordinate of the first corner of the bounding box.
     * @param ya The y-coordinate of the first corner of the bounding box.
     * @param za The z-coordinate of the first corner of the bounding box.
     * @param xb The x-coordinate of the opposite corner of the bounding box.
     * @param yb The y-coordinate of the opposite corner of the bounding box.
     * @param zb The z-coordinate of the opposite corner of the bounding box.
     * @param x The x-coordinate of the point.
     * @param y The y-coordinate of the point.
     * @param z The z-coordinate of the point.
     * @return The minimum distance from the point to the bounding box.
     */
    public static double getMinimumDistanceToBox(double xa, double ya, double za, double xb, double yb, double zb, double x, double y, double z) {
        double xmin = Math.min(xa, xb);
        double xmax = Math.max(xa, xb);
        double ymin = Math.min(ya, yb);
        double ymax = Math.max(ya, yb);
        double zmin = Math.min(za, zb);
        double zmax = Math.max(za, zb);
        double xclamp = Math.max(xmin, Math.min(x, xmax));
        double yclamp = Math.max(ymin, Math.min(y, ymax));
        double zclamp = Math.max(zmin, Math.min(z, zmax));
        double dx = x - xclamp;
        double dy = y - yclamp;
        double dz = z - zclamp;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates the unit direction vector based on the given pitch and yaw angles.
     *
     * @param pitch the pitch angle in degrees
     * @param yaw the yaw angle in degrees
     * @return a Vec3d representing the unit direction vector
     */
    public static Vec3d getUnitDirectionVector(double pitch, double yaw) {
        double pitchRadians = Math.toRadians(pitch);
        double yawRadians = Math.toRadians(yaw);
        double cosPitch = Math.cos(pitchRadians);
        double sinPitch = Math.sin(pitchRadians);
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        double x = -sinYaw * cosPitch;
        double y = sinPitch;
        double z = -cosYaw * cosPitch;
        return new Vec3d(x, y, z);
    }

    /**
     * Calculates the first intersection point of a ray with an axis-aligned bounding box (AABB).
     *
     * @param xa The x-coordinate of the first corner of the AABB.
     * @param ya The y-coordinate of the first corner of the AABB.
     * @param za The z-coordinate of the first corner of the AABB.
     * @param xb The x-coordinate of the opposite corner of the AABB.
     * @param yb The y-coordinate of the opposite corner of the AABB.
     * @param zb The z-coordinate of the opposite corner of the AABB.
     * @param x The x-coordinate of the ray's origin.
     * @param y The y-coordinate of the ray's origin.
     * @param z The z-coordinate of the ray's origin.
     * @param pitch The pitch angle of the ray's direction.
     * @param yaw The yaw angle of the ray's direction.
     * @return A Vec3d representing the intersection point, or a Vec3d with NaN values if there is no intersection.
     */
    public static Vec3d getRaytraceFirstIntersection(double xa, double ya, double za, double xb, double yb, double zb, double x, double y, double z, double pitch, double yaw) {
        double xmin = Math.min(xa, xb);
        double xmax = Math.max(xa, xb);
        double ymin = Math.min(ya, yb);
        double ymax = Math.max(ya, yb);
        double zmin = Math.min(za, zb);
        double zmax = Math.max(za, zb);
        Vec3d dir = getUnitDirectionVector(pitch, yaw);
        double dx = dir.getX();
        double dy = dir.getY();
        double dz = dir.getZ();
        double txa = 0.0;
        double txb = 0.0;
        double tya = 0.0;
        double tyb = 0.0;
        double tza = 0.0;
        double tzb = 0.0;
        if (dx == 0) {
            if (x < xmin || x > xmax) {
                return new Vec3d(Double.NaN, Double.NaN, Double.NaN);
            } else {
                txa = -Double.POSITIVE_INFINITY;
                txb = Double.POSITIVE_INFINITY;
            }
        } else {
            txa = (xmin - x) / dx;
            txb = (xmax - x) / dx;
            if (dx < 0.0) {
                double temp = txa;
                txa = txb;
                txb = temp;
            }
        }
        if (dy == 0) {
            if (y < ymin || y > ymax) {
                return new Vec3d(Double.NaN, Double.NaN, Double.NaN);
            } else {
                tya = -Double.POSITIVE_INFINITY;
                tyb = Double.POSITIVE_INFINITY;
            }
        } else {
            tya = (ymin - y) / dy;
            tyb = (ymax - y) / dy;
            if (dy < 0.0) {
                double temp = tya;
                tya = tyb;
                tyb = temp;
            }
        }
        if (dz == 0) {
            if (z < zmin || z > zmax) {
                return new Vec3d(Double.NaN, Double.NaN, Double.NaN);
            } else {
                tza = -Double.POSITIVE_INFINITY;
                tzb = Double.POSITIVE_INFINITY;
            }
        } else {
            tza = (zmin - z) / dz;
            tzb = (zmax - z) / dz;
            if (dz < 0.0) {
                double temp = tza;
                tza = tzb;
                tzb = temp;
            }
        }
        double tenter = Math.max(Math.max(txa, tya), tza);
        double texit = Math.min(Math.min(txb, tyb), tzb);
        if (tenter <= texit && tenter >= 0.0) {
            return new Vec3d(x + tenter * dx, y + tenter * dy, z + tenter * dz);
        } else {
            return new Vec3d(Double.NaN, Double.NaN, Double.NaN);
        }
    }
}
