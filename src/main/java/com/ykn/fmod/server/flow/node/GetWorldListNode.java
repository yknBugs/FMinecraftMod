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

/**
 * Get all the worlds in the server
 * Inputs:
 * 1. Identifier - The dimension identifier to filter worlds by (optional).
 * Outputs:
 * 1. List - The list of worlds matching the specified dimension, or all worlds if no dimension is specified.
 * Branches: 1 (Next node)
 */
public class GetWorldListNode extends FlowNode {

    public GetWorldListNode(long id, String name) {
        super(id, name, 1, 1, 1, "GetWorldListNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.listworld.title.name", "fmod.node.listworld.title.feat")
            .input("fmod.node.listworld.input.name", "fmod.node.listworld.input.feat", "fmod.node.listworld.input.type")
            .output("fmod.node.listworld.output.name", "fmod.node.listworld.output.feat", "fmod.node.listworld.output.type")
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
                worldList.add(world);
            }
        }

        status.setOutput(0, TypeAdaptor.parse(worldList).collapseList());
    }
    
    private ResourceLocation parseIdentifier(Object obj) throws LogicException {
        if (obj == null) {
            return null;
        } else if (obj instanceof ResourceLocation) {
            return (ResourceLocation) obj;
        } else {
            String str = TypeAdaptor.parse(obj).asString().strip();
            if (str.isEmpty()) {
                return null;
            }
            try {
                return new ResourceLocation(str);
            } catch (Exception e) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
            }
        }
    }
}
