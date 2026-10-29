/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.client;

import com.ykn.fmod.client.base.gui.OptionScreen;
import com.ykn.fmod.client.rule.gui.ParamWidgetRegistry;
import com.ykn.fmod.server.base.util.Util;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Client initialization, reached only through the physical-side check in FMod.
 */
public final class FModClient {

    private FModClient() {
    }

    public static void initialize() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> new OptionScreen(screen)));
        ParamWidgetRegistry.registerDefault();

        Util.LOGGER.info("FMinecraftMod: Client side initialized successfully.");
    }
}
