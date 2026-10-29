/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.tool;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ComponentCall;

/**
 * Detached NBT preparation for item and entity actions. Inputs are never mutated.
 *
 * <p>Every method either returns a fresh, validated value or {@code null}; the two validation
 * failures that cannot be expressed as {@code null} - an unknown spawn type and malformed
 * passenger data - throw {@link IllegalArgumentException}, which the calling action catches and
 * reports as a normal warning.
 */
public final class RuleNbtGame {

    private RuleNbtGame() {
    }

    /**
     * Creates one legal-sized, non-empty item stack with a copied custom tag.
     *
     * @param id    the item id
     * @param count the stack size, at least one and at most the item's maximum
     * @param tag   the custom tag, copied
     * @return the stack, or {@code null} for an unknown item or an illegal count
     * @throws IllegalArgumentException when the tag is too deep
     */
    public static ItemStack item(ResourceLocation id, int count, CompoundTag tag) {
        RuleNbt.validateDepth(tag);
        if (!BuiltInRegistries.ITEM.containsKey(id) || count < 1) {
            return null;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (stack.isEmpty() || count > stack.getMaxStackSize()) {
            return null;
        }
        stack.setCount(count);
        stack.setTag(tag.copy());
        return stack;
    }

    /**
     * Copies spawn data, forces the explicit root type, and removes UUIDs from the whole passenger
     * tree so repeated summons get fresh identities.
     *
     * @param type the entity type id
     * @param tag  the spawn data, copied
     * @return spawn data ready for {@code EntityType.loadEntityRecursive}
     * @throws IllegalArgumentException for an unknown or non-summonable type, malformed passengers,
     *                                  or a tag that is too deep
     */
    public static CompoundTag spawnData(ResourceLocation type, CompoundTag tag) {
        RuleNbt.validateDepth(tag);
        CompoundTag copy = tag.copy();
        copy.putString("id", type.toString());
        prepareEntity(copy);
        return copy;
    }

    /**
     * Component-side variant of {@link #item(ResourceLocation, int, CompoundTag)} that reports a
     * validation failure as a warning instead of an exception.
     *
     * @param call  the current evaluation
     * @param id    the item id
     * @param count the stack size
     * @param tag   the custom tag, copied
     * @return the stack, or {@code null} when the input was rejected
     */
    public static ItemStack item(ComponentCall call, ResourceLocation id, int count, CompoundTag tag) {
        try {
            return item(id, count, tag);
        } catch (IllegalArgumentException exception) {
            call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
            return null;
        }
    }

    /**
     * Component-side variant of {@link #spawnData(ResourceLocation, CompoundTag)} that reports a
     * validation failure as a warning instead of an exception.
     *
     * @param call the current evaluation
     * @param type the entity type id
     * @param tag  the spawn data, copied
     * @return the spawn data, or {@code null} when the input was rejected
     */
    public static CompoundTag spawnData(ComponentCall call, ResourceLocation type, CompoundTag tag) {
        try {
            return spawnData(type, tag);
        } catch (IllegalArgumentException exception) {
            call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
            return null;
        }
    }

    private static void prepareEntity(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                || !BuiltInRegistries.ENTITY_TYPE.get(id).canSummon()) {
            throw new IllegalArgumentException("Unknown or non-summonable entity type");
        }
        tag.remove("UUID");
        if (tag.contains("Passengers")) {
            if (!(tag.get("Passengers") instanceof ListTag passengers)) {
                throw new IllegalArgumentException("Invalid passengers");
            }
            for (Tag passenger : passengers) {
                if (!(passenger instanceof CompoundTag compound)) {
                    throw new IllegalArgumentException("Invalid passenger");
                }
                prepareEntity(compound);
            }
        }
    }
}
