/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A flow node that lists every world of the server, optionally filtered by dimension.
 * Inputs:
 * 1. Enum - The dimension identifier to filter worlds by.
 * Outputs:
 * 1. List[World] - The list of worlds matching the specified dimension.
 * 2. Number - Number of worlds matching the dimension filter.
 * Branches: 1 (Next node)
 */
public class GetWorldListNode extends FlowNode {

    public GetWorldListNode(long id, String name) {
        super(id, name, 1, 2, 1, "GetWorldListNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.listworld.title.name", "fmod.node.listworld.title.feat")
            .input("fmod.node.listworld.input.name", "fmod.node.listworld.input.feat", "fmod.node.listworld.input.type")
            .output("fmod.node.listworld.output.name", "fmod.node.listworld.output.feat", "fmod.node.listworld.output.type")
            .output("fmod.node.listworld.output.count.name", "fmod.node.listworld.output.count.feat", "fmod.node.listworld.output.count.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        ResourceLocation dimensionFilter = parseIdentifier(resolvedInputs.get(0));
        
        Iterable<ServerLevel> worlds = context.getServer().getAllLevels();
        List<ServerLevel> worldList = new ArrayList<>();
        
        for (ServerLevel world : worlds) {
            if (dimensionFilter == null || world.dimension().location().equals(dimensionFilter)) {
                FlowConvert.checkListSize((long) worldList.size() + 1);
                worldList.add(world);
            }
        }

        status.setOutput(0, FlowConvert.unwrapOutput(worldList.isEmpty() ? null : worldList));
        status.setOutput(1, worldList.size());
    }
    
    private ResourceLocation parseIdentifier(Object obj) throws LogicException {
        if (obj == null || obj instanceof String text && text.isBlank()) {
            return null;
        }
        // Dimension ports infer an entity's current world rather than its entity type.
        if (obj instanceof Entity entity) {
            obj = entity.level();
        }
        if (obj instanceof BlockEntity block) {
            if (block.getLevel() == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.id", name, "detached BlockEntity"), null);
            }
            obj = block.getLevel();
        }
        ResourceLocation id = TypeAdaptor.parse(obj).asResourceLocation();
        if (id == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.id", name, String.valueOf(obj)), null);
        }
        return id;
    }
}
