/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;
import java.util.List;
import java.util.UUID;

import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.item.ItemEntity;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;
import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.base.util.Util;

/**
 * A flow node that gives items to a player, splitting stacks and dropping inventory overflow.
 * The item is copied first, so the inputs stay untouched. No outputs.
 * Inputs:
 * 1. Player/UUID/String - Required online Player, player name, or UUID object/string.
 * 2. ItemStack/ResourceLocation/String - ItemStack preserves custom NBT and name. Otherwise infer a registered item identifier. Input is never mutated.
 * 3. Number - Null uses ItemStack count or defaults to 1 for an identifier. Explicit nonnegative integer overrides it; zero is a no-op. Vanilla give limit: 100 stacks.
 * Outputs: None
 * Branches: 1 (Next node)
 */
public class GiveItemToPlayerNode extends FlowNode {

    public GiveItemToPlayerNode(long id, String name) {
        super(id, name, 3, 0, 1, "GiveItemToPlayerNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.giveitem.title.name", "fmod.node.giveitem.title.feat");
        for (String port : List.of("player", "item", "count")) {
            String key = "fmod.node.giveitem.input." + port;
            builder.input(key + ".name", key + ".feat", key + ".type");
        }
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat").build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Object target = inputs.get(0);
        UUID uuid = target instanceof Player player ? player.getUUID() : FlowConvert.uuid(target);
        PlayerList players = context.getServer().getPlayerList();
        ServerPlayer player = uuid != null ? players.getPlayer(uuid) : target instanceof String text ? players.getPlayerByName(text.strip()) : null;
        if (player == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.player", target), null);
        }
        ItemStack template = prepare(inputs.get(1), inputs.get(2));
        int remaining = template.getCount();
        while (remaining > 0) {
            int amount = Math.min(remaining, template.getMaxStackSize());
            ItemStack delivery = template.copyWithCount(amount);
            player.getInventory().add(delivery);
            player.containerMenu.broadcastChanges();
            if (!delivery.isEmpty()) {
                ItemEntity dropped = player.drop(delivery, false);
                if (dropped == null) {
                    throw new LogicException(null, Util.parseTranslatableText("fmod.node.giveitem.error.drop"), null);
                }
                dropped.setNoPickUpDelay();
                dropped.setTarget(player.getUUID());
            }
            remaining -= amount;
        }
    }

    /**
     * Detached delivery template. Validation happens before any inventory mutation.
     */
    static ItemStack prepare(Object item, Object number) throws LogicException {
        ItemStack template;
        int defaultCount;
        if (item instanceof ItemStack stack) {
            template = stack.copy();
            defaultCount = stack.getCount();
        } else {
            ResourceLocation id = TypeAdaptor.parse(item).asResourceLocation();
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.identifier", item), null);
            }
            template = new ItemStack(BuiltInRegistries.ITEM.get(id));
            defaultCount = 1;
        }
        Double count = number == null ? Double.valueOf(defaultCount) : TypeAdaptor.parse(number).asDouble();
        if (count == null || !Double.isFinite(count) || count < 0 || count > Integer.MAX_VALUE || count != Math.rint(count)) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.error.count", number), null);
        }
        if (count == 0) {
            return ItemStack.EMPTY;
        }
        if (template.isEmpty()) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.giveitem.error.empty"), null);
        }
        if (count > (long) template.getMaxStackSize() * 100) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.giveitem.error.limit"), null);
        }
        template.setCount(count.intValue());
        return template;
    }
}
