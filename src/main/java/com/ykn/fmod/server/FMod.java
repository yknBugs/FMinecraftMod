/**
 * Copyright (c) ykn, Xenapte
 * This mod is under the MIT License
 * We appreciate everyone that contributed to this mod.
 */

package com.ykn.fmod.server;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.event.GameEventHandler;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.NodeRegistry;
import com.ykn.fmod.server.rule.tool.RuleRegistry;

/**
 * The only place that talks to the loader about events.
 *
 * <p>Every registration below is a pure forwarder: it reads the loader's arguments and hands them
 * to {@link GameEventHandler}, and does nothing else. No filtering, no conversion, no conditionals,
 * no configuration - that all lives one layer down, so porting to another loader only means
 * rewriting this method and the mixins.
 */
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
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CommandRegistrater.registerCommand(dispatcher));

		// Register events
		registerServerEvents();
		registerPlayerEvents();
		registerWorldEvents();

		// Finish initialization
		Util.LOGGER.info("FMinecraftMod: Server side initialized successfully.");
	}

	private void registerServerEvents() {
		ServerWorldEvents.LOAD.register(GameEventHandler::worldLoadEventDispatcher);
		ServerTickEvents.END_SERVER_TICK.register(GameEventHandler::serverTickEventDispatcher);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> GameEventHandler.playerJoinEventDispatcher(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GameEventHandler.playerLeaveEventDispatcher(handler.getPlayer()));
	}

	private void registerPlayerEvents() {
		PlayerBlockBreakEvents.AFTER.register(GameEventHandler::playerBreakBlockEventDispatcher);
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> GameEventHandler.playerInteractBlockEventDispatcher(player, hand, hit));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> GameEventHandler.playerInteractEntityEventDispatcher(player, hand, entity, hit));
		UseItemCallback.EVENT.register((player, world, hand) -> GameEventHandler.playerItemUseEventDispatcher(player, hand));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(GameEventHandler::playerDimensionChangeEventDispatcher);
		EntitySleepEvents.START_SLEEPING.register(GameEventHandler::playerSleepEventDispatcher);
	}

	private void registerWorldEvents() {
		ServerEntityEvents.ENTITY_LOAD.register(GameEventHandler::entityLoadEventDispatcher);
		ServerChunkEvents.CHUNK_LOAD.register(GameEventHandler::chunkLoadEventDispatcher);
		ServerChunkEvents.CHUNK_UNLOAD.register(GameEventHandler::chunkUnloadEventDispatcher);
	}
}
