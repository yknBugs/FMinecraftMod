/**
 * Copyright (c) ykn, Xenapte
 * This mod is under the MIT License
 * We appreciate everyone that contributed to this mod.
 */

package com.ykn.fmod.client;

import com.ykn.fmod.client.base.gui.OptionScreen;
import com.ykn.fmod.client.rule.gui.ParamWidgetRegistry;
import com.ykn.fmod.server.base.util.Util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Util.MODID, dist = Dist.CLIENT)
public class FModClient {

    public FModClient(ModContainer container) {
        // This code runs only on the physical client, as soon as Minecraft is in a mod-load-ready state.

        // Register the config screen so it shows up in the mods list "Config" button.
        container.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, screen) -> new OptionScreen(screen));

        ParamWidgetRegistry.registerDefault();

        Util.LOGGER.info("FMinecraftMod: Client side initialized successfully.");
    }
}
