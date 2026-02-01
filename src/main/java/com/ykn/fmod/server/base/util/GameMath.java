/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Provides various mathematical utility functions for game development.
 */
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
     * Calculates the Euclidean distance between two points in 3D space.
     *
     * @param xa the x-coordinate of the first point
     * @param ya the y-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param yb the y-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @return the Euclidean distance between the two points
     */
    public static double getEuclideanDistance(double xa, double ya, double za, double xb, double yb, double zb) {
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Calculates the Euclidean distance between two vectors in 3D space.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the Euclidean distance between the two vectors
     */
    public static double getEuclideanDistance(Vec3d a, Vec3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
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
     * Calculates the horizontal Euclidean distance between two points in 3D space.
     * The distance is computed using only the X and Z coordinates, ignoring the Y coordinate.
     *
     * @param xa the x-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @return the horizontal Euclidean distance between the two points
     */
    public static double getHorizonalEuclideanDistance(double xa, double za, double xb, double zb) {
        double dx = xa - xb;
        double dz = za - zb;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calculates the horizontal Euclidean distance between two vectors in 3D space.
     * The distance is computed using only the X and Z coordinates, ignoring the Y coordinate.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the horizontal Euclidean distance between the two vectors
     */
    public static double getHorizonalEuclideanDistance(Vec3d a, Vec3d b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
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
     * Calculates the Manhattan distance between two points in 3D space.
     * The Manhattan distance is the sum of the absolute differences of their Cartesian coordinates.
     *
     * @param xa the x-coordinate of the first point
     * @param ya the y-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param yb the y-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @return the Manhattan distance between the two points
     */
    public static double getManhattanDistance(double xa, double ya, double za, double xb, double yb, double zb) {
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        return Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
    }

    /**
     * Calculates the Manhattan distance between two vectors in 3D space.
     * The Manhattan distance is the sum of the absolute differences of their Cartesian coordinates.
     * 
     * @param a the first vector
     * @param b the second vector
     * @return the Manhattan distance between the two vectors
     */
    public static double getManhattanDistance(Vec3d a, Vec3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
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
     * Calculates the horizontal Manhattan distance between two points in 3D space.
     * The Manhattan distance is the sum of the absolute differences of their Cartesian coordinates.
     *
     * @param xa the x-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @return the horizontal Manhattan distance between the two points
     */
    public static double getHorizonalManhattanDistance(double xa, double za, double xb, double zb) {
        double dx = xa - xb;
        double dz = za - zb;
        return Math.abs(dx) + Math.abs(dz);
    }

    /**
     * Calculates the horizontal Manhattan distance between two vectors in 3D space.
     * The Manhattan distance is the sum of the absolute differences of their Cartesian coordinates.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the horizontal Manhattan distance between the two vectors
     */
    public static double getHorizonalManhattanDistance(Vec3d a, Vec3d b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
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
     * Calculates the Chebyshev distance between two points in 3D space.
     * The Chebyshev distance is the maximum of the absolute differences of their coordinates.
     *
     * @param xa the x-coordinate of the first point
     * @param ya the y-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param yb the y-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @return the Chebyshev distance between the two points
     */
    public static double getChebyshevDistance(double xa, double ya, double za, double xb, double yb, double zb) {
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        return Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
    }

    /**
     * Calculates the Chebyshev distance between two vectors in 3D space.
     * The Chebyshev distance is the maximum of the absolute differences of their coordinates.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the Chebyshev distance between the two vectors
     */
    public static double getChebyshevDistance(Vec3d a, Vec3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
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
     * Calculates the horizontal Chebyshev distance between two points in 3D space.
     * The Chebyshev distance is the maximum of the absolute differences of their coordinates.
     *
     * @param xa the x-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @return the horizontal Chebyshev distance between the two points
     */
    public static double getHorizonalChebyshevDistance(double xa, double za, double xb, double zb) {
        double dx = xa - xb;
        double dz = za - zb;
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    /**
     * Calculates the horizontal Chebyshev distance between two vectors in 3D space.
     * The Chebyshev distance is the maximum of the absolute differences of their coordinates.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the horizontal Chebyshev distance between the two vectors
     */
    public static double getHorizonalChebyshevDistance(Vec3d a, Vec3d b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
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
     * Calculates the vertical distance between two points in 3D space.
     * The vertical distance is the absolute difference of their Y coordinates.
     *
     * @param ya the y-coordinate of the first point
     * @param yb the y-coordinate of the second point
     * @return the vertical distance between the two points
     */
    public static double getVerticalDistance(double ya, double yb) {
        return Math.abs(ya - yb);
    }

    /**
     * Calculates the vertical distance between two vectors in 3D space.
     * The vertical distance is the absolute difference of their Y coordinates.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the vertical distance between the two vectors
     */
    public static double getVerticalDistance(Vec3d a, Vec3d b) {
        return Math.abs(a.getY() - b.getY());
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
     * Calculates the minimum distance from a point to a bounding box.
     *
     * @param box The bounding box to which the distance is calculated.
     * @param p The point from which the distance is calculated.
     * @return The minimum distance from the point to the bounding box.
     */
    public static double getMinimumDistanceToBox(Box box, Vec3d p) {
        return getMinimumDistanceToBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, p.x, p.y, p.z);
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
     * Calculates the pitch angle (rotation around the X-axis) in degrees for a given vector.
     *
     * @param vec the vector for which to calculate the pitch angle
     * @return the pitch angle in degrees
     */
    public static double getPitch(Vec3d vec) {
        double x = vec.getX();
        double y = vec.getY();
        double z = vec.getZ();
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0) {
            return Double.NaN;
        }
        double pitch = Math.asin(y / length);
        return Math.toDegrees(pitch);
    }

    /**
     * Calculates the pitch angle (rotation around the X-axis) in degrees for a given vector.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the pitch angle in degrees
     */
    public static double getPitch(Vec3d a, Vec3d b) {
        double x = b.getX() - a.getX();
        double y = b.getY() - a.getY();
        double z = b.getZ() - a.getZ();
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0) {
            return Double.NaN;
        }
        double pitch = Math.asin(y / length);
        return Math.toDegrees(pitch);
    }

    /**
     * Calculates the pitch angle (rotation around the X-axis) in degrees for a given vector.
     *
     * @param x the x-coordinate of the vector
     * @param y the y-coordinate of the vector
     * @param z the z-coordinate of the vector
     * @return the pitch angle in degrees
     */
    public static double getPitch(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0) {
            return Double.NaN;
        }
        double pitch = Math.asin(y / length);
        return Math.toDegrees(pitch);
    }

    /**
     * Calculates the pitch angle (rotation around the X-axis) in degrees for a given vector.
     *
     * @param xa the x-coordinate of the first vector
     * @param ya the y-coordinate of the first vector
     * @param za the z-coordinate of the first vector
     * @param xb the x-coordinate of the second vector
     * @param yb the y-coordinate of the second vector
     * @param zb the z-coordinate of the second vector
     * @return the pitch angle in degrees
     */
    public static double getPitch(double xa, double ya, double za, double xb, double yb, double zb) {
        double x = xb - xa;
        double y = yb - ya;
        double z = zb - za;
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0) {
            return Double.NaN;
        }
        double pitch = Math.asin(y / length);
        return Math.toDegrees(pitch);
    }

    /**
     * Calculates the yaw angle (rotation around the Y-axis) in degrees for a given vector.
     * Returns yaw in the range [-180°, 180°] for easier angle comparison.
     *
     * @param vec the vector for which to calculate the yaw angle
     * @return the yaw angle in degrees, in the range [-180°, 180°]
     */
    public static double getYaw(Vec3d vec) {
        double x = vec.getX();
        double z = vec.getZ();
        if (x == 0.0 && z == 0.0) {
            return Double.NaN;
        }
        double yaw = Math.atan2(-x, z);
        double d = Math.toDegrees(yaw);
        return d;
    }

    /**
     * Calculates the yaw angle (rotation around the Y-axis) in degrees for a given vector.
     * Returns yaw in the range [-180°, 180°] for easier angle comparison.
     *
     * @param a the first vector
     * @param b the second vector
     * @return the yaw angle in degrees, in the range [-180°, 180°]
     */
    public static double getYaw(Vec3d a, Vec3d b) {
        double x = b.getX() - a.getX();
        double z = b.getZ() - a.getZ();
        if (x == 0.0 && z == 0.0) {
            return Double.NaN;
        }
        double yaw = Math.atan2(-x, z);
        double d = Math.toDegrees(yaw);
        return d;
    }

    /**
     * Calculates the yaw angle (rotation around the Y-axis) in degrees for a given vector.
     * Returns yaw in the range [-180°, 180°] for easier angle comparison.
     *
     * @param x the x-coordinate of the vector
     * @param z the z-coordinate of the vector
     * @return the yaw angle in degrees, in the range [-180°, 180°]
     */
    public static double getYaw(double x, double z) {
        if (x == 0.0 && z == 0.0) {
            return Double.NaN;
        }
        double yaw = Math.atan2(-x, z);
        double d = Math.toDegrees(yaw);
        return d;
    }

    /**
     * Calculates the yaw angle (rotation around the Y-axis) in degrees for a given vector.
     * Returns yaw in the range [-180°, 180°] for easier angle comparison.
     *
     * @param xa the x-coordinate of the first vector
     * @param za the z-coordinate of the first vector
     * @param xb the x-coordinate of the second vector
     * @param zb the z-coordinate of the second vector
     * @return the yaw angle in degrees, in the range [-180°, 180°]
     */
    public static double getYaw(double xa, double za, double xb, double zb) {
        double x = xb - xa;
        double z = zb - za;
        if (x == 0.0 && z == 0.0) {
            return Double.NaN;
        }
        double yaw = Math.atan2(-x, z);
        double d = Math.toDegrees(yaw);
        return d;
    }

    /**
     * Calculates the density of points within a spherical region.
     * 
     * <p>This method counts the number of points from the given iterable that fall within
     * a sphere defined by a center point and radius, then divides by the sphere's volume
     * to determine point density.</p>
     * 
     * @param points an iterable collection of 3D points to evaluate
     * @param center the center point of the spherical region
     * @param radius the radius of the spherical region
     * @return the density value, calculated as the number of points within the sphere
     *         divided by the sphere's volume (4/3 * π * r³)
     */
    public static double getDensity(Iterable<Vec3d> points, Vec3d center, double radius) {
        double radiusSquared = radius * radius;
        int count = 0;
        for (Vec3d point : points) {
            double dx = point.getX() - center.getX();
            double dy = point.getY() - center.getY();
            double dz = point.getZ() - center.getZ();
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared <= radiusSquared) {
                count++;
            }
        }
        double volume = (4.0 / 3.0) * Math.PI * radiusSquared * Math.abs(radius);
        return count / volume;
    }

    /**
     * Calculates the density of entities within a specified radius around a center entity.
     * Only entities in the same world as the center entity are considered in the calculation.
     * 
     * @param points An iterable collection of entities to be evaluated for density calculation
     * @param center The entity at the center of the density calculation
     * @param radius The radius within which to calculate entity density
     * @return The calculated density value based on the positions of entities within the specified radius
     */
    public static double getDensity(Iterable<Entity> points, Entity center, double radius) {
        List<Vec3d> vecPoints = new ArrayList<>();
        for (Entity entity : points) {
            if (entity.getWorld() == center.getWorld()) {
                vecPoints.add(entity.getPos());
            }
        }
        return getDensity(vecPoints, center.getPos(), radius);
    }

    /**
     * Checks if two 3D points are within a specified range of each other.
     * Uses squared distance comparison to avoid expensive square root calculation.
     *
     * @param xa the x-coordinate of the first point
     * @param ya the y-coordinate of the first point
     * @param za the z-coordinate of the first point
     * @param xb the x-coordinate of the second point
     * @param yb the y-coordinate of the second point
     * @param zb the z-coordinate of the second point
     * @param range the maximum distance between the two points
     * @return {@code true} if the distance between the points is less than or equal to the range,
     *         {@code false} otherwise
     */
    public static boolean isInRange(double xa, double ya, double za, double xb, double yb, double zb, double range) {
        double dx = xa - xb;
        double dy = ya - yb;
        double dz = za - zb;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double rangeSquared = range * range;
        return distanceSquared <= rangeSquared;
    }

    /**
     * Checks if two 3D vectors are within a specified range of each other.
     * Uses squared distance comparison to avoid expensive square root calculation.
     *
     * @param a the first vector
     * @param b the second vector
     * @param range the maximum distance between the two vectors
     * @return {@code true} if the distance between the vectors is less than or equal to the range,
     *         {@code false} otherwise
     */
    public static boolean isInRange(Vec3d a, Vec3d b, double range) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double rangeSquared = range * range;
        return distanceSquared <= rangeSquared;
    }

    /**
     * Checks if two entities are within a specified range of each other.
     * <p>
     * This method calculates the Euclidean distance between two entities and compares it
     * against the specified range. The calculation uses squared distances to avoid the
     * computational cost of square root operations.
     * </p>
     *
     * @param a the first entity
     * @param b the second entity
     * @param range the maximum distance between the entities to be considered in range
     * @return {@code true} if both entities are in the same world and the distance between
     *         them is less than or equal to the specified range; {@code false} otherwise
     */
    public static boolean isInRange(Entity a, Entity b, double range) {
        if (a.getWorld() != b.getWorld()) {
            return false;
        }
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double rangeSquared = range * range;
        return distanceSquared <= rangeSquared;
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
                txa = Double.NEGATIVE_INFINITY;
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
                tya = Double.NEGATIVE_INFINITY;
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
                tza = Double.NEGATIVE_INFINITY;
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

    /**
     * Calculates the first intersection point of a ray with an axis-aligned bounding box (AABB).
     *
     * @param box The AABB with which the ray is intersected.
     * @param origin The origin of the ray.
     * @param pitch The pitch angle of the ray's direction.
     * @param yaw The yaw angle of the ray's direction.
     * @return A Vec3d representing the intersection point, or a Vec3d with NaN values if there is no intersection.
     */
    public static Vec3d getRaytraceFirstIntersection(Box box, Vec3d origin, double pitch, double yaw) {
        return getRaytraceFirstIntersection(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, origin.x, origin.y, origin.z, pitch, yaw);
    }
}
