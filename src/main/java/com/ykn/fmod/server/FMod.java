/**
 * Copyright (c) ykn, Xenapte
 * This mod is under the MIT License
 * We appreciate everyone that contributed to this mod.
 */

package com.ykn.fmod.server;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.event.NewLevel;
import com.ykn.fmod.server.base.event.PlayerJoin;
import com.ykn.fmod.server.base.event.WorldTick;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.NodeRegistry;
import com.ykn.fmod.server.rule.tool.RuleRegistry;

public class FMod implements ModInitializer {

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.

		// Load config
		Util.loadServerConfig();
		ServerConfigRegistry.register(Util.getServerConfig());

		// Register Nodes
		NodeRegistry.registerDefaultNodes();

		// Register rules
		RuleRegistry.registerDefault();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandRegistrater.registerCommand(dispatcher);
		});

		// Register events
		ServerWorldEvents.LOAD.register((server, world) -> {
			NewLevel newLevel = new NewLevel(server, world);
			newLevel.onNewLevel();
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			WorldTick.onWorldTick(server);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			PlayerJoin playerJoin = new PlayerJoin(handler.getPlayer());
			playerJoin.onPlayerJoin();
		});

		// Finish initialization
		Util.LOGGER.info("FMinecraftMod: Server side initialized successfully.");
	}
}
