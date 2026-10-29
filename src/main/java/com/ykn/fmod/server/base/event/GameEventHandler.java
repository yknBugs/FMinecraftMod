/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.event;

import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowEventHooks;
import com.ykn.fmod.server.rule.tool.RuleEventHooks;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Entry point of the game event system, and the only place where the loader layer meets both the
 * existing feature code and the two engines.
 *
 * <p>Each method first runs the pre-existing feature of that moment (the death and damage
 * messages in this package, the per-tick player handling in {@link WorldTick}), and then asks the
 * rule side and the flow side to capture their data from the reference arguments and runs the two
 * dispatch tasks. The feature classes never call back into this class; the direction is always
 * loader hook -&gt; this class -&gt; feature / rules / flows.
 *
 * <p>No method builds rule variables or flow outputs itself, and none reads configuration - so a
 * Forge or NeoForge port only has to redo the registrations in {@code FMod} and the mixins in
 * {@code com.ykn.fmod.server.mixin}, not this class.
 *
 * <p>Both capture calls complete before either task runs, so a rule that mutates the world cannot
 * change what the flow engine sees for the same event.
 *
 * <p>Nothing here throws beyond what the features themselves did before: the rule engine catches a
 * failing rule inside {@code RuleContext.trigger}, and the flow engine inside
 * {@code ExecutionContext.execute}. The remaining throw sites are guards in the two capture classes
 * and are listed in {@code docs/migration/event-throws.md}.
 */
public final class GameEventHandler {

    /**
     * How deep nested dispatches may go before the event is dropped. Events can nest (a rule action
     * places a block, which is itself an event), and a ruleset that feeds itself would otherwise
     * recurse without a bound.
     */
    private static final int MAX_DEPTH = 32;

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private GameEventHandler() {
    }

    private static void dispatch(Runnable rules, Runnable flows) {
        int depth = DEPTH.get();
        if (depth >= MAX_DEPTH) {
            Util.LOGGER.warn("FMinecraftMod: Nested event dispatch reached the depth limit of " + MAX_DEPTH + ", dropping this event.");
            return;
        }
        DEPTH.set(depth + 1);
        try {
            rules.run();
            flows.run();
        } finally {
            DEPTH.set(depth);
        }
    }

    public static void playerJoinEventDispatcher(ServerPlayer player) {
        dispatch(RuleEventHooks.playerJoin(player), FlowEventHooks.playerJoin(player));
    }

    public static void playerLeaveEventDispatcher(ServerPlayer player) {
        dispatch(RuleEventHooks.playerLeave(player), FlowEventHooks.playerLeave(player));
    }

    public static void playerBreakBlockEventDispatcher(ServerPlayer player, BlockPos position, BlockState state, BlockEntity blockEntity) {
        dispatch(RuleEventHooks.playerBreakBlock(player, position, state, blockEntity),
                FlowEventHooks.playerBreakBlock(player, position, state, blockEntity));
    }

    public static void playerBreakBlockEventDispatcher(Level world, Player player, BlockPos position, BlockState state, BlockEntity blockEntity) {
        if (player instanceof ServerPlayer serverPlayer) {
            playerBreakBlockEventDispatcher(serverPlayer, position, state, blockEntity);
        }
    }

    public static void playerPlaceBlockEventDispatcher(ServerPlayer player, BlockPos position, ItemStack stack) {
        dispatch(RuleEventHooks.playerPlaceBlock(player, position, stack), FlowEventHooks.playerPlaceBlock(player, position, stack));
    }

