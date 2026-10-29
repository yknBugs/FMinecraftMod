/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;

import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that updates the state of an entity; omitted optional inputs keep their current value.
 * No outputs.
 * Inputs:
 * 1. Entity/UUID/String - Required target entity, UUID object or UUID string.
 * 2. ServerLevel/ResourceLocation/String/Entity/UUID - Null leaves dimension unchanged. Accepts ServerLevel, dimension id/string, or Entity/UUID. Native objects automatically supply registry identifiers; dimension ports use an entity’s current world.
 * 3. Vec3/Entity/UUID/String - Null leaves coordinates unchanged. Accepts Vec3 or Entity/UUID position.
 * 4. Vec2/Entity/UUID/String - Null leaves rotation unchanged. Minecraft (pitch,yaw) degrees; pitch [-90,90]. Entity/UUID supplies its rotation.
 * 5. Vec3/Entity/UUID/String - Null leaves velocity unchanged. Accepts Vec3 or Entity/UUID velocity.
 * 6. Component/String - Null leaves name unchanged. Sets a copied custom Component; strings become literal text.
 * 7. Number - Null leaves health unchanged. Living entities only; finite value from zero to maximum health. Zero may kill.
 * 8. ItemStack/List - Null skips. ItemStack sets main hand. List updates [main,off] prefix: null entries clear, absent entries preserve, extras ignored. Stacks are copied.
 * 9. ItemStack/List - Same equipment semantics for [head,chest,legs,feet]. A single ItemStack sets head only.
 * 10. Entity/UUID/String - Null makes no riding request. Entity/UUID must be active in the destination world. Self-riding and cycles are rejected.
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class UpdateEntityNode extends FlowNode {
    private static final EquipmentSlot[] HAND = { EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND };
    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public UpdateEntityNode(long id, String name) {
        super(id, name, 10, 0, 1, "UpdateEntityNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.updateentity.title.name", "fmod.node.updateentity.title.feat");
        for (String port : List.of("entity", "dimension", "position", "rotation", "velocity", "displayname", "health", "hand", "armor", "riding")) {
            String key = "fmod.node.updateentity.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat").build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Entity entity = FlowConvert.entity(inputs.get(0), context.getServer());
        if (entity.isRemoved() || entity.level().getServer() != context.getServer()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.inactive"), null);
        }
        ServerLevel destination = dimension(inputs.get(1), entity, context);
        Vec3 position = inputs.get(2) == null ? entity.position() : vector(inputs.get(2), context, false);
        Vec2 rotation = inputs.get(3) == null ? null : rotation(inputs.get(3), context);
        Vec3 velocity = inputs.get(4) == null ? null : vector(inputs.get(4), context, true);
        Component displayName = inputs.get(5) == null ? 
                null : inputs.get(5) instanceof Component c ? 
                    c.copy() : Component.literal(TypeAdaptor.parse(inputs.get(5)).asString());
        Double health = inputs.get(6) == null ? null : TypeAdaptor.parse(inputs.get(6)).asDouble();
        ItemStack[] hand = equipment(inputs.get(7), HAND.length);
        ItemStack[] armor = equipment(inputs.get(8), ARMOR.length);
        Entity vehicle = inputs.get(9) == null ? null : FlowConvert.entity(inputs.get(9), context.getServer());
        if (!ServerLevel.isInSpawnableBounds(BlockPos.containing(position))) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.bounds", position), null);
        }
        if (inputs.get(6) != null && (health == null || !Double.isFinite(health) || health < 0 || health > Float.MAX_VALUE)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.health", health), null);
        }
        if ((health != null || hand.length > 0 || armor.length > 0) && !(entity instanceof LivingEntity)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.living"), null);
        }
        if (health != null && health > ((LivingEntity) entity).getMaxHealth()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.healthmax", health), null);
        }
        if (vehicle != null) {
            if (vehicle == entity || vehicle.getUUID().equals(entity.getUUID())) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.selfride"), null);
            }
            if (vehicle.isRemoved() || vehicle.level() != destination) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.vehicle"), null);
            }
            for (Entity ancestor = vehicle; ancestor != null; ancestor = ancestor.getVehicle()) {
                if (ancestor == entity) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.cycle"), null);
                }
            }
        }

        // Resolve and copy every input before the first mutation. A dimension transfer can replace
        // a non-player Entity instance, so subsequent writes must target the destination instance.
        boolean transfer = entity.level() != destination;
        if (transfer || inputs.get(2) != null) {
            UUID uuid = entity.getUUID();
            Vec3 preservedVelocity = entity.getDeltaMovement();
            float pitch = rotation == null ? entity.getXRot() : rotation.x;
            float yaw = rotation == null ? entity.getYRot() : rotation.y;
            if (!entity.teleportTo(destination, position.x, position.y, position.z, Set.of(), yaw, pitch)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.teleport"), null);
            }
            if (transfer) {
                entity = destination.getEntity(uuid);
                if (entity == null) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.transfer"), null);
                }
            }
            if (velocity == null) {
                velocity = preservedVelocity;
            }
        }
        if (rotation != null) {
            if (entity instanceof ServerPlayer player) {
                player.connection.teleport(entity.getX(), entity.getY(), entity.getZ(), rotation.y, rotation.x);
            } else {
                entity.setXRot(rotation.x);
                entity.setYRot(rotation.y);
            }
            entity.setYHeadRot(rotation.y);
            entity.setYBodyRot(rotation.y);
        }
        if (velocity != null) {
            entity.setDeltaMovement(velocity);
            entity.hasImpulse = true;
            entity.hurtMarked = true;
        }
        if (displayName != null) {
            entity.setCustomName(displayName);
        }
        if (entity instanceof LivingEntity living) {
            for (int i = 0; i < hand.length; i++) {
                living.setItemSlot(HAND[i], hand[i]);
            }
            for (int i = 0; i < armor.length; i++) {
                living.setItemSlot(ARMOR[i], armor[i]);
            }
        }
        if (vehicle != null && entity.getVehicle() != vehicle && !entity.startRiding(vehicle, true)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.ride"), null);
        }
        // Health zero may kill the entity, so apply it after the other requested changes.
        if (health != null) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.setHealth(health.floatValue());
            }
        }
    }

    /**
     * Copies only the relevant slot prefix; null port skips all slots, null entries clear them.
     */
    static ItemStack[] equipment(Object input, int slots) throws LogicException {
        if (input == null) {
            return new ItemStack[0];
        }
        List<?> values = input instanceof List<?> list ? list : Collections.singletonList(input);
        ItemStack[] result = new ItemStack[Math.min(slots, values.size())];
        for (int i = 0; i < result.length; i++) {
            Object item = values.get(i);
            if (item != null && !(item instanceof ItemStack)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.equipment"), null);
            }
            result[i] = item == null ? ItemStack.EMPTY : ((ItemStack) item).copy();
        }
        return result;
    }

    private static Object nativeInput(Object input, ExecutionContext context) throws LogicException {
        return FlowConvert.uuid(input) == null ? input : FlowConvert.entity(input, context.getServer());
    }

    private static Vec3 vector(Object input, ExecutionContext context, boolean velocity) throws LogicException {
        Object value = nativeInput(input, context);
        Vec3 result = velocity && value instanceof Entity entity ? entity.getDeltaMovement() : TypeAdaptor.parse(value).asVec3d();
        if (result == null || !Double.isFinite(result.x) || !Double.isFinite(result.y) || !Double.isFinite(result.z)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.vec3", input), null);
        }
        return result;
    }

    private static Vec2 rotation(Object input, ExecutionContext context) throws LogicException {
        Object value = nativeInput(input, context);
        Vec2 result = value instanceof Entity entity ? new Vec2(entity.getXRot(), entity.getYRot()) : TypeAdaptor.parse(value).asVec2f();
        if (result == null || !Float.isFinite(result.x) || !Float.isFinite(result.y) || result.x < -90 || result.x > 90) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.rotation", input), null);
        }
        return new Vec2(result.x, Mth.wrapDegrees(result.y));
    }

    private static ServerLevel dimension(Object input, Entity entity, ExecutionContext context) throws LogicException {
        if (input == null) {
            return (ServerLevel) entity.level();
        }
        Object value = nativeInput(input, context);
        if (value instanceof Entity other) {
            value = other.level();
        }
        if (value instanceof BlockEntity block) {
            if (block.getLevel() == null) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.blockdimension"), null);
            }
            value = block.getLevel();
        }
        if (value instanceof ServerLevel level) {
            if (level.getServer() != context.getServer()) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.updateentity.error.server"), null);
            }
            return level;
        }
        ResourceLocation id = TypeAdaptor.parse(value).asResourceLocation();
        ServerLevel level = id == null ? null : context.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.dimension", input), null);
        }
        return level;
    }
}
