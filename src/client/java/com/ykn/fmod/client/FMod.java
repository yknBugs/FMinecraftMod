package com.ykn.fmod.client;

import com.ykn.fmod.server.base.util.Util;

import net.fabricmc.api.ClientModInitializer;

public class FMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		Util.LOGGER.info("FMinecraftMod: Client side initialized successfully.");
	}
}