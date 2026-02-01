/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.async;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Asynchronously calculates the region with highest entity density in a Minecraft world.
 * <p>
 * This calculator performs a complex spatial analysis to identify the location with the
 * maximum entity density, defined as the number of entities per unit volume within a
 * spherical region. The algorithm considers multiple constraints:
 * <ul>
 *   <li>Minimum radius constraint for the search sphere</li>
 *   <li>Minimum number of entities required within the sphere</li>
 *   <li>Entities must be in the same dimension</li>
 * </ul>
 * <p>
 * The calculation is performed asynchronously to avoid blocking the main server thread,
 * as it has O(n²log₂n) complexity where n is the number of entities. Results are
 * communicated back to the command source upon completion.
 * <p>
 * This class creates immutable snapshots of entity positions to ensure
 * thread-safe processing. Entity references are preserved for result retrieval but
 * must never be accessed in the async context.
 */
public class EntityDensityCalculator extends AsyncTaskExecutor {
    
    // Input parameters
    
    /**
     * The minimum radius (in blocks) for the search sphere.
     * Entities must be at least this distance apart to satisfy the density constraint.
     * Special values:
     * <ul>
     *   <li>NaN: Consider all entities regardless of distance</li>
     *   <li>Infinity: Find dimension with maximum entities</li>
     *   <li>0.0: Allow infinitesimal radius (single point density)</li>
     * </ul>
     */
    private final double minRadius;
    
    /**
     * The minimum number of entities required within the search sphere.
     * The algorithm will find a sphere containing at least this many entities
     * with the highest possible density.
     */
    private final int minNumber;
    
    /**
     * The command context from which this calculation was initiated.
     * Used to send feedback messages to the command source upon completion.
     * May be null if no feedback is required.
     */
    private final CommandContext<ServerCommandSource> context;

    // Snapshot of entities for thread-safe processing
    
    /**
     * Immutable snapshots of all entities to be analyzed.
     * Created at construction time to ensure thread-safe processing in the async context.
     * Each snapshot captures the entity's position, dimension, biome, and type identifier.
     */
    private final List<EntitySnapshot> snapshots;

    // Async Result fields
    
    /**
     * The entity at the center of the highest-density region found.
     * This is the location where the maximum entity density was detected.
     * Null if no valid region was found.
     */
    private volatile EntitySnapshot resultEntity;
    
    /**
     * The nearest entity of the dominant type within the highest-density region.
     * The "dominant type" is the entity type that appears most frequently in the region.
     * This helps identify what is causing the high density.
     * Null if no valid region was found.
     */
    private volatile EntitySnapshot resultCause;
    
    /**
     * The radius (in blocks) of the highest-density region.
     * Special values:
     * <ul>
     *   <li>NaN: Could not determine a valid radius</li>
     *   <li>Infinity: The region spans an entire dimension</li>
     *   <li>0.0: Infinitesimal radius (single point)</li>
     * </ul>
     */
    private volatile double finalRadius;
    
    /**
     * The density (entities per cubic block) of the highest-density region.
     * Calculated as: count / ((4/3) * π * radius³)
     * Special values:
     * <ul>
     *   <li>NaN: Could not calculate density</li>
     *   <li>Infinity: Density at a single point (radius = 0)</li>
     *   <li>0.0: Infinite radius with finite entity count</li>
     * </ul>
     */
    private volatile double finalDensity;
    
    /**
     * The total number of entities within the highest-density region.
     * This includes all entity types within the sphere of radius {@link #finalRadius}
     * centered at {@link #resultEntity}.
     */
    private volatile int finalCount;
    
    /**
     * The number of entities of the dominant type within the highest-density region.
     * The dominant type is the entity type that appears most frequently in the region.
     */
    private volatile int finalNumber;
        
    /**
     * Immutable snapshot of an entity's state at a point in time.
     * <p>
     * This class captures all relevant information about an entity for spatial analysis
     * without retaining references to non-thread-safe Minecraft objects like World.
     * The snapshot includes:
     * <ul>
     *   <li>3D position coordinates (x, y, z)</li>
     *   <li>Dimension identifier for cross-dimension comparisons</li>
     *   <li>Biome identifier for potential biome-based analysis</li>
     *   <li>Entity type identifier for grouping and classification</li>
     *   <li>Original entity reference (for main-thread result retrieval only)</li>
     * </ul>
     * <p>
     * Thread Safety: All fields except {@link #entity} are safe to access from any thread.
     * The {@link #entity} field must only be accessed from the main server thread.
     */
    private static class EntitySnapshot {

