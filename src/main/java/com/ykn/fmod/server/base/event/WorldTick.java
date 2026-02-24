/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.async.EntityDensityCalculator;
import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.BiomeMessage;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.base.util.GameMath;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

public class WorldTick {

    private final MinecraftServer server;

    public WorldTick(MinecraftServer server) {
        this.server = server;
    }

    /**
     * This method is called every tick.
     */
    public void onWorldTick() {
        checkEntityNumber();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            PlayerData playerData = Util.getServerData(server).getPlayerData(player);
            handleAfkPlayers(player, playerData);
            handleChangeBiomePlayer(player, playerData);
            handlePlayerTravelStatus(player, playerData);
            handlePlayerCanSleepStatus(player, playerData);
            playerData.updateLastTickData(player);
        }

        Util.getServerData(server).tick();
    }

    private void checkEntityNumber() {
        ServerData serverData = Util.getServerData(server);
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

    private void handleAfkPlayers(ServerPlayer player, PlayerData playerData) {
        if (playerData.isFacingDirectionChanged(player)) {
            postMessageToBackPlayer(player, playerData);
            playerData.resetAfkTicks();
        } else {
            postMessageToAfkingPlayer(player, playerData);
            playerData.updateAfkTicks();
        }
    }

    private void postMessageToAfkingPlayer(ServerPlayer player, PlayerData playerData) {
        if (playerData.getAfkTicks() > Util.getServerConfig().getInformAfkThreshold() && playerData.getAfkTicks() % 20 == 0) {
            Component mainText = Util.parseTranslatableText("fmod.message.afk.inform.main", player.getDisplayName(), (int) (playerData.getAfkTicks() / 20));
            Component otherText = Util.parseTranslatableText("fmod.message.afk.inform.other", player.getDisplayName());
            Util.getServerConfig().getInformAfk().postMessage(player, mainText, otherText);
        }
        if (playerData.getAfkTicks() == Util.getServerConfig().getBroadcastAfkThreshold()) {
            Component playerName = player.getDisplayName();
            Component coord = Util.parseCoordText(player);
            Component mainText = Util.parseTranslatableText("fmod.message.afk.broadcast.main", playerName, coord);
            Component otherText = Util.parseTranslatableText("fmod.message.afk.broadcast.other", playerName);
            Util.getServerConfig().getBroadcastAfk().postMessage(player, mainText, otherText);
        }
    }

    private void postMessageToBackPlayer(ServerPlayer player, PlayerData playerData) {
        if (playerData.getAfkTicks() >= Util.getServerConfig().getBroadcastAfkThreshold()) {
            Component mainText = Util.parseTranslatableText("fmod.message.afk.stop.main", player.getDisplayName(), (int) (playerData.getAfkTicks() / 20));
            Component otherText = Util.parseTranslatableText("fmod.message.afk.stop.other", player.getDisplayName());
            Util.getServerConfig().getStopAfk().postMessage(player, mainText, otherText);
        }
    }

    private void handleChangeBiomePlayer(ServerPlayer player, PlayerData playerData) {
        ResourceLocation biomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        if (playerData.isBiomeChanged(player)) {
            Util.getServerData(server).submitScheduledTask(new BiomeMessage(player, biomeId));
        }
    }

    private void handlePlayerCanSleepStatus(ServerPlayer player, PlayerData playerData) {
        boolean canSleep = player.level().dimensionType().natural() && !player.level().isDay() && BedBlock.canSetSpawn(player.level());
        boolean cannotSleep = player.level().dimensionType().natural() && player.level().isDay() && BedBlock.canSetSpawn(player.level());
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

    private void handlePlayerTravelStatus(ServerPlayer player, PlayerData playerData) {
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
            handleTeleportedPlayer(player, playerData, snapshot[lastIdx - 1], snapshot[lastIdx]);
            return;
        }

        // Check if player changed dimensions
        if (playerData.isDimensionChanged(player)) {
            playerData.clearPositionHistory(player);
            if (lastIdx > 0) {
                handleTeleportedPlayer(player, playerData, snapshot[lastIdx - 1], snapshot[lastIdx]);
            }
            return;
        }

        // Check if we have enough history
        if (snapshot.length < maxSamples) {
            return; 
        }

        // Check message interval
        if (playerData.getTravelMessageTickPassed() < Util.getServerConfig().getTravelMessageInterval()) {
            return;
        }
        
        // Total distance check
        double totalDistance = GameMath.getHorizontalEuclideanDistance(snapshot[0], snapshot[lastIdx]);
        if (totalDistance < Util.getServerConfig().getTravelTotalDistanceThreshold()) {
            return;
        }

        // Partial distance check
        int interval = Util.getServerConfig().getTravelPartialInterval();
        double partialThreshold = Util.getServerConfig().getTravelPartialDistanceThreshold();
        for (int i = interval; i <= lastIdx; i++) {
            double partialDistance = GameMath.getHorizontalEuclideanDistance(snapshot[i - interval], snapshot[i]);
            if (partialDistance < partialThreshold) {
                return;
            }
        }

        String speedStr = String.format("%.2f", totalDistance / window * 20.0);
        Component mainText = Util.parseTranslatableText("fmod.message.travel.fast.main", player.getDisplayName(), Util.parseCoordText(player), speedStr);
        Component otherText = Util.parseTranslatableText("fmod.message.travel.fast.other", player.getDisplayName(), speedStr);
        Util.getServerConfig().getTravelMessage().postMessage(player, mainText, otherText);
        playerData.setLastTravelMessageTick();
    }

    private void handleTeleportedPlayer(ServerPlayer player, PlayerData playerData, Vec3 fromPos, Vec3 toPos) {
        Component playerName = player.getDisplayName();
        Component fromCoord = Util.parseCoordText(playerData.getLastDimensionId(), playerData.getLastBiomeId(), fromPos.x, fromPos.y, fromPos.z);
        Component toCoord = Util.parseCoordText(player);
        Component mainText = Util.parseTranslatableText("fmod.message.teleport.main", playerName, fromCoord, toCoord);
        Component otherText = Util.parseTranslatableText("fmod.message.teleport.other", playerName);
        Util.getServerConfig().getTeleportMessage().postMessage(player, mainText, otherText);
    }
}
