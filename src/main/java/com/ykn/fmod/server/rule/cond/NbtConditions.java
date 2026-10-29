/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.rule.cond;

import com.ykn.fmod.server.base.util.ItemComponents;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.rule.core.ComponentCall;
import com.ykn.fmod.server.rule.core.Components;
import com.ykn.fmod.server.rule.core.ParamKind;
import com.ykn.fmod.server.rule.core.Params;
import com.ykn.fmod.server.rule.tool.RuleGameAccess;
import com.ykn.fmod.server.rule.tool.RuleNbt;
import com.ykn.fmod.server.rule.tool.RuleState;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Data-flow conditions that read or write NBT and store the result in a variable.
 *
 * <p>Every type here shares one operational shape: resolve the subject, produce a tag, hand it to
 * the writer wrapper which stores it under the {@code variable} parameter. Nothing mutates world
 * data: {@link RuleNbt} works on copies.
 *
 * <p>A malformed path or an over-deep tree is user input, not a bug: it is reported through
 * {@code fmod.rule.warning.nbtget} / {@code nbtwrite} / {@code nbtdelete} and the component fails,
 * leaving the rest of the rule running.
 */
public final class NbtConditions {

    private NbtConditions() {
    }

    /**
     * Registers every NBT data-flow condition type.
     */
    public static void register() {
        Components.writer("CreateEmptyNbt", Params.of("fmod.rule.condition.createemptynbt")
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> new CompoundTag());

        Components.writer("DeleteNbtPath", Params.of("fmod.rule.condition.deletenbtpath")
                .param("nbt", ParamKind.NBT)
                .param("path", ParamKind.NBT_PATH)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    try {
                        return RuleNbt.delete(call.tag(0), call.text(1));
                    } catch (IllegalArgumentException exception) {
                        call.fail(Util.parseTranslatableText("fmod.rule.warning.nbtdelete", call.text(1)));
                        return null;
                    }
                });

        Components.writer("AddNbtValue", Params.of("fmod.rule.condition.addnbtvalue")
                .param("nbt", ParamKind.NBT)
                .param("path", ParamKind.NBT_PATH)
                .param("value", ParamKind.NBT)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> write(call, true));

        Components.writer("UpdateNbtValue", Params.of("fmod.rule.condition.updatenbtvalue")
                .param("nbt", ParamKind.NBT)
                .param("path", ParamKind.NBT_PATH)
                .param("value", ParamKind.NBT)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> write(call, false));

        Components.writer("GetNbtValue", Params.of("fmod.rule.condition.getnbtvalue")
                .param("nbt", ParamKind.NBT)
                .param("path", ParamKind.NBT_PATH)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> read(call, call.tag(0)));

        Components.writer("GetEntityNbt", Params.of("fmod.rule.condition.getentitynbt")
                .param("entity", ParamKind.ENTITY)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> RuleGameAccess.entityNbt(call.context(), call.uuid(0)));

        Components.writer("GetEntityNbtValue", Params.of("fmod.rule.condition.getentitynbtvalue")
                .param("entity", ParamKind.ENTITY)
                .param("path", ParamKind.NBT_PATH)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> read(call, RuleGameAccess.entityNbt(call.context(), call.uuid(0))));

        Components.writer("GetMainHandItemComponents", Params.of("fmod.rule.condition.getmainhanditemcomponents")
                .param("entity", ParamKind.ENTITY)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> RuleGameAccess.mainHandComponents(call.context(), call.uuid(0)));

        Components.writer("GetMainHandItemComponentsValue",
                Params.of("fmod.rule.condition.getmainhanditemcomponentsvalue")
                        .param("entity", ParamKind.ENTITY)
                        .param("path", ParamKind.NBT_PATH)
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> read(call,
                        ItemComponents.encode(RuleGameAccess.mainHandComponents(call.context(), call.uuid(0)),
                                call.context().getServer().registryAccess())));

        Components.writer("GetSlotItemComponents", Params.of("fmod.rule.condition.getslotitemcomponents")
                .param("player", ParamKind.PLAYER)
                .param("slot", ParamKind.INT)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerPlayer player = call.context().getServer().getPlayerList()
                            .getPlayer(call.uuid(0));
                    if (player == null) {
                        return null;
                    }
                    ItemStack stack = RuleState.slot(player.getInventory(), call.integer(1));
                    if (stack == null || stack.isEmpty()) {
                        return null;
                    }
                    return ItemComponents.copy(stack.getComponentsPatch(), player.registryAccess());
                });

        Components.writer("GetBlockEntityNbt", Params.of("fmod.rule.condition.getblockentitynbt")
                .param("dimension", ParamKind.DIMENSION)
                .param("position", ParamKind.POSITION)
                .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    ServerLevel world = RuleGameAccess.level(call.context(), call.resourceId(0));
                    Vec3 position = call.vector(1);
                    if (!RuleGameAccess.loaded(world, position)) {
                        return null;
                    }
                    BlockEntity blockEntity = world.getBlockEntity(BlockPos.containing(position));
                    return blockEntity == null ? null : blockEntity.saveWithFullMetadata(world.registryAccess()).copy();
                });
    }

    /**
     * Reads one NBT path, reporting a malformed path before giving up.
     *
     * @param call   the current evaluation
     * @param source the tree to read, possibly {@code null}
     * @return the single matching value, or {@code null} when there is none
     */
    private static Object read(ComponentCall call, Tag source) {
        try {
            return RuleNbt.get(source, call.text(1));
        } catch (IllegalArgumentException exception) {
            call.fail(Util.parseTranslatableText("fmod.rule.warning.nbtget", call.text(1)));
            return null;
        }
    }

    /**
     * Writes one NBT path, reporting a malformed path before giving up.
     *
     * @param call the current evaluation
     * @param add  {@code true} to add a new entry, {@code false} to update an existing one
     * @return the modified copy, or {@code null} when the write did not apply
     */
    private static Object write(ComponentCall call, boolean add) {
        try {
            return RuleNbt.write(call.tag(0), call.text(1), call.tag(2), add);
        } catch (IllegalArgumentException exception) {
            call.fail(Util.parseTranslatableText("fmod.rule.warning.nbtwrite", call.text(1)));
            return null;
        }
    }
}
