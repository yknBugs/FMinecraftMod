package com.ykn.fmod.client;

import java.util.logging.Logger;

import com.ykn.fmod.server.base.util.Util;

import net.fabricmc.api.ClientModInitializer;

public class FMod implements ClientModInitializer {

	public static final Logger LOGGER = Logger.getLogger(Util.LOGGERNAME);

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		LOGGER.info("FMinecraftMod: Client side initialized successfully.");
	}
}