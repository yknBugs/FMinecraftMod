/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that builds an item stack from an identifier, a count, an optional name and NBT.
 * Inputs:
 * 1. ResourceLocation/String/ItemStack/Item/Block/Entity - Required registered item identifier, as ResourceLocation or string. Native objects automatically supply registry identifiers; dimension ports use an entity’s current world.
 * 2. Number - Default 1. Nonnegative whole number up to Integer.MAX_VALUE; zero makes an empty stack. Oversized stacks are allowed.
 * 3. Component/String - Optional Component/string; overrides the NBT name. Null preserves the NBT name or default item name.
 * 4. CompoundTag/String - Optional copied custom compound or full stack NBT/SNBT (id and Count). Explicit id/count inputs take precedence.
 * Outputs:
 * 1. ItemStack - New detached ItemStack. Later input NBT changes do not affect it.
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
        CompoundTag custom = null;
        if (inputs.get(3) != null) {
            Tag parsed = FlowConvert.snapshotNbt(inputs.get(3));
            if (!(parsed instanceof CompoundTag compound)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.makeitem.error.nbt", inputs.get(3)), null);
            }
            if (compound.contains("id", Tag.TAG_STRING) && compound.contains("Count", Tag.TAG_ANY_NUMERIC)) {
                if (compound.contains("tag") && !(compound.get("tag") instanceof CompoundTag)) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.makeitem.error.tag"), null);
                }
                custom = compound.contains("tag") ? compound.getCompound("tag") : null;
            } else {
                custom = compound;
            }
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id), count.intValue());
        if (custom != null) {
            stack.setTag(custom);
        }
        if (inputs.get(2) != null) {
            Component displayName = inputs.get(2) instanceof Component component ? 
                component.copy() : Component.literal(TypeAdaptor.parse(inputs.get(2)).asString());
            stack.setHoverName(displayName);
        }
        status.setOutput(0, stack);
    }
}
