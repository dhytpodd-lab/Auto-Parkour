package com.autoparkour;

import com.autoparkour.config.ConfigManager;
import com.autoparkour.utils.ModLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;

public class AutoParkourMod implements ModInitializer {

    public static final String MOD_ID = "autoparkour";
    public static final String MOD_NAME = "Automatic Parkour";

    private static AutoParkourMod instance;
    private ConfigManager configManager;
    private ModLogger logger;

    @Override
    public void onInitialize() {
        instance = this;

        // Инициализация логгера
        logger = new ModLogger(MOD_ID);
        logger.info("Initializing Automatic Parkour Mod v1.0.0");

        // Загрузка конфигурации
        configManager = new ConfigManager();
        configManager.loadConfig();

        // Регистрация событий сервера для сохранения конфигурации
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            logger.info("Saving configuration on server stop");
            configManager.saveConfig();
        });

        logger.info("Automatic Parkour Mod initialized successfully!");
    }

    public static AutoParkourMod getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ModLogger getLogger() {
        return logger;
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
