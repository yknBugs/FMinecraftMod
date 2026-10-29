/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.action;

import java.util.Set;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowManager;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.RuleGameAccess;
import com.ykn.fmod.server.rule.tool.RuleManager;
import com.ykn.fmod.server.rule.tool.RuleNbtGame;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Actions that change entities, blocks, or the server's own configuration.
 *
 * <p>Every type validates its input before touching the world and returns {@code false} when the
 * change did not happen; the shared wrapper turns that into a warning and continues, so one action
 * that cannot apply never cancels the rest of the rule.
 */
public final class GameActions {

    private GameActions() {
    }

    /**
     * Registers every game action type.
     */
    public static void register() {
        Components.action("GiveItemToPlayer", Params.of("fmod.rule.action.giveitemtoplayer")
                .param("entity", ParamKind.PLAYER)
                .param("item", ParamKind.RESOURCE_ID)
                .param("count", ParamKind.INT)
                .param("components", ParamKind.ITEM_COMPONENTS),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (!(target instanceof ServerPlayer player)) {
                        return false;
                    }
                    ItemStack stack = RuleNbtGame.item(call, call.resourceId(1), call.integer(2),
                            call.components(3));
                    if (stack == null) {
                        return false;
                    }
                    player.getInventory().add(stack);
                    player.containerMenu.broadcastChanges();
                    if (!stack.isEmpty()) {
                        ItemEntity dropped = player.drop(stack, false);
                        if (dropped == null) {
                            return false;
                        }
                        dropped.setNoPickUpDelay();
                        dropped.setTarget(player.getUUID());
                    }
                    return true;
                });

        Components.action("SetEntityMainHandItem",
                Params.of("fmod.rule.action.setentitymainhanditem")
                        .param("entity", ParamKind.ENTITY)
                        .param("item", ParamKind.RESOURCE_ID)
                        .param("count", ParamKind.INT)
                        .param("components", ParamKind.ITEM_COMPONENTS),
                (call) -> {
                    Entity target = RuleGameAccess.entity(call.context(), call.uuid(0));
                    if (!(target instanceof LivingEntity living)) {
                        return false;
                    }
                    ItemStack stack = RuleNbtGame.item(call, call.resourceId(1), call.integer(2),
                            call.components(3));
                    if (stack == null) {
                        return false;
                    }
                    living.setItemSlot(EquipmentSlot.MAINHAND, stack);
                    if (living instanceof ServerPlayer player) {
                        player.containerMenu.broadcastChanges();
                    }
                    return true;
                });

