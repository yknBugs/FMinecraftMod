/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import java.util.Set;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.LevelChunk;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that places a block in an already loaded chunk and returns the resulting state.
 * Inputs:
 * 1. ResourceLocation/String/Block/ItemStack - Registered block id/name, Block, or nonempty block ItemStack. Items are not consumed; item NBT is ignored.
 * 2. ServerLevel/ResourceLocation/String - Required ServerLevel or dimension identifier. The destination chunk must already be loaded.
 * 3. Vec3 - Required finite Vec3, floored to block coordinates.
 * 4. BlockState - Optional BlockState matching the requested block. Null uses the default state.
 * Outputs:
 * 1. BlockState - Actual state after placement and neighbor updates, including air; null if the destination chunk is no longer available.
 * 2. BlockEntity - Live block entity after placement, or null when absent or unavailable.
 * Branches: 1 (Next node)
 */
public class PlaceBlockNode extends FlowNode {

    public PlaceBlockNode(long id, String name) {
        super(id, name, 4, 2, 1, "PlaceBlockNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.placeblock.title.name", "fmod.node.placeblock.title.feat");
        for (String port : List.of("block", "world", "position", "state")) {
            String key = "fmod.node.placeblock.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        builder.output("fmod.node.placeblock.output.state.name", "fmod.node.placeblock.output.state.feat", "fmod.node.placeblock.output.state.type")
                .output("fmod.node.placeblock.output.entity.name", "fmod.node.placeblock.output.entity.feat", "fmod.node.placeblock.output.entity.type");
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat").build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        BlockState state = state(inputs.get(0), inputs.get(3));
        Object worldInput = inputs.get(1);
        if (worldInput instanceof Entity entity) {
            worldInput = entity.level();
        }
        ServerLevel world;
        if (worldInput instanceof ServerLevel level) {
            world = level;
        } else {
            ResourceLocation id = TypeAdaptor.parse(worldInput).asResourceLocation();
            world = id == null ? null : context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
        }
        if (world == null || world.getServer() != context.getServer()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.dimension", inputs.get(0)), null);
        }
        BlockPos pos = position(inputs.get(2));
        if (!world.isInWorldBounds(pos)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.bounds", pos), null);
        }
        // getChunkNow is a non-loading lookup. No block reads/writes occur before this guard.
        if (world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.chunk", pos), null);
        }
        Clearable.tryClear(world.getBlockEntity(pos));
        BlockInput placement = new BlockInput(state, Set.copyOf(state.getProperties()), null);
        if (!placement.place(world, pos, 2)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.placeblock.error.place"), null);
        }
        world.blockUpdated(pos, state.getBlock());
        LevelChunk chunk = world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        status.setOutput(0, chunk == null ? null : chunk.getBlockState(pos));
        status.setOutput(1, chunk == null ? null : chunk.getBlockEntity(pos));
    }

    /**
     * Resolves a placeable block and optional matching state without reading or
     * changing the world.
     */
    static BlockState state(Object input, Object optionalState) throws LogicException {
        Block block;
        if (input instanceof ItemStack stack) {
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.placeblock.error.item"), null);
            }
            block = item.getBlock();
        } else if (input instanceof BlockItem item) {
            block = item.getBlock();
        } else {
            ResourceLocation id = TypeAdaptor.parse(input).asResourceLocation();
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.identifier", id), null);
            }
            block = BuiltInRegistries.BLOCK.get(id);
        }
        if (optionalState == null) {
            return block.defaultBlockState();
        }
        if (!(optionalState instanceof BlockState state) || state.getBlock() != block) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.placeblock.error.state"), null);
        }
        return state;
    }

    /**
     * Floors finite coordinates to block coordinates; rejects numeric overflow
     * before int conversion.
     */
    static BlockPos position(Object input) throws LogicException {
        Vec3 pos = TypeAdaptor.parse(input).asVec3d();
        if (pos == null || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)
                || pos.x < Integer.MIN_VALUE || pos.y < Integer.MIN_VALUE || pos.z < Integer.MIN_VALUE
                || pos.x > Integer.MAX_VALUE || pos.y > Integer.MAX_VALUE || pos.z > Integer.MAX_VALUE) 
        {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.placeblock.error.position", pos), null);
        }
        return BlockPos.containing(pos);
    }
}
