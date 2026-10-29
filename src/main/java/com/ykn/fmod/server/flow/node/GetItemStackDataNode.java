/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */
package com.ykn.fmod.server.flow.node;

import java.util.List;
import com.ykn.fmod.server.base.util.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

/**
 * A flow node that reads the identifier, count, display name and a full-stack NBT copy of an item stack.
 * Inputs:
 * 1. ItemStack - Required ItemStack. Empty stacks are valid; null is rejected.
 * Outputs:
 * 1. ResourceLocation - Registry identifier, for example minecraft:diamond_sword.
 * 2. Integer - Number of items as an Integer.
 * 3. Component - Copied hover-name Component, including a custom name if present.
 * 4. CompoundTag - Detached full-stack NBT: id, Count and optional tag. Compatible with NbtApplyNode.
 * Branches: 1 (Next node)
 */
public class GetItemStackDataNode extends FlowNode {
    public GetItemStackDataNode(long id,String name) { super(id,name,1,4,1,"GetItemStackDataNode"); }

    @Override
    protected NodeMetadata createMetadata(int inputs,int outputs,int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.itemstackdata.title.name","fmod.node.itemstackdata.title.feat")
            .input("fmod.node.itemstackdata.input.stack.name","fmod.node.itemstackdata.input.stack.feat","fmod.node.itemstackdata.input.stack.type");
        for(String port:List.of("id","count","displayname","nbt")) {
            String key = "fmod.node.itemstackdata.output."+port;
            builder.output(key+".name",key+".feat",key+".type");
        }
        return builder.branch("fmod.node.default.branch.name","fmod.node.default.branch.feat").build(inputs,outputs,branches);
    }

    @Override
    protected void onExecute(ExecutionContext context,NodeStatus status,List<Object> inputs) throws LogicException {
        if(!(inputs.get(0) instanceof ItemStack stack)) {
            throw new LogicException(null,Util.parseTranslatableText("fmod.node.error.classcast",name, metadata.inputNames().get(0), metadata.inputDataTypes().get(0)),null);
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int count = stack.getCount();
        MutableComponent displayName = stack.getHoverName().copy();
        CompoundTag nbt = stack.save(new CompoundTag()).copy();
        status.setOutput(0,id);
        status.setOutput(1,count);
        status.setOutput(2,displayName);
        status.setOutput(3,nbt);
    }
}
