package com.autoparkour.mixin.client;

import com.autoparkour.AutoParkourMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(RunArgs args, CallbackInfo ci) {
        AutoParkourMod.getInstance().getLogger().debug("MinecraftClient initialized");
    }

    // ИСПРАВЛЕНО: используем правильный метод для остановки
    @Inject(method = "stop", at = @At("HEAD"), require = 0)
    private void onStop(CallbackInfo ci) {
        // Этот метод может не существовать, поэтому добавили require=0
        AutoParkourMod.getInstance().getLogger().info("Minecraft shutting down, saving config...");
        AutoParkourMod.getInstance().getConfigManager().saveConfig();
    }

    // ДОБАВЛЕНО: альтернативный метод для 1.21.8
    @Inject(method = "scheduleStop", at = @At("HEAD"))
    private void onScheduleStop(CallbackInfo ci) {
        AutoParkourMod.getInstance().getLogger().info("Minecraft shutting down (scheduled), saving config...");
        AutoParkourMod.getInstance().getConfigManager().saveConfig();
    }
}
