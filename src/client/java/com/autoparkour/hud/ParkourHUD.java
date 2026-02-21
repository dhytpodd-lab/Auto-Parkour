package com.autoparkour.hud;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig;
import net.minecraft.util.math.BlockPos;

public class ParkourHUD {

    private boolean visible = true;
    private String statusMessage = "AutoParkour: OFF";
    private int statusColor = 0xFF5555; // RED
    private int blocksNearby = 0;
    private BlockPos targetBlock = null;
    private String sprintStatus = "OFF";
    private int sprintColor = 0xFF5555;
    private String jumpStatus = "OFF";
    private int jumpColor = 0xFF5555;

    private final ModConfig config;

    public ParkourHUD() {
        this.config = AutoParkourMod.getInstance().getConfigManager().getConfig();
        this.visible = config.isHudVisible();
    }

    public void updateStatus(boolean parkourEnabled, int nearbyBlocks, BlockPos target, boolean autoSprint, boolean autoJump) {
        // Обновление статуса паркура
        if (parkourEnabled) {
            statusMessage = "AutoParkour: ON";
            statusColor = 0x55FF55; // GREEN
        } else {
            statusMessage = "AutoParkour: OFF";
            statusColor = 0xFF5555; // RED
        }

        // Обновление количества блоков
        this.blocksNearby = nearbyBlocks;

        // Обновление целевого блока
        this.targetBlock = target;

        // Обновление статуса спринта
        if (autoSprint) {
            sprintStatus = "ON";
            sprintColor = 0x55FF55;
        } else {
            sprintStatus = "OFF";
            sprintColor = 0xFF5555;
        }

        // Обновление статуса прыжка
        if (autoJump) {
            jumpStatus = "ON";
            jumpColor = 0x55FF55;
        } else {
            jumpStatus = "OFF";
            jumpColor = 0xFF5555;
        }
    }

    public void toggleVisibility() {
        this.visible = !this.visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getStatusColor() {
        return statusColor;
    }

    public int getBlocksNearby() {
        return blocksNearby;
    }

    public BlockPos getTargetBlock() {
        return targetBlock;
    }

    public String getSprintStatus() {
        return sprintStatus;
    }

    public int getSprintColor() {
        return sprintColor;
    }

    public String getJumpStatus() {
        return jumpStatus;
    }

    public int getJumpColor() {
        return jumpColor;
    }
}
