/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.ItemComponents;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.DataReference;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

/**
 * A flow node that replaces a live item's component overrides/removals, retaining its item and count.
 * Inputs:
 * 1. ItemStack/DataComponentPatch - Required live nonempty ItemStack, or a component patch bound to an upstream output. GetVariableNode bindings also update the variable.
 * 2. DataComponentPatch - Complete replacement component overrides/removals, copied before writing.
 * Outputs:
 * 1. DataComponentPatch - Detached previous component overrides/removals.
 * Branches: 1 (Next node)
 */
public class ItemComponentsApplyNode extends FlowNode {

    public ItemComponentsApplyNode(long id, String name) {
        super(id, name, 2, 1, 1, "ItemComponentsApplyNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        return NodeMetadata.builder("fmod.node.itemcomponentsapply.title.name", "fmod.node.itemcomponentsapply.title.feat")
                .input("fmod.node.itemcomponentsapply.input.source.name", "fmod.node.itemcomponentsapply.input.source.feat", "fmod.node.itemcomponentsapply.input.source.type")
                .input("fmod.node.itemcomponentsapply.input.value.name", "fmod.node.itemcomponentsapply.input.value.feat", "fmod.node.itemcomponentsapply.input.value.type")
                .output("fmod.node.itemcomponentsapply.output.old.name", "fmod.node.itemcomponentsapply.output.old.feat", "fmod.node.itemcomponentsapply.output.old.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Object source = inputs.get(0);
        if (!(inputs.get(1) instanceof DataComponentPatch replacement)
                || (!(source instanceof ItemStack) && !(source instanceof DataComponentPatch))) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.itemcomponents"), null);
        }
        HolderLookup.Provider registries = context.getServer().registryAccess();
        try {
            DataComponentPatch old;
            if (source instanceof ItemStack stack) {
                old = ItemComponents.copy(stack.getComponentsPatch(), registries);
                ItemComponents.apply(stack, replacement, registries);
            } else {
                DataComponentPatch components = (DataComponentPatch) source;
                old = ItemComponents.copy(components, registries);
                rebind(context, ItemComponents.copy(replacement, registries));
            }
            status.setOutput(0, old);
        } catch (IllegalArgumentException exception) {
            throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.itemcomponents"), null);
        }
    }

    private void rebind(ExecutionContext context, Object value) throws LogicException {
        DataReference reference = getInput(0);
        if (context == null || reference.getType() != DataReference.ReferenceType.NODE_OUTPUT) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.binding"), null);
        }
        NodeStatus upstream = context.getNodeStatus(reference.getReferenceId());
        if (upstream == null || reference.getReferenceIndex() < 0 || reference.getReferenceIndex() >= upstream.getOutputs().size()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.bindingmissing"), null);
        }
        if (upstream.getNode() instanceof GetVariableNode && reference.getReferenceIndex() == 0) {
            String variable = TypeAdaptor.parse(upstream.getInputs().get(0)).asString();
            context.setVariable(variable, value);
        }
        upstream.setOutput(reference.getReferenceIndex(), value);
    }
}
