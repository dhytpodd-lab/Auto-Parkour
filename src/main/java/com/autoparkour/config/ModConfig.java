package com.autoparkour.config;

import com.autoparkour.AutoParkourMod;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ModConfig {

    // Основные настройки
    private boolean parkourEnabled = false;
    private boolean autoSprintEnabled = true;
    private boolean autoJumpEnabled = true;
    private boolean debugMode = false;

    // Настройки паркура
    private int maxScanRange = 6;
    private int minJumpDistance = 2;
    private int maxJumpDistance = 4;
    private int blockPlacementDelay = 2;
    private double blockPlacementHeight = 0.5;

    // НОВЫЕ НАСТРОЙКИ ДВИЖЕНИЯ
    private boolean allowDiagonalMovement = false;
    private boolean preventOvershoot = true;
    private boolean autoLookAtTarget = false;
    private double lookSpeed = 5.0;
    private boolean preferStraightDirections = true; // НОВОЕ ПОЛЕ

    // Настройки безопасности
    private boolean safeFallEnabled = true;
    private int fallDamageThreshold = 3;
    private boolean avoidLava = true;
    private boolean avoidVoid = true;

    // НОВЫЕ НАСТРОЙКИ БЛОКОВ
    private boolean useBlockWhitelist = false;
    private List<String> allowedBlocks = Arrays.asList(
        "minecraft:stone",
        "minecraft:cobblestone",
        "minecraft:dirt",
        "minecraft:grass_block",
        "minecraft:sand",
        "minecraft:sandstone",
        "minecraft:oak_planks",
        "minecraft:spruce_planks"
    );
      // Добавьте после поля allowedBlocks (примерно строка 54)
   private boolean useIgnoreList = true;
   private List<String> ignoredBlocks = Arrays.asList(
       "minecraft:air",
       "minecraft:cave_air",
       "minecraft:void_air",
       "minecraft:water",
       "minecraft:lava",
       "minecraft:fire",
       "minecraft:soul_fire",
       "minecraft:cactus",
       "minecraft:sweet_berry_bush",
       "minecraft:magma_block",
       "minecraft:campfire",
       "minecraft:soul_campfire"
    );

    // Настройки HUD
    private boolean hudVisible = true;
    private int hudX = 10;
    private int hudY = 10;
    private int hudOpacity = 180;
    private String hudColor = "#00FF00";
    private int hudScale = 100;
    private boolean hudShowCoordinates = true;
    private boolean hudShowTargetBlock = true;

    // Настройки клавиш
    private String keyToggleParkour = "key.keyboard.p";
    private String keyToggleSprint = "key.keyboard.k";
    private String keyToggleHUD = "key.keyboard.h";
    private String keyToggleJump = "key.keyboard.j";
    private String keyOpenConfig = "key.keyboard.o";

    // Геттеры и сеттеры для основных настроек

    public boolean isParkourEnabled() {
        return parkourEnabled;
    }

    public void setParkourEnabled(boolean parkourEnabled) {
        this.parkourEnabled = parkourEnabled;
    }

    public boolean isAutoSprintEnabled() {
        return autoSprintEnabled;
    }

    public void setAutoSprintEnabled(boolean autoSprintEnabled) {
        this.autoSprintEnabled = autoSprintEnabled;
    }

    public boolean isAutoJumpEnabled() {
        return autoJumpEnabled;
    }

    public void setAutoJumpEnabled(boolean autoJumpEnabled) {
        this.autoJumpEnabled = autoJumpEnabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public int getMaxScanRange() {
        return maxScanRange;
    }

    public void setMaxScanRange(int maxScanRange) {
        this.maxScanRange = Math.max(2, Math.min(12, maxScanRange));
    }

    public int getMinJumpDistance() {
        return minJumpDistance;
    }

    public void setMinJumpDistance(int minJumpDistance) {
        this.minJumpDistance = Math.max(1, Math.min(5, minJumpDistance));
    }

    public int getMaxJumpDistance() {
        return maxJumpDistance;
    }

    public void setMaxJumpDistance(int maxJumpDistance) {
        this.maxJumpDistance = Math.max(this.minJumpDistance, Math.min(8, maxJumpDistance));
    }

    public int getBlockPlacementDelay() {
        return blockPlacementDelay;
    }

    public void setBlockPlacementDelay(int blockPlacementDelay) {
        this.blockPlacementDelay = Math.max(0, Math.min(10, blockPlacementDelay));
    }

    public double getBlockPlacementHeight() {
        return blockPlacementHeight;
    }

    public void setBlockPlacementHeight(double blockPlacementHeight) {
        this.blockPlacementHeight = Math.max(0, Math.min(1, blockPlacementHeight));
    }

    // НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ ДВИЖЕНИЯ

    public boolean isAllowDiagonalMovement() {
        return allowDiagonalMovement;
    }

    public void setAllowDiagonalMovement(boolean allowDiagonalMovement) {
        this.allowDiagonalMovement = allowDiagonalMovement;
    }

    public boolean isPreventOvershoot() {
        return preventOvershoot;
    }

    public void setPreventOvershoot(boolean preventOvershoot) {
        this.preventOvershoot = preventOvershoot;
    }

    public boolean isAutoLookAtTarget() {
        return autoLookAtTarget;
    }

    public void setAutoLookAtTarget(boolean autoLookAtTarget) {
        this.autoLookAtTarget = autoLookAtTarget;
    }

    public double getLookSpeed() {
        return lookSpeed;
    }

    public void setLookSpeed(double lookSpeed) {
        this.lookSpeed = Math.max(1.0, Math.min(20.0, lookSpeed));
    }

    public boolean isPreferStraightDirections() { // НОВЫЙ МЕТОД
        return preferStraightDirections;
    }

    public void setPreferStraightDirections(boolean preferStraightDirections) { // НОВЫЙ МЕТОД
        this.preferStraightDirections = preferStraightDirections;
    }

    // Настройки безопасности

    public boolean isSafeFallEnabled() {
        return safeFallEnabled;
    }

    public void setSafeFallEnabled(boolean safeFallEnabled) {
        this.safeFallEnabled = safeFallEnabled;
    }

    public int getFallDamageThreshold() {
        return fallDamageThreshold;
    }

    public void setFallDamageThreshold(int fallDamageThreshold) {
        this.fallDamageThreshold = Math.max(1, Math.min(10, fallDamageThreshold));
    }

    public boolean isAvoidLava() {
        return avoidLava;
    }

    public void setAvoidLava(boolean avoidLava) {
        this.avoidLava = avoidLava;
    }

    public boolean isAvoidVoid() {
        return avoidVoid;
    }

    public void setAvoidVoid(boolean avoidVoid) {
        this.avoidVoid = avoidVoid;
    }

    // Настройки блоков

    public boolean isUseBlockWhitelist() {
        return useBlockWhitelist;
    }

    public void setUseBlockWhitelist(boolean useBlockWhitelist) {
        this.useBlockWhitelist = useBlockWhitelist;
    }

    public List<String> getAllowedBlocks() {
        return allowedBlocks;
    }

    public void setAllowedBlocks(List<String> allowedBlocks) {
        this.allowedBlocks = allowedBlocks;
    }
    // Добавьте после методов для allowedBlocks (примерно строка 280)
    public boolean isUseIgnoreList() {
        return useIgnoreList;
    }

    public void setUseIgnoreList(boolean useIgnoreList) {
        this.useIgnoreList = useIgnoreList;
    }

    public List<String> getIgnoredBlocks() {
        return ignoredBlocks;
    }

    public void setIgnoredBlocks(List<String> ignoredBlocks) {
        this.ignoredBlocks = ignoredBlocks;
    }

    // Настройки HUD

    public boolean isHudVisible() {
        return hudVisible;
    }

    public void setHudVisible(boolean hudVisible) {
        this.hudVisible = hudVisible;
    }

    public int getHudX() {
        return hudX;
    }

    public void setHudX(int hudX) {
        this.hudX = Math.max(0, hudX);
    }

    public int getHudY() {
        return hudY;
    }

    public void setHudY(int hudY) {
        this.hudY = Math.max(0, hudY);
    }

    public int getHudOpacity() {
        return hudOpacity;
    }

    public void setHudOpacity(int hudOpacity) {
        this.hudOpacity = Math.max(0, Math.min(255, hudOpacity));
    }

    public String getHudColor() {
        return hudColor;
    }

    public void setHudColor(String hudColor) {
        if (hudColor.matches("^#[0-9A-Fa-f]{6}$")) {
            this.hudColor = hudColor;
        }
    }

    public Color getHudColorAsRGB() {
        try {
            return Color.decode(hudColor);
        } catch (Exception e) {
            return Color.GREEN;
        }
    }

    public int getHudScale() {
        return hudScale;
    }

    public void setHudScale(int hudScale) {
        this.hudScale = Math.max(50, Math.min(200, hudScale));
    }

    public boolean isHudShowCoordinates() {
        return hudShowCoordinates;
    }

    public void setHudShowCoordinates(boolean hudShowCoordinates) {
        this.hudShowCoordinates = hudShowCoordinates;
    }

    public boolean isHudShowTargetBlock() {
        return hudShowTargetBlock;
    }

    public void setHudShowTargetBlock(boolean hudShowTargetBlock) {
        this.hudShowTargetBlock = hudShowTargetBlock;
    }

    // Настройки клавиш

    public String getKeyToggleParkour() {
        return keyToggleParkour;
    }

    public void setKeyToggleParkour(String keyToggleParkour) {
        this.keyToggleParkour = keyToggleParkour;
    }

    public String getKeyToggleSprint() {
        return keyToggleSprint;
    }

    public void setKeyToggleSprint(String keyToggleSprint) {
        this.keyToggleSprint = keyToggleSprint;
    }

    public String getKeyToggleHUD() {
        return keyToggleHUD;
    }

    public void setKeyToggleHUD(String keyToggleHUD) {
        this.keyToggleHUD = keyToggleHUD;
    }

    public String getKeyToggleJump() {
        return keyToggleJump;
    }

    public void setKeyToggleJump(String keyToggleJump) {
        this.keyToggleJump = keyToggleJump;
    }

    public String getKeyOpenConfig() {
        return keyOpenConfig;
    }

    public void setKeyOpenConfig(String keyOpenConfig) {
        this.keyOpenConfig = keyOpenConfig;
    }
}
