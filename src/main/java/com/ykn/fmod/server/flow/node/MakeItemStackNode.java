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
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A flow node that builds an item stack from an identifier, a count, an optional name and item components.
 * Inputs:
 * 1. ResourceLocation/String/ItemStack/Item/Block/Entity - Required registered item identifier, as ResourceLocation or string. Native objects automatically supply registry identifiers; dimension ports use an entity’s current world.
 * 2. Number - Default 1. Nonnegative whole number up to Integer.MAX_VALUE; zero makes an empty stack. Oversized stacks are allowed.
 * 3. Component/String - Optional Component/string; overrides the custom-name component. Null preserves the existing name or the default item name.
 * 4. DataComponentPatch - Optional component overrides and removals. Explicit id/count inputs take precedence.
 * Outputs:
 * 1. ItemStack - New detached ItemStack. Later input component changes do not affect it.
 * Branches: 1 (Next node)
 */
public class MakeItemStackNode extends FlowNode {

    public MakeItemStackNode(long id, String name) {
        super(id, name, 4, 1, 1, "MakeItemStackNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.makeitem.title.name", "fmod.node.makeitem.title.feat");
        for (String port : List.of("id", "count", "displayname", "nbt")) {
            String key = "fmod.node.makeitem.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        return builder.output("fmod.node.makeitem.output.stack.name", "fmod.node.makeitem.output.stack.feat", "fmod.node.makeitem.output.stack.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        ResourceLocation id = TypeAdaptor.parse(inputs.get(0)).asResourceLocation();
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.identifier", id), null);
        }
        Double count = inputs.get(1) == null ? Double.valueOf(1.0) : TypeAdaptor.parse(inputs.get(1)).asDouble();
        if (count == null || !Double.isFinite(count) || count < 0 || count > Integer.MAX_VALUE || count != Math.rint(count)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.count", count), null);
        }
        if (inputs.get(3) != null && !(inputs.get(3) instanceof DataComponentPatch)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.itemcomponents"), null);
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), count.intValue());
        if (inputs.get(3) instanceof DataComponentPatch components && !stack.isEmpty()) {
            try {
                ItemComponents.apply(stack, components, context.getServer().registryAccess());
            } catch (IllegalArgumentException exception) {
                throw new LogicException(exception, Util.parseTranslatableText("fmod.node.error.itemcomponents"), null);
            }
        }
        if (inputs.get(2) != null) {
            Component displayName = inputs.get(2) instanceof Component component ? 
                component.copy() : Component.literal(TypeAdaptor.parse(inputs.get(2)).asString());
            stack.set(DataComponents.CUSTOM_NAME, displayName);
        }
        status.setOutput(0, stack);
    }
}
