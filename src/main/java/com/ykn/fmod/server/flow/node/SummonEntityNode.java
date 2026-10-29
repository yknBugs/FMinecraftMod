/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that summons an entity in an already loaded chunk, using vanilla summon behaviour.
 * Inputs:
 * 1. ResourceLocation/String - Required summonable entity identifier; overrides NBT id.
 * 2. ServerLevel/ResourceLocation/String - Required ServerLevel or dimension identifier. Destination must already be loaded.
 * 3. Vec3 - Required finite position in summon bounds; overrides NBT Pos.
 * 4. NBT/SNBT - Optional compound NBT/SNBT, copied before use. Null initializes mobs normally; supplied NBT skips initialization. UUIDs are retained.
 * Outputs:
 * 1. Entity - The live spawned entity, including any supplied passengers.
 * Branches: 1 (Next node)
 */
public class SummonEntityNode extends FlowNode {

    public SummonEntityNode(long id, String name) {
        super(id, name, 4, 1, 1, "SummonEntityNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.summonentity.title.name", "fmod.node.summonentity.title.feat");
        for (String port : List.of("id", "world", "position", "nbt")) {
            String key = "fmod.node.summonentity.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        return builder.output("fmod.node.summonentity.output.entity.name", "fmod.node.summonentity.output.entity.feat", "fmod.node.summonentity.output.entity.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Holder.Reference<EntityType<?>> type = type(inputs.get(0));
        CompoundTag data = data(inputs.get(3));
        Vec3 pos = position(inputs.get(2));
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
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.dimension", inputs.get(1)), null);
        }
        BlockPos blockPos = BlockPos.containing(pos);
        if (world.getChunkSource().getChunkNow(blockPos.getX() >> 4, blockPos.getZ() >> 4) == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.chunk", blockPos), null);
        }
        Entity entity;
        try {
            entity = SummonCommand.createEntity(context.getServer().createCommandSourceStack().withLevel(world), type, pos, data, inputs.get(3) == null);
        } catch (CommandSyntaxException exception) {
            throw new LogicException(exception, Util.parseTranslatableText("fmod.node.summonentity.error.create"), null);
        }
        status.setOutput(0, entity);
    }

    /**
     * Resolves only registered, summonable entity types using the shared identifier adapter.
     */
    static Holder.Reference<EntityType<?>> type(Object input) throws LogicException {
        ResourceLocation id = TypeAdaptor.parse(input).asResourceLocation();
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.identifier", id), null);
        }
        Holder.Reference<EntityType<?>> holder = BuiltInRegistries.ENTITY_TYPE.getHolderOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, id));
        if (!holder.value().canSummon()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.summonentity.error.summon"), null);
        }
        return holder;
    }

    /** 
     * Copies supported compound sources; null supplies an empty compound. 
     */
    static CompoundTag data(Object input) throws LogicException {
        if (input == null) {
            return new CompoundTag();
        }
        if (!(FlowConvert.snapshotNbt(input) instanceof CompoundTag compound)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.summonentity.error.nbt"), null);
        }
        return compound;
    }

    /**
     * Validates finite coordinates and vanilla summon bounds, retaining fractional coordinates.
     */
    static Vec3 position(Object input) throws LogicException {
        Vec3 pos = TypeAdaptor.parse(input).asVec3d();
        if (pos == null || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)
                || pos.x < Integer.MIN_VALUE || pos.y < Integer.MIN_VALUE || pos.z < Integer.MIN_VALUE
                || pos.x > Integer.MAX_VALUE || pos.y > Integer.MAX_VALUE || pos.z > Integer.MAX_VALUE
                || !Level.isInSpawnableBounds(BlockPos.containing(pos))) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.summonentity.error.position", input), null);
        }
        return pos;
    }
}
