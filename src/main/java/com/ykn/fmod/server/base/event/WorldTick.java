/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.ykn.fmod.server.base.async.EntityDensityCalculator;
import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.schedule.BiomeMessage;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.base.util.GameMath;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
            playerData.lastPitch = player.getXRot();
            playerData.lastYaw = player.getYRot();
            playerData.lastDimensionId = player.level().dimension().location();
            playerData.lastBiomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        }

        Util.getServerData(server).tick();
    }

    private void checkEntityNumber() {
        ServerData serverData = Util.getServerData(server);
        // Check if have finished previous calculation (Happens every tick because async result must be feeded back immediately after finishing)
        if (serverData.activeDensityCalculator != null && serverData.activeDensityCalculator.isAfterCompletionExecuted()) {
            // If finished, retrieve result and clear the task
            if (serverData.activeDensityCalculator.getEntity() == null || serverData.activeDensityCalculator.getCause() == null) {
                // No result
                Util.broadcastMessage(server, Util.serverConfig.getEntityNumberWarning(), Util.parseTranslatableText("fmod.message.entitywarning", serverData.activeDensityCalculator.getInputNumber()).withStyle(ChatFormatting.RED));
            } else {
                // Has result
                final String totalCount = Integer.toString(serverData.activeDensityCalculator.getInputNumber());
                final Component coordText = Util.parseCoordText(serverData.activeDensityCalculator.getDimension(), serverData.activeDensityCalculator.getBiome(), serverData.activeDensityCalculator.getX(), serverData.activeDensityCalculator.getY(), serverData.activeDensityCalculator.getZ());
                final String entityRadius = String.format("%.2f", serverData.activeDensityCalculator.getRadius());
                final String entityCount = Integer.toString(serverData.activeDensityCalculator.getCount());
                final String causeCount = Integer.toString(serverData.activeDensityCalculator.getNumber());
                final Component entityCauseText = serverData.activeDensityCalculator.getCause().getDisplayName();
                Util.broadcastMessage(server, Util.serverConfig.getEntityDensityWarning(), Util.parseTranslatableText("fmod.message.entitydensity", totalCount, coordText, entityRadius, entityCount, causeCount, entityCauseText).withStyle(ChatFormatting.RED));
            }
            serverData.lastCheckEntityTick = serverData.getServerTick();
            serverData.lastCheckDensityTick = serverData.getServerTick();
            serverData.activeDensityCalculator = null;
            return;
        }

        boolean satisfyNumberCondition = true;
        boolean satisfyDensityCondition = true;

        // If no finished calculation, wait after configured interval
        if (serverData.getTickPassed(serverData.lastCheckEntityTick) < Util.serverConfig.getEntityNumberInterval()) {
            satisfyNumberCondition = false;
        }
        if (serverData.getTickPassed(serverData.lastCheckDensityTick) < Util.serverConfig.getEntityDensityInterval()) {
            satisfyDensityCondition = false;
        }
        if (satisfyNumberCondition == false && satisfyDensityCondition == false) {
            return;
        }

        // Now we should check entity number and density
        List<Entity> allEntities = new ArrayList<>();
        for (ServerLevel world : server.getAllLevels()) {
            List<Entity> entities = Util.getAllEntities(world);
            allEntities.addAll(entities);
        }

        // Check if exceed threshold
        if (allEntities.size() < Util.serverConfig.getEntityNumberThreshold()) {
            serverData.lastCheckEntityTick = serverData.getServerTick();
            satisfyNumberCondition = false;
        }
        if (allEntities.size() < Util.serverConfig.getEntityDensityThreshold()) {
            serverData.lastCheckDensityTick = serverData.getServerTick();
            satisfyDensityCondition = false;
        }
        if (satisfyNumberCondition == false && satisfyDensityCondition == false) {
            return;
        }

        // Check if we have an active calculator
        if (satisfyDensityCondition) {
            if (serverData.activeDensityCalculator == null) {
                // No active calculator, create a new one, we will broadcast result after finishing async calculation
                serverData.lastCheckEntityTick = serverData.getServerTick();
                serverData.lastCheckDensityTick = serverData.getServerTick();
                satisfyNumberCondition = false; // We already start density calculation, no need to start number calculation
                EntityDensityCalculator calculator = new EntityDensityCalculator(null, allEntities, Util.serverConfig.getEntityDensityRadius(), Util.serverConfig.getEntityDensityNumber());
                serverData.activeDensityCalculator = calculator;
                serverData.submitAsyncTask(calculator);
                return;
            } else {
                // Have active calculator, wait for result
                serverData.lastCheckDensityTick = serverData.getServerTick();
            }
        }

        if (satisfyNumberCondition) {
            serverData.lastCheckEntityTick = serverData.getServerTick();
            Util.broadcastMessage(server, Util.serverConfig.getEntityNumberWarning(), Util.parseTranslatableText("fmod.message.entitywarning", allEntities.size()).withStyle(ChatFormatting.RED));
        }
    }

    private void handleAfkPlayers(ServerPlayer player, PlayerData playerData) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        if (Math.abs(pitch - playerData.lastPitch) < 0.01 && Math.abs(yaw - playerData.lastYaw) < 0.01) {
            postMessageToAfkingPlayer(player, playerData);
            playerData.afkTicks++;
        } else {
            postMessageToBackPlayer(player, playerData);
            playerData.afkTicks = 0;
        }
    }

    private void postMessageToAfkingPlayer(ServerPlayer player, PlayerData playerData) {
        if (playerData.afkTicks > Util.serverConfig.getInformAfkingThreshold() && playerData.afkTicks % 20 == 0) {
            Util.postMessage(player, Util.serverConfig.getInformAfkingReceiver(), Util.serverConfig.getInformAfkingLocation(), Util.parseTranslatableText("fmod.message.afk.inform", player.getDisplayName(), (int) (playerData.afkTicks / 20)));
        }
        if (playerData.afkTicks == Util.serverConfig.getBroadcastAfkingThreshold()) {
            Component playerName = player.getDisplayName();
            Component coord = Util.parseCoordText(player);
            MutableComponent text = Util.parseTranslatableText("fmod.message.afk.broadcast", playerName, coord);
            Util.postMessage(player, Util.serverConfig.getBroadcastAfkingReceiver(), Util.serverConfig.getBroadcastAfkingLocation(), text);
        }
    }

    private void postMessageToBackPlayer(ServerPlayer player, PlayerData playerData) {
        if (playerData.afkTicks >= Util.serverConfig.getBroadcastAfkingThreshold()) {
            Util.postMessage(player, Util.serverConfig.getStopAfkingReceiver(), Util.serverConfig.getStopAfkingLocation(), Util.parseTranslatableText("fmod.message.afk.stop", player.getDisplayName(), (int) (playerData.afkTicks / 20)));
        }
    }

    private void handleChangeBiomePlayer(ServerPlayer player, PlayerData playerData) {
        ResourceLocation biomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        if (!biomeId.equals(playerData.lastBiomeId)) {
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
        if (currentCanSleepStatus != null && !currentCanSleepStatus.equals(playerData.lastCanSleep)) {
            if (currentCanSleepStatus) {
                Util.sendMessage(player, Util.serverConfig.getPlayerCanSleepMessage(), Util.parseTranslatableText("fmod.message.sleep.can", player.getDisplayName()));
            } else {
                Util.sendMessage(player, Util.serverConfig.getPlayerCanSleepMessage(), Util.parseTranslatableText("fmod.message.sleep.cannot", player.getDisplayName()));
            }
        }
        playerData.lastCanSleep = currentCanSleepStatus;
    }

    private void handlePlayerTravelStatus(ServerPlayer player, PlayerData playerData) {
        int window = Util.serverConfig.getTravelWindowTicks();
        Deque<Vec3> positions = playerData.recentPositions;

        // Update positions history
        positions.addLast(player.position());
        int maxSamples = window + 1;
        while (positions.size() > maxSamples) {
            positions.removeFirst();
        }

        Vec3[] snapshot = positions.toArray(new Vec3[0]);
        int lastIdx = snapshot.length - 1;

        // Check if the player has teleported
        double teleportThreshold = Util.serverConfig.getTeleportThreshold();
        if (lastIdx > 0 && GameMath.getHorizonalEuclideanDistance(snapshot[lastIdx - 1], snapshot[lastIdx]) > teleportThreshold) {
            positions.clear();
            positions.addLast(player.position());
            handleTeleportedPlayer(player, playerData, snapshot[lastIdx - 1], snapshot[lastIdx]);
            return;
        }

        // Check if player changed dimensions
        ResourceLocation currentDim = player.level().dimension().location();
        if (!currentDim.equals(playerData.lastDimensionId)) {
            positions.clear();
            positions.addLast(player.position());
            if (lastIdx > 0) {
                handleTeleportedPlayer(player, playerData, snapshot[lastIdx - 1], snapshot[lastIdx]);
            }
            return;
        }

        // Check if we have enough history
        if (positions.size() < maxSamples) {
            return; 
        }
        
        // Total distance check
        double totalDistance = GameMath.getHorizonalEuclideanDistance(snapshot[0], snapshot[lastIdx]);
        if (totalDistance < Util.serverConfig.getTravelTotalDistanceThreshold()) {
            return;
        }

        // Partial distance check
        int interval = Util.serverConfig.getTravelPartialInterval();
        double partialThreshold = Util.serverConfig.getTravelPartialDistanceThreshold();
        for (int i = interval; i <= lastIdx; i++) {
            double partialDistance = GameMath.getHorizonalEuclideanDistance(snapshot[i - interval], snapshot[i]);
            if (partialDistance < partialThreshold) {
                return;
            }
        }

        String speedStr = String.format("%.2f", totalDistance / window * 20.0);
        MutableComponent message = Util.parseTranslatableText("fmod.message.travel.fast", player.getDisplayName(), speedStr);
        Util.postMessage(player, Util.serverConfig.getTravelMessageReceiver(), Util.serverConfig.getTravelMessageLocation(), message);

        positions.clear();
        positions.addLast(player.position());
    }

    private void handleTeleportedPlayer(ServerPlayer player, PlayerData playerData, Vec3 fromPos, Vec3 toPos) {
        Component playerName = player.getDisplayName();
        Component fromCoord = Util.parseCoordText(playerData.lastDimensionId, playerData.lastBiomeId, fromPos.x, fromPos.y, fromPos.z);
        Component toCoord = Util.parseCoordText(player);
        Component finalText = Util.parseTranslatableText("fmod.message.teleport", playerName, fromCoord, toCoord);
        Util.postMessage(player, Util.serverConfig.getTeleportMessageReceiver(), Util.serverConfig.getTeleportMessageLocation(), finalText);
    }
}
