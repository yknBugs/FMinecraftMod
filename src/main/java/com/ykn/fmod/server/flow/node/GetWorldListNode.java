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

import net.minecraft.network.chat.Component;
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
        super(id, name, 1, 1, 1);
        this.type = "GetWorldListNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.listworld.title.name");
        Component description = Util.parseTranslatableText("fmod.node.listworld.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.listworld.input.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.listworld.input.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.listworld.input.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.listworld.output.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.listworld.output.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.listworld.output.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
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

        if (worldList.isEmpty()) {
            status.setOutput(0, null);
        } else if (worldList.size() == 1) {
            status.setOutput(0, worldList.get(0));
        } else {
            status.setOutput(0, worldList);
        }
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
