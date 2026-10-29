/**
 * Copyright (c) ykn, Xenapte
 * This mod is under the MIT License
 * We appreciate everyone that contributed to this mod.
 */

package com.ykn.fmod.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import com.ykn.fmod.client.FModClient;
import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.event.GameEventHandler;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.NodeRegistry;
import com.ykn.fmod.server.rule.tool.RuleRegistry;

@Mod(Util.MODID)
public class FMod {

	public FMod() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.

		// Load config
		Util.loadServerConfig();
		ServerConfigRegistry.register(Util.getServerConfig());

		// Register Nodes
		NodeRegistry.registerDefaultNodes();

		// Register rules
		RuleRegistry.registerDefault();

		// Register events
		MinecraftForge.EVENT_BUS.register(this);

		// Keep all client class references behind the physical-side check.
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> FModClient::initialize);

		// Finish initialization
		Util.LOGGER.info("FMinecraftMod: Server side initialized successfully.");
	}

	@SubscribeEvent
	public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandRegistrater.registerCommand(event);
    }

	@SubscribeEvent
	public void onLevelLoad(LevelEvent.Load event) {
		LevelAccessor level = event.getLevel();
		if (level instanceof ServerLevel) {
			ServerLevel world = (ServerLevel) level;
			GameEventHandler.worldLoadEventDispatcher(world.getServer(), world);
		}
	}

	@SubscribeEvent
	public void onWorldTick(ServerTickEvent event) {
		if (event.phase != ServerTickEvent.Phase.END) {
			return;
		}
        GameEventHandler.serverTickEventDispatcher(event.getServer());
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			GameEventHandler.playerJoinEventDispatcher(player);
		}
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();

		if (!entity.isRemoved() && !entity.isAlwaysTicking()) {
			GameEventHandler.entityDeathEventDispatcher(entity, event.getSource());
		}

		if (entity instanceof ServerPlayer player && entity.isAlwaysTicking()) {
			if (!player.isRemoved()) {
				GameEventHandler.playerDeathEventDispatcher(player, event.getSource());
			}
		}
	}

	@SubscribeEvent
	public void onProjectileImpact(ProjectileImpactEvent event) {
		if (event.getRayTraceResult() instanceof EntityHitResult ehr) {
			Projectile projectile = event.getProjectile();
			GameEventHandler.projectileHitEntityEventDispatcher(projectile, ehr);
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			GameEventHandler.playerLeaveEventDispatcher(player);
		}
	}

	@SubscribeEvent
	public void onPlayerInteractBlock(PlayerInteractEvent.RightClickBlock event) {
		GameEventHandler.playerInteractBlockEventDispatcher(event.getEntity(), event.getHand(), event.getHitVec());
	}

	@SubscribeEvent
	public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
		GameEventHandler.playerInteractEntityEventDispatcher(event.getEntity(), event.getHand(), event.getTarget(), null);
	}

	@SubscribeEvent
	public void onPlayerInteractEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		GameEventHandler.playerInteractEntityEventDispatcher(event.getEntity(), event.getHand(), event.getTarget(), new EntityHitResult(event.getTarget(), event.getTarget().position().add(event.getLocalPos())));
	}

	@SubscribeEvent
	public void onPlayerItemUse(PlayerInteractEvent.RightClickItem event) {
		GameEventHandler.playerItemUseEventDispatcher(event.getEntity(), event.getHand());
	}

	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			GameEventHandler.playerDimensionChangeEventDispatcher(player, player.getServer().getLevel(event.getFrom()), player.getServer().getLevel(event.getTo()));
		}
	}

	@SubscribeEvent
	public void onEntityLoad(EntityJoinLevelEvent event) {
		if (event.getLevel() instanceof ServerLevel world) {
			GameEventHandler.entityLoadEventDispatcher(event.getEntity(), world);
		}
	}

	@SubscribeEvent
	public void onChunkLoad(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel world && event.getChunk() instanceof LevelChunk chunk) {
			GameEventHandler.chunkLoadEventDispatcher(world, chunk);
		}
	}

	@SubscribeEvent
	public void onChunkUnload(ChunkEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel world && event.getChunk() instanceof LevelChunk chunk) {
			GameEventHandler.chunkUnloadEventDispatcher(world, chunk);
		}
	}
}
