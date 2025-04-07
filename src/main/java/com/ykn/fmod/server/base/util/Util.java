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

    /**
     * A static instance of the {@link TypeFilter} class that is used to get all the entities that are loaded and not removed in the world.
     */
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

    /**
     * A static instance of the {@link ServerConfig} class.
     * This is used to manage and access server configuration settings.
     */
    public static ServerConfig serverConfig = new ServerConfig();

    /**
     * A static map that associates a MinecraftServer instance with its corresponding ServerData.
     * This map is used to store and manage data related to different Minecraft server instances.
     */
    public static HashMap<MinecraftServer, ServerData> worldData = new HashMap<>();

    /**
     * Retrieves the version of the mod.
     *
     * @return The version of the mod as a String.
     * @throws IllegalStateException If the mod container cannot be found.
     */
    public static String getModVersion() {
        return FabricLoader.getInstance().getModContainer(MODID).orElseThrow(IllegalStateException::new).getMetadata().getVersion().toString();
    }

    /**
     * Retrieves a comma-separated string of the authors of the mod.
     *
     * @return A string containing the names of the authors of the mod, separated by commas.
     * @throws IllegalStateException If the mod container cannot be found.
     */
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

    /**
     * Retrieves the current version of Minecraft being used.
     *
     * @return A string representing the Minecraft version.
     */
    public static String getMinecraftVersion() {
        return SharedConstants.getGameVersion().getName();
    }

    /**
     * Retrieves a list of online players from the given Minecraft server.
     * If the server is null, an empty list is returned.
     *
     * @param server The Minecraft server instance, or null if unavailable.
     * @return A list of {@link ServerPlayerEntity} representing the online players.
     *         Returns an empty list if the server is null.
     */
    @NotNull
    public static List<ServerPlayerEntity> getOnlinePlayers(@Nullable MinecraftServer server) {
        if (server == null) {
            return new ArrayList<>();
        }
        List<ServerPlayerEntity> original = server.getPlayerManager().getPlayerList();
        List<ServerPlayerEntity> copy = new ArrayList<>(original);
        return copy;
    }

    /**
     * Sends an action bar message to the specified player.
     *
     * @param player  The {@link ServerPlayerEntity} to whom the message will be sent. Must not be null.
     * @param message The {@link Text} message to display in the action bar. Must not be null.
     */
    public static void sendActionBarMessage(@NotNull ServerPlayerEntity player, @NotNull Text message) {
        player.networkHandler.sendPacket(new OverlayMessageS2CPacket(message));
    }

    /**
     * Sends an action bar message to all online players on the server.
     *
     * @param server  The Minecraft server instance. If null, the method will return without performing any action.
     * @param message The message to be displayed in the action bar. Must not be null.
     */
    public static void broadcastActionBarMessage(@Nullable MinecraftServer server, @NotNull Text message) {
        if (server == null) {
            return;
        }
        List<ServerPlayerEntity> players = getOnlinePlayers(server);
        for (ServerPlayerEntity player : players) {
            sendActionBarMessage(player, message);
        }
    }

    /**
     * Sends a text message to the specified server player.
     *
     * @param player  The server player to whom the message will be sent. Must not be null.
     * @param message The text message to send to the player. Must not be null.
     */
    public static void sendTextMessage(@NotNull ServerPlayerEntity player, @NotNull Text message) {
        player.sendMessage(message, false);
    }

    /**
     * Broadcasts a text message to all online players on the server and logs the message
     * to the server console.
     *
     * @param server The Minecraft server instance. If null, the method will return without doing anything.
     * @param message The text message to broadcast. Must not be null.
     */
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

    /**
     * Sends a message to the specified player based on the provided message location type.
     *
     * @param player The {@link ServerPlayerEntity} to whom the message will be sent. Must not be null.
     * @param type   The {@link MessageLocation} indicating where the message should be displayed (e.g., CHAT, ACTIONBAR, NONE). Must not be null.
     * @param message The {@link Text} message to be sent. Must not be null.
     */
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

    /**
     * Parses a translatable text key into a {@link MutableText} object, optionally translating it
     * based on the server configuration.
     *
     * @param key  The translation key to be parsed. Must not be null.
     * @param args Optional arguments to format the translatable text.
     * @return A {@link MutableText} object representing the parsed text. If server translation
     *         is enabled, the text is translated and returned as a literal text. Otherwise,
     *         it is returned as a translatable text.
     */
    @NotNull
    public static MutableText parseTranslateableText(@NotNull String key, Object... args) {
        if (serverConfig.isEnableServerTranslation()) {
            String translatedText = new TranslatableText(key, args).getString();
            return new LiteralText(translatedText);
        } else {
            return new TranslatableText(key, args);
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

    /**
     * Retrieves the PlayerData associated with the given ServerPlayerEntity.
     *
     * @param player The ServerPlayerEntity for which the PlayerData is to be retrieved. Must not be null.
     * @return The PlayerData object associated with the specified player.
     */
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

    /**
     * Retrieves all entities from the specified ServerWorld that are loaded and not removed.
     *
     * @param world the ServerWorld instance from which to collect entities.
     * @return a list of all entities in the specified world that meet the criteria.
     */
    @NotNull
    public static List<Entity> getAllEntities(@NotNull ServerWorld world) {
        List<? extends Entity> entities = world.getEntitiesByType(PASSTHROUGH_FILTER, entity -> entity != null && !entity.isRemoved());
        List<Entity> entitiesList = new ArrayList<>();
        for (Entity entity : entities) {
            entitiesList.add(entity);
        }
        return entitiesList;
    }

    /**
     * Retrieves the biome name as a localized text for the given entity's current position.
     *
     * @param entity The entity whose current biome is to be determined. Must not be null.
     * @return A {@link MutableText} representing the localized name of the biome. If the biome
     *         cannot be determined, a default "unknown" text is returned.
     */
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

    /**
     * Generates a concatenated text representation of a collection of entities.
     * Each entity's display name is appended to the resulting text, separated by commas.
     *
     * @param entities A collection of entities whose display names will be included in the text.
     *                 The collection must not be null and may contain any subclass of {@link Entity}.
     * @return A {@link MutableText} object containing the concatenated display names of the entities.
     *         If the collection is empty, an empty text is returned.
     */
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

    /**
     * Overrides the server data for the specified Minecraft server.
     *
     * @param server the Minecraft server whose data is to be overridden, must not be null.
     * @param data   the new server data to associate with the specified server, must not be null.
     */
    public static void overrideServerData(@NotNull MinecraftServer server, @NotNull ServerData data) {
        worldData.put(server, data);
    }

    /**
     * Resets the server data for the specified Minecraft server.
     *
     * @param server the Minecraft server whose data is to be reset; must not be null
     */
    public static void resetServerData(@NotNull MinecraftServer server) {
        ServerData data = new ServerData();
        worldData.put(server, data);
    }
}
