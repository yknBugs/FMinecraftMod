/**
 * Copyright (c) Mod: No-Chat-Reports
 * This file is under the WTFPL License
 */

package com.ykn.fmod.server.base.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ykn.fmod.server.base.util.Util;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The ConfigReader class provides utility methods for reading and writing configuration files
 * in a structured and safe manner. It uses Gson for JSON serialization and deserialization.
 * The configuration files are stored in a specific directory determined by the FabricLoader.
 * 
 * <p>Key Features:
 * <ul>
 *   <li>Ensures configuration files are within the designated directory.</li>
 *   <li>Supports reading and writing JSON configuration files.</li>
 *   <li>Automatically creates directories if they do not exist.</li>
 * </ul>
 * </p>
 * 
 * <p>Usage:
 * <ul>
 *   <li>Use {@link #readFile(String, Class)} to read a configuration file.</li>
 *   <li>Use {@link #writeFile(String, Object)} to write a configuration file.</li>
 *   <li>Use {@link #loadConfig(Class, Supplier, String)} to load a configuration with a fallback instance.</li>
 *   <li>Call {@link #saveFile()} to save the current instance to its associated file.</li>
 * </ul>
 * </p>
 * 
 * <p>Note: This class is designed to be extended for specific configuration types.</p>
 * 
 * <p>Example:
 * <pre>
 * {@code
 * public class MyConfig extends ConfigReader {
 *     private String someSetting;
 * 
 *     public MyConfig() {
 *         super("my_config.json");
 *     }
 * }
 * 
 * MyConfig config = ConfigReader.loadConfig(MyConfig.class, MyConfig::new, "my_config.json");
 * config.saveFile();
 * }
 * </pre>
 * </p>
 */
public class ConfigReader {

    protected static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID);
    protected static final Gson GSON = createGson();
    protected final String FILE_NAME;
    protected final Path FILE_PATH;

    protected ConfigReader(String fileName) {
        this.FILE_NAME = fileName;
        this.FILE_PATH = CONFIG_DIR.resolve(fileName);
    }

    /**
     * Reads a configuration file and deserializes its content into an instance of the specified class.
     *
     * @param <T>         The type of the configuration class, which must extend {@code ConfigReader}.
     * @param fileName    The name of the configuration file to read.
     * @param configClass The class of the configuration object to deserialize into.
     * @return An {@code Optional} containing the deserialized configuration object if successful,
     *         or an empty {@code Optional} if the file could not be read or deserialized.
     */
    public static <T extends ConfigReader> Optional<T> readFile(String fileName, Class<T> configClass) {
        Path file = CONFIG_DIR.resolve(fileName);

        if (!file.normalize().startsWith(CONFIG_DIR.normalize())) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Config file path is outside of the config directory");
            return Optional.empty();
        }
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            return Optional.of(GSON.fromJson(reader, configClass));
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not read config file", e);
            return Optional.empty();
        }
    }

    /**
     * Writes the given configuration object to a file in the configuration directory.
     * 
     * @param <T>      The type of the configuration object to be written.
     * @param fileName The name of the file to write the configuration to.
     * @param config   The configuration object to be serialized and written to the file.
     */
    public static <T> void writeFile(String fileName, T config) {
        Path file = CONFIG_DIR.resolve(fileName);

        if (!file.normalize().startsWith(CONFIG_DIR.normalize())) {
            LoggerFactory.getLogger(Util.LOGGERNAME).warn("FMinecraftMod: Config file path is outside of the config directory");
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                GSON.toJson(config, writer);
            } catch (Exception e) {
                LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not write config file", e);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Util.LOGGERNAME).error("FMinecraftMod: Could not create the config directory", e);
        }
    }

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
        builder.setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getDeclaringClass().equals(ConfigReader.class) || f.getDeclaringClass().equals(ReentrantReadWriteLock.class);
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        });

        // if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
		// 	builder.registerTypeAdapter(ServerAddress.class, ServerAddressAdapter.INSTANCE);
		// }

        return builder.create();
    }

    /**
     * Loads a configuration object of the specified type from a file. If the file does not exist
     * or cannot be read, a fresh instance of the configuration object is created using the provided
     * supplier.
     *
     * @param <T>          The type of the configuration object, which must extend {@code ConfigReader}.
     * @param configClass  The {@code Class} object representing the type of the configuration object.
     * @param freshInstance A {@code Supplier} that provides a fresh instance of the configuration object
     *                      if the file cannot be read.
     * @param fileName     The name of the file from which to load the configuration.
     * @return An instance of the configuration object, either loaded from the file or created fresh.
     */
    public static <T extends ConfigReader> T loadConfig(Class<T> configClass, Supplier<T> freshInstance, String fileName) {
        T config = readFile(fileName, configClass).orElseGet(freshInstance);
        return config;
    }

    /**
     * Retrieves the file path associated with this configuration reader.
     *
     * @return the file path as a {@link Path} object.
     */
    public Path getFilePath() {
        return this.FILE_PATH;
    }

    /**
     * Saves the current configuration object to a file.
     */
    public void saveFile() {
        writeFile(this.FILE_NAME, this);
    }
}
