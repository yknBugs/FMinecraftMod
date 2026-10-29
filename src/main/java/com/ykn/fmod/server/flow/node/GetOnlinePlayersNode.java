/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import java.util.ArrayList;
import java.util.List;

import com.ykn.fmod.server.flow.logic.ExecutionContext;
import com.ykn.fmod.server.flow.logic.FlowNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;
import com.ykn.fmod.server.flow.logic.NodeStatus;

import net.minecraft.server.level.ServerPlayer;

import com.ykn.fmod.server.base.util.TypeAdaptor;
import com.ykn.fmod.server.flow.logic.LogicException;
import com.ykn.fmod.server.flow.tool.FlowConvert;
import net.minecraft.server.players.PlayerList;

/**
 * A flow node that lists the online players or looks one up by name.
 * Inputs:
 * 1. String - Optional player name. Null returns lists; a name returns a single player/UUID or null.
 * Outputs:
 * 1. ServerPlayer/List - Null name: snapshot list of online players. Named lookup: one player or null. A one-element list is returned as its sole element.
 * 2. UUID/List - UUIDs matching the first output: list when name is null, otherwise one UUID or null. A one-element list is returned as its sole element.
 * 3. Number - Total online player count, regardless of the name filter.
 * Branches: 1 (Next node)
 */
public class GetOnlinePlayersNode extends FlowNode {

    public GetOnlinePlayersNode(long id, String name) {
        super(id, name, 1, 3, 1, "GetOnlinePlayersNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.onlineplayers.title.name", "fmod.node.onlineplayers.title.feat")
            .input("fmod.node.onlineplayers.input.name.name", "fmod.node.onlineplayers.input.name.feat", "fmod.node.onlineplayers.input.name.type")
            .output("fmod.node.onlineplayers.output.0.name", "fmod.node.onlineplayers.output.0.feat", "fmod.node.onlineplayers.output.0.type")
            .output("fmod.node.onlineplayers.output.1.name", "fmod.node.onlineplayers.output.1.feat", "fmod.node.onlineplayers.output.1.type")
            .output("fmod.node.onlineplayers.output.2.name", "fmod.node.onlineplayers.output.2.feat", "fmod.node.onlineplayers.output.2.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }

    @Override
    protected void onExecute(ExecutionContext context, NodeStatus status, List<Object> resolvedInputs) throws LogicException {
        PlayerList playerList = context.getServer().getPlayerList();
        int total = playerList.getPlayerCount();
        if (resolvedInputs.get(0) == null) {
            FlowConvert.checkListSize(total);
            List<ServerPlayer> players = new ArrayList<>(playerList.getPlayers());
            status.setOutput(0, FlowConvert.unwrapOutput(players));
            status.setOutput(1, FlowConvert.unwrapOutput(players.stream().map(ServerPlayer::getUUID).toList()));
        } else {
            ServerPlayer player = playerList.getPlayerByName(TypeAdaptor.parse(resolvedInputs.get(0)).asString());
            status.setOutput(0, player);
            status.setOutput(1, player == null ? null : player.getUUID());
        }
        status.setOutput(2, total);
    }
}