        final double x;
        final double y;
        final double z;
        final Identifier dimension;
        final Identifier biome;
        final Identifier entityType;
        final Entity entity;
        
        /**
         * Creates an immutable snapshot of an entity's current state.
         * <p>
         * This constructor extracts all necessary information from the entity and its
         * world context, creating a thread-safe representation that can be safely
         * processed asynchronously.
         *
         * @param entity The entity to snapshot. Must not be null and must have a valid world.
         */
        EntitySnapshot(Entity entity) {
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            this.dimension = entity.getWorld().getRegistryKey().getValue();
            this.biome = entity.getWorld().getBiome(entity.getBlockPos()).getKey().map(key -> key.getValue()).orElse(null);
            this.entityType = EntityType.getId(entity.getType());
            this.entity = entity;
        }

        /**
         * Calculates the squared Euclidean distance to another entity snapshot.
         * <p>
         * Using squared distance avoids expensive square root calculations during
         * comparison operations. The actual distance can be obtained by taking the
         * square root of this value if needed.
         * <p>
         * Entities in different dimensions are considered infinitely far apart.
         *
         * @param other The other entity snapshot to measure distance to.
         * @return The squared distance in blocks², or NaN if the entities are in
         *         different dimensions or if other is null.
         */
        double squaredDistanceTo(EntitySnapshot other) {
            if (other == null || !this.dimension.equals(other.dimension)) {
                return Double.NaN; // Different dimensions
            }
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
    
    /**
     * Constructs a new entity density calculator with the specified parameters.
     * <p>
     * This constructor creates immutable snapshots of all provided entities to ensure
     * thread-safe processing. The actual calculation is performed asynchronously when
     * the task is executed.
     *
     * @param context The command context for sending feedback messages. May be null if
     *                no feedback is required.
     * @param entities An iterable of entities to analyze. Must not be null. Empty iterables
     *                 are allowed and will result in no density region being found.
     * @param minRadius The minimum radius constraint in blocks. Absolute value is used.
     *                  Special values: NaN (no radius constraint), Infinity (dimension-wide),
     *                  0.0 (allow single-point density).
     * @param minNumber The minimum number of entities required in the density region.
     *                  Absolute value is used.
     */
    public EntityDensityCalculator(CommandContext<ServerCommandSource> context, Iterable<Entity> entities, double minRadius, int minNumber) {
        this.context = context;
        this.minRadius = Math.abs(minRadius);
        this.minNumber = Math.abs(minNumber);
        
        // Create snapshots
        this.snapshots = new ArrayList<>();
        for (Entity entity : entities) {
            this.snapshots.add(new EntitySnapshot(entity));
        }

        this.resultEntity = null;
        this.resultCause = null;
        this.finalRadius = Double.NaN;
        this.finalDensity = Double.NaN;
        this.finalCount = 0;
        this.finalNumber = 0;
    }

    /**
     * Groups all entity snapshots by their dimension identifier.
     * <p>
     * This is used to ensure that density calculations only consider entities within
     * the same dimension, as cross-dimensional distances are not meaningful.
     *
     * @return A map where keys are dimension identifiers and values are lists of
     *         entity snapshots in that dimension.
     */
    private Map<Identifier, List<EntitySnapshot>> groupByDimension() {
        Map<Identifier, List<EntitySnapshot>> map = new HashMap<>();
        for (EntitySnapshot snapshot : snapshots) {
            map.computeIfAbsent(snapshot.dimension, k -> new ArrayList<>()).add(snapshot);
        }
        return map;
    }

    /**
     * Identifies the dominant entity type in a collection and returns all entities of that type.
     * <p>
     * The dominant type is defined as the entity type that appears most frequently in the
     * provided list. This helps identify what is causing high entity density (e.g., a mob
     * farm with many cows, or a village with many villagers).
     *
     * @param entities The list of entity snapshots to analyze. Must not be null or empty.
     * @return A list containing all entities of the most common type. If multiple types
     *         tie for most common, returns one of the tied groups.
     */
    private List<EntitySnapshot> getDominantEntities(List<EntitySnapshot> entities) {
        Map<Identifier, List<EntitySnapshot>> typeMap = new HashMap<>();
        int maxCount = 0;
        List<EntitySnapshot> dominantList = new ArrayList<>();
        for (EntitySnapshot snapshot : entities) {
            List<EntitySnapshot> list = typeMap.computeIfAbsent(snapshot.entityType, k -> new ArrayList<>());
            list.add(snapshot);
            if (list.size() > maxCount) {
                maxCount = list.size();
                dominantList = list;
            }
        }
        return dominantList;
    }

    /**
     * Finds the entity nearest to a specified center point from a list of candidates.
     * <p>
     * This is used to identify the closest entity of the dominant type, which helps
     * pinpoint the specific cause of high density within a region.
     *
     * @param entities The list of candidate entities to search. Must not be null.
     * @param center The reference point to measure distances from. Must not be null.
     * @param fallback The entity to return if no valid nearest entity is found
     *                 (e.g., all entities are in different dimensions).
     * @return The nearest entity to the center, or the fallback if none found.
     */
    private EntitySnapshot getNearestEntity(List<EntitySnapshot> entities, EntitySnapshot center, EntitySnapshot fallback) {
        EntitySnapshot nearest = null;
        double minDistSq = Double.POSITIVE_INFINITY;
        for (EntitySnapshot snapshot : entities) {
            double distSq = center.squaredDistanceTo(snapshot);
            if (!Double.isNaN(distSq) && distSq < minDistSq) {
                minDistSq = distSq;
                nearest = snapshot;
            }
        }
        if (nearest == null) {
            return fallback;
        }
        return nearest;
    }
    
    /**
     * Executes the entity density calculation algorithm asynchronously.
     * <p>
     * This method implements a sophisticated spatial analysis algorithm with O(n²log₂n)
     * complexity, where n is the number of entities. The algorithm handles several
     * special cases before performing the main density calculation:
     * <ol>
     *   <li>Empty entity set: Returns no result.</li>
     *   <li>NaN radius: Considers all entities regardless of distance.</li>
     *   <li>Infinite radius: Finds the dimension with maximum entities.</li>
     *   <li>Unachievable minNumber: Returns global statistics.</li>
     *   <li>Zero radius with minNumber ≤ 1: Returns infinitesimal density.</li>
     *   <li>Normal case: Performs full density optimization.</li>
     * </ol>
     * <p>
     * For the main algorithm, it tests each entity as a potential center and finds
     * the optimal radius that maximizes density while satisfying the minRadius and
     * minNumber constraints.
     * <p>
     * Thread Safety: This method runs on a background thread and only accesses
     * thread-safe snapshot data. It calls {@link #markAsyncFinished()} upon completion
     * to signal that results are ready.
     *
     * @see AsyncTaskExecutor#executeAsyncTask()
     */
    @Override
    protected void executeAsyncTask() {
        // Case 1: No entities
        if (snapshots.isEmpty()) {
            resultEntity = null;
            resultCause = null;
            finalRadius = Double.NaN;
            finalDensity = Double.NaN;
            finalCount = 0;
            finalNumber = 0;
            markAsyncFinished();
            return;
        }
        
        // Case 2: minRadius is NaN - all entities are considered within range
        if (Double.isNaN(minRadius)) {
            List<EntitySnapshot> dominantEntities = getDominantEntities(snapshots);
            EntitySnapshot nearestDominant = getNearestEntity(dominantEntities, snapshots.get(0), dominantEntities.get(0));
            resultEntity = snapshots.get(0);
            resultCause = nearestDominant;
            finalRadius = Double.NaN;
            finalDensity = Double.NaN;
            finalCount = snapshots.size();
            finalNumber = dominantEntities.size();
            markAsyncFinished();
            return;
        }
        
        // Case 3: minRadius is infinity - find dimension with max entities
        Map<Identifier, List<EntitySnapshot>> dimensionMap = groupByDimension();
        Identifier maxDimension = null;
        int maxCount = 0;
        for (Map.Entry<Identifier, List<EntitySnapshot>> entry : dimensionMap.entrySet()) {
            if (entry.getValue().size() > maxCount) {
                maxCount = entry.getValue().size();
                maxDimension = entry.getKey();
            }
        }
        List<EntitySnapshot> maxEntities = dimensionMap.get(maxDimension);
        if (Double.isInfinite(minRadius)) {
            List<EntitySnapshot> dominantEntities = getDominantEntities(maxEntities);
            EntitySnapshot nearestDominant = getNearestEntity(dominantEntities, maxEntities.get(0), dominantEntities.get(0));
            resultEntity = maxEntities.get(0);
            resultCause = nearestDominant;
            finalRadius = Double.POSITIVE_INFINITY;
            finalDensity = 0.0;
            finalCount = maxEntities.size();
            finalNumber = dominantEntities.size();
            markAsyncFinished();
            return;
        }

        // Case 4: Check if minNumber is achievable
        if (minNumber > maxCount) {
            List<EntitySnapshot> dominantEntities = getDominantEntities(snapshots);
            EntitySnapshot nearestDominant = getNearestEntity(dominantEntities, snapshots.get(0), dominantEntities.get(0));
            resultEntity = snapshots.get(0);
            resultCause = nearestDominant;
            finalRadius = Double.NaN;
            finalDensity = Double.NaN;
            finalCount = snapshots.size();
            finalNumber = dominantEntities.size();
            markAsyncFinished();
            return;
        }
        
        // Case 5: minRadius == 0 && minNumber <= 1 - infinitesimal radius
        if (minRadius == 0.0 && minNumber <= 1) {
            resultEntity = snapshots.get(0);
            resultCause = snapshots.get(0);
            finalRadius = 0.0;
            finalDensity = Double.POSITIVE_INFINITY;
            finalCount = 1;
            finalNumber = 1;
            markAsyncFinished();
            return;
        }
        
        // Main algorithm: Find maximum density
        double maxDensity = Double.NEGATIVE_INFINITY;
        EntitySnapshot bestEntity = null;
        double bestRadiusSquared = 0.0;
        final double minRadiusSquared = minRadius * minRadius;
        for (List<EntitySnapshot> availableEntities : dimensionMap.values()) {
            if (availableEntities.size() < minNumber) {
                // Not enough entities in this dimension, skip
                continue; 
            }

            for (EntitySnapshot currentCenter : availableEntities) {
                // Collect distances to all other entities in the same dimension
                List<Double> distancesSquared = new ArrayList<>();
                for (EntitySnapshot other : availableEntities) {
                    distancesSquared.add(currentCenter.squaredDistanceTo(other));
                }
                distancesSquared.sort(Double::compare); // assert distancesSquared.get(0) == 0.0 because of self-distance
                int potentialIndex = Math.max(0, minNumber - 1);  // O(n²log₂(n)) complexity
                for (int i = potentialIndex; i < distancesSquared.size(); i++) {
                    if (distancesSquared.get(i) < minRadiusSquared) {
                        continue;
                    }
                    double potentialRadiusSquared = distancesSquared.get(i);
                    double potentialRadius = Math.sqrt(potentialRadiusSquared);
                    double potentialVolume = (4.0 / 3.0) * Math.PI * potentialRadius * potentialRadiusSquared;
                    double potentialDensity = (1.0 + i) / potentialVolume;
                    if (potentialDensity > maxDensity) {
                        maxDensity = potentialDensity;
                        bestEntity = currentCenter;
                        bestRadiusSquared = potentialRadiusSquared;
                    }
                }
            }
        }

        if (bestEntity == null) {
            // Unlikely to happen
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Failed to find candidate entity for density calculation.");
            resultEntity = null;
            resultCause = null;
            finalRadius = Double.NaN;
            finalDensity = Double.NaN;
            finalCount = 0;
            finalNumber = 0;
            markAsyncFinished();
            return;
        }
        
        List<EntitySnapshot> candidateEntities = dimensionMap.get(bestEntity.dimension);
        List<EntitySnapshot> entitiesInRange = new ArrayList<>();
        for (EntitySnapshot snapshot : candidateEntities) {
            double distSq = bestEntity.squaredDistanceTo(snapshot);
            if (!Double.isNaN(distSq) && distSq <= bestRadiusSquared) {
                entitiesInRange.add(snapshot);
            }
        }

        if (entitiesInRange.size() < minNumber || bestRadiusSquared < minRadiusSquared) {
            // Unlikely to happen
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Failed to validate the candidate entity's neighborhood.");
        }

        List<EntitySnapshot> dominantEntities = getDominantEntities(entitiesInRange);
        EntitySnapshot nearestDominant = getNearestEntity(dominantEntities, bestEntity, dominantEntities.get(0));
        resultEntity = bestEntity;
        resultCause = nearestDominant;
        finalRadius = Math.sqrt(bestRadiusSquared);
        finalDensity = maxDensity;
        finalCount = entitiesInRange.size();
        finalNumber = dominantEntities.size();
        markAsyncFinished();
    }

    /**
     * Sends feedback messages to the command source after calculation completes.
     * <p>
     * This method runs on the main server thread after the async calculation finishes.
     * It formats and sends appropriate feedback messages based on whether a high-density
     * region was found.
     * <p>
     * If no context was provided during construction, or if the executing player has
     * disconnected, this method returns silently without sending feedback.
     *
     * @see AsyncTaskExecutor#taskAfterCompletion()
     */
    @Override
    protected void taskAfterCompletion() {
        if (context == null) {
            return;
        }

        if (context.getSource().isExecutedByPlayer()) {
            if (context.getSource().getPlayer() == null || context.getSource().getPlayer().isDisconnected()) {
                LoggerFactory.getLogger(Util.LOGGERNAME).info("FMinecraftMod: Get entity density command executed but the player has disconnected.");
                return;
            }
        }

        if (resultEntity == null || resultCause == null) {
            // No result
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.message.entitywarning", snapshots.size()), false);
        } else {
            // Has result
            final String totalCount = Integer.toString(snapshots.size());
            final Text coordText = Util.parseCoordText(resultEntity.dimension, resultEntity.biome, resultEntity.x, resultEntity.y, resultEntity.z);
            final String entityRadius = String.format("%.2f", finalRadius);
            final String entityCount = Integer.toString(finalCount);
            final String causeCount = Integer.toString(finalNumber);
            final Text entityCauseText = resultCause.entity.getDisplayName();
            context.getSource().sendFeedback(() -> Util.parseTranslatableText("fmod.message.entitydensity", totalCount, coordText, entityRadius, entityCount, causeCount, entityCauseText), false);
        }
    }

    /**
     * Gets the entity at the center of the highest-density region.
     * <p>
     * This is the location where the maximum entity density was detected during
     * the calculation.
     *
     * @return The center entity, or null if no valid density region was found.
     * @throws IllegalStateException If called before the calculation has completed
     *                               (i.e., before {@link #taskAfterCompletion()} has executed).
     */
    @Nullable
    public Entity getEntity() throws IllegalStateException {
        if (!isAfterCompletionExecuted()) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet.");
        }
        if (resultEntity == null) {
            return null;
        }
        return resultEntity.entity;
    }

