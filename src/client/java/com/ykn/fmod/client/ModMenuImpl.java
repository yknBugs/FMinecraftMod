package com.ykn.fmod.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.ykn.fmod.client.base.gui.OptionScreen;
import com.ykn.fmod.server.base.util.Util;

public class ModMenuImpl implements ModMenuApi {

    public ModMenuImpl() {
        Util.LOGGER.info("FMinecraftMod: ModMenuImpl initialized successfully.");
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OptionScreen::new;
        // return screen -> {
        //     return new OptionScreen(screen);
        // };
    }
}
