/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

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
import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.SharedConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.registry.BuiltinRegistries;

public class Util {

    private static final ReentrantReadWriteLock utilLock = new ReentrantReadWriteLock();

    public static final String LOGGERNAME = "FMinecraftMod";
    public static final String MODID = "fminecraftmod";

    public static final TypeFilter<Entity, Entity> PASSTHROUGH_FILTER = new TypeFilter<Entity, Entity>(){
        @Override
        public Entity downcast(Entity entity) {
            return entity;
        }
        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };

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

    public static void sendMessage(@NotNull ServerPlayerEntity player, @NotNull MessageLocation type, @NotNull Text message) {
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

    public static void broadcastMessage(@Nullable MinecraftServer server, @NotNull MessageLocation type, @NotNull Text message) {
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

    public static void postMessage(@NotNull ServerPlayerEntity player, @NotNull MessageReceiver method, @NotNull MessageLocation type, @NotNull Text message) {
        switch (method) {
            case ALL:
                {
                    broadcastMessage(player.getServer(), type, message);
                    break;
                }
            case OP:
                {
                    List<ServerPlayerEntity> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayerEntity p : players) {
                        if (p.hasPermissionLevel(2)) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case SELFOP:
                {
                    List<ServerPlayerEntity> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayerEntity p : players) {
                        if (p.hasPermissionLevel(2) || p == player) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case TEAM:
                {
                    List<ServerPlayerEntity> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayerEntity p : players) {
                        if (p.isTeammate(player) || p == player) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case TEAMOP:
                {
                    List<ServerPlayerEntity> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayerEntity p : players) {
                        if (p.hasPermissionLevel(2) || p.isTeammate(player) || p == player) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case SELF:
                {
                    sendMessage(player, type, message);
                    break;
                }
            case NONE:
                break;
            default:
                LoggerFactory.getLogger(LOGGERNAME).warn("FMinecraftMod: Invalid message method.");
                break;
        }
    }

    @NotNull
    public static MutableText parseTranslateableText(@NotNull String key, Object... args) {
        if (serverConfig.isEnableServerTranslation()) {
            String translatedText = new TranslatableText(key, args).getString();
            return new LiteralText(translatedText);
        } else {
            return new TranslatableText(key, args);
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

    @NotNull
    public static PlayerData getPlayerData(@NotNull ServerPlayerEntity player) {
        return getServerData(player.getServer()).getPlayerData(player);
    }

    /**
     * Safely Retrieves the health of the given entity.
     *
     * @param entity the entity whose health is to be retrieved; can be null
     * @return the health of the entity if it is a LivingEntity and not removed, otherwise 0.0
     */
    public static double getHealth(@Nullable Object entity) {
        if (entity == null) {
            return 0.0;
        }
        if (entity instanceof Double) {
            return (Double) entity;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (livingEntity.isRemoved()) {
                return 0.0;
            }
            return livingEntity.getHealth();
        }
        return 0.0;
    }

    /**
     * Safely Retrieves the maximum health of the given entity.
     *
     * @param entity the entity whose maximum health is to be retrieved; can be null
     * @return the maximum health of the entity if it is a LivingEntity and not removed, otherwise 0.0
     */
    public static double getMaxHealth(@Nullable Object entity) {
        if (entity == null) {
            return 0.0;
        }
        if (entity instanceof Double) {
            return (Double) entity;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (livingEntity.isRemoved()) {
                return 0.0;
            }
            return livingEntity.getMaxHealth();
        }
        return 0.0;
    }

    @NotNull
    public static List<Entity> getAllEntities(@NotNull ServerWorld world) {
        List<? extends Entity> entities = world.getEntitiesByType(PASSTHROUGH_FILTER, entity -> entity != null && !entity.isRemoved());
        List<Entity> entitiesList = new ArrayList<>();
        for (Entity entity : entities) {
            entitiesList.add(entity);
        }
        return entitiesList;
    }

    @NotNull
    public static MutableText getBiomeText(@NotNull Entity entity) {
        Identifier biomeId = BuiltinRegistries.BIOME.getId(entity.getWorld().getBiome(entity.getBlockPos()));
        MutableText biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslateableText("fmod.misc.unknown");
        } else {
            // Vanilla should contain this translation key.
            biomeText = new TranslatableText("biome." + biomeId.toString().replace(":", "."));
        }
        return biomeText;
    }

    @NotNull
    public static MutableText getEntityListText(@NotNull Collection<? extends Entity> entities) {
        MutableText entityListText = new LiteralText("");
        int index = 0;
        for (Entity entity : entities) {
            if (index > 0) {
                entityListText.append(new LiteralText(", "));
            }
            entityListText.append(entity.getDisplayName());
            index++;
        }
        return entityListText;
    }

    public static void overrideServerData(@NotNull MinecraftServer server, @NotNull ServerData data) {
        worldData.put(server, data);
    }

    public static void resetServerData(@NotNull MinecraftServer server) {
        ServerData data = new ServerData();
        worldData.put(server, data);
    }
}
