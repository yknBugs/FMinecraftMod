/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
        super(id, name, 2, 3, 1, "GetBlockNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.getblock.title.name", "fmod.node.getblock.title.feat")
            .input("fmod.node.getblock.input.world.name", "fmod.node.getblock.input.world.feat", "fmod.node.getblock.input.world.type")
            .input("fmod.node.getblock.input.position.name", "fmod.node.getblock.input.position.feat", "fmod.node.getblock.input.position.type")
            .output("fmod.node.getblock.output.identifier.name", "fmod.node.getblock.output.identifier.feat", "fmod.node.getblock.output.identifier.type")
            .output("fmod.node.getblock.output.blockstate.name", "fmod.node.getblock.output.blockstate.feat", "fmod.node.getblock.output.blockstate.type")
            .output("fmod.node.getblock.output.blockentity.name", "fmod.node.getblock.output.blockentity.feat", "fmod.node.getblock.output.blockentity.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        World world = parseWorld(resolvedInputs.get(0));
        Vec3d position = parsePosition(resolvedInputs.get(1));

        BlockPos blockPos = BlockPos.ofFloored(position);
        if (!world.isInBuildLimit(blockPos) || !world.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()), ChunkSectionPos.getSectionCoord(blockPos.getZ())) ) {
            status.setOutput(0, null);
            status.setOutput(1, null);
            status.setOutput(2, null);
            return;
        }

        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        Identifier identifier = Registries.BLOCK.getId(block);
        BlockEntity blockEntity = null;
        if (blockState.hasBlockEntity()) {
            blockEntity = world.getBlockEntity(blockPos);
        }
        status.setOutput(0, identifier);
        status.setOutput(1, blockState);
        status.setOutput(2, blockEntity);
    }

    private World parseWorld(Object obj) throws LogicException {
        if (obj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (obj instanceof World) {
            return (World) obj;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }

    private Vec3d parsePosition(Object obj) throws LogicException {
        if (obj == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(1)), null);
        } else {
            Vec3d position = TypeAdaptor.parse(obj).asVec3d();
            if (position == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(1), this.metadata.inputDataTypes.get(1)), null);
            }
            return position;
        }
    }

}
