/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.config.ConfigReader;
import com.ykn.fmod.server.base.config.ServerConfig;
import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraftforge.fml.ModList;

public class Util {

    private static final ReentrantReadWriteLock utilLock = new ReentrantReadWriteLock();

    public static final String LOGGERNAME = "FMinecraftMod";
    public static final String MODID = "fminecraftmod";

    /**
     * A static instance of the {@link EntityTypeTest} class that is used to get all the entities that are loaded and not removed in the world.
     */
    public static final EntityTypeTest<Entity, Entity> PASSTHROUGH_FILTER = new EntityTypeTest<Entity, Entity>(){
        @Override
        public Entity tryCast(@Nonnull Entity entity) {
            return entity;
        }
        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };

    /**
     * A static instance of the {@link ServerConfig} class.
     * This is used to manage and access server configuration settings.
     */
    public static ServerConfig serverConfig = new ServerConfig();

    /**
     * A static map that associates a MinecraftServer instance with its corresponding ServerData.
     * This map is used to store and manage data related to different Minecraft server instances.
     */
    public static ConcurrentHashMap<MinecraftServer, ServerData> worldData = new ConcurrentHashMap<>();

    /**
     * Retrieves the version of the mod.
     *
     * @return The version of the mod as a String.
     * @throws IllegalStateException If the mod container cannot be found.
     */
    public static String getModVersion() {
        return ModList.get().getModContainerById(MODID).orElseThrow(IllegalStateException::new).getModInfo().getVersion().toString();
    }

    /**
     * Retrieves a comma-separated string of the authors of the mod.
     *
     * @return A string containing the names of the authors of the mod, separated by commas.
     * @throws IllegalStateException If the mod container cannot be found.
     */
    public static String getModAuthors() {
        // Collection<String> authors = ModList.get().getModContainerById(MODID).orElseThrow(IllegalStateException::new).getModInfo().getAuthors();
        // StringBuilder authorsString = new StringBuilder();
        // int index = 0;
        // for (String author : authors) {
        //     if (index > 0) {
        //         authorsString.append(", ");
        //     }
        //     authorsString.append(author);
        //     index++;
        // }
        return "ykn, Xenapte";
    }

    /**
     * Retrieves the current version of Minecraft being used.
     *
     * @return A string representing the Minecraft version.
     */
    public static String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    /**
     * Retrieves a list of online players from the given Minecraft server.
     * If the server is null, an empty list is returned.
     *
     * @param server The Minecraft server instance, or null if unavailable.
     * @return A list of {@link ServerPlayer} representing the online players.
     *         Returns an empty list if the server is null.
     */
    @Nonnull
    public static List<ServerPlayer> getOnlinePlayers(@Nullable MinecraftServer server) {
        if (server == null) {
            return new ArrayList<>();
        }
        List<ServerPlayer> original = server.getPlayerList().getPlayers();
        List<ServerPlayer> copy = new ArrayList<>(original);
        return copy;
    }

    /**
     * Sends an action bar message to the specified player.
     *
     * @param player  The {@link ServerPlayer} to whom the message will be sent. Must not be null.
     * @param message The {@link Component} message to display in the action bar. Must not be null.
     */
    public static void sendActionBarMessage(@Nonnull ServerPlayer player, @Nonnull Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    /**
     * Sends an action bar message to all online players on the server.
     *
     * @param server  The Minecraft server instance. If null, the method will return without performing any action.
     * @param message The message to be displayed in the action bar. Must not be null.
     */
    public static void broadcastActionBarMessage(@Nullable MinecraftServer server, @Nonnull Component message) {
        if (server == null) {
            return;
        }
        List<ServerPlayer> players = getOnlinePlayers(server);
        for (ServerPlayer player : players) {
            sendActionBarMessage(player, message);
        }
    }

    /**
     * Sends a text message to the specified server player.
     *
     * @param player  The server player to whom the message will be sent. Must not be null.
     * @param message The text message to send to the player. Must not be null.
     */
    public static void sendTextMessage(@Nonnull ServerPlayer player, @Nonnull Component message) {
        player.displayClientMessage(message, false);
    }

    /**
     * Broadcasts a text message to all online players on the server and logs the message
     * to the server console.
     *
     * @param server The Minecraft server instance. If null, the method will return without doing anything.
     * @param message The text message to broadcast. Must not be null.
     */
    public static void broadcastTextMessage(@Nullable MinecraftServer server, @Nonnull Component message) {
        if (server == null) {
            return;
        }
        List<ServerPlayer> players = getOnlinePlayers(server);
        for (ServerPlayer player : players) {
            sendTextMessage(player, message);
        }
        // Also broadcast the message to the server console.
        LoggerFactory.getLogger(LOGGERNAME).info(message.getString());
    }

    /**
     * Sends a message to the specified player based on the provided message location type.
     *
     * @param player The {@link ServerPlayer} to whom the message will be sent. Must not be null.
     * @param type   The {@link MessageLocation} indicating where the message should be displayed (e.g., CHAT, ACTIONBAR, NONE). Must not be null.
     * @param message The {@link Component} message to be sent. Must not be null.
     */
    public static void sendMessage(@Nonnull ServerPlayer player, @Nonnull MessageLocation type, @Nonnull Component message) {
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
                LoggerFactory.getLogger(LOGGERNAME).warn("FMinecraftMod: Invalid message type: " + type);
                break;
        }
    }

