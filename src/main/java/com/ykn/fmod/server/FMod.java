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
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.config.ServerConfigRegistry;
import com.ykn.fmod.server.base.event.EntityDeath;
import com.ykn.fmod.server.base.event.LivingEntityDamage;
import com.ykn.fmod.server.base.event.NewLevel;
import com.ykn.fmod.server.base.event.PlayerDeath;
import com.ykn.fmod.server.base.event.ProjectileHitEntity;
import com.ykn.fmod.server.base.event.WorldTick;
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
		NeoForge.EVENT_BUS.register(this);

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
			NewLevel newLevel = new NewLevel(world.getServer(), world);
			newLevel.onNewLevel();
		}
	}

	@SubscribeEvent
	public void onWorldTick(ServerTickEvent.Post event) {
        WorldTick.onWorldTick(event.getServer());
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();

		if (!entity.isRemoved() && !entity.isAlwaysTicking()) {
			EntityDeath entityDeath = new EntityDeath(entity, event.getSource());
			entityDeath.onEntityDeath();
		}

		if (entity instanceof ServerPlayer player && entity.isAlwaysTicking()) {
			if (!player.isRemoved()) {
				PlayerDeath playerDeath = new PlayerDeath(player, event.getSource());
				playerDeath.onPlayerDeath();
			}
		}
	}

	@SubscribeEvent
	public void onLivingHurt(LivingDamageEvent.Pre event) {
		LivingEntity entity = event.getEntity();

		if (!entity.isRemoved()) {
			LivingEntityDamage livingEntityDamage = new LivingEntityDamage(entity, event.getSource(), event.getNewDamage());
			livingEntityDamage.onDamage();
		}
	}

	@SubscribeEvent
	public void onProjectileImpact(ProjectileImpactEvent event) {
		if (event.getRayTraceResult() instanceof EntityHitResult ehr) {
			Projectile projectile = event.getProjectile();
			ProjectileHitEntity projectileHitEntity = new ProjectileHitEntity(projectile, ehr);
			projectileHitEntity.onProjectileHitEntity();
		}	
	}
}
