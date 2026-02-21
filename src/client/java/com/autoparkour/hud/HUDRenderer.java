package com.autoparkour.hud;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.AutoParkourModClient;
import com.autoparkour.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

public class HUDRenderer {

    private final ParkourHUD hud;
    private final ModConfig config;
    private final MinecraftClient client;

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 5;

    public HUDRenderer(ParkourHUD hud) {
        this.hud = hud;
        this.config = AutoParkourMod.getInstance().getConfigManager().getConfig();
        this.client = MinecraftClient.getInstance();
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            if (client == null || client.player == null || !hud.isVisible()) return;

            TextRenderer textRenderer = client.textRenderer;
            if (textRenderer == null) return;

            // Получаем ParkourManager через клиент
            var parkourManager = AutoParkourModClient.getInstance().getParkourManager();
            if (parkourManager == null) return;

            int x = config.getHudX();
            int y = config.getHudY();
            int opacity = config.getHudOpacity();
            Color color = config.getHudColorAsRGB();

            // Создаем полупрозрачный фон
            int width = 150;
            int height = 100;

            // Рисуем фон
            context.fill(x - PADDING, y - PADDING,
                        x + width + PADDING, y + height + PADDING,
                        (opacity << 24) | 0x000000);

            // Рисуем рамку
            context.drawBorder(x - PADDING, y - PADDING,
                              width + PADDING * 2, height + PADDING * 2,
                              color.getRGB());

            int currentY = y;

            // Заголовок
            context.drawText(textRenderer, "§l§nAutoParkour", x, currentY, color.getRGB(), true);
            currentY += LINE_HEIGHT + 2;

            // Статус
            String status = parkourManager.isEnabled() ? "§aВКЛ" : "§cВЫКЛ";
            context.drawText(textRenderer, "Статус: " + status, x, currentY, color.getRGB(), true);
            currentY += LINE_HEIGHT;

            // Блоки рядом
            context.drawText(textRenderer, "Блоки: §a" + parkourManager.getNearbyBlocksCount(), x, currentY, color.getRGB(), true);
            currentY += LINE_HEIGHT;

            // Целевой блок
            if (parkourManager.getCurrentTargetBlock() != null) {
                BlockPos target = parkourManager.getCurrentTargetBlock();
                String targetStr = String.format("Цель: §a%d %d %d", target.getX(), target.getY(), target.getZ());
                context.drawText(textRenderer, targetStr, x, currentY, color.getRGB(), true);
                currentY += LINE_HEIGHT;
            }

            // Настройки движения
            String diagonal = config.isAllowDiagonalMovement() ? "§aДа" : "§cНет";
            context.drawText(textRenderer, "Диагональ: " + diagonal, x, currentY, color.getRGB(), true);
            currentY += LINE_HEIGHT;

            String overshoot = config.isPreventOvershoot() ? "§aДа" : "§cНет";
            context.drawText(textRenderer, "Торможение: " + overshoot, x, currentY, color.getRGB(), true);
            currentY += LINE_HEIGHT;

            // Координаты игрока
            BlockPos playerPos = client.player.getBlockPos();
            String coords = String.format("§7%d %d %d", playerPos.getX(), playerPos.getY(), playerPos.getZ());
            context.drawText(textRenderer, "XYZ: " + coords, x, currentY, 0xAAAAAA, true);

        } catch (Exception e) {
            // Игнорируем ошибки рендеринга
        }
    }
}
