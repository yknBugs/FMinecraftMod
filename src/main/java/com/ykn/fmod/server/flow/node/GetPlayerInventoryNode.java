/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.ykn.fmod.server.base.util.Util;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.players.PlayerList;
import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

/**
 * A flow node that reads an online player's inventory, equipment and selected slot.
 * Inputs:
 * 1. Player/UUID/String - Required Player, UUID object/string, or online player name (case-insensitive).
 * Outputs:
 * 1. List<ItemStack> (36) - 36 copied stacks: hotbar slots 0–8, then storage slots 9–35. Empty slots are retained. A one-element list is returned as its sole element.
 * 2. List<ItemStack> (2) - Exactly [main,off]. Main is the same copied stack as Inventory[SelectedSlot]. A one-element list is returned as its sole element.
 * 3. List<ItemStack> (4) - Exactly [head,chest,legs,feet], copied from the player. A one-element list is returned as its sole element.
 * 4. Integer - Integer hotbar index 0–8; Inventory[index] is HandItem[0].
 * Branches: 1 (Next node)
 */
public class GetPlayerInventoryNode extends FlowNode {

    public GetPlayerInventoryNode(long id, String name) {
        super(id, name, 1, 4, 1, "GetPlayerInventoryNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputs, int outputs, int branches) {
        NodeMetadata.Builder builder = NodeMetadata.builder("fmod.node.playerinventory.title.name", "fmod.node.playerinventory.title.feat")
                .input("fmod.node.playerinventory.input.player.name", "fmod.node.playerinventory.input.player.feat", "fmod.node.playerinventory.input.player.type");
        for (String port : List.of("inventory", "hand", "armor", "selected")) {
            String key = "fmod.node.playerinventory.output." + port;
            builder.output(key + ".name", key + ".feat", key + ".type");
        }
        return builder.branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat").build(inputs, outputs, branches);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> inputs) throws LogicException {
        Object input = inputs.get(0);
        UUID uuid = input instanceof Player player ? player.getUUID() : FlowConvert.uuid(input);
        PlayerList players = context.getServer().getPlayerList();
        ServerPlayer player = uuid != null ? players.getPlayer(uuid) : input instanceof String text ? players.getPlayerByName(text.strip()) : null;
        if (player == null) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.playerinventory.error.player", String.valueOf(input)), null);
        }
        Snapshot snapshot = snapshot(player.getInventory());
        status.setOutput(0, FlowConvert.unwrapOutput(snapshot.inventory()));
        status.setOutput(1, snapshot.hand());
        status.setOutput(2, FlowConvert.unwrapOutput(snapshot.armor()));
        status.setOutput(3, snapshot.selected());
    }

    record Snapshot(List<ItemStack> inventory, List<ItemStack> hand, List<ItemStack> armor, int selected) {
    }

    /**
     * Captures detached stacks while retaining the selected inventory/main-hand
     * alias in the snapshot.
     */
    static Snapshot snapshot(Inventory source) throws LogicException {
        FlowConvert.checkListSize(36);
        int selected = source.selected;
        if (selected < 0 || selected >= 9) {
            throw new LogicException(null, Util.parseTranslatableText("fmod.node.playerinventory.error.selected", selected), null);
        }
        List<ItemStack> inventory = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) {
            inventory.add(source.getItem(i).copy());
        }
        List<ItemStack> hand = List.of(inventory.get(selected), source.getItem(40).copy());
        List<ItemStack> armor = List.of(source.getItem(39).copy(), source.getItem(38).copy(), source.getItem(37).copy(), source.getItem(36).copy());
        return new Snapshot(List.copyOf(inventory), hand, armor, selected);
    }
}
