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

    public static MountConfig mount = new MountConfig();
    public static PetConfig pet = new PetConfig();

    public static class MountConfig {
        public boolean enabled = true;
        public String permission = "westerosmobs.mount";
    }

    public static class PetConfig {
        public boolean enabled = true;
        public String permission = "westerosmobs.pet";
        public int maxPetsPerPlayer = 2;
    }

    private static class ConfigData {
        MountConfig mount = new MountConfig();
        PetConfig pet = new PetConfig();
    }

    public static void load() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            if (Files.exists(CONFIG_FILE)) {
                String json = Files.readString(CONFIG_FILE);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                save(data);
                if (data.mount != null) {
                    mount = data.mount;
                }
                if (data.pet != null) {
                    pet = data.pet;
                }
                LOGGER.info("Mount config: enabled={}, permission={}",
                    mount.enabled, mount.permission);
                LOGGER.info("Pet config: enabled={}, permission={}, maxPets={}",
                    pet.enabled, pet.permission, pet.maxPetsPerPlayer);
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
