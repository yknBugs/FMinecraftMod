package com.ykn.fmod.server.base.event;

import java.util.List;

import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.schedule.BiomeMessage;
import com.ykn.fmod.server.base.schedule.ScheduledTask;
import com.ykn.fmod.server.base.util.MessageLocation;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.block.BedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for (ServerPlayerEntity player : players) {
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
        for (ServerWorld world : server.getWorlds()) {
            List<Entity> entities = Util.getAllEntities(world);
            entityNumber += entities.size();
        }
        if (entityNumber >= Util.serverConfig.getEntityNumberThreshold()) {
            Util.broadcastMessage(server, Util.serverConfig.getEntityNumberWarning(), Util.parseTranslateableText("fmod.message.entitywarning", entityNumber));
        }
    }

    private void handleAfkPlayers(ServerPlayerEntity player, PlayerData playerData) {
        float pitch = player.getPitch();
        float yaw = player.getYaw();
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

    private void postMessageToAfkingPlayer(ServerPlayerEntity player, PlayerData playerData) {
        if (playerData.afkTicks > Util.serverConfig.getInformAfkingThreshold() && playerData.afkTicks % 20 == 0) {
            Util.postMessage(player, Util.serverConfig.getInformAfking(), MessageLocation.ACTIONBAR, Util.parseTranslateableText("fmod.message.afk.inform", player.getDisplayName(), (int) (playerData.afkTicks / 20)));
        }
        if (playerData.afkTicks == Util.serverConfig.getBroadcastAfkingThreshold()) {
            Text playerName = player.getDisplayName();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            String strX = String.format("%.2f", x);
            String strY = String.format("%.2f", y);
            String strZ = String.format("%.2f", z);
            MutableText biomeText = Util.getBiomeText(player);
            MutableText text = Util.parseTranslateableText("fmod.message.afk.broadcast", playerName, biomeText, strX, strY, strZ).styled(style -> style.withClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + strX + " " + strY + " " + strZ)
            ).withHoverEvent(
                new HoverEvent(HoverEvent.Action.SHOW_TEXT, Util.parseTranslateableText("fmod.misc.clicktp"))
            ));
            Util.postMessage(player, Util.serverConfig.getBroadcastAfking(), MessageLocation.CHAT, text);
        }
    }

    private void postMessageToBackPlayer(ServerPlayerEntity player, PlayerData playerData) {
        if (playerData.afkTicks >= Util.serverConfig.getBroadcastAfkingThreshold()) {
            Util.postMessage(player, Util.serverConfig.getStopAfking(), MessageLocation.CHAT, Util.parseTranslateableText("fmod.message.afk.stop", player.getDisplayName(), (int) (playerData.afkTicks / 20)));
        }
    }

    private void handleChangeBiomePlayer(ServerPlayerEntity player, PlayerData playerData) {
        Identifier biomeId = player.getWorld().getBiome(player.getBlockPos()).getKey().map(key -> key.getValue()).orElse(null);
        if (!biomeId.equals(playerData.lastBiomeId)) {
            Util.getServerData(server).submitScheduledTask(new BiomeMessage(player, biomeId));
            playerData.lastBiomeId = biomeId;
        }
    }

    private void handlePlayerCanSleepStatus(ServerPlayerEntity player, PlayerData playerData) {
        boolean canSleep = player.getWorld().getDimension().natural() && !player.getWorld().isDay() && BedBlock.isBedWorking(player.getWorld());
        boolean cannotSleep = player.getWorld().getDimension().natural() && player.getWorld().isDay() && BedBlock.isBedWorking(player.getWorld());
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
