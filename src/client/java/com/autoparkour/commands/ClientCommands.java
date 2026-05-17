package com.autoparkour.commands;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.AutoParkourModClient;
import com.autoparkour.learning.AutoparkourLearning;
import com.autoparkour.parkour.ParkourManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class ClientCommands {

    private ClientCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autoparkour")
                    .then(ClientCommandManager.literal("reload").executes(ClientCommands::reloadConfig))
                    .then(ClientCommandManager.literal("reloadconfig").executes(ClientCommands::reloadConfig))
                    .then(ClientCommandManager.literal("rl").executes(ClientCommands::reloadConfig)));

            dispatcher.register(ClientCommandManager.literal("apreload")
                    .executes(ClientCommands::reloadConfig));
        });
    }

    private static int reloadConfig(CommandContext<FabricClientCommandSource> context) {
        try {
            boolean loaded = AutoParkourMod.getInstance().getConfigManager().reloadConfig();
            AutoparkourLearning.getInstance().load();

            AutoParkourModClient clientMod = AutoParkourModClient.getInstance();
            if (clientMod != null) {
                ParkourManager parkourManager = clientMod.getParkourManager();
                if (parkourManager != null) {
                    parkourManager.reloadRuntimeConfig();
                }
            }

            if (loaded) {
                context.getSource().sendFeedback(Text.literal("[AutoParkour] Конфиг перезагружен."));
            } else {
                context.getSource().sendFeedback(Text.literal("[AutoParkour] Конфиг перезагружен с предупреждениями. Проверь latest.log."));
            }
            return loaded ? 1 : 0;
        } catch (Exception e) {
            AutoParkourMod.getInstance().getLogger().error("Failed to reload config: " + e.getMessage());
            context.getSource().sendError(Text.literal("[AutoParkour] Не удалось перезагрузить конфиг: " + e.getMessage()));
            return 0;
        }
    }
}
