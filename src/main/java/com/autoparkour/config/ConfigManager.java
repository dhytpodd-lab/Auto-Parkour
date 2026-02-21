package com.autoparkour.config;

import com.autoparkour.AutoParkourMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final String CONFIG_FILE = "autoparkour.json";
    private final Path configPath;
    private final Gson gson;
    private ModConfig config;

    public ConfigManager() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
        this.config = new ModConfig();
    }

    public void loadConfig() {
        AutoParkourMod.getInstance().getLogger().info("Loading configuration from: " + configPath);

        if (Files.exists(configPath)) {
            try (Reader reader = new FileReader(configPath.toFile())) {
                ModConfig loadedConfig = gson.fromJson(reader, ModConfig.class);
                if (loadedConfig != null) {
                    config = loadedConfig;
                    AutoParkourMod.getInstance().getLogger().info("Configuration loaded successfully");
                } else {
                    AutoParkourMod.getInstance().getLogger().warn("Config file is empty, using defaults");
                }
            } catch (IOException e) {
                AutoParkourMod.getInstance().getLogger().error("Failed to load config: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            AutoParkourMod.getInstance().getLogger().info("Config file not found, creating with defaults");
            saveConfig();
        }

        validateConfig();
    }

    public void saveConfig() {
        AutoParkourMod.getInstance().getLogger().info("Saving configuration to: " + configPath);

        try (Writer writer = new FileWriter(configPath.toFile())) {
            gson.toJson(config, writer);
            AutoParkourMod.getInstance().getLogger().info("Configuration saved successfully");
        } catch (IOException e) {
            AutoParkourMod.getInstance().getLogger().error("Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateConfig() {
        boolean modified = false;

        // Валидация числовых значений
        if (config.getMaxScanRange() < 2 || config.getMaxScanRange() > 12) {
            config.setMaxScanRange(6);
            modified = true;
        }

        if (config.getMinJumpDistance() < 1 || config.getMinJumpDistance() > 5) {
            config.setMinJumpDistance(2);
            modified = true;
        }

        if (config.getMaxJumpDistance() < config.getMinJumpDistance() || config.getMaxJumpDistance() > 8) {
            config.setMaxJumpDistance(4);
            modified = true;
        }

        if (config.getBlockPlacementDelay() < 0 || config.getBlockPlacementDelay() > 10) {
            config.setBlockPlacementDelay(2);
            modified = true;
        }

        if (config.getBlockPlacementHeight() < 0 || config.getBlockPlacementHeight() > 1) {
            config.setBlockPlacementHeight(0.5);
            modified = true;
        }

        if (config.getHudOpacity() < 0 || config.getHudOpacity() > 255) {
            config.setHudOpacity(180);
            modified = true;
        }

        if (config.getHudScale() < 50 || config.getHudScale() > 200) {
            config.setHudScale(100);
            modified = true;
        }

        // Валидация цвета
        if (!config.getHudColor().matches("^#[0-9A-Fa-f]{6}$")) {
            config.setHudColor("#00FF00");
            modified = true;
        }

        if (modified) {
            AutoParkourMod.getInstance().getLogger().info("Config validation fixed some values");
            saveConfig();
        }
    }

    public ModConfig getConfig() {
        return config;
    }

    public void resetToDefaults() {
        this.config = new ModConfig();
        saveConfig();
        AutoParkourMod.getInstance().getLogger().info("Configuration reset to defaults");
    }
}
