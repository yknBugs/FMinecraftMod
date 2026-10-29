/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.ItemComponents;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.flow.tool.FlowConvert;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * A flow node that edits a detached item component patch through its serialized component paths.
 * Inputs:
 * 1. ItemStack/DataComponentPatch/CompoundTag/String - Item stack, component patch or explicitly encoded compound/SNBT. Null starts an empty patch.
 * 2. String - NBT path into encoded components; quote namespaced keys, e.g. "minecraft:custom_data".flag. Null selects the root.
 * 3. Object - Null deletes; otherwise replace matches or create missing paths. A !component_id key with {} removes an inherited item component.
 * Outputs:
 * 1. DataComponentPatch - Validated, detached component patch; null when deleting the root.
 * 2. Tag/List - Copied old matches: null if missing, one Tag, or a list of Tags. A one-element list is returned as its sole element.
 * 3. Boolean - Whether the selected path/root existed before editing.
 * 4. Number - Number of values changed or removed; zero for an unchanged write or missing deletion.
 * Branches: 1 (Next node)
 */
public class ItemComponentsPatchNode extends FlowNode {

    public ItemComponentsPatchNode(long id, String name) {
        super(id, name, 3, 4, 1, "ItemComponentsPatchNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        return NodeMetadata.builder("fmod.node.itemcomponentspatch.title.name", "fmod.node.itemcomponentspatch.title.feat")
                .input("fmod.node.itemcomponentspatch.input.target.name", "fmod.node.itemcomponentspatch.input.target.feat", "fmod.node.itemcomponentspatch.input.target.type")
                .input("fmod.node.itemcomponentspatch.input.path.name", "fmod.node.itemcomponentspatch.input.path.feat", "fmod.node.itemcomponentspatch.input.path.type")
                .input("fmod.node.itemcomponentspatch.input.value.name", "fmod.node.itemcomponentspatch.input.value.feat", "fmod.node.itemcomponentspatch.input.value.type")
                .output("fmod.node.itemcomponentspatch.output.nbt.name", "fmod.node.itemcomponentspatch.output.nbt.feat", "fmod.node.itemcomponentspatch.output.nbt.type")
                .output("fmod.node.itemcomponentspatch.output.old.name", "fmod.node.itemcomponentspatch.output.old.feat", "fmod.node.itemcomponentspatch.output.old.type")
                .output("fmod.node.itemcomponentspatch.output.existed.name", "fmod.node.itemcomponentspatch.output.existed.feat", "fmod.node.itemcomponentspatch.output.existed.type")
                .output("fmod.node.itemcomponentspatch.output.changed.name", "fmod.node.itemcomponentspatch.output.changed.feat", "fmod.node.itemcomponentspatch.output.changed.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        HolderLookup.Provider registries = context.getServer().registryAccess();
        Object source = inputs.get(0);
        if (source instanceof ItemStack stack) {
            source = stack.getComponentsPatch();
        }
        if (source != null && !(source instanceof DataComponentPatch)
                && !(source instanceof CompoundTag) && !(source instanceof String)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.itemcomponents"), null);
        }
        try {
            String path = inputs.get(1) == null ? null : TypeAdaptor.parse(inputs.get(1)).asString();
            FlowConvert.NbtEdit result = FlowConvert.editNbt(source, path, inputs.get(2), registries);
            if (result.nbt() != null && !(result.nbt() instanceof CompoundTag)) {
                throw new IllegalArgumentException("Item components require a compound root");
            }
            status.setOutput(0, result.nbt() == null ? null : ItemComponents.decode((CompoundTag) result.nbt(), registries));
            status.setOutput(1, FlowConvert.unwrapOutput(result.oldValue()));
            status.setOutput(2, result.existed());
            status.setOutput(3, result.changed());
        } catch (IllegalArgumentException exception) {
            throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.itemcomponents"), null);
        }
    }
}
