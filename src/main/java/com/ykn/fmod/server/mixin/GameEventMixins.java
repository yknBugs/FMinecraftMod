/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.mixin;

import java.util.OptionalInt;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.ykn.fmod.server.base.event.GameEventHandler;
import com.ykn.fmod.server.base.event.ProjectileHitEntity;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Mixin adapters for every game event, both the pre-existing ones and the ones Fabric API does not
 * expose.
 *
 * <p>Each nested class is a hook, not a feature: it reads the vanilla arguments (and, where a value
 * only exists mid-method, stashes it in a {@code @Unique} field) and forwards them to
 * {@link GameEventHandler}. Nothing here builds rule variables, executes flows, or decides whether
 * an event should fire - except where the vanilla hook serves several unrelated triggers and has to
 * say which one it is.
 *
 * <p>Register the nested classes in {@code fmod.server.mixins.json}; a new event needs no change to
 * {@code FMod}.
 *
 * <p>The four hooks that predate this class ({@link EntityDeath}, {@link LivingEntityDamage},
 * {@link PlayerDeath}, {@link ProjectileHitEntity}) keep the guard they had as standalone mixins:
 * they wrap the long-standing message code, so an unexpected exception there is logged instead of
 * escaping into vanilla. The hooks added for rule/flow events have no such guard - the features and
 * engines they call already handle their own failures.
 */
@SuppressWarnings("resource")
public final class GameEventMixins {

    private GameEventMixins() {
    }

    /**
     * Successful block removal, before the block's destroy callback and drops, like Fabric AFTER.
     */
    @Mixin(ServerPlayerGameMode.class)
    public static class BlockBreak {

