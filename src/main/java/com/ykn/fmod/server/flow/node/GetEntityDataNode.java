/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

import com.ykn.fmod.server.flow.tool.FlowConvert;

/**
 * A flow node that reads the common data of an entity in one go.
 * Inputs:
 * 1. Entity - The source entity to read data from.
 * Outputs:
 * 1. Text - The custom display name of the entity.
 * 2. String - The raw custom name string.
 * 3. Enum - The identifier of the entity type.
 * 4. String - The UUID of the entity.
 * 5. World - The world the entity is currently in.
 * 6. Vec3d - The current position of the entity.
 * 7. Vec3d - The current velocity of the entity.
 * 8. Vec2f - The pitch and yaw rotation of the entity.
 * 9. Enum - The identifier of the entity's current dimension.
 * 10. Entity - The vehicle the entity is riding.
 * 11. List[Item] - The hand items of the entity.
 * 12. List[Item] - The armor items of the entity.
 * Branches: 1 (Next node)
 */
public class GetEntityDataNode extends FlowNode {

    public GetEntityDataNode(long id, String name) {
        super(id, name, 1, 12, 1, "GetEntityDataNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.getentity.title.name", "fmod.node.getentity.title.feat")
            .input("fmod.node.getentity.input.source.name", "fmod.node.getentity.input.source.feat", "fmod.node.getentity.input.source.type")
            .output("fmod.node.getentity.output.customtext.name", "fmod.node.getentity.output.customtext.feat", "fmod.node.getentity.output.customtext.type")
            .output("fmod.node.getentity.output.customraw.name", "fmod.node.getentity.output.customraw.feat", "fmod.node.getentity.output.customraw.type")
            .output("fmod.node.getentity.output.typeid.name", "fmod.node.getentity.output.typeid.feat", "fmod.node.getentity.output.typeid.type")
            .output("fmod.node.getentity.output.uuid.name", "fmod.node.getentity.output.uuid.feat", "fmod.node.getentity.output.uuid.type")
            .output("fmod.node.getentity.output.world.name", "fmod.node.getentity.output.world.feat", "fmod.node.getentity.output.world.type")
            .output("fmod.node.getentity.output.position.name", "fmod.node.getentity.output.position.feat", "fmod.node.getentity.output.position.type")
            .output("fmod.node.getentity.output.velocity.name", "fmod.node.getentity.output.velocity.feat", "fmod.node.getentity.output.velocity.type")
            .output("fmod.node.getentity.output.rotation.name", "fmod.node.getentity.output.rotation.feat", "fmod.node.getentity.output.rotation.type")
            .output("fmod.node.getentity.output.dimension.name", "fmod.node.getentity.output.dimension.feat", "fmod.node.getentity.output.dimension.type")
            .output("fmod.node.getentity.output.vehicle.name", "fmod.node.getentity.output.vehicle.feat", "fmod.node.getentity.output.vehicle.type")
            .output("fmod.node.getentity.output.hand.name", "fmod.node.getentity.output.hand.feat", "fmod.node.getentity.output.hand.type")
            .output("fmod.node.getentity.output.armor.name", "fmod.node.getentity.output.armor.feat", "fmod.node.getentity.output.armor.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        Entity entity = parseEntity(resolvedInputs.get(0), context);
        Component customName = entity.getCustomName();
        Level world = entity.level();
        status.setOutput(0, customName);
        status.setOutput(1, customName == null ? null : customName.getString());
        status.setOutput(2, EntityType.getKey(entity.getType()));
        status.setOutput(3, entity.getStringUUID());
        status.setOutput(4, world);
        status.setOutput(5, entity.position());
        status.setOutput(6, entity.getDeltaMovement());
        status.setOutput(7, new Vec2(entity.getXRot(), entity.getYRot()));
        status.setOutput(8, world == null ? null : world.dimension().location());
        status.setOutput(9, entity.getVehicle());

        List<ItemStack> handItems = new ArrayList<>();
        if (entity instanceof LivingEntity livingEntity) {
            handItems.add(livingEntity.getItemBySlot(EquipmentSlot.MAINHAND));
            handItems.add(livingEntity.getItemBySlot(EquipmentSlot.OFFHAND));
        }

        status.setOutput(10, FlowConvert.unwrapOutput(handItems.isEmpty() ? null : handItems));

        List<ItemStack> armorItems = new ArrayList<>();
        if (entity instanceof LivingEntity livingEntity) {
            armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.HEAD));
            armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.CHEST));
            armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.LEGS));
            armorItems.add(livingEntity.getItemBySlot(EquipmentSlot.FEET));
        }
        
        status.setOutput(11, FlowConvert.unwrapOutput(armorItems.isEmpty() ? null : armorItems));
    }

    private Entity parseEntity(Object entityObject, ExecutionContext context) throws LogicException {
        return FlowConvert.entity(entityObject, context.getServer());
    }
}
