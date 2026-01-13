package com.ykn.fmod.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.ykn.fmod.server.base.command.CommandRegistrater;
import com.ykn.fmod.server.base.event.EntityDeath;
import com.ykn.fmod.server.base.event.LivingEntityDamage;
import com.ykn.fmod.server.base.event.NewLevel;
import com.ykn.fmod.server.base.event.PlayerDeath;
import com.ykn.fmod.server.base.event.ProjectileHitEntity;
import com.ykn.fmod.server.base.event.WorldTick;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.NodeRegistry;

@Mod(Util.MODID)
public class FMod {

    public static final Logger LOGGER = LogUtils.getLogger();

	public FMod() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		MinecraftForge.EVENT_BUS.register(this);

		// Register Nodes
		NodeRegistry.registerDefaultNodes();

		Util.loadServerConfig();
		LOGGER.info("FMinecraftMod: Server side initialized successfully.");
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
	public void onWorldTick(ServerTickEvent event) {
		if (event.phase != ServerTickEvent.Phase.END) {
			return;
		}
        WorldTick worldTick = new WorldTick(event.getServer());
        worldTick.onWorldTick();
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
	public void onLivingHurt(LivingHurtEvent event) {
		LivingEntity entity = event.getEntity();

		if (!entity.isRemoved()) {
			LivingEntityDamage livingEntityDamage = new LivingEntityDamage(entity, event.getSource(), event.getAmount());
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