        Components.action("SummonEntity", Params.of("fmod.rule.action.summonentity")
                .param("type", ParamKind.ENTITY_TYPE_ID)
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("nbt", ParamKind.NBT_COMPOUND),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(1));
                    Vec3 position = call.vector(2);
                    if (!RuleGameAccess.loaded(world, position)) {
                        return false;
                    }
                    CompoundTag data = RuleNbtGame.spawnData(call, call.resourceId(0), call.compound(3));
                    if (data == null) {
                        return false;
                    }
                    Entity summoned = EntityType.loadEntityRecursive(data, world, (entity) -> {
                        entity.moveTo(position.x, position.y, position.z, entity.getYRot(),
                                entity.getXRot());
                        return entity;
                    });
                    return summoned != null && world.tryAddFreshEntityWithPassengers(summoned);
                });

        Components.action("SetEntityPosition", Params.of("fmod.rule.action.setentityposition")
                .param("entity", ParamKind.ENTITY)
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    ServerLevel target = RuleGameAccess.level(call.context(), call.resourceId(1));
                    Vec3 position = call.vector(2);
                    if (entity == null || target == null || !RuleGameAccess.finite(position)
                            || !ServerLevel.isInSpawnableBounds(BlockPos.containing(position))) {
                        return false;
                    }
                    return entity.teleportTo(target, position.x, position.y, position.z, Set.of(),
                            entity.getYRot(), entity.getXRot());
                });

        Components.action("SetHealth", Params.of("fmod.rule.action.sethealth")
                .param("entity", ParamKind.ENTITY)
                .param("health", ParamKind.DOUBLE),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    double health = call.number(1);
                    if (!(entity instanceof LivingEntity living) || health < 0
                            || health > living.getMaxHealth()) {
                        return false;
                    }
                    living.setHealth((float) health);
                    return true;
                });

        Components.action("SetHunger", Params.of("fmod.rule.action.sethunger")
                .param("entity", ParamKind.PLAYER)
                .param("hunger", ParamKind.INT),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    int hunger = call.integer(1);
                    if (!(entity instanceof ServerPlayer player) || hunger < 0 || hunger > 20) {
                        return false;
                    }
                    player.getFoodData().setFoodLevel(hunger);
                    player.getFoodData().setSaturation(
                            Math.min(player.getFoodData().getSaturationLevel(), hunger));
                    return true;
                });

        Components.action("PlaceBlock", Params.of("fmod.rule.action.placeblock")
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .param("block", ParamKind.BLOCK_ID),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    Vec3 position = call.vector(1);
                    ResourceLocation block = call.resourceId(2);
                    if (!RuleGameAccess.loaded(world, position)
                            || !BuiltInRegistries.BLOCK.containsKey(block)) {
                        return false;
                    }
                    BlockState state = BuiltInRegistries.BLOCK.get(block).defaultBlockState();
                    BlockPos blockPos = BlockPos.containing(position);
                    return world.getBlockState(blockPos).equals(state)
                            || world.setBlock(blockPos, state, 3);
                });

        Components.action("ApplyEffect", Params.of("fmod.rule.action.applyeffect")
                .param("entity", ParamKind.ENTITY)
                .param("effect", ParamKind.RESOURCE_ID)
                .param("duration", ParamKind.INT)
                .param("amplifier", ParamKind.INT),
                (call) -> {
                    Entity entity = RuleGameAccess.entity(call.context(), call.uuid(0));
                    ResourceLocation id = call.resourceId(1);
                    int duration = call.integer(2);
                    int amplifier = call.integer(3);
                    if (!(entity instanceof LivingEntity living)
                            || !BuiltInRegistries.MOB_EFFECT.containsKey(id)
                            || duration < 1 || amplifier < 0 || amplifier > 255) {
                        return false;
                    }
                    return living.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.getHolder(id).orElseThrow(), duration, amplifier));
                });

        Components.action("SetWorldTime", Params.of("fmod.rule.action.setworldtime")
                .param("time", ParamKind.INT),
                (call) -> {
                    int time = call.integer(0);
                    if (time < 0) {
                        return false;
                    }
                    for (ServerLevel world : call.context().getServer().getAllLevels()) {
                        world.setDayTime(time);
                    }
                    return true;
                });

        Components.action("SetRuleEnabledStatus",
                Params.of("fmod.rule.action.setruleenabledstatus")
                        .param("rule", ParamKind.STRING)
                        .param("enabled", ParamKind.BOOLEAN),
                (call) -> {
                    RuleManager manager = Util.getServerData(call.context().getServer())
                            .getCustomRules().get(call.text(0));
                    if (manager == null) {
                        return false;
                    }
                    manager.setEnabled(call.bool(1));
                    return true;
                });

        Components.action("SetFlowEnabledStatus",
                Params.of("fmod.rule.action.setflowenabledstatus")
                        .param("flow", ParamKind.STRING)
                        .param("enabled", ParamKind.BOOLEAN),
                (call) -> {
                    FlowManager manager = Util.getServerData(call.context().getServer())
                            .getLogicFlows().get(call.text(0));
                    if (manager == null) {
                        return false;
                    }
                    manager.setEnabled(call.bool(1));
                    return true;
                });
    }
}
