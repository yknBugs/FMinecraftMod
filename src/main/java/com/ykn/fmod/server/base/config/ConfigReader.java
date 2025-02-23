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

public class ConfigReader {

    protected static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(Util.MODID);
    protected static final Gson GSON = createGson();
    protected final String FILE_NAME;
    protected final Path FILE_PATH;

    protected ConfigReader(String fileName) {
        this.FILE_NAME = fileName;
        this.FILE_PATH = CONFIG_DIR.resolve(fileName);
    }

    public static <T extends ConfigReader> Optional<T> readFile(String fileName, Class<T> configClass) {
        Path file = CONFIG_DIR.resolve(fileName);

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

    public static <T> void writeFile(String fileName, T config) {
        Path file = CONFIG_DIR.resolve(fileName);

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

    public static <T extends ConfigReader> T loadConfig(Class<T> configClass, Supplier<T> freshInstance, String fileName) {
        T config = readFile(fileName, configClass).orElseGet(freshInstance);
        return config;
    }

    public Path getFilePath() {
        return this.FILE_PATH;
    }

    public void saveFile() {
        writeFile(this.FILE_NAME, this);
    }
}
