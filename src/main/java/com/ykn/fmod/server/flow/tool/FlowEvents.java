/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * The catalog of flow event types.
 *
 * <p>Unlike rule events, a flow event exposes native game objects, so the flow author can feed them
 * straight into node ports without parsing them back out of a UUID. The variables themselves are
 * deliberately not shared with the rule system: the two engines have different consumption models
 * and are declared separately.
 *
 * <p>Every event is a flow start node named {@code <type>Node}; the output order of this catalog is
 * the port order of that node, so new outputs may only be appended.
 */
public final class FlowEvents {

    private FlowEvents() {
    }

    /**
     * One output port of the event node.
     *
     * @param name     the port name, also the suffix of its translation keys
     * @param type     the runtime type of the value, used to document the port
     * @param nullable whether the dispatcher may pass {@code null} for it
     */
    public record Output(String name, Class<?> type, boolean nullable) {
    }

    /**
     * One event type: its type id plus the ordered outputs of its start node.
     */
    public record Definition(String type, List<Output> outputs) {

        public Definition {
            outputs = List.copyOf(outputs);
        }

        /**
         * Returns the translation key prefix of this event.
         *
         * @return {@code fmod.flowevent.<type lowercased>}
         */
        public String key() {
            return "fmod.flowevent." + type.toLowerCase(Locale.ROOT);
        }

        /**
         * Returns the flow node type name this event is dispatched as.
         *
         * @return the node type, {@code <type>Node}
         */
        public String nodeType() {
            return type + "Node";
        }
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        define("PlayerBreakBlockEvent",
                output("player", ServerPlayer.class),
                output("position", Vec3.class),
                output("block", BlockState.class),
                optional("blockentity", BlockEntity.class),
                output("world", ServerLevel.class));
        define("PlayerPlaceBlockEvent",
                output("player", ServerPlayer.class),
                output("position", Vec3.class),
                output("block", BlockState.class),
                output("item", ItemStack.class),
                output("hand", InteractionHand.class),
                output("world", ServerLevel.class));
        define("PlayerInteractBlockEvent",
                output("player", ServerPlayer.class),
                output("hit", BlockHitResult.class),
                output("item", ItemStack.class),
                output("hand", InteractionHand.class),
                output("position", Vec3.class),
                output("world", ServerLevel.class));
        define("PlayerInteractEntityEvent",
                output("player", ServerPlayer.class),
                output("entity", Entity.class),
                output("item", ItemStack.class),
                output("hand", InteractionHand.class),
                optional("hit", EntityHitResult.class));
        define("PlayerLeaveEvent",
                output("player", ServerPlayer.class));
        define("PlayerAfkEvent",
                output("player", ServerPlayer.class),
                output("ticks", Integer.class));
        define("PlayerReturnEvent",
                output("player", ServerPlayer.class),
                output("ticks", Integer.class));
        define("PlayerDimensionChangeEvent",
                output("player", ServerPlayer.class),
                output("origin", ServerLevel.class),
                output("destination", ServerLevel.class));
        define("PlayerTeleportEvent",
                output("player", ServerPlayer.class),
                output("from", Vec3.class),
                output("to", Vec3.class),
                optional("origin", ServerLevel.class),
                output("destination", ServerLevel.class));
        define("PlayerItemUseEvent",
                output("player", ServerPlayer.class),
                output("item", ItemStack.class),
                output("hand", InteractionHand.class));
        define("PlayerItemPickupEvent",
                output("player", ServerPlayer.class),
                output("item", ItemStack.class),
                output("entity", ItemEntity.class));
        define("PlayerCraftEvent",
                output("player", ServerPlayer.class),
                output("item", ItemStack.class));
        define("PlayerEnchantEvent",
                output("player", ServerPlayer.class),
                output("item", ItemStack.class),
                output("cost", Integer.class));
        define("PlayerTradeEvent",
                output("player", ServerPlayer.class),
                output("merchant", Merchant.class),
                output("item", ItemStack.class),
                output("offer", MerchantOffer.class));
        define("PlayerSleepEvent",
                output("player", ServerPlayer.class),
                output("bed", Vec3.class));
        define("PlayerVehicleEnterEvent",
                output("player", ServerPlayer.class),
                output("vehicle", Entity.class));
        define("PlayerVehicleLeaveEvent",
                output("player", ServerPlayer.class),
                output("vehicle", Entity.class));
        define("PlayerAdvancementEvent",
                output("player", ServerPlayer.class),
                output("advancement", AdvancementHolder.class),
                output("criterion", String.class));
        define("PlayerLevelUpEvent",
                output("player", ServerPlayer.class),
                output("previous", Integer.class),
                output("current", Integer.class));
        define("PlayerHungerChangeEvent",
                output("player", ServerPlayer.class),
                output("previous", Integer.class),
                output("current", Integer.class),
                output("saturation", Double.class));
        define("ContainerOpenEvent",
                output("player", ServerPlayer.class),
                output("container", AbstractContainerMenu.class),
                optional("position", Vec3.class),
                optional("block", BlockState.class),
                output("world", ServerLevel.class));
        define("ContainerCloseEvent",
                output("player", ServerPlayer.class),
                output("container", AbstractContainerMenu.class),
                optional("position", Vec3.class),
                optional("block", BlockState.class),
                output("world", ServerLevel.class));

