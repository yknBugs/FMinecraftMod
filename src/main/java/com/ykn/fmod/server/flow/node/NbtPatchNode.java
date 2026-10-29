/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowConvert;

import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.core.component.DataComponentPatch;

/**
 * A flow node that edits a detached NBT copy and returns it; the target is never changed.
 * Inputs:
 * 1. Entity/UUID/BlockEntity/Tag/String/Map/List - Entity, BlockEntity, NBT or SNBT. Null starts an empty compound. UUID objects/canonical UUID strings resolve as entity references first.
 * 2. String - Vanilla NBT path; null selects the entire tree. Supports multiple matches.
 * 3. Object - Null deletes; otherwise replace matches or create missing paths. Accepts NBT, SNBT, numbers, booleans, maps and homogeneous lists.
 * Outputs:
 * 1. Tag - Detached modified tree; null after deleting the root.
 * 2. Tag/List - Copied old matches: null if missing, one Tag, or a list of Tags. A one-element list is returned as its sole element.
 * 3. Boolean - Whether the selected path/root existed before editing.
 * 4. Number - Number of values changed or removed; zero for an unchanged write or missing deletion.
 * Branches: 1 (Next node)
 */
public class NbtPatchNode extends FlowNode {

    public NbtPatchNode(long id, String name) {
        super(id, name, 3, 4, 1, "NbtPatchNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        return NodeMetadata.builder("fmod.node.nbtpatch.title.name", "fmod.node.nbtpatch.title.feat")
                .input("fmod.node.nbtpatch.input.target.name", "fmod.node.nbtpatch.input.target.feat", "fmod.node.nbtpatch.input.target.type")
                .input("fmod.node.nbtpatch.input.path.name", "fmod.node.nbtpatch.input.path.feat", "fmod.node.nbtpatch.input.path.type")
                .input("fmod.node.nbtpatch.input.value.name", "fmod.node.nbtpatch.input.value.feat", "fmod.node.nbtpatch.input.value.type")
                .output("fmod.node.nbtpatch.output.nbt.name", "fmod.node.nbtpatch.output.nbt.feat", "fmod.node.nbtpatch.output.nbt.type")
                .output("fmod.node.nbtpatch.output.old.name", "fmod.node.nbtpatch.output.old.feat", "fmod.node.nbtpatch.output.old.type")
                .output("fmod.node.nbtpatch.output.existed.name", "fmod.node.nbtpatch.output.existed.feat", "fmod.node.nbtpatch.output.existed.type")
                .output("fmod.node.nbtpatch.output.changed.name", "fmod.node.nbtpatch.output.changed.feat", "fmod.node.nbtpatch.output.changed.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        if (inputs.get(0) instanceof DataComponentPatch) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtvalue", "DataComponentPatch"), null);
        }
        FlowConvert.NbtEdit result = FlowConvert.editNbt(
            FlowConvert.source(inputs.get(0), context == null ? null : context.getServer()),
            inputs.get(1) == null ? null : TypeAdaptor.parse(inputs.get(1)).asString(), inputs.get(2),
            context.getServer().registryAccess()
        );
        status.setOutput(0, result.nbt());
        status.setOutput(1, FlowConvert.unwrapOutput(result.oldValue()));
        status.setOutput(2, result.existed());
        status.setOutput(3, result.changed());
    }
}
