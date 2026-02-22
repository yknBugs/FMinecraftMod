/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;

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
    public static ConcurrentHashMap<MinecraftServer, ServerData> worldData = new ConcurrentHashMap<>();

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
    public static MutableText parseTranslatableText(@NotNull String key, Object... args) {
        if (serverConfig.getServerTranslation()) {
            // A trick, by intentionally not passing args to translatable(), we still keep the "%" patterns here.
            String translatedText = Text.translatable(key).getString();
            // A trick, by intentionally using the original translation key,
            // So if the client does have the translation, it can still ignore our translated text,
            // but if the client does not have the translation, it can still show our translated text.
            return Text.translatableWithFallback(key, translatedText, args);
        } else {
            return Text.translatable(key, args);
        }
    }

    /**
     * Parses coordinate information into a formatted {@link MutableText} object.
     * The text includes the dimension, biome, and coordinates (x, y, z) with click and hover events.
     *
     * @param dimension The dimension identifier where the coordinates are located. Must not be null.
     * @param biome     The biome identifier at the specified coordinates. Can be null.
     * @param x         The x-coordinate.
     * @param y         The y-coordinate.
     * @param z         The z-coordinate.
     * @return A {@link MutableText} object representing the formatted coordinate information
     *         with click and hover events for teleportation.
     */
    @NotNull
    public static MutableText parseCoordText(@NotNull Identifier dimension, @Nullable Identifier biome, double x, double y, double z) {
        String strX = String.format("%.2f", x);
        String strY = String.format("%.2f", y);
        String strZ = String.format("%.2f", z);
        MutableText biomeText = getBiomeText(biome);
        return parseTranslatableText("fmod.misc.coord", biomeText, strX, strY, strZ).styled(style -> style.withClickEvent(
            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/execute in " + dimension.toString() + " run tp @s " + strX + " " + strY + " " + strZ)
        ).withHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, parseTranslatableText("fmod.misc.clicktp").formatted(Formatting.GREEN))   
        ));
    }

    /**
     * Parses the coordinate information of the given entity into a formatted {@link MutableText} object.
     * The text includes the dimension, biome, and coordinates (x, y, z) with click and hover events.
     *
     * @param entity The entity whose coordinate information is to be parsed. Must not be null.
     * @return A {@link MutableText} object representing the formatted coordinate information
     *         with click and hover events for teleportation.
     */
    @NotNull
    public static MutableText parseCoordText(@NotNull Entity entity) {
        Identifier dimension = entity.getWorld().getRegistryKey().getValue();
        Identifier biome = entity.getWorld().getBiome(entity.getBlockPos()).getKey().map(key -> key.getValue()).orElse(null);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        return parseCoordText(dimension, biome, x, y, z);
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
            data = new ServerData(server);
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
        List<Entity> entities = new ArrayList<>();
        world.collectEntitiesByType(PASSTHROUGH_FILTER, entity -> entity != null && !entity.isRemoved(), entities, Integer.MAX_VALUE);
        return entities;
    }

    /**
     * Finds the dominant entity type from the given list of entities.
     * 
     * The dominant entity type is determined by which entity type appears most frequently
     * in the provided list.
     * 
     * @param entities a list of entities to analyze
     * @return a Map.Entry containing:
     *         <li> key: an example Entity of the most frequent type
     *         <li> value: the count of entities of that type
     *         <li> Returns null if the input list is empty
     */
    public static Map.Entry<Entity, Integer> getDominantEntities(List<Entity> entities) {
        Map<Identifier, List<Entity>> entityMap = new HashMap<>();
        for (Entity entity : entities) {
            Identifier entityId = EntityType.getId(entity.getType());
            entityMap.computeIfAbsent(entityId, k -> new ArrayList<>()).add(entity);
        }
        Map.Entry<Entity, Integer> dominantEntry = null;
        for (Map.Entry<Identifier, List<Entity>> entry : entityMap.entrySet()) {
            List<Entity> entityList = entry.getValue();
            if (dominantEntry == null || entityList.size() > dominantEntry.getValue()) {
                dominantEntry = Map.entry(entityList.get(0), entityList.size());
            }
        }
        return dominantEntry;
    }

    /**
     * Retrieves the biome name as a localized text for the given biome identifier.
     * 
     * @param biomeId The identifier of the biome. Can be null.
     * @return A {@link MutableText} representing the localized name of the biome. If the biomeId is null,
     *         a default "unknown" text is returned.
     */
    @NotNull
    public static MutableText getBiomeText(@Nullable Identifier biomeId) {
        MutableText biomeText = null;
        if (biomeId == null) {
            biomeText = Util.parseTranslatableText("fmod.misc.unknown");
        } else {
            // Vanilla should contain this translation key.
            biomeText = Text.translatable("biome." + biomeId.toString().replace(":", "."));
        }
        return biomeText;
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
        Identifier biomeId = entity.getWorld().getBiome(entity.getBlockPos()).getKey().map(key -> key.getValue()).orElse(null);
        return getBiomeText(biomeId);
    }

    /**
     * Generates a formatted text representation of a boolean value, displaying "On" in green for true and "Off" in red for false.
     *
     * @param value the boolean value to be represented as text
     * @return a {@link MutableText} object containing the formatted text representation of the boolean value
     */
    @NotNull
    public static MutableText getBooleanText(boolean value) {
        if (value) {
            return parseTranslatableText("options.on").formatted(Formatting.GREEN);
        } else {
            return parseTranslatableText("options.off").formatted(Formatting.RED);
        }
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
        MutableText entityListText = Text.literal("");
        int index = 0;
        for (Entity entity : entities) {
            if (index > 0) {
                entityListText.append(Text.literal(", "));
            }
            entityListText.append(entity.getDisplayName());
            index++;
        }
        return entityListText;
    }

    /**
     * Executes a command with the specified permission level and output handler.
     *
     * @param output the {@link CommandOutput} to receive command execution results
     * @param source the {@link Entity} executing the command
     * @param command the command string to execute
     * @param permissionLevel the permission level to execute the command with
     * @return the result code of the command execution
     */
    public static int runCommand(@NotNull CommandOutput output, @NotNull Entity source, @NotNull String command, int permissionLevel) {
        ServerCommandSource commandSource = source.getCommandSource().withLevel(permissionLevel).withOutput(output);
        return source.getServer().getCommandManager().executeWithPrefix(commandSource, command);
    }

    /**
     * Overrides the server data for the specified Minecraft server.
     *
     * @param server the Minecraft server whose data is to be overridden, must not be null.
     * @param data   the new server data to associate with the specified server, must not be null.
     */
    public static void overrideServerData(@NotNull MinecraftServer server, @NotNull ServerData data) {
        ServerData existingData = worldData.get(server);
        if (existingData != null) {
            existingData.shutdownAsyncTaskPool();
            LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Existing ServerData instance found and shut down the thread pool.");
        }
        worldData.put(server, data);
    }

    /**
     * Resets the server data for the specified Minecraft server.
     *
     * @param server the Minecraft server whose data is to be reset; must not be null
     */
    public static void resetServerData(@NotNull MinecraftServer server) {
        ServerData existingData = worldData.get(server);
        if (existingData != null) {
            existingData.shutdownAsyncTaskPool();
            LoggerFactory.getLogger(LOGGERNAME).info("FMinecraftMod: Existing ServerData instance found and shut down the thread pool.");
        }
        ServerData data = new ServerData(server);
        worldData.put(server, data);
    }
}
