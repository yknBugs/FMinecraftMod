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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;

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
        Entity entity = parseEntity(resolvedInputs.get(0));
        Text customName = entity.getCustomName();
        World world = entity.getWorld();
        status.setOutput(0, customName);
        status.setOutput(1, customName == null ? null : customName.getString());
        status.setOutput(2, EntityType.getId(entity.getType()));
        status.setOutput(3, entity.getUuidAsString());
        status.setOutput(4, world);
        status.setOutput(5, entity.getPos());
        status.setOutput(6, entity.getVelocity());
        status.setOutput(7, new Vec2f(entity.getPitch(), entity.getYaw()));
        status.setOutput(8, world == null ? null : world.getRegistryKey().getValue());
        status.setOutput(9, entity.getVehicle());

        List<ItemStack> handItems = new ArrayList<>();
        for (ItemStack itemStack : entity.getHandItems()) {
            handItems.add(itemStack);
        }

        status.setOutput(10, TypeAdaptor.parse(handItems).collapseList());

        List<ItemStack> armorItems = new ArrayList<>();
        for (ItemStack itemStack : entity.getArmorItems()) {
            armorItems.add(itemStack);
        }
        
        status.setOutput(11, TypeAdaptor.parse(armorItems).collapseList());
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
