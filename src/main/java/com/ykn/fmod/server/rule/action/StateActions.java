/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ComponentCall;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.RuleGameAccess;
import com.ykn.fmod.server.rule.tool.RuleNbtGame;
import com.ykn.fmod.server.rule.tool.RuleState;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Actions that change a player's progression, inventory or score, or an entity's tags and motion.
 *
 * <p>Every type validates before writing and returns {@code false} when nothing changed; the shared
 * wrapper reports that and continues with the next action.
 */
public final class StateActions {

    private StateActions() {
    }

    /**
     * Registers every state action type.
     */
    public static void register() {
        Components.action("RemoveEntity", Params.of("fmod.rule.action.removeentity")
                .param("entity", ParamKind.ENTITY),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (target == null || target instanceof ServerPlayer) {
                        return false;
                    }
                    target.discard();
                    return true;
                });

        Components.action("AddTag", Params.of("fmod.rule.action.addtag")
                .param("entity", ParamKind.ENTITY)
                .param("tag", ParamKind.STRING),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    String tag = call.text(1);
                    return target != null && !tag.isEmpty()
                            && (target.getTags().contains(tag) || target.addTag(tag));
                });

        Components.action("RemoveTag", Params.of("fmod.rule.action.removetag")
                .param("entity", ParamKind.ENTITY)
                .param("tag", ParamKind.STRING),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    String tag = call.text(1);
                    return target != null && !tag.isEmpty()
                            && (!target.getTags().contains(tag) || target.removeTag(tag));
                });

        Components.action("SetXpLevel", Params.of("fmod.rule.action.setxplevel")
                .param("player", ParamKind.PLAYER)
                .param("amount", ParamKind.INT),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    int level = call.integer(1);
                    if (!(target instanceof ServerPlayer player) || level < 0
                            || level > Integer.MAX_VALUE / 9) {
                        return false;
                    }
                    player.setExperienceLevels(level);
                    return true;
                });

        Components.action("GiveXp", Params.of("fmod.rule.action.givexp")
                .param("player", ParamKind.PLAYER)
                .param("amount", ParamKind.INT),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    int amount = call.integer(1);
                    if (!(target instanceof ServerPlayer player) || amount < 0
                            || (long) player.totalExperience + amount > Integer.MAX_VALUE) {
                        return false;
                    }
                    player.giveExperiencePoints(amount);
                    return true;
                });

        Components.action("GiveXpLevel", Params.of("fmod.rule.action.givexplevel")
                .param("player", ParamKind.PLAYER)
                .param("amount", ParamKind.INT),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    int amount = call.integer(1);
                    if (!(target instanceof ServerPlayer player) || amount < 0
                            || (long) player.experienceLevel + amount > Integer.MAX_VALUE / 9) {
                        return false;
                    }
                    player.giveExperienceLevels(amount);
                    return true;
                });

        Components.action("ClearInventory", Params.of("fmod.rule.action.clearinventory")
                .param("player", ParamKind.PLAYER),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (!(target instanceof ServerPlayer player)) {
                        return false;
                    }
                    player.getInventory().clearContent();
                    RuleState.syncInventory(player);
                    return true;
                });

        Components.action("DropItem", Params.of("fmod.rule.action.dropitem")
                .param("player", ParamKind.PLAYER)
                .param("item", ParamKind.RESOURCE_ID)
                .param("count", ParamKind.INT),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (!(target instanceof ServerPlayer player)) {
                        return false;
                    }
                    ResourceLocation id = call.resourceId(1);
                    int remaining = call.integer(2);
                    Integer available;
                    try {
                        available = RuleState.itemCount(player.getInventory(), id);
                    } catch (ArithmeticException exception) {
                        call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
                        return false;
                    }
                    if (remaining < 1 || available == null || available < remaining) {
                        return false;
                    }
                    for (int index = 0; index < player.getInventory().getContainerSize()
                            && remaining > 0; index++) {
                        ItemStack stack = player.getInventory().getItem(index);
                        if (stack.isEmpty()
                                || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) {
                            continue;
                        }
                        int count = Math.min(remaining, stack.getCount());
                        // Spawn a copy before removing it, so a failed drop does not lose this slot.
                        ItemStack dropped = stack.copy();
                        dropped.setCount(count);
                        if (player.drop(dropped, false, true) == null) {
                            return false;
                        }
                        stack.shrink(count);
                        remaining -= count;
                        RuleState.syncInventory(player);
                    }
                    return remaining == 0;
                });

        Components.action("SetItemSlot", Params.of("fmod.rule.action.setitemslot")
                .param("player", ParamKind.PLAYER)
                .param("slot", ParamKind.INT)
                .param("item", ParamKind.RESOURCE_ID)
                .param("nbt", ParamKind.NBT_COMPOUND),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (!(target instanceof ServerPlayer player)) {
                        return false;
                    }
                    int index = call.integer(1);
                    if (RuleState.slot(player.getInventory(), index) == null) {
                        return false;
                    }
                    ItemStack stack = RuleNbtGame.item(call, call.resourceId(2), 1, call.compound(3));
                    if (stack == null) {
                        return false;
                    }
                    player.getInventory().setItem(index, stack);
                    RuleState.syncInventory(player);
                    return true;
                });

        Components.action("SetItemCount", Params.of("fmod.rule.action.setitemcount")
                .param("player", ParamKind.PLAYER)
                .param("slot", ParamKind.INT)
                .param("count", ParamKind.INT),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (!(target instanceof ServerPlayer player)
                            || !RuleState.setCount(player.getInventory(), call.integer(1),
                                    call.integer(2))) {
                        return false;
                    }
                    RuleState.syncInventory(player);
                    return true;
                });

        Components.action("SetScore", Params.of("fmod.rule.action.setscore")
                .param("objective", ParamKind.STRING)
                .param("holder", ParamKind.STRING)
                .param("score", ParamKind.INT),
                (call) -> writeScore(call, false));

        Components.action("AddScore", Params.of("fmod.rule.action.addscore")
                .param("objective", ParamKind.STRING)
                .param("holder", ParamKind.STRING)
                .param("score", ParamKind.INT),
                (call) -> writeScore(call, true));

        Components.action("SetDifficulty", Params.of("fmod.rule.action.setdifficulty")
                .param("difficulty", ParamKind.STRING),
                (call) -> {
                    Difficulty difficulty = Difficulty.byName(call.text(0));
                    if (difficulty == null
                            || (call.context().getServer().isHardcore()
                                    && difficulty != Difficulty.HARD)) {
                        return false;
                    }
                    call.context().getServer().setDifficulty(difficulty, true);
                    return true;
                });

        Components.action("SetVelocity", Params.of("fmod.rule.action.setvelocity")
                .param("entity", ParamKind.ENTITY)
                .param("velocity", ParamKind.VECTOR),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    Vec3 velocity = call.vector(1);
                    if (target == null || !RuleGameAccess.finite(velocity)) {
                        return false;
                    }
                    target.setDeltaMovement(velocity);
                    target.hasImpulse = true;
                    target.hurtMarked = true;
                    return true;
                });

        Components.action("SetRotation", Params.of("fmod.rule.action.setrotation")
                .param("entity", ParamKind.ENTITY)
                .param("rotation", ParamKind.ROTATION),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    Vec2 rotation = call.get(1, Vec2.class);
                    if (target == null || !RuleState.validRotation(rotation)) {
                        return false;
                    }
                    float yaw = Mth.wrapDegrees(rotation.y);
                    if (target instanceof ServerPlayer player) {
                        player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw,
                                rotation.x);
                    } else {
                        target.setYRot(yaw);
                        target.setXRot(rotation.x);
                    }
                    target.setYHeadRot(yaw);
                    target.setYBodyRot(yaw);
                    return true;
                });
    }

    /**
     * Writes or adds to a scoreboard score, reporting an overflow as a failed action.
     *
     * @param call the current evaluation
     * @param add  {@code true} to add to the current score, {@code false} to replace it
     * @return {@code true} when the score was written
     */
    private static boolean writeScore(ComponentCall call, boolean add) {
        try {
            return RuleState.writeScore(call.context().getServer().getScoreboard(),
                    call.text(0), call.text(1), call.integer(2), add);
        } catch (ArithmeticException exception) {
            call.fail(Util.parseTranslatableText("fmod.rule.warning.operationfailed"));
            return false;
        }
    }
}
