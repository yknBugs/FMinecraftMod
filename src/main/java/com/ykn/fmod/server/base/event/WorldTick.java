/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.async.EntityDensityCalculator;
import com.ykn.fmod.server.base.data.AfkState;
import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.BiomeMessage;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.base.util.GameMath;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.phys.Vec3;

/**
 * The per-tick player features: AFK, biome, travel/teleport, sleep status, entity warnings.
 *
 * <p>This class is the existing feature only; {@link GameEventHandler#serverTickEventDispatcher}
 * runs it and then dispatches the events its {@link PlayerTickReport}s describe, so the feature
 * never calls the dispatcher itself.
 */
public final class WorldTick {

    private WorldTick() {
    }

    /**
     * Runs one server tick of the existing per-tick features and reports what the tick produced.
     *
     * <p>The report is what {@link GameEventHandler#serverTickEventDispatcher} turns into events:
     * this class detects the transitions (AFK on/off, teleport, level and hunger change) while it
     * already has the previous-tick state at hand, but does not dispatch anything itself.
     *
     * @param server the server being ticked
     * @return one report per online player, in the order they were ticked
     */
    public static List<PlayerTickReport> runFeatures(MinecraftServer server) {
        ServerData serverData = Util.getServerData(server);
        checkEntityNumber(server, serverData);
        List<PlayerTickReport> reports = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerData playerData = serverData.getPlayerData(player);
            AfkTransition afk = handleAfkPlayers(player, playerData);
            handleChangeBiomePlayer(player, serverData, playerData);
            PlayerTeleport teleport = handlePlayerTravelStatus(player, playerData);
            handlePlayerCanSleepStatus(player, playerData);
            int oldLevel = playerData.getLastLevel();
            int oldHunger = playerData.getLastHunger();
            int newLevel = player.experienceLevel;
            int newHunger = player.getFoodData().getFoodLevel();
            playerData.updateLastTickData(player);
            reports.add(new PlayerTickReport(player, afk, teleport, oldLevel, newLevel, oldHunger, newHunger));
        }
        return reports;
    }

    /**
     * What one player's feature pass detected, for {@link GameEventHandler} to dispatch.
     *
     * @param player      the player that was ticked
     * @param afk         the AFK transition of this tick
     * @param teleport    the detected teleport, or {@code null}
     * @param oldLevel    experience level before this tick
     * @param newLevel    experience level after this tick
     * @param oldHunger   food level before this tick
     * @param newHunger   food level after this tick
     */
    public record PlayerTickReport(ServerPlayer player, AfkTransition afk, PlayerTeleport teleport,
            int oldLevel, int newLevel, int oldHunger, int newHunger) {

        /**
         * Whether the player just crossed the AFK threshold.
         *
         * @return {@code true} when this tick is the transition into AFK
         */
        public boolean enteredAfk() {
            return afk.direction() > 0;
        }

        /**
         * Whether the player just came back from being AFK.
         *
         * @return {@code true} when this tick is the transition out of AFK
         */
        public boolean returnedFromAfk() {
            return afk.direction() < 0;
        }

        /**
         * Whether the player's experience level rose during this tick.
         *
         * @return {@code true} when the level went up
         */
        public boolean leveledUp() {
            return newLevel > oldLevel;
        }

        /**
         * Whether the player's food level changed during this tick.
         *
         * @return {@code true} when the food level is different
         */
        public boolean hungerChanged() {
            return newHunger != oldHunger;
        }
    }

    /**
     * An AFK episode transition detected by one tick.
     *
     * @param direction {@code 1} entered AFK, {@code -1} returned, {@code 0} no transition
     * @param ticks     the tick count at the moment of the transition
     */
    public record AfkTransition(int direction, int ticks) {

        /** No transition happened. */
        public static final AfkTransition NONE = new AfkTransition(0, 0);
    }

    /**
     * A detected teleport, kept until the event is dispatched.
     *
     * @param origin the dimension the player was in
     * @param from   the position before the teleport
     * @param to     the position after the teleport
     */
    public record PlayerTeleport(ResourceLocation origin, Vec3 from, Vec3 to) {
    }

    private static void checkEntityNumber(MinecraftServer server, ServerData serverData) {
        // Check if have finished previous calculation (Happens every tick because async result must be feeded back immediately after finishing)
        EntityDensityCalculator activeCalculator = serverData.getActiveDensityCalculator();
        if (activeCalculator != null && activeCalculator.isAfterCompletionExecuted()) {
            // If finished, retrieve result and clear the task
            if (activeCalculator.getEntity() == null || activeCalculator.getCause() == null) {
                // No result
                Component mainText = Util.parseTranslatableText("fmod.message.entitywarning.main", activeCalculator.getInputNumber()).withStyle(ChatFormatting.RED);
                Component otherText = Util.parseTranslatableText("fmod.message.entitywarning.other").withStyle(ChatFormatting.RED);
                Util.getServerConfig().getEntityNumberWarning().postMessage(server, mainText, otherText);
            } else {
                // Has result
                final String totalCount = Integer.toString(activeCalculator.getInputNumber());
                final Component coordText = Util.parseCoordText(activeCalculator.getDimension(), activeCalculator.getBiome(), activeCalculator.getX(), activeCalculator.getY(), activeCalculator.getZ());
                final Component biomeText = Util.getBiomeText(activeCalculator.getBiome());
                final String entityRadius = String.format("%.2f", activeCalculator.getRadius());
                final String entityCount = Integer.toString(activeCalculator.getCount());
                final String causeCount = Integer.toString(activeCalculator.getNumber());
                final Component entityCauseText = activeCalculator.getCause().getDisplayName();
                Component mainText = Util.parseTranslatableText("fmod.message.entitydensity.main", totalCount, coordText, entityRadius, entityCount, causeCount, entityCauseText).withStyle(ChatFormatting.RED);
                Component otherText = Util.parseTranslatableText("fmod.message.entitydensity.other", biomeText, entityRadius, entityCount).withStyle(ChatFormatting.RED);
                Util.getServerConfig().getEntityDensityWarning().postMessage(server, mainText, otherText);
            }
            serverData.setLastCheckEntityTick();
            serverData.setLastCheckDensityTick();
            serverData.tryRemoveActiveDensityCalculator();
            return;
        }

        boolean satisfyNumberCondition = true;
        boolean satisfyDensityCondition = true;

        // If no finished calculation, wait after configured interval
        if (serverData.getCheckEntityTickPassed() < Util.getServerConfig().getEntityNumberInterval()) {
            satisfyNumberCondition = false;
        }
        if (serverData.getCheckDensityTickPassed() < Util.getServerConfig().getEntityDensityInterval()) {
            satisfyDensityCondition = false;
        }
        if (!satisfyNumberCondition && !satisfyDensityCondition) {
            return;
        }

        // Now we should check entity number and density
        List<Entity> allEntities = new ArrayList<>();
        for (ServerLevel world : server.getAllLevels()) {
            List<Entity> entities = Util.getAllEntities(world);
            allEntities.addAll(entities);
        }

        // Check if exceed threshold
        if (allEntities.size() < Util.getServerConfig().getEntityNumberThreshold()) {
            serverData.setLastCheckEntityTick();
            satisfyNumberCondition = false;
        }
        if (allEntities.size() < Util.getServerConfig().getEntityDensityThreshold()) {
            serverData.setLastCheckDensityTick();
            satisfyDensityCondition = false;
        }
        if (!satisfyNumberCondition && !satisfyDensityCondition) {
            return;
        }

        // Check if we have an active calculator
        if (satisfyDensityCondition) {
            if (activeCalculator == null) {
                // No active calculator, create a new one, we will broadcast result after finishing async calculation
                serverData.setLastCheckEntityTick();
                serverData.setLastCheckDensityTick();
                satisfyNumberCondition = false; // We already start density calculation, no need to start number calculation
                EntityDensityCalculator calculator = new EntityDensityCalculator(null, allEntities, Util.getServerConfig().getEntityDensityRadius(), Util.getServerConfig().getEntityDensityNumber());
                serverData.trySetActiveDensityCalculator(calculator);
                return;
            } else {
                // Have active calculator, wait for result
                serverData.setLastCheckDensityTick();
            }
        }

        if (satisfyNumberCondition) {
            serverData.setLastCheckEntityTick();
            Component mainText = Util.parseTranslatableText("fmod.message.entitywarning.main", allEntities.size()).withStyle(ChatFormatting.RED);
            Component otherText = Util.parseTranslatableText("fmod.message.entitywarning.other").withStyle(ChatFormatting.RED);
            Util.getServerConfig().getEntityNumberWarning().postMessage(server, mainText, otherText);
        }
    }

    private static AfkTransition handleAfkPlayers(ServerPlayer player, PlayerData playerData) {
        boolean moved = playerData.isFacingDirectionChanged(player);
        int ticks = playerData.getAfkTicks();
        int transition = playerData.afkState().update(ticks,
                Util.getServerConfig().getBroadcastAfkThreshold(), moved);
        if (moved) {
            postMessageToBackPlayer(player, playerData, transition == AfkState.RETURNED);
            playerData.resetAfkTicks();
            return transition == AfkState.RETURNED ? new AfkTransition(-1, ticks) : AfkTransition.NONE;
        }
        postMessageToAfkingPlayer(player, playerData, transition == AfkState.ENTERED);
        playerData.updateAfkTicks();
        return transition == AfkState.ENTERED ? new AfkTransition(1, ticks) : AfkTransition.NONE;
    }

    private static void postMessageToAfkingPlayer(ServerPlayer player, PlayerData playerData, boolean becomesAfk) {
        if (playerData.getAfkTicks() > Util.getServerConfig().getInformAfkThreshold() && playerData.getAfkTicks() % 20 == 0) {
            Component mainText = Util.parseTranslatableText("fmod.message.afk.inform.main", player.getDisplayName(), (int) (playerData.getAfkTicks() / 20));
            Component otherText = Util.parseTranslatableText("fmod.message.afk.inform.other", player.getDisplayName());
            Util.getServerConfig().getInformAfk().postMessage(player, mainText, otherText);
        }
        if (becomesAfk) {
            Component playerName = player.getDisplayName();
            Component coord = Util.parseCoordText(player);
            Component mainText = Util.parseTranslatableText("fmod.message.afk.broadcast.main", playerName, coord);
            Component otherText = Util.parseTranslatableText("fmod.message.afk.broadcast.other", playerName);
            Util.getServerConfig().getBroadcastAfk().postMessage(player, mainText, otherText);
        }
    }

    private static void postMessageToBackPlayer(ServerPlayer player, PlayerData playerData, boolean wasAfk) {
        if (wasAfk) {
            Component mainText = Util.parseTranslatableText("fmod.message.afk.stop.main", player.getDisplayName(), (int) (playerData.getAfkTicks() / 20));
            Component otherText = Util.parseTranslatableText("fmod.message.afk.stop.other", player.getDisplayName());
            Util.getServerConfig().getStopAfk().postMessage(player, mainText, otherText);
        }
    }

    private static void handleChangeBiomePlayer(ServerPlayer player, ServerData serverData, PlayerData playerData) {
        ResourceLocation biomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(ResourceKey::location).orElse(null);
        if (playerData.isBiomeChanged(player)) {
            serverData.submitScheduledTask(new BiomeMessage(player, biomeId));
        }
    }

    private static void handlePlayerCanSleepStatus(ServerPlayer player, PlayerData playerData) {
        boolean canSleep = player.level().dimensionType().natural() && !player.level().isDay() && player.level().dimensionType().bedWorks();
        boolean cannotSleep = player.level().dimensionType().natural() && player.level().isDay() && player.level().dimensionType().bedWorks();
        Boolean currentCanSleepStatus = null;
        if (canSleep != cannotSleep) {
            // We know the information
            currentCanSleepStatus = canSleep;
        }
        // Show message if status changed
        if (currentCanSleepStatus != null && !currentCanSleepStatus.equals(playerData.getLastCanSleep())) {
            if (currentCanSleepStatus) {
                Component mainText = Util.parseTranslatableText("fmod.message.sleep.can.main", player.getDisplayName(), Util.parseCoordText(player));
                Component otherText = Util.parseTranslatableText("fmod.message.sleep.can.other", player.getDisplayName());
                Util.getServerConfig().getPlayerCanSleepMessage().postMessage(player, mainText, otherText);
            } else {
                Component mainText = Util.parseTranslatableText("fmod.message.sleep.cannot.main", player.getDisplayName(), Util.parseCoordText(player));
                Component otherText = Util.parseTranslatableText("fmod.message.sleep.cannot.other", player.getDisplayName());
                Util.getServerConfig().getPlayerCanSleepMessage().postMessage(player, mainText, otherText);
            }
        }
        playerData.setLastCanSleep(currentCanSleepStatus);
    }

    private static PlayerTeleport handlePlayerTravelStatus(ServerPlayer player, PlayerData playerData) {
        int window = Util.getServerConfig().getTravelWindow();

        // Update positions history
        int maxSamples = window + 1;
        playerData.updatePositionHistory(player, maxSamples);

        Vec3[] snapshot = playerData.getRecentPositions();
        int lastIdx = snapshot.length - 1;

        // Check if the player has teleported
        double teleportThreshold = Util.getServerConfig().getTeleportThreshold();
        if (lastIdx > 0 && GameMath.getHorizontalEuclideanDistance(snapshot[lastIdx - 1], snapshot[lastIdx]) > teleportThreshold) {
            playerData.clearPositionHistory(player);
            return reportTeleport(player, playerData, snapshot[lastIdx - 1], snapshot[lastIdx]);
        }

        // Check if player changed dimensions
        if (playerData.isDimensionChanged(player)) {
            playerData.clearPositionHistory(player);
            return lastIdx > 0 ? reportTeleport(player, playerData, snapshot[lastIdx - 1], snapshot[lastIdx]) : null;
        }

        // Check if we have enough history
        if (snapshot.length < maxSamples) {
            return null;
        }

        // Check message interval
        if (playerData.getTravelMessageTickPassed() < Util.getServerConfig().getTravelMessageInterval()) {
            return null;
        }

        // Total distance check
        double totalDistance = GameMath.getHorizontalEuclideanDistance(snapshot[0], snapshot[lastIdx]);
        if (totalDistance < Util.getServerConfig().getTravelTotalDistanceThreshold()) {
            return null;
        }

        // Partial distance check
        int interval = Util.getServerConfig().getTravelPartialInterval();
        double partialThreshold = Util.getServerConfig().getTravelPartialDistanceThreshold();
        for (int i = interval; i <= lastIdx; i++) {
            double partialDistance = GameMath.getHorizontalEuclideanDistance(snapshot[i - interval], snapshot[i]);
            if (partialDistance < partialThreshold) {
                return null;
            }
        }

        String speedStr = String.format("%.2f", totalDistance / window * 20.0);
        Component mainText = Util.parseTranslatableText("fmod.message.travel.fast.main", player.getDisplayName(), Util.parseCoordText(player), speedStr);
        Component otherText = Util.parseTranslatableText("fmod.message.travel.fast.other", player.getDisplayName(), speedStr);
        Util.getServerConfig().getTravelMessage().postMessage(player, mainText, otherText);
        playerData.setLastTravelMessageTick();
        return null;
    }

    private static PlayerTeleport reportTeleport(ServerPlayer player, PlayerData playerData, Vec3 fromPos, Vec3 toPos) {
        ResourceLocation origin = playerData.getLastDimensionId();
        Component playerName = player.getDisplayName();
        Component fromCoord = Util.parseCoordText(origin, playerData.getLastBiomeId(), fromPos.x, fromPos.y, fromPos.z);
        Component toCoord = Util.parseCoordText(player);
        Component mainText = Util.parseTranslatableText("fmod.message.teleport.main", playerName, fromCoord, toCoord);
        Component otherText = Util.parseTranslatableText("fmod.message.teleport.other", playerName);
        Util.getServerConfig().getTeleportMessage().postMessage(player, mainText, otherText);
        return new PlayerTeleport(origin, fromPos, toPos);
    }

}
