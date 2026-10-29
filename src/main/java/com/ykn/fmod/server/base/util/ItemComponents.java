/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.base.util;

import java.util.ArrayList;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import com.ykn.fmod.server.rule.tool.RuleNbt;

/** Registry-aware conversion at the boundary between item components and NBT. */
public final class ItemComponents {

    private ItemComponents() {
    }

    /** A saved constant is decoded against the executing world's registries, not at file load. */
    public record Constant(String snbt) {
        public Constant {
            RuleNbt.compound(snbt);
        }

        @Override
        public String toString() {
            return snbt;
        }
    }

    public static CompoundTag encode(DataComponentPatch components, HolderLookup.Provider registries) {
        if (components == null) {
            return null;
        }
        return (CompoundTag) DataComponentPatch.CODEC.encodeStart(
                RegistryOps.create(NbtOps.INSTANCE, registries), components).getOrThrow(IllegalArgumentException::new);
    }

    public static DataComponentPatch decode(CompoundTag nbt, HolderLookup.Provider registries) {
        RuleNbt.validateDepth(nbt);
        return DataComponentPatch.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registries), nbt.copy())
                .getOrThrow(IllegalArgumentException::new);
    }

    public static DataComponentPatch copy(DataComponentPatch components, HolderLookup.Provider registries) {
        return decode(encode(components, registries), registries);
    }

    /** Replaces overrides and removals, retaining the live stack's item and count. */
    public static void apply(ItemStack stack, DataComponentPatch components, HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot apply components to an empty stack");
        }
        ItemStack replacement = new ItemStack(stack.getItemHolder(), stack.getCount(), copy(components, registries));
        ItemStack.validateComponents(replacement.getComponents()).getOrThrow(IllegalArgumentException::new);
        for (DataComponentType<?> type : new ArrayList<>(stack.getComponents().keySet())) {
            stack.remove(type);
        }
        stack.applyComponents(replacement.getComponents());
    }
}
