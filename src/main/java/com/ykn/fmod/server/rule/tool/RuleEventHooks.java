/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ykn.fmod.server.base.data.PlayerData;
import com.ykn.fmod.server.base.data.ServerData;
import com.ykn.fmod.server.base.util.Util;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Captures the rule-side variables of a game event and returns the task that dispatches them.
 *
 * <p>The split matters: every value, including the hidden {@code __...__} aliases that carry live
 * game objects, is read <em>before</em> any rule runs, so one rule cannot change what the next one
 * or the flow engine sees.
 *
 * <p>Both halves of an event (this class and
 * {@code com.ykn.fmod.server.flow.tool.FlowEventHooks}) are called before either task executes; see
 * {@code com.ykn.fmod.server.base.event.GameEventHandler}.
 *
 * <p>The events that predate the catalog capture their variables explicitly, because their
 * variable lists are frozen and must keep matching
 * {@code com.ykn.fmod.server.rule.event.RuleEvents}.
 */
public final class RuleEventHooks {

    private RuleEventHooks() {
    }

    public static Runnable playerJoin(ServerPlayer player) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("player", player.getUUID());
        variables.put("x", player.getX());
        variables.put("y", player.getY());
        variables.put("z", player.getZ());
        variables.put("pitch", Double.valueOf(player.getXRot()));
        variables.put("yaw", Double.valueOf(player.getYRot()));
        variables.put("position", player.position());
        variables.put("rotation", player.getRotationVector());
        variables.put("health", Double.valueOf(player.getHealth()));
        variables.put("dimension", player.level().dimension().location());
        variables.put("biome", biome(player));
        variables.put("name", player.getDisplayName().getString());
        variables.put("cansleep", canSleep(player));
        variables.put("__player__", player);
        variables.put("__world__", player.level());
        variables.put("__name__", player.getDisplayName());
        return prepare("PlayerJoinEvent", player.getServer(), variables);
    }

    public static Runnable entityDamage(LivingEntity entity, DamageSource damageSource, float amount) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("entity", entity.getUUID());
        variables.put("damage", Double.valueOf(amount));
        variables.put("cause", identity(damageSource.getEntity()));
        variables.put("source", identity(damageSource.getDirectEntity()));
        variables.put("x", entity.getX());
        variables.put("y", entity.getY());
        variables.put("z", entity.getZ());
        variables.put("position", entity.position());
        variables.put("dimension", entity.level().dimension().location());
        variables.put("biome", biome(entity));
        variables.put("pitch", Double.valueOf(entity.getXRot()));
        variables.put("yaw", Double.valueOf(entity.getYRot()));
        variables.put("rotation", entity.getRotationVector());
        variables.put("message", damageSource.getMsgId());
        variables.put("exhaustion", Double.valueOf(damageSource.getFoodExhaustion()));
        variables.put("health", Double.valueOf(entity.getHealth()));
        variables.put("name", entity.getDisplayName().getString());
        variables.put("__entity__", entity);
        variables.put("__cause__", damageSource.getEntity());
        variables.put("__source__", damageSource.getDirectEntity());
        variables.put("__world__", entity.level());
        variables.put("__name__", entity.getDisplayName());
        return prepare("EntityDamageEvent", entity.getServer(), variables);
    }

    public static Runnable entityDeath(LivingEntity entity, DamageSource damageSource) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("entity", entity.getUUID());
        variables.put("cause", identity(damageSource.getEntity()));
        variables.put("source", identity(damageSource.getDirectEntity()));
        variables.put("x", entity.getX());
        variables.put("y", entity.getY());
        variables.put("z", entity.getZ());
        variables.put("position", entity.position());
        variables.put("dimension", entity.level().dimension().location());
        variables.put("biome", biome(entity));
        variables.put("message", entity.getCombatTracker().getDeathMessage().getString());
        variables.put("name", entity.getDisplayName().getString());
        variables.put("__entity__", entity);
        variables.put("__cause__", damageSource.getEntity());
        variables.put("__source__", damageSource.getDirectEntity());
        variables.put("__world__", entity.level());
        variables.put("__message__", entity.getCombatTracker().getDeathMessage());
        variables.put("__name__", entity.getDisplayName());
        return prepare("EntityDeathEvent", entity.getServer(), variables);
    }

    public static Runnable projectileHitEntity(Projectile projectile, Entity shooter, EntityHitResult hit, double distance) {
        Entity victim = hit.getEntity();
        Map<String, Object> variables = new HashMap<>();
        variables.put("entity", victim.getUUID());
        variables.put("shooter", shooter.getUUID());
        variables.put("projectile", projectile.getUUID());
        variables.put("x", victim.getX());
        variables.put("y", victim.getY());
        variables.put("z", victim.getZ());
        variables.put("position", victim.position());
        variables.put("dimension", victim.level().dimension().location());
        variables.put("biome", biome(victim));
        variables.put("pitch", Double.valueOf(victim.getXRot()));
        variables.put("yaw", Double.valueOf(victim.getYRot()));
        variables.put("rotation", victim.getRotationVector());
        variables.put("sx", shooter.getX());
        variables.put("sy", shooter.getY());
        variables.put("sz", shooter.getZ());
        variables.put("sposition", shooter.position());
        variables.put("sdimension", shooter.level().dimension().location());
        variables.put("sbiome", biome(shooter));
        variables.put("spitch", Double.valueOf(shooter.getXRot()));
        variables.put("syaw", Double.valueOf(shooter.getYRot()));
        variables.put("srotation", shooter.getRotationVector());
        variables.put("distance", distance);
        variables.put("health", Util.getHealth(victim));
        variables.put("name", victim.getDisplayName().getString());
        variables.put("sname", shooter.getDisplayName().getString());
        variables.put("__entity__", victim);
        variables.put("__shooter__", shooter);
        variables.put("__projectile__", projectile);
        variables.put("__world__", victim.level());
        variables.put("__sworld__", shooter.level());
        variables.put("__name__", victim.getDisplayName());
        variables.put("__sname__", shooter.getDisplayName());
        return prepare("ProjectileHitEntityEvent", projectile.getServer(), variables);
    }

    public static Runnable serverTick(MinecraftServer server) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tick", Util.getServerData(server).getServerTick());
        return prepare("ServerTickEvent", server, variables);
    }

    public static Runnable playerTick(ServerPlayer player) {
        ServerData data = Util.getServerData(player.getServer());
        PlayerData playerData = data.getPlayerData(player);
        Map<String, Object> variables = new HashMap<>();
        variables.put("tick", data.getServerTick());
        variables.put("player", player.getUUID());
        variables.put("x", player.getX());
        variables.put("y", player.getY());
        variables.put("z", player.getZ());
        variables.put("position", player.position());
        variables.put("dimension", player.level().dimension().location());
        variables.put("biome", biome(player));
        variables.put("pitch", Double.valueOf(player.getXRot()));
        variables.put("yaw", Double.valueOf(player.getYRot()));
        variables.put("rotation", player.getRotationVector());
        variables.put("health", Double.valueOf(player.getHealth()));
        variables.put("name", player.getDisplayName().getString());
        variables.put("afk", playerData.getAfkTicks());
        variables.put("lastpitch", Double.valueOf(playerData.getLastPitch()));
        variables.put("lastyaw", Double.valueOf(playerData.getLastYaw()));
        variables.put("lastrotation", new Vec2(playerData.getLastPitch(), playerData.getLastYaw()));
        variables.put("lastbiome", playerData.getLastBiomeId());
        variables.put("lastdimension", playerData.getLastDimensionId());
        variables.put("cansleep", playerData.getLastCanSleep());
        variables.put("__player__", player);
        variables.put("__world__", player.level());
        variables.put("__name__", player.getDisplayName());
        return prepare("PlayerTickEvent", player.getServer(), variables);
    }

    // Player events declared through RuleEvents.playerEvent: they all share the player block.
    public static Runnable playerBreakBlock(ServerPlayer player, BlockPos position, BlockState state, BlockEntity blockEntity) {
        Map<String, Object> variables = player(player);
        block(variables, position, state, blockEntity);
        return prepare("PlayerBreakBlockEvent", player.serverLevel(), variables);
    }

    public static Runnable playerPlaceBlock(ServerPlayer player, BlockPos position, ItemStack stack) {
        Map<String, Object> variables = player(player);
        block(variables, position, player.level().getBlockState(position), player.level().getBlockEntity(position));
        item(variables, stack);
        variables.put("hand", hand(player, stack).name());
        return prepare("PlayerPlaceBlockEvent", player.serverLevel(), variables);
    }

    public static Runnable playerInteractBlock(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        BlockPos position = hit.getBlockPos();
        Map<String, Object> variables = player(player);
        block(variables, position, player.level().getBlockState(position), player.level().getBlockEntity(position));
        item(variables, player.getItemInHand(hand));
        variables.put("hand", hand.name());
        variables.put("face", hit.getDirection().getName());
        variables.put("hitposition", hit.getLocation());
        return prepare("PlayerInteractBlockEvent", player.serverLevel(), variables);
    }

    public static Runnable playerInteractEntity(ServerPlayer player, InteractionHand hand, Entity target, EntityHitResult hit) {
        Map<String, Object> variables = player(player);
        item(variables, player.getItemInHand(hand));
        variables.put("entity", target.getUUID());
        variables.put("ex", target.getX());
        variables.put("ey", target.getY());
        variables.put("ez", target.getZ());
        variables.put("eposition", target.position());
        variables.put("ebiome", biome(target));
        variables.put("ename", target.getDisplayName().getString());
        variables.put("__entity__", target);
        variables.put("__ename__", target.getDisplayName());
        variables.put("target", target.getUUID());
        variables.put("targettype", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        variables.put("hand", hand.name());
        variables.put("hitposition", hit == null ? null : hit.getLocation());
        variables.put("__target__", target);
        return prepare("PlayerInteractEntityEvent", player.serverLevel(), variables);
    }

    public static Runnable playerLeave(ServerPlayer player) {
        return prepare("PlayerLeaveEvent", player.serverLevel(), player(player));
    }

    public static Runnable playerAfk(ServerPlayer player, int ticks) {
        Map<String, Object> variables = player(player);
        variables.put("afk", Long.valueOf(ticks));
        return prepare("PlayerAfkEvent", player.serverLevel(), variables);
    }

    public static Runnable playerReturn(ServerPlayer player, int ticks) {
        Map<String, Object> variables = player(player);
        variables.put("afk", Long.valueOf(ticks));
        return prepare("PlayerReturnEvent", player.serverLevel(), variables);
    }

    public static Runnable playerDimensionChange(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
        Map<String, Object> variables = player(player);
        variables.put("fromdimension", origin.dimension().location());
        variables.put("todimension", destination.dimension().location());
        return prepare("PlayerDimensionChangeEvent", player.serverLevel(), variables);
    }

    public static Runnable playerTeleport(ServerPlayer player, ResourceLocation origin, Vec3 from, Vec3 to) {
        Map<String, Object> variables = player(player);
        variables.put("fromdimension", origin);
        variables.put("fromposition", from);
        variables.put("toposition", to);
        variables.put("fromx", from.x);
        variables.put("fromy", from.y);
        variables.put("fromz", from.z);
        variables.put("tox", to.x);
        variables.put("toy", to.y);
        variables.put("toz", to.z);
        return prepare("PlayerTeleportEvent", player.serverLevel(), variables);
    }

    public static Runnable playerItemUse(ServerPlayer player, InteractionHand hand) {
        Map<String, Object> variables = player(player);
        item(variables, player.getItemInHand(hand));
        variables.put("hand", hand.name());
        return prepare("PlayerItemUseEvent", player.serverLevel(), variables);
    }

    public static Runnable playerItemPickup(ServerPlayer player, ItemStack stack, ItemEntity itemEntity) {
        Map<String, Object> variables = player(player);
        item(variables, stack);
        variables.put("itementity", itemEntity.getUUID());
        variables.put("__itementity__", itemEntity);
        return prepare("PlayerItemPickupEvent", player.serverLevel(), variables);
    }

    public static Runnable playerCraft(ServerPlayer player, ItemStack stack) {
        Map<String, Object> variables = player(player);
        item(variables, stack);
        return prepare("PlayerCraftEvent", player.serverLevel(), variables);
    }

    public static Runnable playerEnchant(ServerPlayer player, ItemStack stack, int cost) {
        Map<String, Object> variables = player(player);
        item(variables, stack);
        variables.put("cost", cost);
        return prepare("PlayerEnchantEvent", player.serverLevel(), variables);
    }

    public static Runnable playerTrade(ServerPlayer player, Merchant merchant, ItemStack stack, CompoundTag offer) {
        Map<String, Object> variables = player(player);
        item(variables, stack);
        Entity merchantEntity = merchant instanceof Entity entity ? entity : null;
        variables.put("merchant", merchantEntity == null ? null : merchantEntity.getUUID());
        variables.put("offer", offer.copy());
        variables.put("__merchant__", merchantEntity);
        return prepare("PlayerTradeEvent", player.serverLevel(), variables);
    }

    public static Runnable playerSleep(ServerPlayer player, BlockPos bed) {
        Map<String, Object> variables = player(player);
        variables.put("bedposition", Vec3.atLowerCornerOf(bed));
        variables.put("tx", (double) bed.getX());
        variables.put("ty", (double) bed.getY());
        variables.put("tz", (double) bed.getZ());
        return prepare("PlayerSleepEvent", player.serverLevel(), variables);
    }

    public static Runnable playerVehicleEnter(ServerPlayer player, Entity vehicle) {
        Map<String, Object> variables = player(player);
        variables.put("vehicle", vehicle.getUUID());
        variables.put("vehicletype", BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()));
        variables.put("__vehicle__", vehicle);
        return prepare("PlayerVehicleEnterEvent", player.serverLevel(), variables);
    }

    public static Runnable playerVehicleLeave(ServerPlayer player, Entity vehicle) {
        Map<String, Object> variables = player(player);
        variables.put("vehicle", vehicle.getUUID());
        variables.put("vehicletype", BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()));
        variables.put("__vehicle__", vehicle);
        return prepare("PlayerVehicleLeaveEvent", player.serverLevel(), variables);
    }

    public static Runnable playerAdvancement(ServerPlayer player, Advancement advancement, String criterion) {
        Map<String, Object> variables = player(player);
        variables.put("advancement", advancement.getId());
        variables.put("criterion", criterion);
        return prepare("PlayerAdvancementEvent", player.serverLevel(), variables);
    }

    public static Runnable playerLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        Map<String, Object> variables = player(player);
        variables.put("oldlevel", oldLevel);
        variables.put("newlevel", newLevel);
        variables.put("delta", newLevel - oldLevel);
        return prepare("PlayerLevelUpEvent", player.serverLevel(), variables);
    }

    public static Runnable playerHungerChange(ServerPlayer player, int oldHunger, int newHunger) {
        Map<String, Object> variables = player(player);
        variables.put("oldhunger", oldHunger);
        variables.put("newhunger", newHunger);
        variables.put("saturation", Double.valueOf(player.getFoodData().getSaturationLevel()));
        return prepare("PlayerHungerChangeEvent", player.serverLevel(), variables);
    }

    public static Runnable containerOpen(ServerPlayer player) {
        return container("ContainerOpenEvent", player);
    }

    public static Runnable containerClose(ServerPlayer player) {
        return container("ContainerCloseEvent", player);
    }

    public static Runnable entitySpawn(Entity entity, ServerLevel world) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("entity", entity.getUUID());
        variables.put("entitytype", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        variables.put("position", entity.position());
        variables.put("x", entity.getX());
        variables.put("y", entity.getY());
        variables.put("z", entity.getZ());
        variables.put("rotation", entity.getRotationVector());
        variables.put("velocity", entity.getDeltaMovement());
        variables.put("__entity__", entity);
        return prepare("EntitySpawnEvent", world, variables);
    }

    public static Runnable lightningStrike(LightningBolt lightning, ServerLevel world) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("entity", lightning.getUUID());
        variables.put("position", lightning.position());
        variables.put("x", lightning.getX());
        variables.put("y", lightning.getY());
        variables.put("z", lightning.getZ());
        variables.put("cause", identity(lightning.getCause()));
        variables.put("__entity__", lightning);
        return prepare("LightningStrikeEvent", world, variables);
    }

    public static Runnable explosion(ServerLevel world, Explosion explosion, Vec3 position, float radius, boolean fire) {
        Entity source = explosion.getDirectSourceEntity();
        Map<String, Object> variables = new HashMap<>();
        variables.put("position", position);
        variables.put("x", position.x);
        variables.put("y", position.y);
        variables.put("z", position.z);
        variables.put("radius", Double.valueOf(radius));
        variables.put("fire", Boolean.valueOf(fire));
        variables.put("source", identity(source));
        variables.put("blocks", explosion.getToBlow().stream().map(Vec3::atLowerCornerOf).toList());
        variables.put("__source__", source);
        variables.put("__explosion__", explosion);
        return prepare("ExplosionEvent", world, variables);
    }

    public static Runnable weatherChange(ServerLevel world, boolean raining, boolean thundering, boolean oldRaining, boolean oldThundering) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("raining", Boolean.valueOf(raining));
        variables.put("thundering", Boolean.valueOf(thundering));
        variables.put("oldraining", Boolean.valueOf(oldRaining));
        variables.put("oldthundering", Boolean.valueOf(oldThundering));
        return prepare("WeatherChangeEvent", world, variables);
    }

    public static Runnable chunkLoad(ServerLevel world, LevelChunk chunk) {
        return chunk("ChunkLoadEvent", world, chunk);
    }

    public static Runnable chunkUnload(ServerLevel world, LevelChunk chunk) {
        return chunk("ChunkUnloadEvent", world, chunk);
    }

    /**
     * Fills the block of variables every player event declares.
     *
     * @param player the player the event is about
     * @return a mutable variable map holding the player block and its hidden aliases
     */
    private static Map<String, Object> player(ServerPlayer player) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("player", player.getUUID());
        variables.put("name", player.getDisplayName().getString());
        variables.put("position", player.position());
        variables.put("rotation", player.getRotationVector());
        variables.put("velocity", player.getDeltaMovement());
        variables.put("health", Double.valueOf(player.getHealth()));
        variables.put("hunger", player.getFoodData().getFoodLevel());
        variables.put("level", player.experienceLevel);
        variables.put("x", player.getX());
        variables.put("y", player.getY());
        variables.put("z", player.getZ());
        variables.put("pitch", Double.valueOf(player.getXRot()));
        variables.put("yaw", Double.valueOf(player.getYRot()));
        variables.put("biome", biome(player));
        variables.put("cansleep", canSleep(player));
        variables.put("__player__", player);
        variables.put("__name__", player.getDisplayName().copy());
        variables.put("__world__", player.level());
        return variables;
    }

    private static Runnable chunk(String type, ServerLevel world, LevelChunk chunk) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("chunkx", chunk.getPos().x);
        variables.put("chunkz", chunk.getPos().z);
        variables.put("position", new Vec3(chunk.getPos().getMinBlockX(), world.getMinBuildHeight(), chunk.getPos().getMinBlockZ()));
        variables.put("__chunk__", chunk);
        return prepare(type, world, variables);
    }

    private static Runnable container(String type, ServerPlayer player) {
        // The player's own inventory menu is open almost all the time, so it is not an event.
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu) {
            return () -> {
            };
        }
        MenuType<?> menuType = menu.getType();
        Map<String, Object> variables = player(player);
        variables.put("containerid", menu.containerId);
        variables.put("containertype", menuType == null ? null : BuiltInRegistries.MENU.getKey(menuType));
        variables.put("__container__", menu);
        // Most container menus are backed by a block entity; when one is, the rule also gets the
        // block it was opened on. Crafting-style menus have none, so those fields stay absent.
        BlockEntity source = containerBlockEntity(menu);
        if (source != null) {
            BlockPos pos = source.getBlockPos();
            variables.put("block", BuiltInRegistries.BLOCK.getKey(source.getBlockState().getBlock()));
            variables.put("blockposition", Vec3.atLowerCornerOf(pos));
            variables.put("bx", (double) pos.getX());
            variables.put("by", (double) pos.getY());
            variables.put("bz", (double) pos.getZ());
        }
        return prepare(type, player.serverLevel(), variables);
    }

    private static BlockEntity containerBlockEntity(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof BlockEntity blockEntity) {
                return blockEntity;
            }
        }
        return null;
    }
    private static void item(Map<String, Object> variables, ItemStack stack) {
        variables.put("item", BuiltInRegistries.ITEM.getKey(stack.getItem()));
        variables.put("count", stack.getCount());
        variables.put("itemnbt", stack.hasTag() ? stack.getTag().copy() : new CompoundTag());
        variables.put("__item__", stack.copy());
    }

    private static void block(Map<String, Object> variables, BlockPos position, BlockState state, BlockEntity blockEntity) {
        variables.put("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        variables.put("blockposition", Vec3.atLowerCornerOf(position));
        variables.put("bx", (double) position.getX());
        variables.put("by", (double) position.getY());
        variables.put("bz", (double) position.getZ());
        variables.put("blocknbt", blockEntity == null ? new CompoundTag() : blockEntity.saveWithFullMetadata());
        variables.put("__blockpos__", position.immutable());
        variables.put("__blockstate__", state);
    }

    private static InteractionHand hand(ServerPlayer player, ItemStack stack) {
        return stack == player.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static UUID identity(Entity entity) {
        return entity == null ? null : entity.getUUID();
    }

    private static ResourceLocation biome(Entity entity) {
        return entity.level().getBiome(entity.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
    }

    private static Boolean canSleep(ServerPlayer player) {
        return player.level().dimensionType().natural() && !player.level().isDay() && player.level().dimensionType().bedWorks();
    }

    private static Runnable prepare(String type, ServerLevel world, Map<String, Object> values) {
        return prepare(type, world.getServer(), values, world);
    }

    private static Runnable prepare(String type, MinecraftServer server, Map<String, Object> values) {
        return prepare(type, server, values, null);
    }

    private static Runnable prepare(String type, MinecraftServer server, Map<String, Object> values, ServerLevel world) {
        if (server == null) {
            return () -> {
            };
        }
        ServerData data = Util.getServerData(server);
        Map<String, Object> snapshot = new HashMap<>(values);
        snapshot.put("tick", data.getServerTick());
        if (world != null) {
            snapshot.put("dimension", world.dimension().location());
            snapshot.put("__world__", world);
        }
        List<RuleManager> rules = new ArrayList<>(data.gatherRuleByEventType(type, true));
        return () -> {
            for (RuleManager rule : rules) {
                if (!rule.isEnabled()) {
                    continue;
                }
                rule.trigger(data, new HashMap<>(snapshot));
            }
        };
    }
}
