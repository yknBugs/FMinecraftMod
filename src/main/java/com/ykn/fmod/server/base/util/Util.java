package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.config.ConfigReader;
import com.ykn.fmod.server.base.config.ServerConfig;
import com.ykn.fmod.server.base.data.ServerData;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.SharedConstants;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Util {

    private static final ReentrantReadWriteLock utilLock = new ReentrantReadWriteLock();

    public static final String LOGGERNAME = "FMinecraftMod";
    public static final String MODID = "fminecraftmod";

    public static ServerConfig serverConfig = new ServerConfig();
    public static HashMap<MinecraftServer, ServerData> worldData = new HashMap<>();

    public static String getModVersion() {
        return FabricLoader.getInstance().getModContainer(MODID).orElseThrow(IllegalStateException::new).getMetadata().getVersion().toString();
    }

    public static String getModAuthors() {
        Collection<Person> authors = FabricLoader.getInstance().getModContainer(MODID).orElseThrow(IllegalStateException::new).getMetadata().getAuthors();
        StringBuilder authorsString = new StringBuilder();
        int index = 0;
        for (Person author : authors) {
            if (index > 0) {
                authorsString.append(", ");
            }
            authorsString.append(author.getName());
            index++;
        }
        return authorsString.toString();
    }

    public static String getMinecraftVersion() {
        return SharedConstants.getGameVersion().getName();
    }

    @NotNull
    public static List<ServerPlayerEntity> getOnlinePlayers(@Nullable MinecraftServer server) {
        if (server == null) {
            return new ArrayList<>();
        }
        List<ServerPlayerEntity> original = server.getPlayerManager().getPlayerList();
        List<ServerPlayerEntity> copy = new ArrayList<>(original);
        return copy;
    }

    public static void sendActionBarMessage(@NotNull ServerPlayerEntity player, @NotNull Text message) {
        player.networkHandler.sendPacket(new OverlayMessageS2CPacket(message));
    }

    public static void broadcastActionBarMessage(@Nullable MinecraftServer server, @NotNull Text message) {
        if (server == null) {
            return;
        }
        List<ServerPlayerEntity> players = getOnlinePlayers(server);
        for (ServerPlayerEntity player : players) {
            sendActionBarMessage(player, message);
        }
    }

    public static void sendTextMessage(@NotNull ServerPlayerEntity player, @NotNull Text message) {
        player.sendMessage(message, false);
    }

    public static void broadcastTextMessage(@Nullable MinecraftServer server, @NotNull Text message) {
        if (server == null) {
            return;
        }
        List<ServerPlayerEntity> players = getOnlinePlayers(server);
        for (ServerPlayerEntity player : players) {
            sendTextMessage(player, message);
        }
        // Also broadcast the message to the server console.
        LoggerFactory.getLogger(LOGGERNAME).info(message.getString());
    }

    public static void sendMessage(@NotNull ServerPlayerEntity player, @NotNull MessageType type, @NotNull Text message) {
        switch (type) {
            case NONE:
                break;
            case CHAT:
                sendTextMessage(player, message);
                break;
            case ACTIONBAR:
                sendActionBarMessage(player, message);
                break;
            default:
                LoggerFactory.getLogger(LOGGERNAME).warn("FMinecraftMod: Invalid message type.");
                break;
        }
    }

    public static void broadcastMessage(@Nullable MinecraftServer server, @NotNull MessageType type, @NotNull Text message) {
        switch (type) {
            case NONE:
                break;
            case CHAT:
                broadcastTextMessage(server, message);
                break;
            case ACTIONBAR:
                broadcastActionBarMessage(server, message);
                break;
            default:
                LoggerFactory.getLogger(LOGGERNAME).warn("FMinecraftMod: Invalid message type.");
                break;
        }
    }

    @NotNull
    public static MutableText parseTranslateableText(@NotNull String key, Object... args) {
        if (serverConfig.isEnableServerTranslation()) {
            String translatedText = Text.translatable(key, args).getString();
            return Text.literal(translatedText);
        } else {
            return Text.translatable(key, args);
        }
    }

    public static void loadServerConfig() {
        utilLock.writeLock().lock();
        try {
            serverConfig = ConfigReader.loadConfig(ServerConfig.class, ServerConfig::new, "server.json");
        } finally {
            utilLock.writeLock().unlock();
        }
        LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Server config loaded.");
    }

    public static void saveServerConfig() {
        utilLock.writeLock().lock();
        try {
            serverConfig.saveFile();
        } finally {
            utilLock.writeLock().unlock();
        }
        LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Server config saved.");
    }

    @NotNull
    public static ServerData getServerData(@NotNull MinecraftServer server) {
        ServerData data = worldData.get(server);
        if (data == null) {
            data = new ServerData();
            worldData.put(server, data);
            LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: A new instance of ServerData was created.");
        }
        return data;
    }

    public static void overrideServerData(@NotNull MinecraftServer server, @NotNull ServerData data) {
        worldData.put(server, data);
    }

    public static void resetServerData(@NotNull MinecraftServer server) {
        ServerData data = new ServerData();
        worldData.put(server, data);
    }
}
