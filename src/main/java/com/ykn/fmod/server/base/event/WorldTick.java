package com.ykn.fmod.server.base.event;

import java.util.List;

import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.schedule.BiomeMessage;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.BedBlock;

public class WorldTick {

    private MinecraftServer server;

    public WorldTick(MinecraftServer server) {
        this.server = server;
    }

    /**
     * This method is called every tick.
     */
    public void onWorldTick() {
        if (Util.getServerData(server).getServerTick() % Util.serverConfig.getEntityNumberInterval() == 0) {
            checkEntityNumber();
        }   

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            PlayerData playerData = Util.getServerData(server).getPlayerData(player);
            handleAfkPlayers(player, playerData);
            handleChangeBiomePlayer(player, playerData);
            handlePlayerCanSleepStatus(player, playerData);
        }

        List<ScheduledTask> scheduledTasks = Util.getServerData(server).getScheduledTasks();
        scheduledTasks.forEach(ScheduledTask::tick);
        scheduledTasks.removeIf(ScheduledTask::isFinished);

        Util.getServerData(server).tick();
    }

    private void checkEntityNumber() {
        int entityNumber = 0;
        for (ServerLevel world : server.getAllLevels()) {
            List<Entity> entities = Util.getAllEntities(world);
            entityNumber += entities.size();
        }
        if (entityNumber >= Util.serverConfig.getEntityNumberThreshold()) {
            Util.broadcastMessage(server, Util.serverConfig.getEntityNumberWarning(), Util.parseTranslateableText("fmod.message.entitywarning", entityNumber));
        }
    }

    private void handleAfkPlayers(ServerPlayer player, PlayerData playerData) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        if (Math.abs(pitch - playerData.lastPitch) < 0.01 && Math.abs(yaw - playerData.lastYaw) < 0.01) {
            playerData.afkTicks++;
            postMessageToAfkingPlayer(player, playerData);
        } else {
            postMessageToBackPlayer(player, playerData);
            playerData.afkTicks = 0;
            playerData.lastPitch = pitch;
            playerData.lastYaw = yaw;
        }
    }

    private void postMessageToAfkingPlayer(ServerPlayer player, PlayerData playerData) {
        if (playerData.afkTicks > Util.serverConfig.getInformAfkingThreshold() && playerData.afkTicks % 20 == 0) {
            Util.postMessage(player, Util.serverConfig.getInformAfking(), MessageLocation.ACTIONBAR, Util.parseTranslateableText("fmod.message.afk.inform", player.getDisplayName(), (int) (playerData.afkTicks / 20)));
        }
        if (playerData.afkTicks == Util.serverConfig.getBroadcastAfkingThreshold()) {
            Component playerName = player.getDisplayName();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            double pitch = player.getXRot();
            double yaw = player.getYRot();
            String strDim = player.level().dimension().location().toString();;
            String strX = String.format("%.2f", x);
            String strY = String.format("%.2f", y);
            String strZ = String.format("%.2f", z);
            String strPitch = String.format("%.2f", pitch);
            String strYaw = String.format("%.2f", yaw);
            MutableComponent biomeText = Util.getBiomeText(player);
            MutableComponent text = Util.parseTranslateableText("fmod.message.afk.broadcast", playerName, biomeText, strX, strY, strZ).withStyle(style -> style.withClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + strDim + " run tp @s " + strX + " " + strY + " " + strZ + " " + strYaw + " " + strPitch)
            ).withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
            ));
            Util.postMessage(player, Util.serverConfig.getBroadcastAfking(), MessageLocation.CHAT, text);
        }
    }

    private void postMessageToBackPlayer(ServerPlayer player, PlayerData playerData) {
        if (playerData.afkTicks >= Util.serverConfig.getBroadcastAfkingThreshold()) {
            Util.postMessage(player, Util.serverConfig.getStopAfking(), MessageLocation.CHAT, Util.parseTranslateableText("fmod.message.afk.stop", player.getDisplayName(), (int) (playerData.afkTicks / 20)));
        }
    }

    private void handleChangeBiomePlayer(ServerPlayer player, PlayerData playerData) {
        ResourceLocation biomeId = player.level().getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        if (!biomeId.equals(playerData.lastBiomeId)) {
            Util.getServerData(server).submitScheduledTask(new BiomeMessage(player, biomeId));
            playerData.lastBiomeId = biomeId;
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
                Util.sendMessage(player, Util.serverConfig.getPlayerCanSleepMessage(), Util.parseTranslateableText("fmod.message.sleep.can", player.getDisplayName()));
            } else {
                Util.sendMessage(player, Util.serverConfig.getPlayerCanSleepMessage(), Util.parseTranslateableText("fmod.message.sleep.cannot", player.getDisplayName()));
            }
        }
        playerData.lastCanSleep = currentCanSleepStatus;
    }
}
