/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Node that retrieves a value from an NBT compound
 * Inputs:
 * 1. Block/Entity/ItemStack/NbtCompound - The source to retrieve from.
 * 2. String - The NBT path to the desired value.
 * 3. Double - The scale factor to apply to numeric values (null means 1.0).
 * Outputs:
 * 1. NbtCompound/Primitive - The retrieved value, or null if not found.
 * Branches: 1 (Next node)
 */
public class GetNbtValueNode extends FlowNode {

    public GetNbtValueNode(long id, String name) {
        super(id, name, 3, 1, 1);
        this.type = "GetNbtValueNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.getnbt.title.name");
        Component description = Util.parseTranslatableText("fmod.node.getnbt.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.getnbt.input.source.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getnbt.input.source.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getnbt.input.source.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getnbt.input.path.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getnbt.input.path.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getnbt.input.path.type"));
        inputNames.add(Util.parseTranslatableText("fmod.node.getnbt.input.scale.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getnbt.input.scale.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getnbt.input.scale.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.getnbt.output.value.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getnbt.output.value.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getnbt.output.value.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description,
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Tag sourceNbt = parseSource(resolvedInputs.get(0));
        NbtPathArgument.NbtPath nbtPath = parsePath(resolvedInputs.get(1));
        Double scale = TypeAdaptor.parse(resolvedInputs.get(2)).asDouble();

        if (sourceNbt == null) {
            status.setOutput(0, null);
            return;
        }
        double scaleValue = scale == null ? 1.0 : scale;

        try {
            if (NbtPathArgument.NbtPath.isTooDeep(sourceNbt, 0)) {
                throw NbtPathArgument.ERROR_DATA_TOO_DEEP.create();
            }

            List<Tag> collection = nbtPath.get(sourceNbt);
            Object result = convertCollection(collection, scaleValue);
            status.setOutput(0, result);
        } catch (CommandSyntaxException e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.pathsyntax", resolvedInputs.get(1)), e.getMessage());
        } catch (Exception e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.parse", resolvedInputs.get(1)), e.getMessage());
        }
    }

    private Tag parseSource(Object input) throws LogicException {
        if (input == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (input instanceof Entity) {
            Entity entity = (Entity) input;
            EntityDataAccessor entityData = new EntityDataAccessor(entity);
            return entityData.getData();
        } else if (input instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) input;
            CompoundTag itemNbt = itemStack.getTag();
            return itemNbt;
        } else if (input instanceof BlockEntity) {
            BlockEntity blockEntity = (BlockEntity) input;
            BlockDataAccessor blockData = new BlockDataAccessor(blockEntity, blockEntity.getBlockPos());
            return blockData.getData();
        } else if (input instanceof Tag) {
            Tag nbtCompound = (Tag) input;
            return nbtCompound;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }

    private NbtPathArgument.NbtPath parsePath(Object path) throws LogicException {
        String inputPath = TypeAdaptor.parse(path).asString();
        try {
            NbtPathArgument.NbtPath nbtPath = NbtPathArgument.nbtPath().parse(new StringReader(inputPath));
            return nbtPath;
        } catch (CommandSyntaxException e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.pathsyntax", inputPath), e.getMessage());
        } catch (Exception e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.parse", inputPath), e.getMessage());
        }
    }

    private Object convertCollection(List<Tag> collection, double scale) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        if (collection.size() == 1) {
            return convertElement(collection.get(0), scale);
        }
        List<Object> results = new ArrayList<>();
        for (Tag element : collection) {
            results.add(convertElement(element, scale));
        }
        return results;
    }

    private Object convertElement(Tag element, double scale) {
        if (element == null) {
            return null;
        }

        if (element instanceof ByteTag) {
            ByteTag nbtByte = (ByteTag) element;
            return (double) nbtByte.getAsByte() * scale;
        }

        if (element instanceof ShortTag) {
            ShortTag nbtShort = (ShortTag) element;
            return (double) nbtShort.getAsShort() * scale;
        }

        if (element instanceof IntTag) {
            IntTag nbtInt = (IntTag) element;
            return (double) nbtInt.getAsInt() * scale;
        }

        if (element instanceof LongTag) {
            LongTag nbtLong = (LongTag) element;
            return (double) nbtLong.getAsLong() * scale;
        }

        if (element instanceof FloatTag) {
            FloatTag nbtFloat = (FloatTag) element;
            return (double) nbtFloat.getAsFloat() * scale;
        }

        if (element instanceof DoubleTag) {
            DoubleTag nbtDouble = (DoubleTag) element;
            return nbtDouble.getAsDouble() * scale;
        }

        if (element instanceof ByteArrayTag) {
            ByteArrayTag nbtByteArray = (ByteArrayTag) element;
            byte[] array = nbtByteArray.getAsByteArray();
            List<Double> converted = new ArrayList<>();
            for (byte b : array) {
                converted.add((double) b * scale);
            }
            return converted;
        }

        if (element instanceof StringTag) {
            StringTag nbtString = (StringTag) element;
            return nbtString.getAsString();
        }

        if (element instanceof ListTag) {
            return element;
        }

        if (element instanceof CompoundTag) {
            return element;
        }

        if (element instanceof IntArrayTag) {
            IntArrayTag nbtIntArray = (IntArrayTag) element;
            int[] array = nbtIntArray.getAsIntArray();
            List<Double> converted = new ArrayList<>();
            for (int i : array) {
                converted.add((double) i * scale);
            }
            return converted;
        }

        if (element instanceof LongArrayTag) {
            LongArrayTag nbtLongArray = (LongArrayTag) element;
            long[] array = nbtLongArray.getAsLongArray();
            List<Double> converted = new ArrayList<>();
            for (long l : array) {
                converted.add((double) l * scale);
            }
            return converted;
        }

        return element;
    }
}
