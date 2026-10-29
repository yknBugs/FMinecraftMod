/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;
import java.util.List;
import java.util.HashSet;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.commands.data.EntityDataAccessor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import com.ykn.fmod.server.flow.logic.DataReference;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CollectionTag;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that writes NBT back to a live target and returns its previous NBT.
 * Item component values can be serialized into the replacement NBT.
 * Inputs:
 * 1. Entity/UUID/BlockEntity/Tag/String - Mutable NBT or live Entity/BlockEntity. Immutable SNBT/tags require an upstream output binding; GetVariableNode also updates its variable.
 * 2. Entity/UUID/BlockEntity/Tag/String - Complete replacement NBT from a native source, Tag or SNBT. Never merged; later edits to this value do not affect the source.
 * Outputs:
 * 1. Tag - Detached source NBT snapshot taken before replacement.
 * Branches: 1 (Next node)
 */
public class NbtApplyNode extends FlowNode {
    public NbtApplyNode(long id, String name) {
        super(id, name, 2, 1, 1, "NbtApplyNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        return NodeMetadata.builder("fmod.node.nbtapply.title.name", "fmod.node.nbtapply.title.feat")
                .input("fmod.node.nbtapply.input.source.name", "fmod.node.nbtapply.input.source.feat", "fmod.node.nbtapply.input.source.type")
                .input("fmod.node.nbtapply.input.value.name", "fmod.node.nbtapply.input.value.feat", "fmod.node.nbtapply.input.value.type")
                .output("fmod.node.nbtapply.output.old.name", "fmod.node.nbtapply.output.old.feat", "fmod.node.nbtapply.output.old.type")
                .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
                .build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Object source = FlowConvert.source(inputs.get(0), context == null ? null : context.getServer());
        if (source instanceof DataComponentPatch) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.nbtvalue", "DataComponentPatch"), null);
        }
        Object value = FlowConvert.source(inputs.get(1), context == null ? null : context.getServer());
        HolderLookup.Provider registries = context == null ? null : context.getServer().registryAccess();
        Tag old = FlowConvert.snapshotNbt(source, registries);
        Tag replacement = FlowConvert.snapshotNbt(value, registries);
        if (source instanceof CompoundTag compound && replacement instanceof CompoundTag next) {
            for (String key : new HashSet<>(compound.getAllKeys())) {
                compound.remove(key);
            }
            for (String key : next.getAllKeys()) {
                compound.put(key, next.get(key));
            }
        } else if (source instanceof CollectionTag<?> collection && replacement instanceof CollectionTag<?> next
                && source.getClass() == replacement.getClass()) {
            FlowConvert.checkListSize(next.size());
            collection.clear();
            for (Tag entry : next) {
                collection.addTag(collection.size(), entry);
            }
        } else if (source instanceof Entity entity) {
            if (entity instanceof Player) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.playernbt"), null);
            }
            if (!(replacement instanceof CompoundTag next)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.entitynbt"), null);
            }
            // Load through a fresh instance so omitted serialized fields start at type defaults.
            Entity fresh = entity.getType().create(entity.level());
            if (fresh == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.entitydefaults"), null);
            }
            fresh.load(next);
            CompoundTag normalized = fresh.saveWithoutId(new CompoundTag());
            try {
                entity.setCustomName(null);
                entity.getTags().clear();
                if (entity instanceof LivingEntity living) {
                    living.removeAllEffects();
                }
                new EntityDataAccessor(entity).setData(normalized);
            } catch (CommandSyntaxException failure) {
                try {
                    new EntityDataAccessor(entity).setData((CompoundTag) old);
                } catch (CommandSyntaxException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw new LogicException(failure, Util.parseTranslatableText("fmod.node.nbtapply.error.entitydata"), null);
            }
        } else if (source instanceof BlockEntity block) {
            if (!(replacement instanceof CompoundTag next)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.blocknbt"), null);
            }
            BlockEntity fresh = block.getType().create(block.getBlockPos(), block.getBlockState());
            if (fresh == null || block.getLevel() == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.blocklevel"), null);
            }
            fresh.loadWithComponents(next, block.getLevel().registryAccess());
            block.getLevel().setBlockEntity(fresh);
            fresh.setChanged();
            block.getLevel().sendBlockUpdated(block.getBlockPos(), block.getBlockState(), block.getBlockState(), 3);
            // A live world location is itself a writable destination; output rebinding is optional.
            if (getInput(0).getType() == DataReference.ReferenceType.NODE_OUTPUT) {
                rebind(context, fresh);
            }
        } else {
            rebind(context, source instanceof String ? replacement.toString() : replacement);
        }
        status.setOutput(0, old);
    }

    private void rebind(ExecutionContext context, Object value) throws LogicException {
        DataReference reference = getInput(0);
        if (context == null || reference.getType() != DataReference.ReferenceType.NODE_OUTPUT) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.binding"), null);
        }
        NodeStatus upstream = context.getNodeStatus(reference.getReferenceId());
        if (upstream == null || reference.getReferenceIndex() < 0 || reference.getReferenceIndex() >= upstream.getOutputs().size()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.nbtapply.error.bindingmissing"), null);
        }
        if (upstream.getNode() instanceof GetVariableNode && reference.getReferenceIndex() == 0) {
            String variable = TypeAdaptor.parse(upstream.getInputs().get(0)).asString();
            context.setVariable(variable, value);
        }
        upstream.setOutput(reference.getReferenceIndex(), value);
    }
}
