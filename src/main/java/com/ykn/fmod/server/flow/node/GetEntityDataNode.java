/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;

/**
 * Node that retrieves data from an entity
 * Inputs:
 * 1. Entity - The source entity.
 * Outputs:
 * 1. Text - The custom display name of the entity, if available.
 * 2. String - The raw custom name of the entity, if available.
 * 3. Identifier - The type identifier of the entity.
 * 4. String - The UUID of the entity.
 * 5. World - The current world/dimension of the entity, if applicable.
 * 6. Vec3d - The current coordinates of the entity, if applicable.
 * 7. Vec3d - The currecnt velocity of the entity, if applicable.
 * 8. Vec2f - The current rotation (pitch and yaw) of the entity, if applicable.
 * 9. Identifier - The dimension identifier of the entity's current world, if applicable.
 * 10. Entity - The vehicle the entity is currently riding, if applicable.
 * 11. List<ItemStack> - The hand contents of the entity, if applicable.
 * 12. List<ItemStack> - The armor contents of the entity, if applicable.
 * Branches: 1 (Next node)
 */
public class GetEntityDataNode extends FlowNode {

    public GetEntityDataNode(long id, String name) {
        super(id, name, 1, 12, 1);
        this.type = "GetEntityDataNode";
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        Component displayName = Util.parseTranslatableText("fmod.node.getentity.title.name");
        Component description = Util.parseTranslatableText("fmod.node.getentity.title.feat");
        List<Component> inputNames = new ArrayList<>();
        List<Component> inputDescriptions = new ArrayList<>();
        List<Component> inputDataTypes = new ArrayList<>();
        inputNames.add(Util.parseTranslatableText("fmod.node.getentity.input.source.name"));
        inputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.input.source.feat"));
        inputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.input.source.type"));
        List<Component> outputNames = new ArrayList<>();
        List<Component> outputDescriptions = new ArrayList<>();
        List<Component> outputDataTypes = new ArrayList<>();
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.customtext.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.customtext.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.customtext.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.customraw.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.customraw.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.customraw.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.typeid.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.typeid.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.typeid.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.uuid.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.uuid.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.uuid.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.world.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.world.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.world.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.position.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.position.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.position.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.velocity.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.velocity.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.velocity.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.rotation.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.rotation.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.rotation.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.dimension.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.dimension.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.dimension.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.vehicle.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.vehicle.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.vehicle.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.hand.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.hand.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.hand.type"));
        outputNames.add(Util.parseTranslatableText("fmod.node.getentity.output.armor.name"));
        outputDescriptions.add(Util.parseTranslatableText("fmod.node.getentity.output.armor.feat"));
        outputDataTypes.add(Util.parseTranslatableText("fmod.node.getentity.output.armor.type"));
        List<Component> branchNames = new ArrayList<>();
        List<Component> branchDescriptions = new ArrayList<>();
        branchNames.add(Util.parseTranslatableText("fmod.node.default.branch.name"));
        branchDescriptions.add(Util.parseTranslatableText("fmod.node.default.branch.feat"));
        return new NodeMetadata(inputNumber, outputNumber, branchNumber, displayName, description,
            inputNames, inputDescriptions, inputDataTypes, outputNames, outputDescriptions, outputDataTypes, branchNames, branchDescriptions);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Entity entity = parseEntity(resolvedInputs.get(0));
        Component customName = entity.getCustomName();
        status.setOutput(0, customName);
        status.setOutput(1, customName == null ? null : customName.getString());
        status.setOutput(2, EntityType.getKey(entity.getType()));
        status.setOutput(3, entity.getStringUUID());
        status.setOutput(4, entity.level());
        status.setOutput(5, entity.position());
        status.setOutput(6, entity.getDeltaMovement());
        status.setOutput(7, new Vec2(entity.getXRot(), entity.getYRot()));
        status.setOutput(8, entity.level() == null ? null : entity.level().dimension().location());
        status.setOutput(9, entity.getVehicle());

        List<ItemStack> handItems = new ArrayList<>();
        for (ItemStack itemStack : entity.getHandSlots()) {
            handItems.add(itemStack);
        }

        if (handItems.isEmpty()) {
            status.setOutput(10, null);
        } else if (handItems.size() == 1) {
            status.setOutput(10, handItems.get(0));
        } else {
            status.setOutput(10, handItems);
        }

        List<ItemStack> armorItems = new ArrayList<>();
        for (ItemStack itemStack : entity.getArmorSlots()) {
            armorItems.add(itemStack);
        }
        
        if (armorItems.isEmpty()) {
            status.setOutput(11, null);
        } else if (armorItems.size() == 1) {
            status.setOutput(11, armorItems.get(0));
        } else {
            status.setOutput(11, armorItems);
        }
    }

    private Entity parseEntity(Object entityObject) throws LogicException {
        if (entityObject == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.inputnull", this.name, this.metadata.inputNames.get(0)), null);
        } else if (entityObject instanceof Entity) {
            return (Entity) entityObject;
        } else {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.classcast", this.name, this.metadata.inputNames.get(0), this.metadata.inputDataTypes.get(0)), null);
        }
    }
}
