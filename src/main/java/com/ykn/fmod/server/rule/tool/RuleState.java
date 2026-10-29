/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

/**
 * Shared server-thread state access and validation.
 *
 * <p>Queries never create scoreboard entries and never mutate world state; only the explicit
 * {@code setCount} / {@code writeScore} helpers write, and both validate first.
 *
 * <p>Two helpers add counts with {@link Math#addExact}, so a pathological score or stack count
 * raises {@link ArithmeticException} instead of wrapping around. Callers treat that as a failed
 * component: it is reported and the component evaluates to {@code false}, exactly like the draft
 * branch's recoverable-arithmetic path.
 */
public final class RuleState {

    private RuleState() {
    }

    /**
     * Reads one inventory slot.
     *
     * @param inventory the player inventory
     * @param slot      0-35 storage and hotbar, 36-39 armor, 40 offhand
     * @return the stack, or {@code null} for an invalid slot index
     */
    public static ItemStack slot(Inventory inventory, int slot) {
        return slot >= 0 && slot < inventory.getContainerSize() ? inventory.getItem(slot) : null;
    }

    /**
     * Counts matching items across storage, armor, and offhand.
     *
     * @param inventory the player inventory
     * @param id        the item id
     * @return the total count, or {@code null} for an unknown or air item
     * @throws ArithmeticException when the total exceeds {@link Integer#MAX_VALUE}
     */
    public static Integer itemCount(Inventory inventory, ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (item.isEmpty()) {
            return null;
        }
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item.getItem())) {
                count = Math.addExact(count, stack.getCount());
            }
        }
        return count;
    }

    /**
     * Reads an existing score without creating one.
     *
     * @param board     the scoreboard
     * @param objective the objective name
     * @param holder    the holder name
     * @return the score, or {@code null} when the objective or the holder's entry is missing
     */
    public static Integer score(Scoreboard board, String objective, String holder) {
        Objective target = board.getObjective(objective);
        if (target == null) {
            return null;
        }
        ReadOnlyScoreInfo score = board.getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), target);
        return score == null ? null : score.value();
    }

    /**
     * Writes a writable objective.
     *
     * @param board     the scoreboard
     * @param objective the objective name
     * @param holder    the holder name
     * @param value     the value to set, or the amount to add
     * @param add       {@code true} to add to the current score, {@code false} to replace it
     * @return {@code true} when the score was written
     * @throws ArithmeticException when an addition overflows {@code int}
     */
    public static boolean writeScore(Scoreboard board, String objective, String holder, int value,
            boolean add) {
        Objective target = board.getObjective(objective);
        if (target == null || target.getCriteria().isReadOnly() || holder.isEmpty()) {
            return false;
        }
        Integer previous = score(board, objective, holder);
        int result = add ? Math.addExact(previous == null ? 0 : previous, value) : value;
        board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(holder), target).set(result);
        return true;
    }

    /**
     * Reads one block-state property as its canonical string.
     *
     * @param state the block state
     * @param name  the property name
     * @return the property value, or {@code null} when the block has no such property
     */
    public static String property(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? null : propertyValue(state, property);
    }

    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /**
     * Sets one existing stack's count, clearing the slot when {@code count} is zero.
     *
     * @param inventory the player inventory
     * @param index     the slot index
     * @param count     the new count, at least zero and at most the stack's maximum
     * @return {@code true} when the inventory changed
     */
    public static boolean setCount(Inventory inventory, int index, int count) {
        ItemStack stack = slot(inventory, index);
        if (stack == null || count < 0
                || (count > 0 && (stack.isEmpty() || count > stack.getMaxStackSize()))) {
            return false;
        }
        ItemStack replacement = stack.copy();
        replacement.setCount(count);
        inventory.setItem(index, count == 0 ? ItemStack.EMPTY : replacement);
        inventory.setChanged();
        return true;
    }

    /**
     * Syncs a player's inventory after server-side modifications.
     *
     * @param player the player whose inventory changed
     */
    public static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    /**
     * Validates a rotation, which is {@code Vec2(pitch, yaw)} in degrees.
     *
     * @param rotation the rotation to check
     * @return {@code true} when both angles are finite and the pitch lies in {@code [-90, 90]}
     */
    public static boolean validRotation(Vec2 rotation) {
        return Float.isFinite(rotation.x) && Float.isFinite(rotation.y)
                && rotation.x >= -90 && rotation.x <= 90;
    }
}