        @Redirect(
            method = "removeBlock",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/block/state/BlockState;onDestroyedByPlayer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/level/material/FluidState;)Z",
                remap = false
            ),
            remap = false
        )
        private boolean fmod$blockRemoved(BlockState state, Level level, BlockPos pos, Player player, boolean canHarvest, FluidState fluid) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            boolean removed = state.onDestroyedByPlayer(level, pos, player, canHarvest, fluid);
            if (removed && player instanceof ServerPlayer serverPlayer) {
                GameEventHandler.playerBreakBlockEventDispatcher(serverPlayer, pos, state, blockEntity);
            }
            return removed;
        }
    }

    /**
     * Actual sleep start, after the sleeping position and pose are set, like Fabric START_SLEEPING.
     */
    @Mixin(LivingEntity.class)
    public static class Sleep {

        @Inject(method = "startSleeping(Lnet/minecraft/core/BlockPos;)V", at = @At("RETURN"))
        private void fmod$sleepStarted(BlockPos pos, CallbackInfo info) {
            GameEventHandler.playerSleepEventDispatcher((LivingEntity) (Object) this, pos);
        }
    }

    /**
     * Living entity death, excluding players (their death goes through {@link PlayerDeath}).
     */
    @Mixin(LivingEntity.class)
    public static class EntityDeath {

        @Inject(
            method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;getKillCredit()Lnet/minecraft/world/entity/LivingEntity;",
                shift = At.Shift.AFTER
            )
        )
        private void fmod$entityDeath(final DamageSource damageSource, CallbackInfo info) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (!entity.isRemoved() && !(entity instanceof Player)) {
                GameEventHandler.entityDeathEventDispatcher(entity, damageSource);
            }
        }
    }

    /**
     * Living entity damage, taken at the head of {@code hurt} so it sees the damage before it is
     * applied.
     */
    @Mixin(LivingEntity.class)
    public static class LivingEntityDamage {

        @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
        private void fmod$damaged(final DamageSource damageSource, final float amount, CallbackInfoReturnable<Boolean> info) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (!entity.isRemoved()) {
                GameEventHandler.entityDamageEventDispatcher(entity, damageSource, amount);
            }
        }
    }

    /**
     * Player death. Players are excluded from {@link EntityDeath}, so this hook feeds the same death
     * event through the player path.
     */
    @Mixin(ServerPlayer.class)
    public static class PlayerDeath {

        @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
        private void fmod$playerDeath(final DamageSource damageSource, CallbackInfo info) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (!player.isRemoved()) {
                GameEventHandler.playerDeathEventDispatcher(player, damageSource);
            }
        }
    }

    /**
     * Projectile hit, taken at the head of {@code onHitEntity} so the hit result is still available.
     */
    @Mixin(Projectile.class)
    public static class ProjectileHit {

        @Inject(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"))
        private void fmod$projectileHit(final EntityHitResult entityHitResult, CallbackInfo info) {
            Projectile projectile = (Projectile) (Object) this;
            GameEventHandler.projectileHitEntityEventDispatcher(projectile, entityHitResult);
        }
    }

    /**
     * Block placement, taken from the vanilla placed-block criterion because Fabric API has no
     * placement event.
     */
    @Mixin(ItemUsedOnLocationTrigger.class)
    public static class Placement {

        @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
        private void fmod$placedBlock(ServerPlayer player, BlockPos position, ItemStack stack, CallbackInfo info) {
            // This trigger class also serves the other "item used on block" criteria; only the
            // placed-block instance is the placement event.
            if ((Object) this != CriteriaTriggers.PLACED_BLOCK) {
                return;
            }
            GameEventHandler.playerPlaceBlockEventDispatcher(player, position, stack);
        }
    }

    /**
     * Item pickup. The picked-up count is the difference between the stack before and after the
     * touch, because {@code playerTouch} merges into the inventory instead of moving the stack.
     */
    @Mixin(ItemEntity.class)
    public static class Pickup {

        @Unique
        private ItemStack fmod$beforePickup;

        @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
        private void fmod$capturePickup(Player player, CallbackInfo info) {
            if (player instanceof ServerPlayer) {
                fmod$beforePickup = ((ItemEntity) (Object) this).getItem().copy();
            }
        }

        @Inject(method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"))
        private void fmod$pickedUp(Player player, CallbackInfo info) {
            ItemStack before = fmod$beforePickup;
            fmod$beforePickup = null;
            if (!(player instanceof ServerPlayer serverPlayer) || before == null) {
                return;
            }
            ItemEntity entity = (ItemEntity) (Object) this;
            int picked = before.getCount() - (entity.isRemoved() ? 0 : entity.getItem().getCount());
            if (picked <= 0) {
                return;
            }
            before.setCount(picked);
            GameEventHandler.playerItemPickupEventDispatcher(serverPlayer, before, entity);
        }
    }

    /**
     * Crafting, hooked on the craft bookkeeping of the result slot so it sees the produced stack and
     * its count before the slot is cleared.
     */
    @Mixin(ResultSlot.class)
    public static class Craft {

        @Redirect(method = "checkTakeAchievements", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onCraftedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;I)V"))
        private void fmod$crafted(ItemStack stack, Level level, Player player, int count) {
            ItemStack crafted = stack.copy();
            crafted.setCount(count);
            stack.onCraftedBy(level, player, count);
            if (player instanceof ServerPlayer serverPlayer && count > 0) {
                GameEventHandler.playerCraftEventDispatcher(serverPlayer, crafted);
            }
        }
    }

    @Mixin(EnchantedItemTrigger.class)
    public static class Enchant {

        @Inject(method = "trigger", at = @At("RETURN"))
        private void fmod$enchanted(ServerPlayer player, ItemStack stack, int cost, CallbackInfo info) {
            GameEventHandler.playerEnchantEventDispatcher(player, stack, cost);
        }
    }

    /**
     * Villager trading. {@code notifyTrade} is the point where the offer is still reachable, so the
     * result and the offer NBT are copied there and dispatched once {@code onTake} returns.
     */
    @Mixin(MerchantResultSlot.class)
    public static class Trade {

        @Shadow
        @Final
        private Player player;

        @Unique
        private Merchant fmod$merchant;

        @Unique
        private ItemStack fmod$tradeItem;

        @Unique
        private CompoundTag fmod$tradeOffer;

        @Inject(method = "onTake", at = @At("HEAD"))
        private void fmod$clearTrade(Player player, ItemStack stack, CallbackInfo info) {
            fmod$merchant = null;
            fmod$tradeItem = null;
            fmod$tradeOffer = null;
        }

        @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
        private void fmod$captureTrade(Merchant merchant, MerchantOffer offer) {
            merchant.notifyTrade(offer);
            if (this.player instanceof ServerPlayer) {
                fmod$merchant = merchant;
                fmod$tradeItem = offer.getResult().copy();
                RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, this.player.registryAccess());
                fmod$tradeOffer = (CompoundTag) MerchantOffer.CODEC.encodeStart(ops, offer).getOrThrow(IllegalArgumentException::new);
            }
        }

        @Inject(method = "onTake", at = @At("RETURN"))
        private void fmod$traded(Player player, ItemStack stack, CallbackInfo info) {
            Merchant merchant = fmod$merchant;
            ItemStack item = fmod$tradeItem;
            CompoundTag offer = fmod$tradeOffer;
            fmod$merchant = null;
            fmod$tradeItem = null;
            fmod$tradeOffer = null;
            if (merchant == null || item == null || offer == null || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            GameEventHandler.playerTradeEventDispatcher(serverPlayer, merchant, item, offer);
        }
    }

    /**
     * Container open and close. Fabric API has no menu event, and the horse inventory bypasses
     * {@code openMenu}, so both entry points are hooked.
     */
    @Mixin(ServerPlayer.class)
    public static class Menus {

        @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;", at = @At("RETURN"))
        private void fmod$openedMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> info) {
            if (info.getReturnValue().isPresent()) {
                GameEventHandler.containerOpenEventDispatcher((ServerPlayer) (Object) this);
            }
        }

        @Inject(method = "openHorseInventory", at = @At("RETURN"))
        private void fmod$openedHorseInventory(AbstractHorse horse, Container inventory, CallbackInfo info) {
            GameEventHandler.containerOpenEventDispatcher((ServerPlayer) (Object) this);
        }

        @Inject(method = "doCloseContainer", at = @At("HEAD"))
        private void fmod$closingContainer(CallbackInfo info) {
            GameEventHandler.containerCloseEventDispatcher((ServerPlayer) (Object) this);
        }
    }

    /**
     * Vehicle enter and leave. {@code removeVehicle} is called with the player still riding, so the
     * vehicle is captured on entry and reported after the call if the seat is really gone.
     */
    @Mixin(Entity.class)
    public static class Vehicles {

        @Unique
        private Entity fmod$leavingVehicle;

        @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("RETURN"))
        private void fmod$enteredVehicle(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> info) {
            if ((Object) this instanceof ServerPlayer player && Boolean.TRUE.equals(info.getReturnValue())) {
                GameEventHandler.playerVehicleEnterEventDispatcher(player, vehicle);
            }
        }

        @Inject(method = "removeVehicle", at = @At("HEAD"))
        private void fmod$captureLeavingVehicle(CallbackInfo info) {
            if ((Object) this instanceof ServerPlayer player) {
                fmod$leavingVehicle = player.getVehicle();
            }
        }

        @Inject(method = "removeVehicle", at = @At("RETURN"))
        private void fmod$leftVehicle(CallbackInfo info) {
            Entity old = fmod$leavingVehicle;
            fmod$leavingVehicle = null;
            if ((Object) this instanceof ServerPlayer player && old != null && player.getVehicle() != old) {
                GameEventHandler.playerVehicleLeaveEventDispatcher(player, old);
            }
        }
    }

    /**
     * Advancement completion, hooked where the rewards are granted so already-completed
     * advancements do not fire again.
     */
    @Mixin(PlayerAdvancements.class)
    public static class Advancements {

        @Shadow
        private ServerPlayer player;

        @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V", shift = At.Shift.AFTER))
        private void fmod$advancementAwarded(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> info) {
            GameEventHandler.playerAdvancementEventDispatcher(player, advancement, criterion);
        }
    }

    @Mixin(Explosion.class)
    public static class Explosions {

        @Shadow
        @Final
        private Level level;

        @Shadow
        @Final
        private double x;

        @Shadow
        @Final
        private double y;

        @Shadow
        @Final
        private double z;

        @Shadow
        @Final
        private float radius;

        @Shadow
        @Final
        private boolean fire;

        @Inject(method = "explode()V", at = @At("RETURN"))
        private void fmod$exploded(CallbackInfo info) {
            if (!(level instanceof ServerLevel world)) {
                return;
            }
            GameEventHandler.explosionEventDispatcher(world, (Explosion) (Object) this, new Vec3(x, y, z), radius, fire);
        }
    }

    /**
     * Weather changes. Vanilla has no weather event, so the previous state is kept per level and
     * compared after each weather cycle.
     */
    @Mixin(ServerLevel.class)
    public static class Weather {

        @Unique
        private boolean fmod$weatherKnown;

        @Unique
        private boolean fmod$raining;

        @Unique
        private boolean fmod$thundering;

        @Inject(method = "<init>", at = @At("RETURN"))
        private void fmod$initialWeather(CallbackInfo info) {
            ServerLevel world = (ServerLevel) (Object) this;
            fmod$raining = world.getLevelData().isRaining();
            fmod$thundering = world.getLevelData().isThundering();
            fmod$weatherKnown = true;
        }

        @Inject(method = "advanceWeatherCycle", at = @At("RETURN"))
        private void fmod$weatherChanged(CallbackInfo info) {
            ServerLevel world = (ServerLevel) (Object) this;
            boolean raining = world.getLevelData().isRaining();
            boolean thundering = world.getLevelData().isThundering();
            boolean oldRaining = fmod$raining;
            boolean oldThundering = fmod$thundering;
            fmod$raining = raining;
            fmod$thundering = thundering;
            if (!fmod$weatherKnown || (raining == oldRaining && thundering == oldThundering)) {
                return;
            }
            GameEventHandler.weatherChangeEventDispatcher(world, raining, thundering, oldRaining, oldThundering);
        }
    }
}
