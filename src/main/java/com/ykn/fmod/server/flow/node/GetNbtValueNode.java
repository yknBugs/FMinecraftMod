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

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.BlockDataObject;
import net.minecraft.command.EntityDataObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;

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
        return NodeMetadata.builder("fmod.node.getnbt.title.name", "fmod.node.getnbt.title.feat")
            .input("fmod.node.getnbt.input.source.name", "fmod.node.getnbt.input.source.feat", "fmod.node.getnbt.input.source.type")
            .input("fmod.node.getnbt.input.path.name", "fmod.node.getnbt.input.path.feat", "fmod.node.getnbt.input.path.type")
            .input("fmod.node.getnbt.input.scale.name", "fmod.node.getnbt.input.scale.feat", "fmod.node.getnbt.input.scale.type")
            .output("fmod.node.getnbt.output.value.name", "fmod.node.getnbt.output.value.feat", "fmod.node.getnbt.output.value.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        NbtElement sourceNbt = parseSource(resolvedInputs.get(0));
        NbtPathArgumentType.NbtPath nbtPath = parsePath(resolvedInputs.get(1));
        Double scale = TypeAdaptor.parse(resolvedInputs.get(2)).asDouble();

        if (sourceNbt == null) {
            status.setOutput(0, null);
            return;
        }
        double scaleValue = scale == null ? 1.0 : scale;

        try {
            if (NbtPathArgumentType.NbtPath.isTooDeep(sourceNbt, 0)) {
                throw NbtPathArgumentType.TOO_DEEP_EXCEPTION.create();
            }

            List<NbtElement> collection = nbtPath.get(sourceNbt);
            Object result = convertCollection(collection, scaleValue);
            status.setOutput(0, result);
        } catch (CommandSyntaxException e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.pathsyntax", resolvedInputs.get(1)), e.getMessage());
        } catch (Exception e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.parse", resolvedInputs.get(1)), e.getMessage());
        }
    }

    private NbtElement parseSource(Object input) throws LogicException {
        if (input == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (input instanceof Entity) {
            Entity entity = (Entity) input;
            EntityDataObject entityData = new EntityDataObject(entity);
            return entityData.getNbt();
        } else if (input instanceof ItemStack) {
            ItemStack itemStack = (ItemStack) input;
            NbtCompound itemNbt = itemStack.getNbt();
            return itemNbt;
        } else if (input instanceof BlockEntity) {
            BlockEntity blockEntity = (BlockEntity) input;
            BlockDataObject blockData = new BlockDataObject(blockEntity, blockEntity.getPos());
            return blockData.getNbt();
        } else if (input instanceof NbtElement) {
            NbtElement nbtCompound = (NbtElement) input;
            return nbtCompound;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }

    private NbtPathArgumentType.NbtPath parsePath(Object path) throws LogicException {
        String inputPath = TypeAdaptor.parse(path).asString();
        try {
            NbtPathArgumentType.NbtPath nbtPath = NbtPathArgumentType.nbtPath().parse(new StringReader(inputPath));
            return nbtPath;
        } catch (CommandSyntaxException e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.pathsyntax", inputPath), e.getMessage());
        } catch (Exception e) {
            throw new LogicException(e, Util.parseTranslatableText("fmod.node.getnbt.error.parse", inputPath), e.getMessage());
        }
    }

    private Object convertCollection(List<NbtElement> collection, double scale) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        if (collection.size() == 1) {
            return convertElement(collection.get(0), scale);
        }
        List<Object> results = new ArrayList<>();
        for (NbtElement element : collection) {
            results.add(convertElement(element, scale));
        }
        return results;
    }

    private Object convertElement(NbtElement element, double scale) {
        if (element == null) {
            return null;
        }

        if (element instanceof NbtByte) {
            NbtByte nbtByte = (NbtByte) element;
            return (double) nbtByte.byteValue() * scale;
        }

        if (element instanceof NbtShort) {
            NbtShort nbtShort = (NbtShort) element;
            return (double) nbtShort.shortValue() * scale;
        }

        if (element instanceof NbtInt) {
            NbtInt nbtInt = (NbtInt) element;
            return (double) nbtInt.intValue() * scale;
        }

        if (element instanceof NbtLong) {
            NbtLong nbtLong = (NbtLong) element;
            return (double) nbtLong.longValue() * scale;
        }

        if (element instanceof NbtFloat) {
            NbtFloat nbtFloat = (NbtFloat) element;
            return (double) nbtFloat.floatValue() * scale;
        }

        if (element instanceof NbtDouble) {
            NbtDouble nbtDouble = (NbtDouble) element;
            return nbtDouble.doubleValue() * scale;
        }

        if (element instanceof NbtByteArray) {
            NbtByteArray nbtByteArray = (NbtByteArray) element;
            byte[] array = nbtByteArray.getByteArray();
            List<Double> converted = new ArrayList<>();
            for (byte b : array) {
                converted.add((double) b * scale);
            }
            return converted;
        }

        if (element instanceof NbtString) {
            NbtString nbtString = (NbtString) element;
            return nbtString.asString();
        }

        if (element instanceof NbtList) {
            return element;
        }

        if (element instanceof NbtCompound) {
            return element;
        }

        if (element instanceof NbtIntArray) {
            NbtIntArray nbtIntArray = (NbtIntArray) element;
            int[] array = nbtIntArray.getIntArray();
            List<Double> converted = new ArrayList<>();
            for (int i : array) {
                converted.add((double) i * scale);
            }
            return converted;
        }

        if (element instanceof NbtLongArray) {
            NbtLongArray nbtLongArray = (NbtLongArray) element;
            long[] array = nbtLongArray.getLongArray();
            List<Double> converted = new ArrayList<>();
            for (long l : array) {
                converted.add((double) l * scale);
            }
            return converted;
        }

        return element;
    }
}
