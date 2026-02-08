package com.westeroscraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WesterosMobsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("westerosmobs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Path.of("config/westerosmobs.json");

    public static boolean mountEnabled = true;
    public static boolean petEnabled = true;

    private static class ConfigData {
        boolean mountEnabled = true;
        boolean petEnabled = true;
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                save(data);
                mountEnabled = data.mountEnabled;
                petEnabled = data.petEnabled;
                LOGGER.info("Config: mountEnabled={}, petEnabled={}", mountEnabled, petEnabled);
            } else {
                saveDefaults();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    private static void saveDefaults() {
        try {
            ConfigData data = new ConfigData();
            String json = GSON.toJson(data);
            Files.writeString(CONFIG_FILE, json);
            LOGGER.info("Created default config file");
        } catch (IOException e) {
            LOGGER.error("Failed to create default config", e);
        }
    }

    private static void save(ConfigData data) {
        try {
            String json = GSON.toJson(data);
            Files.writeString(CONFIG_FILE, json);
            LOGGER.info("Updated config file with current defaults");
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