        define("EntitySpawnEvent",
                output("entity", Entity.class),
                output("position", Vec3.class),
                output("world", ServerLevel.class));
        define("LightningStrikeEvent",
                output("lightning", LightningBolt.class),
                optional("cause", ServerPlayer.class),
                output("world", ServerLevel.class));
        define("ExplosionEvent",
                output("explosion", Explosion.class),
                output("position", Vec3.class),
                output("radius", Float.class),
                output("fire", Boolean.class),
                output("world", ServerLevel.class),
                optional("source", Entity.class),
                output("blocks", List.class));
        define("WeatherChangeEvent",
                output("world", ServerLevel.class),
                output("raining", Boolean.class),
                output("thundering", Boolean.class),
                output("oldraining", Boolean.class),
                output("oldthundering", Boolean.class));
        define("ChunkLoadEvent",
                output("chunk", LevelChunk.class),
                output("chunkx", Double.class),
                output("chunkz", Double.class),
                output("world", ServerLevel.class));
        define("ChunkUnloadEvent",
                output("chunk", LevelChunk.class),
                output("chunkx", Double.class),
                output("chunkz", Double.class),
                output("world", ServerLevel.class));

        // Events that predate the catalog; their port order is frozen.
        define("PlayerJoinEvent",
                output("player", ServerPlayer.class));
        define("EntityDamageEvent",
                output("entity", LivingEntity.class),
                output("amount", Double.class),
                output("damagetype", DamageType.class),
                optional("cause", Entity.class),
                optional("direct", Entity.class),
                optional("position", Vec3.class),
                output("source", DamageSource.class));
        define("EntityDeathEvent",
                output("entity", LivingEntity.class),
                output("damagetype", DamageType.class),
                optional("cause", Entity.class),
                optional("direct", Entity.class),
                optional("position", Vec3.class),
                output("message", Component.class),
                output("source", DamageSource.class));
        define("ProjectileHitEntityEvent",
                output("projectile", Projectile.class),
                output("shooter", Entity.class),
                output("entity", Entity.class),
                output("hit", EntityHitResult.class),
                output("distance", Double.class));
        define("ServerTickEvent",
                output("server", MinecraftServer.class),
                output("tick", Integer.class));
        define("PlayerTickEvent",
                output("player", ServerPlayer.class),
                output("tick", Integer.class));
    }

    private static Output output(String name, Class<?> type) {
        return new Output(name, type, false);
    }

    private static Output optional(String name, Class<?> type) {
        return new Output(name, type, true);
    }

    private static void define(String type, Output... outputs) {
        DEFINITIONS.put(type, new Definition(type, Arrays.asList(outputs)));
    }

    /**
     * Returns the declaration registered under {@code type}.
     *
     * @param type the event type, without the {@code Node} suffix
     * @return the matching declaration
     * @throws NullPointerException if no event is registered under that type
     */
    public static Definition get(String type) {
        return Objects.requireNonNull(DEFINITIONS.get(type), type);
    }

    /**
     * Returns every declaration, in registration order.
     *
     * @return an unmodifiable view of all declarations
     */
    public static List<Definition> all() {
        return Collections.unmodifiableList(new ArrayList<>(DEFINITIONS.values()));
    }
}
