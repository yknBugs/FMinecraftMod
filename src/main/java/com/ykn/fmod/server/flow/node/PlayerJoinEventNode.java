/**
 * Copyright (c) ykn
 * This file is under the MIT License
 */

package com.ykn.fmod.server.flow.node;

import com.ykn.fmod.server.flow.logic.EventNode;
import com.ykn.fmod.server.flow.logic.NodeMetadata;

/**
 * A flow node that triggers when a player joins the world.
 * Inputs: None
 * Outputs:
 * 1. Player - The player that joined.
 * 2. Vec3d - The position of the player.
 * Branches: 1 (Next node)
 */
public class PlayerJoinEventNode extends EventNode {

    public PlayerJoinEventNode(long id, String name) {
        super(id, name, 0, 2, 1, "PlayerJoinEventNode");
    }

    @Override
    protected NodeMetadata createMetadata(int inputNumber, int outputNumber, int branchNumber) {
        return NodeMetadata.builder("fmod.node.joinevt.title.name", "fmod.node.joinevt.title.feat")
            .output("fmod.node.joinevt.output.player.name", "fmod.node.joinevt.output.player.feat", "fmod.node.joinevt.output.player.type")
            .output("fmod.node.joinevt.output.position.name", "fmod.node.joinevt.output.position.feat", "fmod.node.joinevt.output.position.type")
            .branch("fmod.node.default.branch.name", "fmod.node.default.branch.feat")
            .build(inputNumber, outputNumber, branchNumber);
    }
}