    public static InteractionResult playerInteractBlockEventDispatcher(Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            dispatch(RuleEventHooks.playerInteractBlock(serverPlayer, hand, hit), FlowEventHooks.playerInteractBlock(serverPlayer, hand, hit));
        }
        return InteractionResult.PASS;
    }

    public static InteractionResult playerInteractEntityEventDispatcher(Player player, InteractionHand hand, Entity target, EntityHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            dispatch(RuleEventHooks.playerInteractEntity(serverPlayer, hand, target, hit), FlowEventHooks.playerInteractEntity(serverPlayer, hand, target, hit));
        }
        return InteractionResult.PASS;
    }

    public static InteractionResultHolder<ItemStack> playerItemUseEventDispatcher(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            dispatch(RuleEventHooks.playerItemUse(serverPlayer, hand), FlowEventHooks.playerItemUse(serverPlayer, hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    public static void playerDimensionChangeEventDispatcher(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        dispatch(RuleEventHooks.playerDimensionChange(player, origin, destination), FlowEventHooks.playerDimensionChange(player, origin, destination));
    }

    public static void playerTeleportEventDispatcher(ServerPlayer player, ResourceLocation origin, Vec3 from, Vec3 to) {
        dispatch(RuleEventHooks.playerTeleport(player, origin, from, to), FlowEventHooks.playerTeleport(player, origin, from, to));
    }

    public static void playerAfkEventDispatcher(ServerPlayer player, int ticks) {
        dispatch(RuleEventHooks.playerAfk(player, ticks), FlowEventHooks.playerAfk(player, ticks));
    }

    public static void playerReturnEventDispatcher(ServerPlayer player, int ticks) {
        dispatch(RuleEventHooks.playerReturn(player, ticks), FlowEventHooks.playerReturn(player, ticks));
    }

    public static void playerItemPickupEventDispatcher(ServerPlayer player, ItemStack stack, ItemEntity itemEntity) {
        dispatch(RuleEventHooks.playerItemPickup(player, stack, itemEntity), FlowEventHooks.playerItemPickup(player, stack, itemEntity));
    }

    public static void playerCraftEventDispatcher(ServerPlayer player, ItemStack stack) {
        dispatch(RuleEventHooks.playerCraft(player, stack), FlowEventHooks.playerCraft(player, stack));
    }

    public static void playerEnchantEventDispatcher(ServerPlayer player, ItemStack stack, int cost) {
        dispatch(RuleEventHooks.playerEnchant(player, stack, cost), FlowEventHooks.playerEnchant(player, stack, cost));
    }

    public static void playerTradeEventDispatcher(ServerPlayer player, Merchant merchant, ItemStack stack, CompoundTag offer) {
        dispatch(RuleEventHooks.playerTrade(player, merchant, stack, offer), FlowEventHooks.playerTrade(player, merchant, stack, offer));
    }

    public static void playerSleepEventDispatcher(ServerPlayer player, BlockPos bed) {
        dispatch(RuleEventHooks.playerSleep(player, bed), FlowEventHooks.playerSleep(player, bed));
    }

    public static void playerSleepEventDispatcher(LivingEntity entity, BlockPos bed) {
        if (entity instanceof ServerPlayer player) {
            playerSleepEventDispatcher(player, bed);
        }
    }

    public static void playerVehicleEnterEventDispatcher(ServerPlayer player, Entity vehicle) {
        dispatch(RuleEventHooks.playerVehicleEnter(player, vehicle), FlowEventHooks.playerVehicleEnter(player, vehicle));
    }

    public static void playerVehicleLeaveEventDispatcher(ServerPlayer player, Entity vehicle) {
        dispatch(RuleEventHooks.playerVehicleLeave(player, vehicle), FlowEventHooks.playerVehicleLeave(player, vehicle));
    }

    public static void playerAdvancementEventDispatcher(ServerPlayer player, Advancement advancement, String criterion) {
        dispatch(RuleEventHooks.playerAdvancement(player, advancement, criterion), FlowEventHooks.playerAdvancement(player, advancement, criterion));
    }

    public static void playerLevelUpEventDispatcher(ServerPlayer player, int oldLevel, int newLevel) {
        dispatch(RuleEventHooks.playerLevelUp(player, oldLevel, newLevel), FlowEventHooks.playerLevelUp(player, oldLevel, newLevel));
    }

    public static void playerHungerChangeEventDispatcher(ServerPlayer player, int oldHunger, int newHunger) {
        dispatch(RuleEventHooks.playerHungerChange(player, oldHunger, newHunger), FlowEventHooks.playerHungerChange(player, oldHunger, newHunger));
    }

    public static void containerOpenEventDispatcher(ServerPlayer player) {
        dispatch(RuleEventHooks.containerOpen(player), FlowEventHooks.containerOpen(player));
    }

    public static void containerCloseEventDispatcher(ServerPlayer player) {
        dispatch(RuleEventHooks.containerClose(player), FlowEventHooks.containerClose(player));
    }

    public static void entityDamageEventDispatcher(LivingEntity entity, DamageSource damageSource, float amount) {
        if (!LivingEntityDamage.onDamage(entity, damageSource, amount)) {
            return;
        }
        dispatch(RuleEventHooks.entityDamage(entity, damageSource, amount), FlowEventHooks.entityDamage(entity, damageSource, amount));
    }

    public static void entityDeathEventDispatcher(LivingEntity entity, DamageSource damageSource) {
        if (!EntityDeath.onEntityDeath(entity, damageSource)) {
            return;
        }
        dispatch(RuleEventHooks.entityDeath(entity, damageSource), FlowEventHooks.entityDeath(entity, damageSource));
    }

    public static void playerDeathEventDispatcher(ServerPlayer player, DamageSource damageSource) {
        if (!PlayerDeath.onPlayerDeath(player, damageSource)) {
            return;
        }
        // Players are excluded from the entity death hook, so their death feeds the same death
        // event from here.
        dispatch(RuleEventHooks.entityDeath(player, damageSource), FlowEventHooks.entityDeath(player, damageSource));
    }

    public static void projectileHitEntityEventDispatcher(Projectile projectile, EntityHitResult hit) {
        ProjectileHitEntity.Hit report = ProjectileHitEntity.onProjectileHit(projectile, hit);
        if (report == null) {
            return;
        }
        dispatch(RuleEventHooks.projectileHitEntity(projectile, report.shooter(), hit, report.distance()),
                FlowEventHooks.projectileHitEntity(projectile, report.shooter(), hit, report.distance()));
    }

    public static void entitySpawnEventDispatcher(Entity entity, ServerLevel world) {
        dispatch(RuleEventHooks.entitySpawn(entity, world), FlowEventHooks.entitySpawn(entity, world));
    }

    public static void lightningStrikeEventDispatcher(LightningBolt lightning, ServerLevel world) {
        dispatch(RuleEventHooks.lightningStrike(lightning, world), FlowEventHooks.lightningStrike(lightning, world));
    }

    public static void explosionEventDispatcher(ServerLevel world, Explosion explosion, Vec3 position, float radius, boolean fire) {
        dispatch(RuleEventHooks.explosion(world, explosion, position, radius, fire), FlowEventHooks.explosion(world, explosion, position, radius, fire));
    }

    public static void weatherChangeEventDispatcher(ServerLevel world, boolean raining, boolean thundering, boolean oldRaining, boolean oldThundering) {
        dispatch(RuleEventHooks.weatherChange(world, raining, thundering, oldRaining, oldThundering),
                FlowEventHooks.weatherChange(world, raining, thundering, oldRaining, oldThundering));
    }

    public static void chunkLoadEventDispatcher(ServerLevel world, LevelChunk chunk) {
        dispatch(RuleEventHooks.chunkLoad(world, chunk), FlowEventHooks.chunkLoad(world, chunk));
    }

    public static void chunkUnloadEventDispatcher(ServerLevel world, LevelChunk chunk) {
        dispatch(RuleEventHooks.chunkUnload(world, chunk), FlowEventHooks.chunkUnload(world, chunk));
    }

    /**
     * Dispatches the two events that share the entity-load hook: an entity appearing in a world,
     * and a lightning bolt being created.
     */
    public static void entityLoadEventDispatcher(Entity entity, ServerLevel world) {
        entitySpawnEventDispatcher(entity, world);
        if (entity instanceof LightningBolt lightning) {
            lightningStrikeEventDispatcher(lightning, world);
        }
    }

    /**
     * Runs the world-load feature and dispatches the tick events.
     */
    public static void worldLoadEventDispatcher(MinecraftServer server, ServerLevel world) {
        NewLevel.onNewLevel(server, world);
    }

    /**
     * Runs one server tick: the existing per-tick features, then the events they produced.
     *
     * <p>{@link WorldTick#runFeatures} reports the transitions it detected (AFK on/off, teleport,
     * level and hunger change) instead of dispatching them, so every event still goes through this
     * class, and the events see the state as it is after the feature pass.
     */
    public static void serverTickEventDispatcher(MinecraftServer server) {
        List<WorldTick.PlayerTickReport> reports = WorldTick.runFeatures(server);
        dispatch(RuleEventHooks.serverTick(server), FlowEventHooks.serverTick(server));
        for (WorldTick.PlayerTickReport report : reports) {
            if (report.returnedFromAfk()) {
                playerReturnEventDispatcher(report.player(), report.afk().ticks());
            }
            if (report.enteredAfk()) {
                playerAfkEventDispatcher(report.player(), report.afk().ticks());
            }
            if (report.teleport() != null) {
                playerTeleportEventDispatcher(report.player(), report.teleport().origin(), report.teleport().from(), report.teleport().to());
            }
            if (report.leveledUp()) {
                playerLevelUpEventDispatcher(report.player(), report.oldLevel(), report.newLevel());
            }
            if (report.hungerChanged()) {
                playerHungerChangeEventDispatcher(report.player(), report.oldHunger(), report.newHunger());
            }
            dispatch(RuleEventHooks.playerTick(report.player()), FlowEventHooks.playerTick(report.player()));
        }
        Util.getServerData(server).tick();
    }
}
