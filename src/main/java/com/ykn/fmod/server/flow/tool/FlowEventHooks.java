/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Captures the flow-side outputs of a game event and returns the task that dispatches them.
 *
 * <p>Flow events hand over the live game objects, in the port order declared by
 * {@link FlowEvents}. They are deliberately separate from the rule-side variables: a flow does not
 * see UUIDs and a rule does not see native objects.
 *
 * <p>The capture happens before any flow runs; see
 * {@code com.ykn.fmod.server.base.event.GameEventHandler}.
 */
public final class FlowEventHooks {

    private FlowEventHooks() {
    }

    public static Runnable playerBreakBlock(ServerPlayer player, BlockPos position, BlockState state, BlockEntity blockEntity) {
        return prepare("PlayerBreakBlockEvent", player.getServer(), player, Vec3.atLowerCornerOf(position), state, blockEntity, player.serverLevel());
    }

    public static Runnable playerPlaceBlock(ServerPlayer player, BlockPos position, ItemStack stack) {
        InteractionHand hand = stack == player.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return prepare("PlayerPlaceBlockEvent", player.getServer(), player, Vec3.atLowerCornerOf(position), player.level().getBlockState(position), stack.copy(), hand, player.serverLevel());
    }

    public static Runnable playerInteractBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        return prepare("PlayerInteractBlockEvent", player.getServer(), player, hit, player.getItemInHand(hand).copy(), hand, Vec3.atLowerCornerOf(hit.getBlockPos()), player.serverLevel());
    }

    public static Runnable playerInteractEntity(ServerPlayer player, InteractionHand hand, Entity target, EntityHitResult hit) {
        return prepare("PlayerInteractEntityEvent", player.getServer(), player, target, player.getItemInHand(hand).copy(), hand, hit);
    }

    public static Runnable playerLeave(ServerPlayer player) {
        return prepare("PlayerLeaveEvent", player.getServer(), player);
    }

    public static Runnable playerAfk(ServerPlayer player, int ticks) {
        return prepare("PlayerAfkEvent", player.getServer(), player, ticks);
    }

    public static Runnable playerReturn(ServerPlayer player, int ticks) {
        return prepare("PlayerReturnEvent", player.getServer(), player, ticks);
    }

    public static Runnable playerDimensionChange(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        return prepare("PlayerDimensionChangeEvent", player.getServer(), player, origin, destination);
    }

    public static Runnable playerTeleport(ServerPlayer player, ResourceLocation origin, Vec3 from, Vec3 to) {
        MinecraftServer server = player.getServer();
        ServerLevel originLevel = origin == null || server == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, origin));
        return prepare("PlayerTeleportEvent", server, player, from, to, originLevel, player.serverLevel());
    }

    public static Runnable playerItemUse(ServerPlayer player, InteractionHand hand) {
        return prepare("PlayerItemUseEvent", player.getServer(), player, player.getItemInHand(hand).copy(), hand);
    }

    public static Runnable playerItemPickup(ServerPlayer player, ItemStack stack, ItemEntity itemEntity) {
        return prepare("PlayerItemPickupEvent", player.getServer(), player, stack.copy(), itemEntity);
    }

    public static Runnable playerCraft(ServerPlayer player, ItemStack stack) {
        return prepare("PlayerCraftEvent", player.getServer(), player, stack.copy());
    }

    public static Runnable playerEnchant(ServerPlayer player, ItemStack stack, int cost) {
        return prepare("PlayerEnchantEvent", player.getServer(), player, stack.copy(), cost);
    }

    public static Runnable playerTrade(ServerPlayer player, Merchant merchant, ItemStack stack, CompoundTag offer) {
        return prepare("PlayerTradeEvent", player.getServer(), player, merchant, stack.copy(), new MerchantOffer(offer.copy()));
    }

    public static Runnable playerSleep(ServerPlayer player, BlockPos bed) {
        return prepare("PlayerSleepEvent", player.getServer(), player, Vec3.atLowerCornerOf(bed));
    }

    public static Runnable playerVehicleEnter(ServerPlayer player, Entity vehicle) {
        return prepare("PlayerVehicleEnterEvent", player.getServer(), player, vehicle);
    }

    public static Runnable playerVehicleLeave(ServerPlayer player, Entity vehicle) {
        return prepare("PlayerVehicleLeaveEvent", player.getServer(), player, vehicle);
    }

    public static Runnable playerAdvancement(ServerPlayer player, Advancement advancement, String criterion) {
        return prepare("PlayerAdvancementEvent", player.getServer(), player, advancement, criterion);
    }

    public static Runnable playerLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        return prepare("PlayerLevelUpEvent", player.getServer(), player, oldLevel, newLevel);
    }

    public static Runnable playerHungerChange(ServerPlayer player, int oldHunger, int newHunger) {
        return prepare("PlayerHungerChangeEvent", player.getServer(), player, oldHunger, newHunger, Double.valueOf(player.getFoodData().getSaturationLevel()));
    }

    public static Runnable containerOpen(ServerPlayer player) {
        return container("ContainerOpenEvent", player);
    }

    public static Runnable containerClose(ServerPlayer player) {
        return container("ContainerCloseEvent", player);
    }

    public static Runnable entitySpawn(Entity entity, ServerLevel world) {
        return prepare("EntitySpawnEvent", world.getServer(), entity, entity.position(), world);
    }

    public static Runnable lightningStrike(LightningBolt lightning, ServerLevel world) {
        return prepare("LightningStrikeEvent", world.getServer(), lightning, lightning.getCause(), world);
    }

    public static Runnable explosion(ServerLevel world, Explosion explosion, Vec3 position, float radius, boolean fire) {
        return prepare("ExplosionEvent", world.getServer(), explosion, position, Float.valueOf(radius), Boolean.valueOf(fire),
                world, explosion.getDirectSourceEntity(), explosion.getToBlow().stream().map(BlockPos::immutable).toList());
    }

    public static Runnable weatherChange(ServerLevel world, boolean raining, boolean thundering, boolean oldRaining, boolean oldThundering) {
        return prepare("WeatherChangeEvent", world.getServer(), world,
                Boolean.valueOf(raining), Boolean.valueOf(thundering), Boolean.valueOf(oldRaining), Boolean.valueOf(oldThundering));
    }

    public static Runnable chunkLoad(ServerLevel world, LevelChunk chunk) {
        return prepare("ChunkLoadEvent", world.getServer(), chunk, chunkCentreX(chunk), chunkCentreZ(chunk), world);
    }

    public static Runnable chunkUnload(ServerLevel world, LevelChunk chunk) {
        return prepare("ChunkUnloadEvent", world.getServer(), chunk, chunkCentreX(chunk), chunkCentreZ(chunk), world);
    }

    public static Runnable playerJoin(ServerPlayer player) {
        return prepare("PlayerJoinEvent", player.getServer(), player);
    }

    public static Runnable entityDamage(LivingEntity entity, DamageSource damageSource, float amount) {
        return prepare("EntityDamageEvent", entity.getServer(), entity, Double.valueOf(amount), damageSource.type(),
                damageSource.getEntity(), damageSource.getDirectEntity(), damageSource.getSourcePosition(), damageSource);
    }

    public static Runnable entityDeath(LivingEntity entity, DamageSource damageSource) {
        Component message = entity.getCombatTracker().getDeathMessage().copy();
        return prepare("EntityDeathEvent", entity.getServer(), entity, damageSource.type(),
                damageSource.getEntity(), damageSource.getDirectEntity(), damageSource.getSourcePosition(), message, damageSource);
    }

    public static Runnable projectileHitEntity(Projectile projectile, Entity shooter, EntityHitResult hit, double distance) {
        return prepare("ProjectileHitEntityEvent", projectile.getServer(), projectile, shooter, hit.getEntity(), hit, Double.valueOf(distance));
    }

    public static Runnable serverTick(MinecraftServer server) {
        return prepare("ServerTickEvent", server, server, Util.getServerData(server).getServerTick());
    }

    public static Runnable playerTick(ServerPlayer player) {
        return prepare("PlayerTickEvent", player.getServer(), player, Util.getServerData(player.getServer()).getServerTick());
    }

    private static Runnable container(String type, ServerPlayer player) {
        // The player's own inventory menu is open almost all the time, so it is not an event.
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu) {
            return () -> {
            };
        }
        // Block containers carry their block entity on the first slot, which is where the block
        // coordinates and the block type for the event come from; crafting-style menus have none.
        BlockEntity source = containerBlockEntity(menu);
        Vec3 position = source == null ? null : Vec3.atLowerCornerOf(source.getBlockPos());
        BlockState block = source == null ? null : source.getBlockState();
        return prepare(type, player.getServer(), player, menu, position, block, player.serverLevel());
    }

    private static double chunkCentreX(LevelChunk chunk) {
        return chunk.getPos().getMinBlockX() + 8.0;
    }

    private static double chunkCentreZ(LevelChunk chunk) {
        return chunk.getPos().getMinBlockZ() + 8.0;
    }

    private static BlockEntity containerBlockEntity(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof BlockEntity blockEntity) {
                return blockEntity;
            }
        }
        return null;
    }
    private static Runnable prepare(String type, MinecraftServer server, Object... values) {
        if (server == null) {
            return () -> {
            };
        }
        FlowEvents.Definition event = FlowEvents.get(type);
        List<FlowEvents.Output> ports = event.outputs();
        if (values.length != ports.size()) {
            Util.LOGGER.error("FMinecraftMod: Flow event " + type + " declares " + ports.size() + " outputs but the dispatcher supplied " + values.length + ".");
            return () -> {
            };
        }
        ServerData data = Util.getServerData(server);
        List<Object> outputs = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(values)));
        List<FlowManager> flows = new ArrayList<>(data.gatherFlowByFirstNodeType(event.nodeType(), true));
        return () -> {
            for (FlowManager flow : flows) {
                if (!flow.isEnabled()) {
                    continue;
                }
                flow.execute(data, new ArrayList<>(outputs), null);
            }
        };
    }
}
