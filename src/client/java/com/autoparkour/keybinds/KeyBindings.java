package com.autoparkour.keybinds;

import com.autoparkour.AutoParkourMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String KEY_CATEGORY = "key.category.autoparkour";
    public static final String KEY_TOGGLE_PARKOUR = "key.autoparkour.toggle";
    public static final String KEY_TOGGLE_SPRINT = "key.autoparkour.sprint";
    public static final String KEY_TOGGLE_HUD = "key.autoparkour.hud";
    public static final String KEY_TOGGLE_JUMP = "key.autoparkour.jump";
    public static final String KEY_OPEN_CONFIG = "key.autoparkour.config";
    public static final String KEY_SCAN_RANGE_UP = "key.autoparkour.scan_up";
    public static final String KEY_SCAN_RANGE_DOWN = "key.autoparkour.scan_down";

    private static KeyBinding toggleParkourKey;
    private static KeyBinding toggleSprintKey;
    private static KeyBinding toggleHUDKey;
    private static KeyBinding toggleJumpKey;
    private static KeyBinding openConfigKey;
    private static KeyBinding scanRangeUpKey;
    private static KeyBinding scanRangeDownKey;

    public void register() {
        AutoParkourMod.getInstance().getLogger().info("Registering key bindings");

        // Основные клавиши
        toggleParkourKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_TOGGLE_PARKOUR,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                KEY_CATEGORY
        ));

        toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_TOGGLE_SPRINT,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KEY_CATEGORY
        ));

        toggleHUDKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_TOGGLE_HUD,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY
        ));

        toggleJumpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_TOGGLE_JUMP,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KEY_CATEGORY
        ));

        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_OPEN_CONFIG,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                KEY_CATEGORY
        ));

        // Клавиши для настройки на лету
        scanRangeUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_SCAN_RANGE_UP,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));

        scanRangeDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                KEY_SCAN_RANGE_DOWN,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KEY_CATEGORY
        ));

        AutoParkourMod.getInstance().getLogger().info("Key bindings registered");
    }

    public static KeyBinding getToggleParkourKey() {
        return toggleParkourKey;
    }

    public static KeyBinding getToggleSprintKey() {
        return toggleSprintKey;
    }

    public static KeyBinding getToggleHUDKey() {
        return toggleHUDKey;
    }

    public static KeyBinding getToggleJumpKey() {
        return toggleJumpKey;
    }

    public static KeyBinding getOpenConfigKey() {
        return openConfigKey;
    }

    public static KeyBinding getScanRangeUpKey() {
        return scanRangeUpKey;
    }

    public static KeyBinding getScanRangeDownKey() {
        return scanRangeDownKey;
    }
}
