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
import com.ykn.fmod.server.rule.tool.RuleNbt;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Copy-on-write item component operations using quoted, namespaced NBT paths. */
public final class ItemComponentsConditions {

    private ItemComponentsConditions() {
    }

    public static void register() {
        Components.writer("CreateEmptyItemComponents",
                Params.of("fmod.rule.condition.createemptyitemcomponents")
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> DataComponentPatch.EMPTY);

        Components.predicate("CheckItemComponentsValue",
                Params.of("fmod.rule.condition.checkitemcomponentsvalue")
                        .param("components", ParamKind.ITEM_COMPONENTS)
                        .param("path", ParamKind.NBT_PATH)
                        .param("expected", ParamKind.NBT),
                (call) -> {
                    try {
                        return RuleNbt.check(source(call), call.text(1), call.tag(2));
                    } catch (IllegalArgumentException exception) {
                        call.warn(Util.parseTranslatableText("fmod.rule.warning.itemcomponents"));
                        return false;
                    }
                });

        Components.writer("DeleteItemComponentsPath",
                Params.of("fmod.rule.condition.deleteitemcomponentspath")
                        .param("components", ParamKind.ITEM_COMPONENTS)
                        .param("path", ParamKind.NBT_PATH)
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> edit(call, 0));

        Components.writer("AddItemComponentsValue",
                Params.of("fmod.rule.condition.additemcomponentsvalue")
                        .param("components", ParamKind.ITEM_COMPONENTS)
                        .param("path", ParamKind.NBT_PATH)
                        .param("value", ParamKind.NBT)
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> edit(call, 1));

        Components.writer("UpdateItemComponentsValue",
                Params.of("fmod.rule.condition.updateitemcomponentsvalue")
                        .param("components", ParamKind.ITEM_COMPONENTS)
                        .param("path", ParamKind.NBT_PATH)
                        .param("value", ParamKind.NBT)
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> edit(call, 2));

        Components.writer("GetItemComponentsValue",
                Params.of("fmod.rule.condition.getitemcomponentsvalue")
                        .param("components", ParamKind.ITEM_COMPONENTS)
                        .param("path", ParamKind.NBT_PATH)
                        .output("variable", ParamKind.VARIABLE_NAME),
                (call) -> {
                    try {
                        return RuleNbt.get(source(call), call.text(1));
                    } catch (IllegalArgumentException exception) {
                        call.fail(Util.parseTranslatableText("fmod.rule.warning.itemcomponents"));
                        return null;
                    }
                });
    }

    private static CompoundTag source(ComponentCall call) {
        return ItemComponents.encode(call.components(0), call.context().getServer().registryAccess());
    }

    private static Object edit(ComponentCall call, int operation) {
        try {
            Tag changed;
            if (operation == 0) {
                changed = RuleNbt.delete(source(call), call.text(1));
            } else {
                changed = RuleNbt.write(source(call), call.text(1), call.tag(2), operation == 1);
            }
            if (changed == null) {
                return null;
            }
            return ItemComponents.decode((CompoundTag) changed, call.context().getServer().registryAccess());
        } catch (IllegalArgumentException exception) {
            call.fail(Util.parseTranslatableText("fmod.rule.warning.itemcomponents"));
            return null;
        }
    }
}
