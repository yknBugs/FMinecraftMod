package com.ykn.fmod.server;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.event.NewLevel;
import com.ykn.fmod.server.base.event.WorldTick;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

public class FMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(Util.LOGGERNAME);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		CommandRegistrater commandRegistrater = new CommandRegistrater(LOGGER);
		commandRegistrater.registerCommand();

		// Register events
		ServerWorldEvents.LOAD.register((server, world) -> {
			NewLevel newLevel = new NewLevel(server, world);
			newLevel.onNewLevel();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			WorldTick worldTick = new WorldTick(server);
			worldTick.onWorldTick();
		});

		// Register Nodes
		NodeRegistry.registerDefaultNodes();

		Util.loadServerConfig();
		LOGGER.info("FMinecraftMod: Server side initialized successfully.");
	}
}