    /**
     * Gets the nearest entity of the dominant type within the highest-density region.
     * <p>
     * The "dominant type" is the entity type that appears most frequently in the region.
     * This entity represents the primary cause of the high density.
     *
     * @return The cause entity, or null if no valid density region was found.
     * @throws IllegalStateException If called before the calculation has completed
     *                               (i.e., before {@link #taskAfterCompletion()} has executed).
     */
    @Nullable
    public Entity getCause() throws IllegalStateException {
        if (!isAfterCompletionExecuted()) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet.");
        }
        if (resultCause == null) {
            return null;
        }
        return resultCause.entity;
    }

    /**
     * Gets the x-coordinate of the highest-density region's center.
     *
     * @return The x-coordinate in blocks.
     * @throws IllegalStateException If called before the calculation has completed,
     *                               or if no entity was found.
     */
    public double getX() throws IllegalStateException {
        if (!isAfterCompletionExecuted() || resultEntity == null) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet or no entity found.");
        }
        return resultEntity.x;
    }

    /**
     * Gets the y-coordinate of the highest-density region's center.
     *
     * @return The y-coordinate in blocks.
     * @throws IllegalStateException If called before the calculation has completed,
     *                               or if no entity was found.
     */
    public double getY() throws IllegalStateException {
        if (!isAfterCompletionExecuted() || resultEntity == null) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet or no entity found.");
        }
        return resultEntity.y;
    }

    /**
     * Gets the z-coordinate of the highest-density region's center.
     *
     * @return The z-coordinate in blocks.
     * @throws IllegalStateException If called before the calculation has completed,
     *                               or if no entity was found.
     */
    public double getZ() throws IllegalStateException {
        if (!isAfterCompletionExecuted() || resultEntity == null) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet or no entity found.");
        }
        return resultEntity.z;
    }

    /**
     * Gets the dimension identifier of the highest-density region.
     *
     * @return The dimension identifier (e.g., "minecraft:overworld", "minecraft:the_nether").
     * @throws IllegalStateException If called before the calculation has completed,
     *                               or if no entity was found.
     */
    public Identifier getDimension() throws IllegalStateException {
        if (!isAfterCompletionExecuted() || resultEntity == null) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet or no entity found.");
        }
        return resultEntity.dimension;
    }

    /**
     * Gets the biome identifier at the highest-density region's center.
     *
     * @return The biome identifier (e.g., "minecraft:plains", "minecraft:desert"),
     *         or null if the biome has no identifier.
     * @throws IllegalStateException If called before the calculation has completed,
     *                               or if no entity was found.
     */
    public Identifier getBiome() throws IllegalStateException {
        if (!isAfterCompletionExecuted() || resultEntity == null) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet or no entity found.");
        }
        return resultEntity.biome;
    }

    /**
     * Gets the radius of the highest-density region.
     * <p>
     * This is the radius of the sphere centered at the result entity that contains
     * the calculated number of entities with maximum density.
     *
     * @return The radius in blocks. May be NaN (no valid radius), Infinity (dimension-wide),
     *         or 0.0 (infinitesimal/single-point density).
     * @throws IllegalStateException If called before the calculation has completed.
     */
    public double getRadius() throws IllegalStateException {
        if (!isAfterCompletionExecuted()) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet.");
        }
        return finalRadius;
    }

    /**
     * Gets the density of the highest-density region.
     * <p>
     * Density is calculated as the number of entities divided by the volume of the sphere:
     * density = count / ((4/3) * π * radius³)
     *
     * @return The density in entities per cubic block. May be NaN (could not calculate),
     *         Infinity (radius = 0), or 0.0 (infinite radius).
     * @throws IllegalStateException If called before the calculation has completed.
     */
    public double getDensity() throws IllegalStateException {
        if (!isAfterCompletionExecuted()) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet.");
        }
        return finalDensity;
    }

    /**
     * Gets the total number of entities within the highest-density region.
     * <p>
     * This includes all entity types within the sphere of the calculated radius.
     *
     * @return The total entity count in the region.
     * @throws IllegalStateException If called before the calculation has completed.
     */
    public int getCount() throws IllegalStateException {
        if (!isAfterCompletionExecuted()) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet.");
        }
        return finalCount;
    }

    /**
     * Gets the number of entities of the dominant type within the highest-density region.
     * <p>
     * The dominant type is the entity type that appears most frequently in the region.
     * This count represents how many entities of that specific type were found.
     *
     * @return The count of dominant-type entities in the region.
     * @throws IllegalStateException If called before the calculation has completed.
     */
    public int getNumber() throws IllegalStateException {
        if (!isAfterCompletionExecuted()) {
            throw new IllegalStateException("EntityDensityCalculator: Task not finished yet.");
        }
        return finalNumber;
    }

    /**
     * Gets the total number of entities provided as input to this calculator.
     * <p>
     * This represents the size of the original entity collection that was analyzed,
     * regardless of the calculation results.
     *
     * @return The total number of input entities.
     */
    public int getInputNumber() {
        return snapshots.size();
    }
}
