/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import java.util.UUID;

import com.ykn.fmod.server.rule.core.RuleContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Server-thread lookups shared by rule conditions and actions. A missing target returns
 * {@code null} instead of throwing, so a component can report it as a normal warning.
 */
public final class RuleGameAccess {

    private RuleGameAccess() {
    }

    /**
     * Resolves a dimension id to the loaded level.
     *
     * @param context   the current rule context
     * @param dimension the dimension id, as resolved from a {@code ParamKind.DIMENSION} parameter
     * @return the level, or {@code null} when the id names no loaded dimension
     */
    public static ServerLevel level(RuleContext context, Object dimension) {
        return context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,
                (ResourceLocation) dimension));
    }

    /**
     * Finds an entity by UUID in any loaded level.
     *
     * @param context the current rule context
     * @param id      the entity UUID
     * @return the entity, or {@code null} when it is not loaded anywhere
     */
    public static Entity entity(RuleContext context, Object id) {
        for (ServerLevel level : context.getServer().getAllLevels()) {
            Entity entity = level.getEntity((UUID) id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Looks up an online player by username or UUID string.
     *
     * @param context  the current rule context
     * @param identity a player name, or the string form of a UUID
     * @return the player, or {@code null} when neither lookup matches
     */
    public static ServerPlayer player(RuleContext context, String identity) {
        ServerPlayer player = context.getServer().getPlayerList().getPlayerByName(identity);
        if (player != null) {
            return player;
        }
        try {
            return context.getServer().getPlayerList().getPlayer(UUID.fromString(identity));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Returns the borrowed main-hand stack of an entity.
     *
     * @param context the current rule context
     * @param id      the entity UUID
     * @return the stack, or {@link ItemStack#EMPTY} for an absent or non-living entity
     */
    public static ItemStack mainHand(RuleContext context, Object id) {
        Entity entity = entity(context, id);
        return entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
    }

    /**
     * Returns a detached copy of an entity's main-hand item tag.
     *
     * @param context the current rule context
     * @param id      the entity UUID
     * @return a copy of the tag, an empty compound for an untagged item, or {@code null} for an empty hand
     */
    public static CompoundTag mainHandNbt(RuleContext context, Object id) {
        ItemStack stack = mainHand(context, id);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
    }

    /**
     * Returns a detached vanilla entity-data snapshot.
     *
     * @param context the current rule context
     * @param id      the entity UUID
     * @return a copy of the entity data, or {@code null} for an absent entity
     */
    public static CompoundTag entityNbt(RuleContext context, Object id) {
        Entity entity = entity(context, id);
        return entity == null ? null : new EntityDataAccessor(entity).getData().copy();
    }

    /**
     * Whether a position is finite on all three axes.
     *
     * @param position the position to test
     * @return {@code true} when no component is {@code NaN} or infinite
     */
    public static boolean finite(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }

    /**
     * Checks world bounds and chunk presence without loading or generating a chunk.
     *
     * @param level    the level to test in
     * @param position the position to test
     * @return {@code true} when the position is inside loaded terrain
     */
    public static boolean loaded(ServerLevel level, Vec3 position) {
        if (level == null || !finite(position)) {
            return false;
        }
        BlockPos block = BlockPos.containing(position);
        return level.isInWorldBounds(block) && level.hasChunk(block.getX() >> 4, block.getZ() >> 4);
    }
}
