package com.autoparkour.config;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.hud.HUDConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new HUDConfigScreen(parent, AutoParkourMod.getInstance().getConfigManager().getConfig());
    }
}
