package com.ykn.fmod.server;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.util.Util;

public class FMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(Util.LOGGERNAME);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		CommandRegistrater commandRegistrater = new CommandRegistrater(LOGGER);
		commandRegistrater.registerCommand();

		Util.loadServerConfig();
		LOGGER.info("FMinecraftMod: Server side initialized successfully.");
	}
}