    /**
     * Broadcasts a message to players on the server based on the specified message location type.
     *
     * @param server The Minecraft server instance. Can be null if no server is available.
     * @param type   The location type where the message should be broadcasted. Must not be null.
     * @param message The message to broadcast. Must not be null.
     */
    public static void broadcastMessage(@Nullable MinecraftServer server, @Nonnull MessageLocation type, @Nonnull Component message) {
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
                LoggerFactory.getLogger(LOGGERNAME).warn("FMinecraftMod: Invalid message type: " + type);
                break;
        }
    }

    /**
     * Sends a message to players based on the specified delivery method.
     *
     * @param player  The player who will receive the message. Must not be null.
     * @param method  The delivery method for the message. Must not be null.
     * @param type    The location type of the message (e.g., chat, action bar). Must not be null.
     * @param message The message content to be sent. Must not be null.
     */
    public static void postMessage(@Nonnull ServerPlayer player, @Nonnull MessageReceiver method, @Nonnull MessageLocation type, @Nonnull Component message) {
        switch (method) {
            case ALL:
                {
                    broadcastMessage(player.getServer(), type, message);
                    break;
                }
            case OP:
                {
                    List<ServerPlayer> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayer p : players) {
                        if (p.hasPermissions(2)) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case SELFOP:
                {
                    List<ServerPlayer> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayer p : players) {
                        if (p.hasPermissions(2) || p == player) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case TEAM:
                {
                    List<ServerPlayer> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayer p : players) {
                        if (p.isAlliedTo(player) || p == player) {
                            sendMessage(p, type, message);
                        }
                    }
                    break;
                }
            case TEAMOP:
                {
                    List<ServerPlayer> players = getOnlinePlayers(player.getServer());
                    for (ServerPlayer p : players) {
                        if (p.hasPermissions(2) || p.isAlliedTo(player) || p == player) {
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

    /**
     * Parses a translatable text key into a {@link MutableComponent} object, optionally translating it
     * based on the server configuration.
     *
     * @param key  The translation key to be parsed. Must not be null.
     * @param args Optional arguments to format the translatable text.
     * @return A {@link MutableComponent} object representing the parsed text. If server translation
     *         is enabled, the text is translated and returned as a literal text. Otherwise,
     *         it is returned as a translatable text.
     */
    @Nonnull
    public static MutableComponent parseTranslateableText(@Nonnull String key, Object... args) {
        if (serverConfig.isEnableServerTranslation()) {
            // A trick, by intentionally not passing args to translatable(), we still keep the "%" patterns here.
            String translatedText = Component.translatable(key).getString();
            // A trick, by intentionally using a non-existent translation key, 
            // we can make sure it always triggers the fallback, which is already the translated text.
            return Component.translatableWithFallback("fmod.a.nonexistent.translation.key", translatedText, args);
        } else {
            return Component.translatable(key, args);
        }
    }

    /**
     * Loads the server configuration from the "server.json" file.
     * This method acquires a write lock to ensure thread safety while loading the configuration.
     */
    public static void loadServerConfig() {
        utilLock.writeLock().lock();
        try {
            serverConfig = ConfigReader.loadConfig(ServerConfig.class, ServerConfig::new, "server.json");
        } finally {
            utilLock.writeLock().unlock();
        }
        LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Server config loaded.");
    }

    /**
     * Saves the server configuration to a file.
     * This method acquires a write lock to ensure thread safety while saving the configuration.
     */
    public static void saveServerConfig() {
        utilLock.writeLock().lock();
        try {
            serverConfig.saveFile();
        } finally {
            utilLock.writeLock().unlock();
        }
        LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Server config saved.");
    }

    /**
     * Retrieves the {@link ServerData} associated with the given {@link MinecraftServer}.
     * If no existing {@link ServerData} is found, a new instance is created, stored, and returned.
     *
     * @param server the {@link MinecraftServer} instance for which the {@link ServerData} is requested
     * @return the {@link ServerData} associated with the given server
     */
    @Nonnull
    public static ServerData getServerData(@Nonnull MinecraftServer server) {
        ServerData data = worldData.get(server);
        if (data == null) {
            data = new ServerData();
            worldData.put(server, data);
            LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: A new instance of ServerData was created.");
        }
        return data;
    }

    /**
     * Retrieves the PlayerData associated with the given ServerPlayerEntity.
     *
     * @param player The ServerPlayerEntity for which the PlayerData is to be retrieved. Must not be null.
     * @return The PlayerData object associated with the specified player.
     */
    @Nonnull
    public static PlayerData getPlayerData(@Nonnull ServerPlayer player) {
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

    /**
     * Retrieves all entities from the specified ServerWorld that are loaded and not removed.
     *
     * @param world the ServerWorld instance from which to collect entities.
     * @return a list of all entities in the specified world that meet the criteria.
     */
    @Nonnull
    public static List<Entity> getAllEntities(@Nonnull ServerLevel world) {
        List<Entity> entities = new ArrayList<>();
        world.getEntities(PASSTHROUGH_FILTER, entity -> entity != null && !entity.isRemoved(), entities, Integer.MAX_VALUE);
        return entities;
    }

    /**
     * Retrieves the biome name as a localized text for the given entity's current position.
     *
     * @param entity The entity whose current biome is to be determined. Must not be null.
     * @return A {@link MutableComponent} representing the localized name of the biome. If the biome
     *         cannot be determined, a default "unknown" text is returned.
     */
    @Nonnull
    public static MutableComponent getBiomeText(@Nonnull Entity entity) {
        ResourceLocation biomeId = entity.level().getBiome(entity.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
        MutableComponent biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslateableText("fmod.misc.unknown");
        } else {
            // Vanilla should contain this translation key.
            biomeText = Component.translatable("biome." + biomeId.toString().replace(":", "."));
        }
        return biomeText;
    }

    /**
     * Generates a concatenated text representation of a collection of entities.
     * Each entity's display name is appended to the resulting text, separated by commas.
     *
     * @param entities A collection of entities whose display names will be included in the text.
     *                 The collection must not be null and may contain any subclass of {@link Entity}.
     * @return A {@link MutableComponent} object containing the concatenated display names of the entities.
     *         If the collection is empty, an empty text is returned.
     */
    @Nonnull
    public static MutableComponent getEntityListText(@Nonnull Collection<? extends Entity> entities) {
        MutableComponent entityListText = Component.literal("");
        int index = 0;
        for (Entity entity : entities) {
            if (index > 0) {
                entityListText.append(Component.literal(", "));
            }
            entityListText.append(entity.getDisplayName());
            index++;
        }
        return entityListText;
    }

    /**
     * Overrides the server data for the specified Minecraft server.
     *
     * @param server the Minecraft server whose data is to be overridden, must not be null.
     * @param data   the new server data to associate with the specified server, must not be null.
     */
    public static void overrideServerData(@Nonnull MinecraftServer server, @Nonnull ServerData data) {
        ServerData existingData = worldData.get(server);
        if (existingData != null) {
            existingData.globalRequestPool.shutdown();
            LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Existing ServerData instance found and shut down the global thread pool.");
        }
        worldData.put(server, data);
    }

    /**
     * Resets the server data for the specified Minecraft server.
     *
     * @param server the Minecraft server whose data is to be reset; must not be null
     */
    public static void resetServerData(@Nonnull MinecraftServer server) {
        ServerData existingData = worldData.get(server);
        if (existingData != null) {
            existingData.globalRequestPool.shutdown();
            LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Existing ServerData instance found and shut down the global thread pool.");
        }
        ServerData data = new ServerData();
        worldData.put(server, data);
    }
}
