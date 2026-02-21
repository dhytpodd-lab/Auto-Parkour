package com.autoparkour;

import com.autoparkour.hud.HUDRenderer;
import com.autoparkour.hud.ParkourHUD;
import com.autoparkour.keybinds.KeyBindings;
import com.autoparkour.keybinds.KeyInputHandler;
import com.autoparkour.parkour.ParkourManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class AutoParkourModClient implements ClientModInitializer {

    private static AutoParkourModClient instance;
    private ParkourManager parkourManager;
    private ParkourHUD parkourHUD;
    private HUDRenderer hudRenderer;
    private KeyBindings keyBindings;
    private KeyInputHandler keyInputHandler;

    @Override
    public void onInitializeClient() {
        instance = this;

        AutoParkourMod.getInstance().getLogger().info("Initializing client side");

        try {
            // Инициализация менеджера паркура
            parkourManager = new ParkourManager();

            // Инициализация HUD
            parkourHUD = new ParkourHUD();

            // Инициализация рендерера HUD
            MinecraftClient client = MinecraftClient.getInstance();
            hudRenderer = new HUDRenderer(parkourHUD);

            // Инициализация клавиш
            keyBindings = new KeyBindings();
            keyBindings.register();

            // Инициализация обработчика клавиш
            keyInputHandler = new KeyInputHandler(parkourManager, parkourHUD);

            // Регистрация обработчика тиков клиента
            ClientTickEvents.END_CLIENT_TICK.register(clientTick -> {
                try {
                    if (clientTick.player != null && !clientTick.isPaused()) {
                        if (keyInputHandler != null) {
                            keyInputHandler.handleTick(clientTick);
                        }

                        if (parkourManager != null && parkourManager.isEnabled()) {
                            parkourManager.tick(clientTick);
                        }
                    }
                } catch (Exception e) {
                    AutoParkourMod.getInstance().getLogger().error("Error in client tick: " + e.getMessage());
                }
            });

            // Регистрация рендера HUD
            HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
                try {
                    if (parkourHUD != null && parkourHUD.isVisible() && hudRenderer != null) {
                        hudRenderer.render(drawContext, tickDelta);
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки рендеринга
                }
            });

            AutoParkourMod.getInstance().getLogger().info("Client side initialized successfully");

        } catch (Exception e) {
            AutoParkourMod.getInstance().getLogger().error("Failed to initialize client side: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static AutoParkourModClient getInstance() {
        return instance;
    }

    public ParkourManager getParkourManager() {
        return parkourManager;
    }

    public ParkourHUD getParkourHUD() {
        return parkourHUD;
    }
}
