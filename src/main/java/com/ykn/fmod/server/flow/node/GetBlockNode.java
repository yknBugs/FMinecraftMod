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

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Get a block at a specified position
 * Inputs:
 * 1. World - The world to get the block from.
 * 2. Vec3d - The position of the block to get.
 * Outputs:
 * 1. Identifier - The identifier of the block at the specified position.
 * 2. BlockState - The block state of the block at the specified position.
 * 3. BlockEntity - The block entity of the block at the specified position, if any.
 */
public class GetBlockNode extends FlowNode {

    public GetBlockNode(long id, String name) {
        super(id, name, 2, 3, 1);
        this.type = "GetBlockNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.getblock.title.name");
        Component description = Util.parseTranslatableText("fmod.node.getblock.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.getblock.input.world.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getblock.input.world.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getblock.input.world.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getblock.input.position.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getblock.input.position.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getblock.input.position.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.getblock.output.identifier.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getblock.output.identifier.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getblock.output.identifier.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getblock.output.blockstate.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getblock.output.blockstate.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getblock.output.blockstate.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getblock.output.blockentity.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getblock.output.blockentity.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getblock.output.blockentity.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description, 
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Level world = parseWorld(resolvedInputs.get(0));
        Vec3 position = parsePosition(resolvedInputs.get(1));
        int x = (int) Math.round(position.x);
        int y = (int) Math.round(position.y);
        int z = (int) Math.round(position.z);

        BlockPos blockPos = new BlockPos(x, y, z);
        if (world.isInWorldBounds(blockPos) == false || world.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)) == false) {
            status.setOutput(0, null);
            status.setOutput(1, null);
            status.setOutput(2, null);
            return;
        }

        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        ResourceLocation identifier = ForgeRegistries.BLOCKS.getKey(block);
        BlockEntity blockEntity = null;
        if (blockState.hasBlockEntity()) {
            blockEntity = world.getBlockEntity(blockPos);
        }
        status.setOutput(0, identifier);
        status.setOutput(1, blockState);
        status.setOutput(2, blockEntity);
    }

    private Level parseWorld(Object obj) throws LogicException {
        if (obj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (obj instanceof Level) {
            return (Level) obj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }

    private Vec3 parsePosition(Object obj) throws LogicException {
        if (obj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(1)), null);
        } else {
            Vec3 position = TypeAdaptor.parse(obj).asVec3d();
            if (position == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
            }
            return position;
        }
    }

}
