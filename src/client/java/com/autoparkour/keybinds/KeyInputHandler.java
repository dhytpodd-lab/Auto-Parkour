package com.autoparkour.keybinds;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig; // <-- ЭТОТ ИМПОРТ НУЖЕН
import com.autoparkour.hud.ParkourHUD;
import com.autoparkour.hud.HUDConfigScreen;
import com.autoparkour.parkour.ParkourManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class KeyInputHandler {

    private final ParkourManager parkourManager;
    private final ParkourHUD parkourHUD;
    private final ModConfig config; // Теперь компилятор знает этот класс
    private long lastPressTime = 0;
    private static final long PRESS_COOLDOWN = 200;

    public KeyInputHandler(ParkourManager parkourManager, ParkourHUD parkourHUD) {
        this.parkourManager = parkourManager;
        this.parkourHUD = parkourHUD;
        this.config = AutoParkourMod.getInstance().getConfigManager().getConfig();
    }

    public void handleTick(MinecraftClient client) {
        if (client.player == null) return;

        long currentTime = System.currentTimeMillis();
        boolean cooldownPassed = currentTime - lastPressTime > PRESS_COOLDOWN;

        // Обработка нажатий клавиш
        while (KeyBindings.getToggleParkourKey().wasPressed()) {
            if (cooldownPassed) {
                parkourManager.toggleEnabled();
                client.player.sendMessage(
                        Text.literal("§6[AutoParkour] §fПаркур " +
                                (parkourManager.isEnabled() ? "§aвключен" : "§cвыключен")),
                        true
                );
                lastPressTime = currentTime;
            }
        }

        while (KeyBindings.getToggleSprintKey().wasPressed()) {
            if (cooldownPassed) {
                boolean newState = !config.isAutoSprintEnabled();
                config.setAutoSprintEnabled(newState);
                AutoParkourMod.getInstance().getConfigManager().saveConfig();
                client.player.sendMessage(
                        Text.literal("§6[AutoParkour] §fАвто-спринт " +
                                (newState ? "§aвключен" : "§cвыключен")),
                        true
                );
                lastPressTime = currentTime;
            }
        }

        while (KeyBindings.getToggleHUDKey().wasPressed()) {
            if (cooldownPassed) {
                parkourHUD.toggleVisibility();
                config.setHudVisible(parkourHUD.isVisible());
                AutoParkourMod.getInstance().getConfigManager().saveConfig();
                client.player.sendMessage(
                        Text.literal("§6[AutoParkour] §fHUD " +
                                (parkourHUD.isVisible() ? "§aвключен" : "§cвыключен")),
                        true
                );
                lastPressTime = currentTime;
            }
        }

        while (KeyBindings.getToggleJumpKey().wasPressed()) {
            if (cooldownPassed) {
                boolean newState = !config.isAutoJumpEnabled();
                config.setAutoJumpEnabled(newState);
                AutoParkourMod.getInstance().getConfigManager().saveConfig();
                client.player.sendMessage(
                        Text.literal("§6[AutoParkour] §fАвто-прыжок " +
                                (newState ? "§aвключен" : "§cвыключен")),
                        true
                );
                lastPressTime = currentTime;
            }
        }

        while (KeyBindings.getOpenConfigKey().wasPressed()) {
            if (cooldownPassed) {
                client.setScreen(new HUDConfigScreen(client.currentScreen, config));
                lastPressTime = currentTime;
            }
        }

        while (KeyBindings.getScanRangeUpKey().wasPressed()) {
            if (cooldownPassed) {
                int newRange = config.getMaxScanRange() + 1;
                config.setMaxScanRange(newRange);
                AutoParkourMod.getInstance().getConfigManager().saveConfig();
                client.player.sendMessage(
                        Text.literal("§6[AutoParkour] §fДальность сканирования: §a" + config.getMaxScanRange()),
                        true
                );
                lastPressTime = currentTime;
            }
        }

        while (KeyBindings.getScanRangeDownKey().wasPressed()) {
            if (cooldownPassed) {
                int newRange = config.getMaxScanRange() - 1;
                config.setMaxScanRange(newRange);
                AutoParkourMod.getInstance().getConfigManager().saveConfig();
                client.player.sendMessage(
                        Text.literal("§6[AutoParkour] §fДальность сканирования: §a" + config.getMaxScanRange()),
                        true
                );
                lastPressTime = currentTime;
            }
        }

        // Обработка зажатых клавиш для постоянных действий
        if (parkourManager.isEnabled()) {
            // Здесь можно добавить обработку зажатых клавиш
        }
    }
}
